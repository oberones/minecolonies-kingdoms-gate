package com.kingdomsgate.cult.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Counts consecutive audible server ticks separately for each listener within one speech. */
public final class ListeningSession {
    private final int requiredTicks;
    private final Map<UUID, Integer> ticks = new HashMap<>();
    private final Set<UUID> credited = new HashSet<>();
    public ListeningSession(int requiredTicks) { this.requiredTicks = requiredTicks; }
    public boolean sample(UUID listener, boolean audible) {
        if (!audible) { ticks.remove(listener); return false; }
        if (credited.contains(listener)) return false;
        if (ticks.merge(listener, 1, Integer::sum) < requiredTicks) return false;
        credited.add(listener);
        return true;
    }
}
