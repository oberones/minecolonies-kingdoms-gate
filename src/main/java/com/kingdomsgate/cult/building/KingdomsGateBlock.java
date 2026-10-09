package com.kingdomsgate.cult.building;

import com.kingdomsgate.cult.CultMod;
import com.kingdomsgate.cult.CultRegistry;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class KingdomsGateBlock extends AbstractBlockHut<KingdomsGateBlock> {
    @Override public String getHutName() { return "kingdoms_gate"; }
    @Override public ResourceLocation getRegistryName() { return CultMod.id(getHutName()); }
    @Override public BuildingEntry getBuildingEntry() { return CultRegistry.GATE.get(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        TileEntityColonyBuilding entity = new TileEntityColonyBuilding(CultRegistry.GATE_ENTITY.get(), pos, state);
        entity.registryName = CultMod.id(getHutName());
        entity.setPackName("Kingdom's Gate");
        entity.setBlueprintPath("mysticism/kingdoms_gate1.blueprint");
        return entity;
    }
    @Override public boolean canPlaceAt(BlockPos pos, Player player) {
        if (player.level().isClientSide) return true; // Server placement remains authoritative.
        IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(player.level(), pos);
        boolean allowed = colony == null || colony.getServerBuildingManager().getBuildings().values().stream()
                .noneMatch(b -> b instanceof KingdomsGateBuilding && !b.getPosition().equals(pos));
        if (!allowed) player.displayClientMessage(Component.translatable("minecolonies_cult.only_one_gate"), true);
        return allowed;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && !canPlaceAt(context.getClickedPos(), context.getPlayer())) return null;
        return super.getStateForPlacement(context);
    }
}
