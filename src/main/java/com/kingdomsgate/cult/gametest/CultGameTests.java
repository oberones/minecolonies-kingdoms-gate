package com.kingdomsgate.cult.gametest;

import com.kingdomsgate.cult.CultMod;
import com.kingdomsgate.cult.CultRegistry;
import com.kingdomsgate.cult.domain.CultRules;
import com.kingdomsgate.cult.runtime.CultSavedData;
import com.kingdomsgate.cult.runtime.CultService;
import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE;
import com.ldtteam.structurize.blueprints.v1.BlueprintUtil;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import com.kingdomsgate.cult.building.KingdomsGateBuilding;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.io.InputStream;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Operator-only integration checks, also used by the isolated GameTest server. */
@GameTestHolder(CultMod.ID)
@PrefixGameTestTemplate(false)
public final class CultGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void buildingHiringAndNativeHappiness(GameTestHelper helper) {
        var level = helper.getLevel();
        var position = helper.absolutePos(new BlockPos(2,1,2));
        var player = FakePlayerFactory.getMinecraft(level);
        var colony = IColonyManager.getInstance().createColony(level, position, player, "Gate integration", "Kingdom's Gate");
        helper.assertTrue(colony != null, "Test colony created");
        helper.setBlock(new BlockPos(2,1,2), CultRegistry.GATE_BLOCK.get());
        var tile = (TileEntityColonyBuilding)level.getBlockEntity(position);
        var building = (KingdomsGateBuilding)colony.getServerBuildingManager().addNewBuilding(tile, level);
        helper.assertTrue(building != null, "Hut registers with colony manager");
        building.setBuildingLevel(1);
        building.onUpgradeComplete(null, 1);
        helper.assertTrue(building.isBuilt(), "Construction completion activates building");
        var citizen = colony.getCitizenManager().createAndRegisterCivilianData();
        citizen.setIsChild(false);
        var module = building.getModule(CultRegistry.WORK);
        helper.assertTrue(module.assignCitizen(citizen), "Normal worker module hires leader");
        helper.assertTrue(citizen.getJob().getJobRegistryEntry() == CultRegistry.LEADER.get(), "Citizen receives the leader job");
        helper.assertTrue(!CultRegistry.GATE_BLOCK.get().canPlaceAt(position.offset(1,0,0), player), "Second gate refused in same colony");
        var state = CultService.state(colony);
        state.observe(level.getDayTime(), 1, CultRules.defaults());
        double before = citizen.getCitizenHappinessHandler().getHappiness(colony, citizen);
        state.enrollLeader(citizen.getUUID(), state.day(), CultRules.defaults());
        CultService.refresh(citizen);
        double after = citizen.getCitizenHappinessHandler().getHappiness(colony, citizen);
        helper.assertTrue(Double.isFinite(after) && after >= before && after <= 10, "Membership boosts native happiness without exceeding cap");
        helper.assertTrue(citizen.getCitizenHappinessHandler().getModifier(CultService.HAPPINESS_ID).getFactor(citizen) == 1.25, "Level one native factor");
        for (int upgrade=2; upgrade<=5; upgrade++) {
            building.setBuildingLevel(upgrade);
            building.setBlueprintPath("mysticism/kingdoms_gate" + upgrade + ".blueprint");
            building.onUpgradeComplete(null, upgrade);
            state.observe(level.getDayTime(), upgrade, CultRules.defaults());
            CultService.refresh(citizen);
            helper.assertTrue(citizen.getCitizenHappinessHandler().getModifier(CultService.HAPPINESS_ID).getFactor(citizen)
                    == CultRules.defaults().factor(upgrade), "Upgraded native happiness factor");
            helper.assertTrue(citizen.getCitizenHappinessHandler().getHappiness(colony, citizen) <= 10, "Native cap survives upgrades");
        }
        var tag = building.serializeNBT(level.registryAccess());
        var restored = (KingdomsGateBuilding)CultRegistry.GATE.get().produceBuilding(position, colony);
        restored.deserializeNBT(level.registryAccess(), tag);
        helper.assertTrue(restored.isBuilt() && restored.getBuildingLevel() == 5, "Upgraded building save round trip");
        IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, level);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void registrationsAndBlockEntity(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.get(CultMod.id("kingdoms_gate")) == CultRegistry.GATE_BLOCK.get(), "Gate block registered");
        helper.assertTrue(CultRegistry.GATE.get().getRegistryName().equals(CultMod.id("kingdoms_gate")), "Building entry registered");
        helper.assertTrue(CultRegistry.LEADER.get().getKey().equals(CultMod.id("cult_leader")), "Leader job registered");
        helper.setBlock(new BlockPos(1,1,1), CultRegistry.GATE_BLOCK.get());
        var entity = helper.getBlockEntity(new BlockPos(1,1,1));
        helper.assertTrue(entity != null && entity.getType() == CultRegistry.GATE_ENTITY.get(), "Gate has its own valid block entity type");
        var dependency = net.neoforged.fml.ModList.get().getModContainerById(CultMod.ID).orElseThrow().getModInfo()
                .getDependencies().stream().filter(d -> d.getModId().equals("minecolonies")).findFirst().orElseThrow();
        helper.assertTrue(dependency.getVersionRange().containsVersion(new org.apache.maven.artifact.versioning.DefaultArtifactVersion(
                "1.1.1343-1.21.1-snapshot")), "Loader accepts exact reference version");
        helper.assertTrue(!dependency.getVersionRange().containsVersion(new org.apache.maven.artifact.versioning.DefaultArtifactVersion(
                "1.1.1403-1.21.1-snapshot")), "Loader dependency range rejects superseded target");
        helper.assertTrue(!dependency.getVersionRange().containsVersion(new org.apache.maven.artifact.versioning.DefaultArtifactVersion(
                "1.1.1342-1.21.1-snapshot")), "Loader dependency range rejects older versions");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void happinessBridgeSurvivesSerialization(GameTestHelper helper) {
        var handler = new CitizenHappinessHandler();
        helper.assertTrue(handler.getModifier(CultService.HAPPINESS_ID) != null, "Mixin installs custom modifier before loading");
        CompoundTag tag = new CompoundTag();
        handler.write(helper.getLevel().registryAccess(), tag, true);
        var restored = new CitizenHappinessHandler();
        restored.read(helper.getLevel().registryAccess(), tag, true);
        helper.assertTrue(restored.getModifier(CultService.HAPPINESS_ID) != null, "Custom modifier retained across native save read");
        var client = new CitizenHappinessHandler();
        client.read(helper.getLevel().registryAccess(), tag, false);
        helper.assertTrue(client.getModifier(CultService.HAPPINESS_ID) != null, "Custom modifier retained across client view read");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cultSaveRoundTripAndIsolation(GameTestHelper helper) {
        CultSavedData original = new CultSavedData();
        UUID member = UUID.randomUUID();
        original.colony(1).observe(0, 3, CultRules.defaults());
        original.colony(1).enrollLeader(member, 0, CultRules.defaults());
        original.colony(1).observe(100, 0, CultRules.defaults());
        original.colony(2).hear(member, 0, CultRules.defaults());
        var restored = CultSavedData.load(original.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(restored.colony(1).isMember(member), "Membership survives NBT");
        helper.assertTrue(!restored.colony(2).isMember(member), "Colonies remain isolated");
        helper.assertTrue(restored.colony(1).graceDeadline(CultRules.defaults()) == 168100, "Grace deadline survives NBT");
        restored.removeColony(1);
        helper.assertTrue(!restored.colony(1).isMember(member), "Deleted colony membership removed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fiveBlueprintsAndEveryOrientation(GameTestHelper helper) throws Exception {
        for (int level=1; level<=5; level++) {
            String path = "/blueprints/minecolonies_cult/kingdoms_gate/mysticism/kingdoms_gate" + level + ".blueprint";
            CompoundTag tag;
            try (InputStream stream = CultGameTests.class.getResourceAsStream(path)) {
                helper.assertTrue(stream != null, "Pack includes level " + level);
                tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            }
            for (RotationMirror rotation : RotationMirror.values()) {
                var blueprint = BlueprintUtil.readBlueprintFromNBT(tag.copy(), helper.getLevel().registryAccess());
                helper.assertTrue(blueprint != null && blueprint.getMissingMods().length == 0, "Native reader accepts blueprint");
                blueprint.setRotationMirror(rotation, helper.getLevel());
                var anchor = blueprint.getPrimaryBlockOffset();
                helper.assertTrue(blueprint.getBlockState(anchor).is(CultRegistry.GATE_BLOCK.get()), "Rotation preserves hut anchor");
                var hutTag = blueprint.getBlockInfoAsMap().get(anchor).getTileEntityData();
                var positions = IBlueprintDataProviderBE.readTagPosMapFrom(hutTag.getCompound("blueprintDataProvider"));
                long slots = positions.values().stream().filter(v -> v.contains("cult_gather")).count();
                helper.assertTrue(slots >= 350, "Gathering slots survive rotation");
                for (var entry : positions.entrySet()) {
                    if (!entry.getValue().contains("cult_gather")) continue;
                    BlockPos pos = anchor.offset(entry.getKey());
                    helper.assertTrue(blueprint.getBlockState(pos).isAir() && blueprint.getBlockState(pos.above()).isAir(), "Standing slot is clear");
                    helper.assertTrue(!blueprint.getBlockState(pos.below()).isAir(), "Standing slot is supported");
                }
            }
        }
        helper.succeed();
    }
}
