package com.kingdomsgate.cult.gametest;

import com.kingdomsgate.cult.CultMod;
import com.kingdomsgate.cult.CultRegistry;
import com.kingdomsgate.cult.building.KingdomsGateBuilding;
import com.kingdomsgate.cult.domain.CultRules;
import com.kingdomsgate.cult.runtime.CultSavedData;
import com.kingdomsgate.cult.runtime.CultService;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native request fulfillment and stationary recruitment with scarce ordinary books. */
@GameTestHolder(CultMod.ID)
@PrefixGameTestTemplate(false)
public final class CultBookGameTests {
    @GameTest(template = "empty", timeoutTicks = 2400, batch = "cult_books")
    public static void booksUseNativeRequestsAndLimitConversions(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        level.setWeatherParameters(0, 100000, false, false);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var colony = IColonyManager.getInstance().createColony(level, pos, FakePlayerFactory.getMinecraft(level), "Capacity fixture", "Kingdom's Gate");
        CultSavedData.get(level).removeColony(colony.getID());
        helper.setBlock(new BlockPos(2, 1, 2), CultRegistry.GATE_BLOCK.get());
        var tile = (TileEntityColonyBuilding) level.getBlockEntity(pos);
        try (var stream = CultBookGameTests.class.getResourceAsStream("/blueprints/minecolonies_cult/kingdoms_gate/mysticism/kingdoms_gate1.blueprint")) {
            tile.loadWithComponents(NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap()).getList("tile_entities", 10).getCompound(0), level.registryAccess());
        }
        var gate = (KingdomsGateBuilding) colony.getServerBuildingManager().addNewBuilding(tile, level);
        gate.setStructurePack("Kingdom's Gate");
        gate.setBlueprintPath("mysticism/kingdoms_gate1.blueprint");
        gate.setBuildingLevel(1); gate.onUpgradeComplete(null, 1);
        gate.getModule(CultRegistry.WORK).setHiringMode(com.minecolonies.api.colony.buildings.HiringMode.MANUAL);
        var corners = gate.getCorners();
        for (int x = corners.getA().getX() >> 4; x <= corners.getB().getX() >> 4; x++)
            for (int z = corners.getA().getZ() >> 4; z <= corners.getB().getZ() >> 4; z++) level.setChunkForced(x, z, true);
        for (int x = -4; x <= 4; x++) for (int z = -3; z <= 9; z++)
            level.setBlockAndUpdate(pos.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        List<EntityCitizen> actors = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            var citizen = colony.getCitizenManager().createAndRegisterCivilianData();
            citizen.setIsChild(false); citizen.setSaturation(100);
            // Explicit native entity binding avoids a random spawn-point search in this tiny fixture.
            var entity = (EntityCitizen) com.minecolonies.api.entity.ModEntities.CITIZEN.create(level);
            entity.setUUID(citizen.getUUID()); entity.setCitizenId(citizen.getId());
            entity.getCitizenColonyHandler().setColonyId(colony.getID());
            entity.setPos(pos.getX() + i % 3 - .5, pos.getY(), pos.getZ() + i / 3 + 2.5);
            level.addFreshEntity(entity);
            entity.getCitizenColonyHandler().registerWithColony(colony.getID(), citizen.getId());
            helper.assertTrue(entity.getCitizenData() == citizen, "Native actor bound to fixture citizen");
            entity.setNoAi(true); new CitizenAI(entity); actors.add(entity);
        }
        var leader = actors.getFirst().getCitizenData();
        actors.getFirst().getInventoryCitizen().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK));
        for (var assigned : List.copyOf(gate.getModule(CultRegistry.WORK).getAssignedCitizen()))
            gate.getModule(CultRegistry.WORK).removeCitizen(assigned);
        helper.assertTrue(gate.getModule(CultRegistry.WORK).assignCitizen(leader), "Leader assigned through native module; level=" + gate.getBuildingLevel()
                + "; assigned=" + gate.getModule(CultRegistry.WORK).getAssignedCitizen().size() + "; job=" + leader.getJob());
        long day = level.getDayTime() / 24000 + 10;
        level.setDayTime(day * 24000 + 6000);
        var state = CultService.state(colony);
        state.observe(level.getDayTime(), 1, leader.getUUID(), CultRules.defaults());
        for (var actor : actors.subList(1, 6)) for (long credited = day - 5; credited < day; credited++)
            state.hear(actor.getCitizenData().getUUID(), credited, CultRules.defaults());
        helper.onEachTick(() -> {
            // Keep the worker within native pickup range; this test does not assert pathfinding.
            actors.getFirst().getNavigation().stop();
            actors.getFirst().setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
            actors.forEach(a -> a.getCitizenAI().tick());
        });
        helper.runAtTickTime(250, () -> {
            helper.assertTrue(state.recruitCount() == 0 && state.progress(actors.get(1).getCitizenData().getUUID()) == 5,
                    "A writable book cannot fund recruitment or change pending progress");
            var ai = ((com.kingdomsgate.cult.building.CultLeaderJob) leader.getJob()).getWorkerAI();
            for (int i = 0; i < 10; i++) ai.ensureBook();
            helper.assertTrue(bookRequests(gate, leader) == 1, "Repeated checks keep one ordinary-book request");
            deliverBook(helper, gate, leader);
        });
        helper.runAtTickTime(1100, () -> {
            helper.assertTrue(state.recruitCount() == 1 && state.isMember(actors.get(1).getCitizenData().getUUID()),
                    "Native pickup resumes speech and only the first listener converts with one book; actual=" + state.recruitCount()
                            + "; worker=" + ((com.kingdomsgate.cult.building.CultLeaderJob) leader.getJob()).getWorkerAI().getState()
                            + "; book=" + ((com.kingdomsgate.cult.building.CultLeaderJob) leader.getJob()).getWorkerAI().hasBook()
                            + "; completed=" + gate.hasCitizenCompletedRequests(leader)
                            + "; history=" + ((com.kingdomsgate.cult.building.CultLeaderJob) leader.getJob()).getWorkerAI().getStateAI().getHistory().getString()
                            + "; completedRequests=" + gate.getCompletedRequestsOfCitizenOrBuilding(leader).stream().map(r ->
                                    r.getDeliveries() + " at " + colony.getRequestManager().getResolverForRequest(r.getId()).getLocation()).toList()
                            + "; workerpos=" + actors.getFirst().position() + "; hut=" + gate.getLocation()
                            + "; open=" + bookRequests(gate, leader) + "; citizen=" + actors.getFirst().getCitizenAI().getState());
            helper.assertTrue(state.progress(actors.get(2).getCitizenData().getUUID()) == 5,
                    "Book exhaustion preserves the next listener's pending final speech");
            helper.assertTrue(bookRequests(gate, leader) == 1, "Conversion requests a replacement book");
            helper.assertTrue(actors.getFirst().getInventoryCitizen().getStackInSlot(0).is(net.minecraft.world.item.Items.WRITABLE_BOOK),
                    "Nonordinary book is never consumed");
            deliverBook(helper, gate, leader);
        });
        helper.runAtTickTime(2100, () -> {
            helper.assertTrue(state.recruitCount() == 2, "Replacement delivery enables exactly one further conversion");
            helper.assertTrue(bookRequests(gate, leader) == 1, "Second conversion requests another replacement");
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, level);
            for (int x = corners.getA().getX() >> 4; x <= corners.getB().getX() >> 4; x++)
                for (int z = corners.getA().getZ() >> 4; z <= corners.getB().getZ() >> 4; z++) level.setChunkForced(x, z, false);
            helper.succeed();
        });
    }

    private static int bookRequests(KingdomsGateBuilding gate, com.minecolonies.api.colony.ICitizenData leader) {
        return gate.getOpenRequestsOfTypeFiltered(leader, com.minecolonies.api.util.constant.TypeConstants.DELIVERABLE,
                request -> request.getRequest().matches(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK))).size();
    }

    /** Simulate fulfillment into hut storage, leaving acceptance and pickup to the native worker AI. */
    private static void deliverBook(GameTestHelper helper, KingdomsGateBuilding gate, com.minecolonies.api.colony.ICitizenData leader) {
        helper.assertTrue(helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                gate.getPosition(), null) != null, "Hut exposes inventory to native pickup and couriers");
        var book = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK);
        var remainder = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(gate.getItemHandlerCap(), book.copy(), false);
        helper.assertTrue(remainder.isEmpty(), "Hut accepts delivered book");
        helper.assertTrue(gate.hasCitizenCompletedRequests(leader), "Native request marked fulfilled");
    }
}
