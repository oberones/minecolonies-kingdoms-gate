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
        state.observe(level.getDayTime(), 1, citizen.getUUID(), CultRules.defaults());
        double before = citizen.getCitizenHappinessHandler().getHappiness(colony, citizen);
        helper.assertTrue(state.progress(citizen.getUUID()) == 0, "Leader role does not award recruitment credits");
        CultService.refresh(citizen);
        double after = citizen.getCitizenHappinessHandler().getHappiness(colony, citizen);
        helper.assertTrue(Double.isFinite(after) && after >= before && after <= 10, "Membership boosts native happiness without exceeding cap");
        helper.assertTrue(citizen.getCitizenHappinessHandler().getModifier(CultService.HAPPINESS_ID).getFactor(citizen) == 1.25, "Level one native factor");
        for (int upgrade=2; upgrade<=5; upgrade++) {
            building.setBuildingLevel(upgrade);
            building.setBlueprintPath("mysticism/kingdoms_gate" + upgrade + ".blueprint");
            building.onUpgradeComplete(null, upgrade);
            state.observe(level.getDayTime(), upgrade, citizen.getUUID(), CultRules.defaults());
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
        original.colony(1).observe(0, 3, UUID.randomUUID(), CultRules.defaults());
        for (int day = 0; day < 6; day++) original.colony(1).hear(member, day, CultRules.defaults());
        original.colony(1).observe(100, 0, null, CultRules.defaults());
        original.colony(2).observe(0, 1, UUID.randomUUID(), CultRules.defaults());
        original.colony(2).hear(member, 0, CultRules.defaults());
        var restored = CultSavedData.load(original.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(restored.colony(1).isMember(member), "Membership survives NBT");
        helper.assertTrue(!restored.colony(2).isMember(member), "Colonies remain isolated");
        helper.assertTrue(restored.colony(1).graceDeadline(CultRules.defaults()) == 168100, "Grace deadline survives NBT");
        restored.removeColony(1);
        helper.assertTrue(!restored.colony(1).isMember(member), "Deleted colony membership removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void leadershipSaveMigrationAndOverfullRoundTrip(GameTestHelper helper) {
        CultSavedData data = new CultSavedData();
        var state = data.colony(101);
        UUID leader = new UUID(0, 100), partial = new UUID(0, 99);
        state.observe(0, 3, leader, CultRules.defaults());
        for (int i = 0; i < 10; i++) for (int day = 0; day < 6; day++)
            state.hear(new UUID(0, i), day, CultRules.defaults());
        state.hear(partial, 0, CultRules.defaults());
        state.observe(100, 1, leader, CultRules.defaults());
        var root = data.save(new CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(root.getInt("version") == 2, "Writes version two");
        var restored = CultSavedData.load(root, helper.getLevel().registryAccess()).colony(101);
        helper.assertTrue(restored.snapshot().equals(state.snapshot()) && !restored.canRecruit(), "Complete round trip waits for native reconciliation");
        helper.assertTrue(restored.isMember(leader) && !restored.isPermanentMember(leader) && restored.progress(leader) == 0,
                "Role-only leader remains separate from speech progress");
        restored.observe(101, 1, leader, CultRules.defaults());
        helper.assertTrue(restored.recruitCount() == 10 && !restored.canRecruit() && restored.progress(partial) == 1,
                "Overfull membership and partial progress survive reload");
        restored.observe(102, 0, null, CultRules.defaults());
        helper.assertTrue(!restored.isMember(leader) && restored.memberCount() == 10, "Dismissal removes only the temporary role");

        // A v1 file cannot distinguish recruited members from former automatic leaders.
        root.putInt("version", 1);
        root.getList("colonies", 10).getCompound(0).remove("leader");
        var migrated = CultSavedData.load(root, helper.getLevel().registryAccess()).colony(101);
        helper.assertTrue(migrated.snapshot().citizens().equals(state.snapshot().citizens()) && migrated.leader() == null,
                "Version one preserves every permanent membership and partial record");
        migrated.observe(103, 1, leader, CultRules.defaults());
        helper.assertTrue(migrated.recruitCount() == 10 && !migrated.canRecruit(), "Legacy overfull colony cannot recruit");
        root.putInt("version", 3);
        boolean rejected = false;
        try { CultSavedData.load(root, helper.getLevel().registryAccess()); }
        catch (IllegalStateException expected) { rejected = true; }
        helper.assertTrue(rejected, "Unknown future save versions remain protected");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fiveBlueprintsAndEveryOrientation(GameTestHelper helper) throws Exception {
        int[] widths = {9, 13, 17, 23, 29}, depths = {13, 17, 23, 29, 35};
        var previousBounds = new java.util.HashMap<RotationMirror, int[]>();
        for (int level=1; level<=5; level++) {
            String path = "/blueprints/minecolonies_cult/kingdoms_gate/mysticism/kingdoms_gate" + level + ".blueprint";
            CompoundTag tag;
            try (InputStream stream = CultGameTests.class.getResourceAsStream(path)) {
                helper.assertTrue(stream != null, "Pack includes level " + level);
                tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            }
            helper.assertTrue(tag.getShort("size_x") == widths[level-1] && tag.getShort("size_z") == depths[level-1], "Expected growing footprint");
            for (RotationMirror rotation : RotationMirror.values()) {
                var blueprint = BlueprintUtil.readBlueprintFromNBT(tag.copy(), helper.getLevel().registryAccess());
                helper.assertTrue(blueprint != null && blueprint.getMissingMods().length == 0, "Native reader accepts blueprint");
                blueprint.setRotationMirror(rotation, helper.getLevel());
                var anchor = blueprint.getPrimaryBlockOffset();
                helper.assertTrue(blueprint.getBlockState(anchor).is(CultRegistry.GATE_BLOCK.get()), "Rotation preserves hut anchor");
                var hutTag = blueprint.getBlockInfoAsMap().get(anchor).getTileEntityData();
                var positions = IBlueprintDataProviderBE.readTagPosMapFrom(hutTag.getCompound("blueprintDataProvider"));
                long slots = positions.values().stream().filter(v -> v.contains("cult_gather")).count();
                helper.assertTrue(slots == CultRules.capacity(level) + 1, "Exactly one place per recruit plus leader survives rotation");
                helper.assertTrue(positions.values().stream().filter(v -> v.contains("cult_leader_gather")).count() == 1, "One reserved leader position");
                var provider = hutTag.getCompound("blueprintDataProvider");
                var a = com.ldtteam.structurize.api.BlockPosUtil.readFromNBT(provider, "corner1");
                var b = com.ldtteam.structurize.api.BlockPosUtil.readFromNBT(provider, "corner2");
                int[] bounds = {Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                        Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())};
                helper.assertTrue(bounds[0] == -anchor.getX() && bounds[1] == -anchor.getY() && bounds[2] == -anchor.getZ()
                        && bounds[3] == blueprint.getSizeX()-1-anchor.getX() && bounds[4] == blueprint.getSizeY()-1-anchor.getY()
                        && bounds[5] == blueprint.getSizeZ()-1-anchor.getZ(), "Corners exactly describe the rotated blueprint relative to its hut");
                int[] previous = previousBounds.put(rotation, bounds);
                if (previous != null) for (int axis = 0; axis < 3; axis++)
                    helper.assertTrue(bounds[axis] <= previous[axis] && bounds[axis+3] >= previous[axis+3], "Upgrade expands around the same world anchor");
                BlockPos entrance = anchor.offset(positions.entrySet().stream().filter(e -> e.getValue().contains("cult_entrance")).findFirst().orElseThrow().getKey());
                var reachable = new java.util.HashSet<BlockPos>();
                var queue = new java.util.ArrayDeque<BlockPos>();
                reachable.add(entrance); queue.add(entrance);
                while (!queue.isEmpty()) {
                    BlockPos current = queue.remove();
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        BlockPos next = current.relative(direction);
                        if (next.getX() < 0 || next.getX() >= blueprint.getSizeX() || next.getZ() < 0 || next.getZ() >= blueprint.getSizeZ()) continue;
                        if (!reachable.contains(next) && blueprint.getBlockState(next).isAir() && blueprint.getBlockState(next.above()).isAir()
                                && blueprint.getBlockState(next.below()).isFaceSturdy(helper.getLevel(), next.below(), net.minecraft.core.Direction.UP)) {
                            reachable.add(next); queue.add(next);
                        }
                    }
                }
                for (var entry : positions.entrySet()) {
                    if (!entry.getValue().contains("cult_gather")) continue;
                    BlockPos pos = anchor.offset(entry.getKey());
                    helper.assertTrue(blueprint.getBlockState(pos).isAir() && blueprint.getBlockState(pos.above()).isAir(), "Standing slot is clear");
                    helper.assertTrue(blueprint.getBlockState(pos.below()).isFaceSturdy(helper.getLevel(), pos.below(), net.minecraft.core.Direction.UP), "Standing slot is supported");
                    helper.assertTrue(reachable.contains(pos), "Standing slot is connected to the public entrance");
                }
            }
        }
        helper.succeed();
    }
}
