package com.kingdomsgate.cult;

import com.kingdomsgate.cult.building.*;
import com.kingdomsgate.cult.client.CultStatusView;
import com.kingdomsgate.cult.runtime.CultService;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.citizen.happiness.HappinessRegistry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import com.minecolonies.core.colony.buildings.views.EmptyView;
import com.minecolonies.core.colony.jobs.views.DefaultJobView;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import com.minecolonies.api.items.ItemBlockHut;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CultRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CultMod.ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CultMod.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CultMod.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CultMod.ID);
    public static final DeferredRegister<JobEntry> JOBS = DeferredRegister.create(CommonMinecoloniesAPIImpl.JOBS, CultMod.ID);
    public static final DeferredRegister<BuildingEntry> BUILDINGS = DeferredRegister.create(CommonMinecoloniesAPIImpl.BUILDINGS, CultMod.ID);
    public static final DeferredRegister<HappinessRegistry.HappinessFunctionEntry> HAPPINESS = DeferredRegister.create(CommonMinecoloniesAPIImpl.HAPPINESS_FUNCTION, CultMod.ID);
    public static final DeferredHolder<net.minecraft.world.level.block.Block, KingdomsGateBlock> GATE_BLOCK = BLOCKS.register("kingdoms_gate", KingdomsGateBlock::new);
    public static final DeferredHolder<Item, ItemBlockHut> GATE_ITEM = ITEMS.register("kingdoms_gate", () -> new ItemBlockHut(GATE_BLOCK.get(), new Item.Properties()));
    static { TABS.register("kingdoms_gate", () -> CreativeModeTab.builder().title(Component.translatable("block.minecolonies_cult.kingdoms_gate"))
            .icon(() -> new ItemStack(GATE_ITEM.get())).displayItems((parameters, output) -> output.accept(GATE_ITEM.get())).build()); }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityColonyBuilding>> GATE_ENTITY = ENTITIES.register("kingdoms_gate",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileEntityColonyBuilding(CultRegistry.GATE_ENTITY.get(), pos, state), GATE_BLOCK.get()).build(null));
    public static final DeferredHolder<JobEntry, JobEntry> LEADER = JOBS.register("cult_leader", () -> new JobEntry.Builder()
            .setRegistryName(CultMod.id("cult_leader")).setJobProducer(CultLeaderJob::new)
            .setJobViewProducer(() -> DefaultJobView::new).createJobEntry());
    public static final BuildingEntry.ModuleProducer<WorkerBuildingModule, WorkerBuildingModuleView> WORK = new BuildingEntry.ModuleProducer<>(
            CultMod.ID + ":leader_work", () -> new WorkerBuildingModule(LEADER.get(), Skill.Knowledge, Skill.Mana, false, b -> b.getBuildingLevel() > 0 ? 1 : 0),
            () -> WorkerBuildingModuleView::new);
    public static final BuildingEntry.ModuleProducer<CultStatusModule, CultStatusView> STATUS = new BuildingEntry.ModuleProducer<>(
            CultMod.ID + ":cult_status", CultStatusModule::new, () -> CultStatusView::new);
    public static final DeferredHolder<BuildingEntry, BuildingEntry> GATE = BUILDINGS.register("kingdoms_gate", () -> new BuildingEntry.Builder()
            .setRegistryName(CultMod.id("kingdoms_gate")).setBuildingBlock(GATE_BLOCK.get())
            .setBuildingProducer(KingdomsGateBuilding::new).setBuildingViewProducer(() -> EmptyView::new)
            .addBuildingModuleProducer(WORK).addBuildingModuleProducer(STATUS)
            .addBuildingModuleProducer(BuildingModules.STATS_MODULE).addBuildingModuleProducer(BuildingModules.MIN_STOCK).createBuildingEntry());
    static { HAPPINESS.register("cult_membership", () -> new HappinessRegistry.HappinessFunctionEntry(CultService::happiness)); }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); JOBS.register(bus); BUILDINGS.register(bus); HAPPINESS.register(bus); TABS.register(bus);
    }
    private CultRegistry() {}
}
