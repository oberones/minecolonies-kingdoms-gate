package com.kingdomsgate.cult.domain;

/** Immutable balance settings shared by the server integration and deterministic rules. */
public record CultRules(int speeches, int graceDays, int ritualDays, int baseRadius,
                        int radiusPerLevel, double baseFactor, double factorPerLevel) {
    public static CultRules defaults() { return new CultRules(6, 7, 7, 8, 2, 1.25, .25); }
    public CultRules {
        if (speeches < 1 || graceDays < 1 || ritualDays < 1 || baseRadius < 1 || radiusPerLevel < 0
                || !Double.isFinite(baseFactor) || baseFactor <= 1 || !Double.isFinite(factorPerLevel) || factorPerLevel < 0)
            throw new IllegalArgumentException("Invalid cult balance settings");
    }
    public int radius(int level) { return baseRadius + radiusPerLevel * (Math.clamp(level, 1, 5) - 1); }
    public double factor(int level) { return level <= 0 ? 1 : baseFactor + factorPerLevel * (Math.clamp(level, 1, 5) - 1); }
}
