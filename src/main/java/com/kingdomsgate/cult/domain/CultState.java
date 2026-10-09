package com.kingdomsgate.cult.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent colony rules; transient navigation and ceremony participants deliberately live elsewhere. */
public final class CultState {
    public static final long DAY = 24000;
    public record Member(int speeches, long lastHeardDay, boolean joined) {}
    public record Snapshot(long time, long inactiveSince, int level, long nextRitualDay, UUID leader, Map<UUID, Member> citizens) {}
    private final Map<UUID, Member> citizens = new HashMap<>();
    private long time;
    private long inactiveSince = -1;
    private int level;
    private long nextRitualDay = -1;
    private UUID leader;
    // Restored saves cannot recruit until native staffing and the completed level are observed.
    private int activeLevel;

    /** Observe the durable building/staffing state, never interpreting entity unloading as job loss. */
    public void observe(long worldTime, int completedLevel, UUID assignedLeader, CultRules rules) {
        time = Math.max(time, worldTime);
        activeLevel = assignedLeader == null ? 0 : Math.clamp(completedLevel, 0, 5);
        leader = activeLevel > 0 ? assignedLeader : null;
        if (activeLevel > 0) {
            level = Math.clamp(activeLevel, 1, 5);
            inactiveSince = -1;
            if (nextRitualDay < 0) nextRitualDay = day() + rules.ritualDays();
        } else if (level > 0) {
            if (inactiveSince < 0) inactiveSince = time;
            if (time >= graceDeadline(rules)) {
                citizens.clear();
                level = 0;
                inactiveSince = -1;
            }
        }
    }

    /** Credit a complete speech exactly once on each forward-moving colony day. */
    public boolean hear(UUID id, long day, CultRules rules) {
        Member old = citizens.getOrDefault(id, new Member(0, -1, false));
        if (!canRecruit() || isMember(id) || day <= old.lastHeardDay) return false;
        int count = Math.min(rules.speeches(), old.speeches + 1);
        citizens.put(id, new Member(count, day, count >= rules.speeches()));
        return true;
    }

    /** Consume a calendar opportunity before checking availability, preventing deferred or duplicate rituals. */
    public boolean consumeRitual(long worldTime, CultRules rules) {
        if (worldTime < time || nextRitualDay < 0) return false;
        time = worldTime;
        long today = day();
        if (today > nextRitualDay) {
            nextRitualDay += ((today - nextRitualDay + rules.ritualDays() - 1) / rules.ritualDays()) * rules.ritualDays();
        }
        if (today != nextRitualDay || worldTime % DAY < 16000) return false;
        nextRitualDay += rules.ritualDays();
        return worldTime % DAY < 18000;
    }

    public boolean isPermanentMember(UUID id) { return citizens.containsKey(id) && citizens.get(id).joined; }
    public boolean isMember(UUID id) { return id.equals(leader) || isPermanentMember(id); }
    public boolean canHear(UUID id) { return canRecruit() && !isMember(id) && (!citizens.containsKey(id) || citizens.get(id).lastHeardDay < day()); }
    public int progress(UUID id) { return citizens.containsKey(id) ? citizens.get(id).speeches : 0; }
    public int recruitCount() { return (int) citizens.entrySet().stream().filter(e -> e.getValue().joined && !e.getKey().equals(leader)).count(); }
    public int memberCount() { return recruitCount() + (leader == null ? 0 : 1); }
    public int recruitCapacity() { return CultRules.capacity(activeLevel); }
    public boolean canRecruit() { return recruitCapacity() > 0 && recruitCount() < recruitCapacity(); }
    public UUID leader() { return leader; }
    public void remove(UUID id) { citizens.remove(id); if (id.equals(leader)) { leader = null; activeLevel = 0; } }
    public int bonusLevel() { return level; }
    public long day() { return time / DAY; }
    public long time() { return time; }
    public long nextRitualDay() { return nextRitualDay; }
    public long graceDeadline(CultRules rules) { return inactiveSince < 0 ? -1 : inactiveSince + rules.graceDays() * DAY; }
    public Snapshot snapshot() { return new Snapshot(time, inactiveSince, level, nextRitualDay, leader, Map.copyOf(citizens)); }
    public static CultState restore(Snapshot snapshot) {
        CultState state = new CultState();
        state.time = Math.max(0, snapshot.time);
        state.inactiveSince = snapshot.inactiveSince;
        state.level = Math.clamp(snapshot.level, 0, 5);
        state.nextRitualDay = snapshot.nextRitualDay;
        state.leader = snapshot.leader;
        state.citizens.putAll(snapshot.citizens);
        return state;
    }
}
