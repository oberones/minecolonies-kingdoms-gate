package com.kingdomsgate.cult.domain;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CultStateTest {
    private final UUID citizen = UUID.randomUUID();
    private final UUID leader = UUID.randomUUID();
    private final CultRules rules = CultRules.defaults();

    @Test void conversionRequiresSixDistinctDaysAndDoesNotRepeat() {
        CultState state = new CultState();
        state.observe(0, 1, leader, rules);
        for (int day = 0; day < 5; day++) {
            assertTrue(state.hear(citizen, day, rules));
            assertFalse(state.hear(citizen, day, rules));
            assertFalse(state.isMember(citizen));
        }
        assertTrue(state.hear(citizen, 5, rules));
        assertTrue(state.isMember(citizen));
        assertFalse(state.hear(citizen, 6, rules));
        assertFalse(state.hear(citizen, 3, rules));
    }

    @Test void faithExpiresOnlyAfterFullGraceAndRestorationCancelsExpiry() {
        CultState state = new CultState();
        state.observe(100, 3, leader, rules);
        for (int day = 0; day < 6; day++) state.hear(citizen, day, rules);
        state.observe(200, 0, leader, rules);
        state.observe(200 + 7 * 24000 - 1, 0, leader, rules);
        assertTrue(state.isMember(citizen));
        assertEquals(3, state.bonusLevel());
        state.observe(200 + 7 * 24000, 0, leader, rules);
        assertFalse(state.isMember(citizen));
        assertEquals(0, state.progress(citizen));
        state.observe(200000, 1, leader, rules);
        for (int day = 0; day < 6; day++) state.hear(citizen, day, rules);
        state.observe(201000, 0, leader, rules);
        state.observe(202000, 5, leader, rules);
        state.observe(500000, 5, leader, rules);
        assertTrue(state.isMember(citizen));
        assertEquals(-1, state.graceDeadline(rules));
        assertEquals(5, state.bonusLevel());
    }

    @Test void ritualIsConsumedBeforeStartAndMissedWeeksNeverReplay() {
        CultState state = new CultState();
        state.observe(0, 1, leader, rules);
        assertEquals(7, state.nextRitualDay());
        assertFalse(state.consumeRitual(7 * 24000 + 15999, rules));
        assertTrue(state.consumeRitual(7 * 24000 + 16000, rules));
        assertFalse(state.consumeRitual(7 * 24000 + 17000, rules));
        assertFalse(state.consumeRitual(14 * 24000 + 19000, rules));
        assertEquals(21, state.nextRitualDay());
        assertFalse(state.consumeRitual(40 * 24000, rules));
        assertEquals(42, state.nextRitualDay());
    }

    @Test void backwardsTimeDoesNotRepeatRecruitmentOrExtendGrace() {
        CultState state = new CultState();
        state.observe(240000, 1, leader, rules);
        state.hear(citizen, state.day(), rules);
        state.observe(240100, 0, leader, rules);
        long deadline = state.graceDeadline(rules);
        state.observe(0, 0, leader, rules);
        assertEquals(10, state.day());
        assertFalse(state.hear(citizen, state.day(), rules));
        assertEquals(deadline, state.graceDeadline(rules));
    }

    @Test void snapshotRestoresMembershipGraceAndConsumedSchedule() {
        CultState original = new CultState();
        original.observe(0, 4, leader, rules);
        for (int day = 0; day < 6; day++) original.hear(citizen, day, rules);
        original.consumeRitual(7 * 24000 + 16000, rules);
        original.observe(7 * 24000 + 17000, 0, leader, rules);
        CultState restored = CultState.restore(original.snapshot());
        assertTrue(restored.isMember(citizen));
        assertFalse(restored.consumeRitual(7 * 24000 + 17500, rules));
        assertEquals(original.graceDeadline(rules), restored.graceDeadline(rules));
        assertEquals(4, restored.bonusLevel());
        restored.remove(citizen);
        assertFalse(restored.isMember(citizen));
        assertTrue(original.isMember(citizen));
    }

    @Test void listeningMustBeContinuousAndListenerSpecific() {
        ListeningSession session = new ListeningSession(100);
        UUID other = UUID.randomUUID();
        for (int i = 0; i < 99; i++) assertFalse(session.sample(citizen, true));
        assertFalse(session.sample(other, true));
        assertFalse(session.sample(citizen, false));
        for (int i = 0; i < 99; i++) assertFalse(session.sample(citizen, true));
        assertTrue(session.sample(citizen, true));
        assertFalse(session.sample(citizen, true));
    }

    @Test void defaultsScaleReachAndBonusWithoutChangingThreshold() {
        assertEquals(6, rules.speeches());
        assertEquals(8, rules.radius(1));
        assertEquals(16, rules.radius(5));
        assertEquals(1.25, rules.factor(1));
        assertEquals(2.25, rules.factor(5));
        assertEquals(1, rules.factor(0));
    }
}
