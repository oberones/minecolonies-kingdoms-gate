package com.kingdomsgate.cult.building;

import com.kingdomsgate.cult.CultConfig;
import com.kingdomsgate.cult.domain.CultRules;
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
        var leader = CultService.leader((KingdomsGateBuilding) building);
        boolean operational = building.isBuilt() && building.getBuildingLevel() > 0 && leader != null;
        var citizens = building.getColony().getCitizenManager().getCitizens().stream().filter(c -> !c.isChild())
                .sorted(java.util.Comparator.comparingInt(com.minecolonies.api.colony.ICitizenData::getId))
                .map(c -> new CultStatusData.Citizen(c.getName(), state.isPermanentMember(c.getUUID()) || operational && c == leader,
                        operational && c == leader, state.progress(c.getUUID()))).toList();
        int recruits = (int) state.snapshot().citizens().entrySet().stream()
                .filter(e -> e.getValue().joined() && !(operational && e.getKey().equals(leader.getUUID()))).count();
        new CultStatusData(recruits, CultRules.capacity(building.isBuilt() ? building.getBuildingLevel() : 0), rules.speeches(),
                state.nextRitualDay(), state.day(),
                state.graceDeadline(rules) < 0 ? -1 : Math.max(0, state.graceDeadline(rules) - state.time()),
                operational, leader == null ? "" : leader.getName(), citizens).write(buffer);
    }
}
