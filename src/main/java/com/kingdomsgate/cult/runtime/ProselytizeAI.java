package com.kingdomsgate.cult.runtime;

import com.kingdomsgate.cult.building.CultLeaderJob;
import com.kingdomsgate.cult.building.KingdomsGateBuilding;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;

/** MineColonies owns the working-hours decision; the coordinator samples hearing on every server tick. */
public final class ProselytizeAI extends AbstractEntityAIBasic<CultLeaderJob, KingdomsGateBuilding> {
    private long lastWorkTick = Long.MIN_VALUE;
    public ProselytizeAI(CultLeaderJob job) { super(job); }
    @Override public Class<KingdomsGateBuilding> getExpectedBuildingClass() { return KingdomsGateBuilding.class; }
    @Override public void tick() { lastWorkTick = world.getGameTime(); }
    public boolean isWorking(long tick) { return lastWorkTick != Long.MIN_VALUE && tick - lastWorkTick <= 5; }
    @Override public void resetAI() { super.resetAI(); lastWorkTick = Long.MIN_VALUE; }
    @Override public void onRemoval() { lastWorkTick = Long.MIN_VALUE; super.onRemoval(); }
}
