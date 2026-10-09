package com.kingdomsgate.cult.runtime;

import com.kingdomsgate.cult.CultConfig;
import com.kingdomsgate.cult.building.KingdomsGateBuilding;
import com.kingdomsgate.cult.domain.CultRules;
import com.kingdomsgate.cult.domain.CultState;
import com.kingdomsgate.cult.domain.ListeningSession;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.raid.Raid;
import org.joml.Vector3f;

/** Transient movement ownership. No navigation or participant state is resumed from a save. */
final class ColonyRuntime {
    private final ServerLevel level;
    private final IColony colony;
    private final CultSavedData data;
    private final CultState state;
    private KingdomsGateBuilding gate;
    private ICitizenData leader;
    private UUID previousLeader;
    private final Map<UUID, Participant> participants = new HashMap<>();
    private final Set<UUID> attemptedToday = new HashSet<>();
    private EntityCitizen target;
    private ListeningSession speech;
    private int speechTicks;
    private long targetSince;
    private long nextSpeechTick;
    private boolean recruitmentPaused;
    private long routeDay = -1;
    private long ritualDay = -1;
    private long lastWorldTime = -1;
    private BlockPos focus;

    private static final class Participant {
        final EntityCitizen entity;
        final BlockPos slot;
        boolean awakened;
        Participant(EntityCitizen entity, BlockPos slot) { this.entity = entity; this.slot = slot; }
    }

    ColonyRuntime(ServerLevel level, IColony colony) {
        this.level = level; this.colony = colony; this.data = CultSavedData.get(level); this.state = data.colony(colony.getID());
    }
    boolean owns(IColony candidate) { return colony == candidate; }

    void tick() {
        CultRules rules = CultConfig.rules();
        long now = level.getDayTime();
        if (level.getGameTime() % 20 == 0 || gate == null) updateColony(now, rules);
        boolean opportunity = state.consumeRitual(now, rules);
        if (opportunity) {
            data.setDirty();
            level.getDataStorage().save();
            if (operational() && areaLoaded() && !raid() && available(leaderEntity())) beginRitual(now);
        }
        if (ritualDay >= 0) {
            if (now / CultState.DAY != ritualDay || now % CultState.DAY >= 18600 || now < lastWorldTime
                    || !operational() || !areaLoaded() || raid() || !available(leaderEntity())) {
                cancel();
            } else {
                participants.entrySet().removeIf(entry -> {
                    if (available(entry.getValue().entity)) return false;
                    release(entry.getValue()); return true;
                });
                if (now % CultState.DAY >= 18000 && level.getGameTime() % 20 == 0) effects();
            }
        }
        if (ritualDay < 0) recruit(rules);
        lastWorldTime = now;
    }

    private void updateColony(long now, CultRules rules) {
        gate = CultService.gate(colony); leader = CultService.leader(gate);
        int oldLevel = state.bonusLevel();
        int oldMembers = state.memberCount();
        state.observe(now, operational() ? gate.getBuildingLevel() : 0, operational() ? leader.getUUID() : null, rules);
        if (ritualDay >= 0 && !Objects.equals(previousLeader, state.leader())) cancel();
        // Citizen manager records survive entity unloading; prune only after colony data is active.
        if (colony.isActive()) {
            Set<UUID> existing = new HashSet<>();
            colony.getCitizenManager().getCitizens().forEach(c -> existing.add(c.getUUID()));
            state.snapshot().citizens().keySet().stream().filter(id -> !existing.contains(id)).forEach(state::remove);
        }
        if (oldLevel != state.bonusLevel() || oldMembers != state.memberCount() || !Objects.equals(previousLeader, state.leader()))
            colony.getCitizenManager().getCitizens().forEach(CultService::refresh);
        previousLeader = state.leader();
        data.setDirty();
        if (gate != null && level.getGameTime() % 100 == 0) gate.markDirty();
    }

    private boolean operational() { return gate != null && gate.isBuilt() && gate.getBuildingLevel() > 0 && leader != null && leader.getJob() instanceof com.kingdomsgate.cult.building.CultLeaderJob; }
    private EntityCitizen leaderEntity() { return leader == null ? null : leader.getEntity().filter(EntityCitizen.class::isInstance).map(EntityCitizen.class::cast).orElse(null); }

    private boolean available(EntityCitizen entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved() || entity.getCitizenData() == null || entity.getCitizenData().isChild()) return false;
        if (!level.hasChunkAt(entity.blockPosition()) || entity.getCitizenData().getCitizenDiseaseHandler().isSick()
                || entity.getCitizenData().getCitizenDiseaseHandler().isHurt() || entity.getCitizenData().getSaturation() <= 1
                || entity.getCitizenData().getCitizenMournHandler().isMourning()) return false;
        if (entity.getLastHurtByMob() != null && entity.tickCount - entity.getLastHurtByMobTimestamp() < 100) return false;
        if (entity.getTarget() != null && entity.getTarget().isAlive()) return false;
        return level.getEntitiesOfClass(Monster.class, entity.getBoundingBox().inflate(8), m -> m.isAlive()).isEmpty();
    }

    private boolean raid() {
        if (colony.getRaiderManager().isRaided() || activeRaid(colony.getCenter()) || gate != null && activeRaid(gate.getPosition())) return true;
        return colony.getCitizenManager().getCitizens().stream().anyMatch(c -> c.getEntity().map(e -> activeRaid(e.blockPosition())).orElse(false));
    }
    private boolean activeRaid(BlockPos pos) { Raid raid = level.getRaidAt(pos); return raid != null && raid.isActive() && !raid.isStopped(); }
    private boolean areaLoaded() {
        if (gate == null || level.getChunkSource().getChunkNow(gate.getPosition().getX() >> 4, gate.getPosition().getZ() >> 4) == null) return false;
        var corners = gate.getCorners();
        for (int x = corners.getA().getX() >> 4; x <= corners.getB().getX() >> 4; x++)
            for (int z = corners.getA().getZ() >> 4; z <= corners.getB().getZ() >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return true;
    }

    private void recruit(CultRules rules) {
        if (!state.canRecruit()) { recruitmentPaused = true; clearSpeech(); return; }
        if (recruitmentPaused) { attemptedToday.clear(); nextSpeechTick = 0; recruitmentPaused = false; }
        EntityCitizen speaker = leaderEntity();
        if (!operational() || speaker == null || level.getDayTime() % CultState.DAY >= 12600
                || !CultService.working(speaker) || !available(speaker) || raid()) { clearSpeech(); return; }
        if (routeDay != state.day()) { attemptedToday.clear(); routeDay = state.day(); }
        if (level.getGameTime() < nextSpeechTick) return;
        if (target == null) {
            target = colony.getCitizenManager().getCitizens().stream().filter(c -> !c.isChild() && state.canHear(c.getUUID()) && !attemptedToday.contains(c.getUUID()))
                    .map(c -> c.getEntity().orElse(null)).filter(EntityCitizen.class::isInstance).map(EntityCitizen.class::cast)
                    .filter(this::available).filter(c -> !c.getCitizenSleepHandler().isAsleep())
                    .min(Comparator.<EntityCitizen>comparingInt(c -> state.progress(c.getCitizenData().getUUID())).thenComparingDouble(speaker::distanceToSqr)).orElse(null);
            if (target == null) { nextSpeechTick = level.getGameTime() + 100; return; }
            attemptedToday.add(target.getCitizenData().getUUID()); targetSince = level.getGameTime();
        }
        if (!available(target) || level.getGameTime() - targetSince > 800) { clearSpeech(); return; }
        if (speech == null) {
            if (speaker.distanceToSqr(target) > 16 || !speaker.hasLineOfSight(target)) {
                if (level.getGameTime() % 20 == 0) EntityNavigationUtils.walkToPos(speaker, target.blockPosition(), 2, true);
                return;
            }
            speaker.getNavigation().stop(); speech = new ListeningSession(100); speechTicks = 0;
            level.players().stream().filter(p -> p.distanceToSqr(speaker) < 24 * 24).forEach(p -> p.displayClientMessage(Component.translatable("minecolonies_cult.sermon", speaker.getName()), false));
        }
        speaker.getLookControl().setLookAt(target, 30, 30);
        if (speechTicks % 25 == 0) { speaker.swing(InteractionHand.MAIN_HAND); speaker.playSound(SoundEvents.VILLAGER_AMBIENT, .45f, .65f); }
        // Stable native IDs decide the final place when several listeners finish together.
        for (ICitizenData citizen : colony.getCitizenManager().getCitizens().stream().sorted(Comparator.comparingInt(ICitizenData::getId)).toList()) {
            EntityCitizen listener = citizen.getEntity().filter(EntityCitizen.class::isInstance).map(EntityCitizen.class::cast).orElse(null);
            boolean audible = available(listener) && listener != speaker && !listener.getCitizenSleepHandler().isAsleep()
                    && speaker.distanceToSqr(listener) <= Math.pow(rules.radius(gate.getBuildingLevel()), 2) && speaker.hasLineOfSight(listener);
            if (speech.sample(citizen.getUUID(), audible) && state.hear(citizen.getUUID(), state.day(), rules)) {
                CultService.refresh(citizen); data.setDirty();
            }
        }
        if (!state.canRecruit() || ++speechTicks >= 100) { clearSpeech(); nextSpeechTick = level.getGameTime() + 100; }
    }
    private void clearSpeech() { target = null; speech = null; speechTicks = 0; }

    private void beginRitual(long now) {
        clearSpeech();
        List<BlockPos> slots = new ArrayList<>(new HashSet<>(gate.getLocationsFromTag("cult_gather")));
        slots.removeIf(pos -> !safeSlot(pos));
        var leaderSlots = gate.getLocationsFromTag("cult_leader_gather");
        slots.sort(Comparator.<BlockPos>comparingInt(pos -> leaderSlots.contains(pos) ? 0 : 1).thenComparingLong(BlockPos::asLong));
        focus = gate.getLocationsFromTag("cult_focus").stream().findFirst().orElse(gate.getPosition());
        List<EntityCitizen> attendees = colony.getCitizenManager().getCitizens().stream().filter(c -> state.isMember(c.getUUID()))
                .map(c -> c.getEntity().orElse(null)).filter(EntityCitizen.class::isInstance).map(EntityCitizen.class::cast)
                .filter(this::available).sorted(Comparator.comparingInt(c -> c == leaderEntity() ? -1 : c.getCitizenData().getId())).toList();
        if (slots.isEmpty() || attendees.isEmpty() || !attendees.contains(leaderEntity())) return;
        ritualDay = now / CultState.DAY;
        int count = Math.min(Math.min(attendees.size(), slots.size()), CultRules.capacity(gate.getBuildingLevel()) + 1);
        for (int index = 0; index < count; index++) {
            EntityCitizen entity = attendees.get(index);
            participants.put(entity.getCitizenData().getUUID(), new Participant(entity, slots.get(index)));
        }
    }
    private boolean safeSlot(BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    boolean attending(EntityCitizen entity) {
        if (ritualDay < 0 || entity.getCitizenData() == null) return false;
        Participant participant = participants.get(entity.getCitizenData().getUUID());
        if (participant == null) return false;
        if (colony.getRaiderManager().isRaided() || activeRaid(entity.blockPosition()) || !available(entity)) {
            release(participant); participants.remove(entity.getCitizenData().getUUID()); return false;
        }
        return true;
    }
    void participantTick(EntityCitizen entity) {
        if (!attending(entity)) return;
        Participant participant = participants.get(entity.getCitizenData().getUUID());
        if (!participant.awakened) {
            if (entity.getCitizenSleepHandler().isAsleep()) entity.getCitizenSleepHandler().onWakeUp();
            participant.awakened = true;
        }
        if (!safeSlot(participant.slot)) { dropParticipant(participant); return; }
        boolean arrived = entity.distanceToSqr(participant.slot.getX() + .5, participant.slot.getY(), participant.slot.getZ() + .5) < 2.25;
        if (!arrived) {
            if (level.getDayTime() % CultState.DAY >= 18000) { dropParticipant(participant); return; }
            if (level.getGameTime() % 10 == 0) EntityNavigationUtils.walkToPos(entity, participant.slot, 0, true);
            return;
        }
        entity.getNavigation().stop();
        entity.getLookControl().setLookAt(focus.getX() + .5, focus.getY() + 1.5, focus.getZ() + .5);
        if (level.getDayTime() % CultState.DAY >= 18000 && level.getGameTime() % 40 == 0) entity.swing(InteractionHand.MAIN_HAND);
    }
    private void effects() {
        level.sendParticles(new DustParticleOptions(new Vector3f(.65f, .12f, .9f), 1.2f), focus.getX() + .5, focus.getY() + 1.5, focus.getZ() + .5, 18, 1.8, 1.1, 1.8, .01);
        if (level.getGameTime() % 80 == 0) level.playSound(null, focus, SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, .7f, .5f);
        if (level.getGameTime() % 120 == 0) level.playSound(null, focus, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, .6f, .5f);
    }
    private void release(Participant participant) {
        EntityCitizen entity = participant.entity;
        if (entity.isRemoved()) return;
        if (entity.getCitizenAI().getState() == RitualState.RITUAL) {
            entity.getNavigation().stop();
            entity.getCitizenAI().reset();
        }
    }
    private void dropParticipant(Participant participant) {
        if (participant.entity == leaderEntity()) { cancel(); return; }
        release(participant);
        participants.remove(participant.entity.getCitizenData().getUUID());
    }
    void cancel() { participants.values().forEach(this::release); participants.clear(); ritualDay = -1; clearSpeech(); }
}
