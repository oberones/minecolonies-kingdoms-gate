package com.kingdomsgate.cult.building;

import com.kingdomsgate.cult.CultConfig;
import com.kingdomsgate.cult.runtime.CultService;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.buildings.modules.ITickingModule;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Read-only cult status travels over MineColonies' permission-checked building view protocol. */
public final class CultStatusModule extends AbstractBuildingModule implements ITickingModule {
    @Override public void onColonyTick(IColony colony) { if (colony.getWorld().getGameTime() % 100 == 0) markDirty(); }
    @Override public void serializeToView(RegistryFriendlyByteBuf buffer) {
        var state = CultService.state(building.getColony());
        var rules = CultConfig.rules();
        buffer.writeInt(state.memberCount()); buffer.writeInt(rules.speeches());
        buffer.writeLong(state.nextRitualDay()); buffer.writeLong(state.day());
        buffer.writeLong(state.graceDeadline(rules) < 0 ? -1 : Math.max(0, state.graceDeadline(rules) - state.time()));
        buffer.writeBoolean(building.isBuilt() && CultService.leader((KingdomsGateBuilding) building) != null);
        var citizens = building.getColony().getCitizenManager().getCitizens().stream().filter(c -> !c.isChild()).toList();
        buffer.writeVarInt(citizens.size());
        for (var citizen : citizens) {
            buffer.writeUtf(citizen.getName()); buffer.writeBoolean(state.isMember(citizen.getUUID())); buffer.writeInt(state.progress(citizen.getUUID()));
        }
    }
}
