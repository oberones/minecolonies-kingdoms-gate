package com.kingdomsgate.cult.domain;

import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CultCapacityTest {
    private final CultRules rules = CultRules.defaults();
    private final UUID leader = new UUID(0, 100);

    private CultState active(int level) {
        CultState state = new CultState();
        state.observe(0, level, leader, rules);
        return state;
    }

    private void recruit(CultState state, UUID citizen) {
        for (int day = 0; day < rules.speeches(); day++) assertTrue(state.hear(citizen, day, rules));
    }

    @Test void eachLevelHasItsOwnRecruitLimitAndOneSeparateLeader() {
        int[] limits = {4, 8, 16, 24, 32};
        assertEquals(0, CultRules.capacity(0));
        for (int level = 1; level <= 5; level++) {
            CultState state = active(level);
            assertEquals(limits[level - 1], state.recruitCapacity());
            for (int i = 0; i < limits[level - 1]; i++) recruit(state, new UUID(0, i));
            assertEquals(limits[level - 1], state.recruitCount());
            assertEquals(limits[level - 1] + 1, state.memberCount());
            assertFalse(state.canRecruit());
            assertFalse(state.hear(new UUID(0, 999), 20, rules));
            assertEquals(0, state.progress(leader));
        }
    }

    @Test void finalPlaceIsCheckedForEveryListenerAndBlockedProgressIsUntouched() {
        CultState state = active(1);
        for (int i = 0; i < 3; i++) recruit(state, new UUID(0, i));
        UUID first = new UUID(0, 10), second = new UUID(0, 11);
        for (int day = 0; day < 5; day++) {
            assertTrue(state.hear(first, day, rules));
            assertTrue(state.hear(second, day, rules));
        }
        assertTrue(state.hear(first, 5, rules));
        var before = state.snapshot();
        assertFalse(state.hear(second, 5, rules));
        assertFalse(state.canHear(second));
        assertEquals(before, state.snapshot());
        state.remove(first);
        assertTrue(state.hear(second, 5, rules));
        assertEquals(4, state.recruitCount());
    }

    @Test void upgradeResumesPartialProgressWithoutAutoConversion() {
        CultState state = active(1);
        UUID waiting = new UUID(0, 10);
        assertTrue(state.hear(waiting, 0, rules));
        for (int i = 0; i < 4; i++) recruit(state, new UUID(0, i));
        assertFalse(state.hear(waiting, 1, rules));
        state.observe(CultState.DAY, 2, leader, rules);
        assertEquals(8, state.recruitCapacity());
        assertEquals(1, state.progress(waiting));
        assertTrue(state.hear(waiting, 1, rules));
        assertFalse(state.isMember(waiting));
    }

    @Test void leaderRoleDoesNotCreatePermanentMembershipOrConsumePartialProgress() {
        CultState state = active(1);
        UUID next = new UUID(0, 11);
        assertTrue(state.hear(next, 0, rules));
        state.observe(100, 1, next, rules);
        assertFalse(state.isMember(leader));
        assertTrue(state.isMember(next));
        assertEquals(1, state.progress(next));
        assertFalse(state.hear(next, 1, rules));
        state.observe(200, 0, null, rules);
        assertFalse(state.isMember(next));
        assertEquals(1, state.progress(next));
        assertFalse(state.canRecruit());
        state.observe(CultState.DAY, 1, leader, rules);
        assertTrue(state.hear(next, 1, rules));
    }

    @Test void recruitedLeaderIsCountedOnceAndRetainedWhenSteppingDownOverCapacity() {
        CultState state = active(1);
        UUID promoted = new UUID(0, 0);
        for (int i = 0; i < 4; i++) recruit(state, new UUID(0, i));
        state.observe(10, 1, promoted, rules);
        assertEquals(3, state.recruitCount());
        assertEquals(4, state.memberCount());
        recruit(state, new UUID(0, 4));
        state.observe(20, 1, leader, rules);
        assertEquals(5, state.recruitCount());
        assertEquals(6, state.memberCount());
        assertTrue(state.isMember(promoted));
        assertFalse(state.canRecruit());
    }

    @Test void smallerGatePreservesMembersAndRestoredStateWaitsForStaffingReconciliation() {
        CultState state = active(2);
        for (int i = 0; i < 8; i++) recruit(state, new UUID(0, i));
        CultState restored = CultState.restore(state.snapshot());
        assertEquals(leader, restored.leader());
        assertFalse(restored.canRecruit());
        restored.observe(100, 1, leader, rules);
        assertEquals(8, restored.recruitCount());
        assertFalse(restored.canRecruit());
        restored.observe(200, 0, null, rules);
        assertEquals(8, restored.memberCount());
        assertEquals(1, restored.bonusLevel());
        restored.observe(200 + 7 * CultState.DAY, 0, null, rules);
        assertEquals(0, restored.memberCount());
    }

    @Test void legacyMembershipsArePreservedEvenWithoutAnAssignedLeader() {
        var members = new HashMap<UUID, CultState.Member>();
        for (int i = 0; i < 10; i++) members.put(new UUID(0, i), new CultState.Member(6, 5, true));
        CultState state = CultState.restore(new CultState.Snapshot(100, -1, 1, 7, null, members));
        state.observe(100, 1, leader, rules);
        assertEquals(10, state.recruitCount());
        assertFalse(state.canRecruit());
        assertEquals(members, state.snapshot().citizens());
    }
}
