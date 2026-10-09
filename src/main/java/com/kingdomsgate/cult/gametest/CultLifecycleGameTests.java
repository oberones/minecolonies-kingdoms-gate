package com.kingdomsgate.cult.gametest;

import com.kingdomsgate.cult.CultMod;
import com.kingdomsgate.cult.CultRegistry;
import com.kingdomsgate.cult.building.KingdomsGateBuilding;
import com.kingdomsgate.cult.runtime.CultService;
import com.kingdomsgate.cult.runtime.RitualState;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Controlled entity fixture: real native AI, stationary listeners, accelerated calendar. */
@GameTestHolder(CultMod.ID)
@PrefixGameTestTemplate(false)
public final class CultLifecycleGameTests {
    @GameTest(template = "empty", timeoutTicks = 2600, batch = "cult_lifecycle")
    public static void recruitmentAndRitualCleanup(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        level.setWeatherParameters(0, 100000, false, false);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        var pos = helper.absolutePos(new BlockPos(2,1,2));
        var colony = IColonyManager.getInstance().createColony(level, pos,
                FakePlayerFactory.getMinecraft(level), "Cult lifecycle fixture", "Kingdom's Gate");
        com.kingdomsgate.cult.runtime.CultSavedData.get(level).removeColony(colony.getID());
        helper.setBlock(new BlockPos(2,1,2), CultRegistry.GATE_BLOCK.get());
        var tile = (TileEntityColonyBuilding) level.getBlockEntity(pos);
        try (var stream = CultLifecycleGameTests.class.getResourceAsStream(
                "/blueprints/minecolonies_cult/kingdoms_gate/mysticism/kingdoms_gate1.blueprint")) {
            var blueprint = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            tile.loadWithComponents(blueprint.getList("tile_entities", 10).getCompound(0), level.registryAccess());
        }
        var gate = (KingdomsGateBuilding)colony.getServerBuildingManager().addNewBuilding(tile, level);
        gate.setBuildingLevel(1);
        gate.onUpgradeComplete(null, 1);
        helper.assertTrue(!gate.getLocationsFromTag("cult_gather").isEmpty(), "Native building reads gathering markers");
        var corners = gate.getCorners();
        for (int x = corners.getA().getX() >> 4; x <= corners.getB().getX() >> 4; x++)
            for (int z = corners.getA().getZ() >> 4; z <= corners.getB().getZ() >> 4; z++) level.setChunkForced(x,z,true);
        for (int x=-14; x<=14; x++) for (int z=-3; z<=31; z++)
            level.setBlockAndUpdate(pos.offset(x,-1,z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var leader = colony.getCitizenManager().createAndRegisterCivilianData();
        var listener = colony.getCitizenManager().createAndRegisterCivilianData();
        var child = colony.getCitizenManager().createAndRegisterCivilianData();
        leader.setIsChild(false); listener.setIsChild(false); child.setIsChild(true);
        colony.getCitizenManager().spawnOrCreateCitizen(leader, level, pos.offset(0,0,2));
        colony.getCitizenManager().spawnOrCreateCitizen(listener, level, pos.offset(2,0,2));
        colony.getCitizenManager().spawnOrCreateCitizen(child, level, pos.offset(1,0,2));
        helper.assertTrue(gate.getModule(CultRegistry.WORK).assignCitizen(leader), "Leader assigned");
        var speaker = (EntityCitizen) leader.getEntity().orElseThrow();
        var hearer = (EntityCitizen) listener.getEntity().orElseThrow();
        var young = (EntityCitizen) child.getEntity().orElseThrow();
        speaker.setPos(pos.getX()+.5, pos.getY(), pos.getZ()+2.5);
        hearer.setPos(pos.getX()+2.5, pos.getY(), pos.getZ()+2.5);
        young.setPos(pos.getX()+1.5, pos.getY(), pos.getZ()+2.5);
        for (var entity : new EntityCitizen[]{speaker, hearer, young}) {
            entity.setNoAi(true);
            entity.getCitizenData().setSaturation(100);
            new CitizenAI(entity);
        }
        long startDay = level.getDayTime() / 24000 + 1;
        level.setDayTime(startDay * 24000 + 6000);
        helper.onEachTick(() -> {
            long tick = helper.getTick();
            long day = Math.min(5, tick / 300);
            if (tick < 1800) level.setDayTime((startDay + day) * 24000 + 6000 + tick % 300);
            // Keep the fixtures still; production navigation is deliberately not claimed by this test.
            if (tick % 5 == 0) {
                speaker.getCitizenAI().tick(); hearer.getCitizenAI().tick(); young.getCitizenAI().tick();
            }
        });
        for (int day=0; day<6; day++) {
            final int expected = day + 1;
            helper.runAtTickTime(day * 300 + 280, () -> {
                var state = CultService.state(colony);
                helper.assertTrue(state.progress(listener.getUUID()) == expected,
                        "Exactly one full speech credited on distinct day " + expected + "; actual=" + state.progress(listener.getUUID())
                                + "; leader state=" + speaker.getCitizenAI().getState() + "; distance=" + speaker.distanceTo(hearer)
                                + "; sick=" + leader.getCitizenDiseaseHandler().isSick() + "; hurt=" + leader.getCitizenDiseaseHandler().isHurt()
                                + "; listener sick=" + listener.getCitizenDiseaseHandler().isSick() + "; listener hurt=" + listener.getCitizenDiseaseHandler().isHurt()
                                + "; rain=" + level.isRaining() + "; raid=" + colony.getRaiderManager().isRaided()
                                + "; loaded=" + level.hasChunkAt(speaker.blockPosition()) + "; time=" + level.getDayTime()
                                + "; LOS=" + speaker.hasLineOfSight(hearer) + "; saturation=" + leader.getSaturation());
                helper.assertTrue(state.progress(child.getUUID()) == 0, "Children never recruited");
            });
        }
        helper.runAtTickTime(1800, () -> {
            helper.assertTrue(CultService.state(colony).isMember(listener.getUUID()), "Six days grant membership");
            listener.setAsleep(true);
            level.setDayTime((startDay + 7) * 24000 + 16000);
        });
        helper.runAtTickTime(1850, () -> {
            helper.assertTrue(CultService.attending(hearer), "Sleeping member invited into ritual; next="
                    + CultService.state(colony).nextRitualDay() + "; day=" + level.getDayTime()/24000);
            helper.assertTrue(hearer.getCitizenAI().getState() == RitualState.RITUAL, "Native citizen AI enters ritual state");
            helper.assertTrue(!hearer.getCitizenSleepHandler().isAsleep(), "Native wake handler releases sleeping citizen");
            var slots = gate.getLocationsFromTag("cult_gather").stream().sorted(java.util.Comparator.comparingLong(BlockPos::asLong)).toList();
            speaker.setPos(slots.get(0).getX()+.5, slots.get(0).getY(), slots.get(0).getZ()+.5);
            hearer.setPos(slots.get(1).getX()+.5, slots.get(1).getY(), slots.get(1).getZ()+.5);
            level.setDayTime((startDay + 7) * 24000 + 18000);
        });
        helper.runAtTickTime(1880, () -> {
            helper.assertTrue(CultService.attending(speaker) && CultService.attending(hearer), "Arrived citizens remain through ceremony phase");
            level.setDayTime((startDay + 7) * 24000 + 18600);
        });
        helper.runAtTickTime(1900, () -> {
            helper.assertTrue(!CultService.attending(hearer), "Ceremony ends on schedule");
            helper.assertTrue(hearer.getCitizenAI().getState() != RitualState.RITUAL, "Normal AI restored");
            helper.assertTrue(leader.getJob().getJobRegistryEntry() == CultRegistry.LEADER.get() && listener.getJob() == null,
                    "Assignments preserved");
            level.setDayTime((startDay + 14) * 24000 + 16000);
        });
        helper.runAtTickTime(1950, () -> {
            helper.assertTrue(CultService.attending(hearer), "Following week's gathering starts");
            colony.getRaiderManager().setPassThroughRaid();
        });
        helper.runAtTickTime(1990, () -> {
            helper.assertTrue(!CultService.attending(hearer) && hearer.getCitizenAI().getState() != RitualState.RITUAL,
                    "MineColonies raid cancels ritual and releases AI");
            helper.assertTrue(CultService.state(colony).graceDeadline(com.kingdomsgate.cult.domain.CultRules.defaults()) == -1,
                    "A raid alone does not trigger faith decay");
            gate.getModule(CultRegistry.WORK).removeCitizen(leader);
        });
        helper.runAtTickTime(2030, () -> {
            var state = CultService.state(colony);
            helper.assertTrue(state.graceDeadline(com.kingdomsgate.cult.domain.CultRules.defaults()) > state.time()
                    && state.isMember(listener.getUUID()) && state.bonusLevel() == 1, "Unhiring starts grace and preserves bonus");
            helper.assertTrue(gate.getModule(CultRegistry.WORK).assignCitizen(leader), "Replacement leader can be hired normally");
        });
        helper.runAtTickTime(2070, () -> {
            helper.assertTrue(CultService.state(colony).graceDeadline(com.kingdomsgate.cult.domain.CultRules.defaults()) == -1,
                    "Restoring staffing cancels faith decay");
            gate.getModule(CultRegistry.WORK).removeCitizen(leader);
        });
        helper.runAtTickTime(2110, () -> {
            level.setDayTime(CultService.state(colony).graceDeadline(com.kingdomsgate.cult.domain.CultRules.defaults()) + 1);
        });
        helper.runAtTickTime(2150, () -> {
            var state = CultService.state(colony);
            helper.assertTrue(!state.isMember(listener.getUUID()) && state.progress(listener.getUUID()) == 0 && state.bonusLevel() == 0,
                    "Expired grace clears membership and recruitment");
            helper.assertTrue(CultService.happiness(listener) == 1, "Expired native happiness is neutral");
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, level);
            for (int x = corners.getA().getX() >> 4; x <= corners.getB().getX() >> 4; x++)
                for (int z = corners.getA().getZ() >> 4; z <= corners.getB().getZ() >> 4; z++) level.setChunkForced(x,z,false);
            helper.succeed();
        });
    }
}
