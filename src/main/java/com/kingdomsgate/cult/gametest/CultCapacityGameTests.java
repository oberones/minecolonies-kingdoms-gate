package com.kingdomsgate.cult.gametest;

import com.kingdomsgate.cult.CultMod;
import com.kingdomsgate.cult.CultRegistry;
import com.kingdomsgate.cult.building.CultStatusData;
import com.kingdomsgate.cult.building.KingdomsGateBuilding;
import com.kingdomsgate.cult.domain.CultRules;
import com.kingdomsgate.cult.runtime.CultSavedData;
import com.kingdomsgate.cult.runtime.CultService;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.core.colony.workorders.WorkOrderBuilding;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Stationary native actors test recruitment and ceremony limits without claiming natural pathfinding. */
@GameTestHolder(CultMod.ID)
@PrefixGameTestTemplate(false)
public final class CultCapacityGameTests {
    @GameTest(template = "empty", timeoutTicks = 1000, batch = "cult_capacity")
    public static void fullGateUpgradeAndUniqueCeremonyPlaces(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        level.setWeatherParameters(0, 100000, false, false);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var colony = IColonyManager.getInstance().createColony(level, pos, FakePlayerFactory.getMinecraft(level), "Capacity fixture", "Kingdom's Gate");
        CultSavedData.get(level).removeColony(colony.getID());
        helper.setBlock(new BlockPos(2, 1, 2), CultRegistry.GATE_BLOCK.get());
        var tile = (TileEntityColonyBuilding) level.getBlockEntity(pos);
        try (var stream = CultCapacityGameTests.class.getResourceAsStream("/blueprints/minecolonies_cult/kingdoms_gate/mysticism/kingdoms_gate1.blueprint")) {
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
        actors.getFirst().getInventoryCitizen().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 5));
        for (var assigned : List.copyOf(gate.getModule(CultRegistry.WORK).getAssignedCitizen()))
            gate.getModule(CultRegistry.WORK).removeCitizen(assigned);
        helper.assertTrue(gate.getModule(CultRegistry.WORK).assignCitizen(leader), "Leader assigned through native module; level=" + gate.getBuildingLevel()
                + "; assigned=" + gate.getModule(CultRegistry.WORK).getAssignedCitizen().size() + "; job=" + leader.getJob());
        long day = level.getDayTime() / 24000 + 10;
        level.setDayTime(day * 24000 + 6000);
        var state = CultService.state(colony);
        state.observe(level.getDayTime(), 1, leader.getUUID(), CultRules.defaults());
        for (var actor : actors.subList(1, 6)) for (long credited = day - 5; credited < day; credited++)
            helper.assertTrue(state.hear(actor.getCitizenData().getUUID(), credited, CultRules.defaults()), "Prepares five pending final speeches");
        helper.onEachTick(() -> {
            if (helper.getTick() % 5 == 0) actors.forEach(a -> a.getCitizenAI().tick());
        });
        helper.runAtTickTime(250, () -> {
            helper.assertTrue(actors.getFirst().getInventoryCitizen().getStackInSlot(0).getCount() == 1,
                    "Four simultaneous converts consume exactly four carried books");
            helper.assertTrue(state.recruitCount() == 4 && state.memberCount() == 5, "One native speech fills exactly four recruit places");
            helper.assertTrue(!state.isMember(actors.get(5).getCitizenData().getUUID()) && state.progress(actors.get(5).getCitizenData().getUUID()) == 5,
                    "Stable citizen IDs decide contention; fifth listener keeps five speeches");
            var order = WorkOrderBuilding.create(WorkOrderType.UPGRADE, gate);
            colony.getWorkManager().addWorkOrder(order, false);
            helper.assertTrue(order.getTargetLevel() == 2 && gate.getBuildingLevel() == 1, "Pending native upgrade targets level two");
        });
        helper.runAtTickTime(400, () -> {
            helper.assertTrue(state.recruitCapacity() == 4 && state.progress(actors.get(5).getCitizenData().getUUID()) == 5, "Pending work cannot unlock recruitment or advance paused progress");
            helper.assertTrue(actors.getFirst().getInventoryCitizen().getStackInSlot(0).getCount() == 1,
                    "Capacity pause consumes no books");
            assertStatus(helper, gate, 4, 4, "full", leader.getName());
            gate.setBuildingLevel(2); gate.onUpgradeComplete(null, 2);
        });
        helper.runAtTickTime(650, () -> {
            helper.assertTrue(state.recruitCount() == 5 && state.isMember(actors.get(5).getCitizenData().getUUID()), "Completed upgrade resumes the pending recruit");
            helper.assertTrue(actors.getFirst().getInventoryCitizen().getStackInSlot(0).isEmpty(),
                    "Upgrade conversion consumes the last carried book");
            assertStatus(helper, gate, 5, 8, "active", leader.getName());
            gate.setBuildingLevel(1); gate.onUpgradeComplete(null, 1);
        });
        helper.runAtTickTime(690, () -> {
            assertStatus(helper, gate, 5, 4, "over_capacity", leader.getName());
            helper.assertTrue(state.recruitCount() == 5 && !state.canRecruit(), "Smaller Gate retains excess membership");
            level.setDayTime(state.nextRitualDay() * 24000 + 16000);
        });
        helper.runAtTickTime(735, () -> {
            helper.assertTrue(actors.stream().filter(CultService::attending).count() == 5 && !CultService.attending(actors.get(5)), "Leader and first four citizens attend; excess member retains membership outside ritual");
            var leaderSlots = gate.getLocationsFromTag("cult_leader_gather");
            var slots = gate.getLocationsFromTag("cult_gather").stream().sorted(Comparator.<BlockPos>comparingInt(p -> leaderSlots.contains(p) ? 0 : 1).thenComparingLong(BlockPos::asLong)).toList();
            for (int i = 0; i < 5; i++) actors.get(i).setPos(slots.get(i).getX() + .5, slots.get(i).getY(), slots.get(i).getZ() + .5);
            level.setDayTime(state.day() * 24000 + 18000);
        });
        helper.runAtTickTime(780, () -> {
            helper.assertTrue(actors.stream().filter(CultService::attending).count() == 5, "Each invited actor occupies its own ceremony position through performance");
            helper.assertTrue(CultService.happiness(actors.get(5).getCitizenData()) == 1.25, "Excluded attendee retains membership bonus");
            gate.getModule(CultRegistry.WORK).removeCitizen(leader);
        });
        helper.runAtTickTime(830, () -> {
            helper.assertTrue(!state.isMember(leader.getUUID()) && state.progress(leader.getUUID()) == 0 && CultService.happiness(leader) == 1,
                    "Dismissal removes role benefits without leaving permanent membership");
            helper.assertTrue(actors.stream().noneMatch(CultService::attending), "Leadership loss releases all participants");
            assertStatus(helper, gate, 5, 4, "grace", "");
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, level);
            for (int x = corners.getA().getX() >> 4; x <= corners.getB().getX() >> 4; x++)
                for (int z = corners.getA().getZ() >> 4; z <= corners.getB().getZ() >> 4; z++) level.setChunkForced(x, z, false);
            helper.succeed();
        });
    }

    /** Decode through the exact data contract used by the client view, without loading client-only classes. */
    private static void assertStatus(GameTestHelper helper, KingdomsGateBuilding gate, int recruits, int capacity, String condition, String leader) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            gate.getModule(CultRegistry.STATUS).serializeToView(buffer);
            var status = CultStatusData.read(buffer);
            helper.assertTrue(status.recruits() == recruits && status.capacity() == capacity && status.conditionKey().equals(condition)
                    && status.leader().equals(leader) && !buffer.isReadable(), "Status payload matches recruitment, leadership and condition: " + status);
            helper.assertTrue(status.citizens().stream().filter(CultStatusData.Citizen::leader).count() == (leader.isEmpty() ? 0 : 1), "Leader row is identified separately");
        } finally { buffer.release(); }
    }
}
