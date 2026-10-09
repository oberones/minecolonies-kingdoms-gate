package com.kingdomsgate.cult.runtime;

import com.kingdomsgate.cult.CultConfig;
import com.kingdomsgate.cult.CultMod;
import com.kingdomsgate.cult.building.CultLeaderJob;
import com.kingdomsgate.cult.building.KingdomsGateBuilding;
import com.kingdomsgate.cult.domain.CultRules;
import com.kingdomsgate.cult.domain.CultState;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.citizen.happiness.DynamicHappinessSupplier;
import com.minecolonies.api.entity.citizen.happiness.StaticHappinessModifier;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Server-only coordinator. Durable staffing comes from colony data, not loaded citizen entities. */
public final class CultService {
    public static final String HAPPINESS_ID = "minecolonies_cult_membership";
    private static final Map<ServerLevel, Map<Integer, ColonyRuntime>> RUNTIMES = new WeakHashMap<>();
    public static CultState state(IColony colony) { return CultSavedData.get((ServerLevel) colony.getWorld()).colony(colony.getID()); }
    public static StaticHappinessModifier modifier() {
        return new StaticHappinessModifier(HAPPINESS_ID, 1, new DynamicHappinessSupplier(CultMod.id("cult_membership")));
    }
    public static double happiness(ICitizenData citizen) {
        if (citizen == null || !(citizen.getColony().getWorld() instanceof ServerLevel)) return 1;
        CultState state = state(citizen.getColony());
        // Never grant a restored role-only leader benefits before checking native staffing.
        var gate = gate(citizen.getColony());
        var leader = leader(gate);
        boolean leads = gate != null && gate.isBuilt() && gate.getBuildingLevel() > 0
                && leader != null && leader.getUUID().equals(citizen.getUUID());
        return state.isPermanentMember(citizen.getUUID()) || leads ? CultConfig.rules().factor(state.bonusLevel()) : 1;
    }
    public static void refresh(ICitizenData citizen) {
        citizen.getCitizenHappinessHandler().addModifier(modifier());
        citizen.getCitizenHappinessHandler().getHappiness(citizen.getColony(), citizen);
        citizen.markDirty(0);
    }
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Map<Integer, ColonyRuntime> runtimes = RUNTIMES.computeIfAbsent(level, ignored -> new HashMap<>());
        for (IColony colony : IColonyManager.getInstance().getColonies(level)) {
            // A deleted colony ID can be reused before the next tick; never retain its old runtime.
            runtimes.compute(colony.getID(), (id, existing) -> {
                if (existing != null && existing.owns(colony)) return existing;
                if (existing != null) existing.cancel();
                return new ColonyRuntime(level, colony);
            }).tick();
        }
        // Colony records are loaded independently of chunks. A removed colony can never own an active ceremony.
        runtimes.entrySet().removeIf(entry -> {
            if (IColonyManager.getInstance().getColonyByWorld(entry.getKey(), level) != null) return false;
            entry.getValue().cancel(); CultSavedData.get(level).removeColony(entry.getKey()); return true;
        });
    }
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Map<Integer, ColonyRuntime> runtimes = RUNTIMES.remove(level);
            if (runtimes != null) runtimes.values().forEach(ColonyRuntime::cancel);
        }
    }
    static ColonyRuntime runtime(EntityCitizen citizen) {
        if (!(citizen.level() instanceof ServerLevel level) || citizen.getCitizenData() == null) return null;
        Map<Integer, ColonyRuntime> worlds = RUNTIMES.get(level);
        return worlds == null ? null : worlds.get(citizen.getCitizenData().getColony().getID());
    }
    public static boolean attending(EntityCitizen citizen) {
        ColonyRuntime runtime = runtime(citizen);
        return runtime != null && runtime.attending(citizen);
    }
    public static void participantTick(EntityCitizen citizen) {
        ColonyRuntime runtime = runtime(citizen);
        if (runtime != null) runtime.participantTick(citizen);
    }
    public static KingdomsGateBuilding gate(IColony colony) {
        return colony.getServerBuildingManager().getBuildings().values().stream()
                .filter(KingdomsGateBuilding.class::isInstance).map(KingdomsGateBuilding.class::cast)
                .min(java.util.Comparator.comparingLong(b -> b.getPosition().asLong())).orElse(null);
    }
    public static ICitizenData leader(KingdomsGateBuilding gate) {
        return gate == null ? null : gate.getAllAssignedCitizen().stream()
                .filter(c -> !c.isChild() && c.getJob() instanceof CultLeaderJob && c.getWorkBuilding() == gate)
                .findFirst().orElse(null);
    }
    static boolean working(EntityCitizen citizen) {
        return citizen.getCitizenAI().getState() == CitizenAIState.WORKING
                && citizen.getCitizenData().getJob() instanceof CultLeaderJob job
                && job.getWorkerAI() != null && job.getWorkerAI().isWorking(citizen.level().getGameTime());
    }
    private CultService() {}
}
