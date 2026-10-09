package com.kingdomsgate.cult.building;

import com.kingdomsgate.cult.runtime.ProselytizeAI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.kingdomsgate.cult.CultMod;
import net.minecraft.resources.ResourceLocation;

public final class CultLeaderJob extends AbstractJob<ProselytizeAI, CultLeaderJob> {
    public CultLeaderJob(ICitizenData citizen) { super(citizen); }
    @Override public ProselytizeAI generateAI() { return new ProselytizeAI(this); }
    @Override public ResourceLocation getModel() { return CultMod.id("cult_leader"); }
}
