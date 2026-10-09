package com.kingdomsgate.cult.building;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;

public final class KingdomsGateBuilding extends AbstractBuilding {
    public KingdomsGateBuilding(IColony colony, BlockPos pos) { super(colony, pos); }
    @Override public String getSchematicName() { return "kingdoms_gate"; }
    @Override public int getMaxBuildingLevel() { return 5; }
}
