package com.kingdomsgate.cult;

import com.kingdomsgate.cult.domain.CultRules;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class CultConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue SPEECHES = BUILDER.comment("Distinct days of complete speeches required to join.").defineInRange("speechesRequired", 6, 1, 100);
    public static final ModConfigSpec.IntValue GRACE = BUILDER.defineInRange("faithGraceDays", 7, 1, 365);
    public static final ModConfigSpec.IntValue INTERVAL = BUILDER.defineInRange("ritualIntervalDays", 7, 1, 365);
    public static final ModConfigSpec.IntValue RADIUS = BUILDER.defineInRange("baseHearingRadius", 8, 1, 32);
    public static final ModConfigSpec.IntValue RADIUS_STEP = BUILDER.defineInRange("hearingRadiusPerLevel", 2, 0, 8);
    public static final ModConfigSpec.DoubleValue FACTOR = BUILDER.comment("Native happiness factor, with weight 1; not flat happiness points.").defineInRange("baseHappinessFactor", 1.25, 1.01, 5);
    public static final ModConfigSpec.DoubleValue FACTOR_STEP = BUILDER.defineInRange("happinessFactorPerLevel", .25, 0, 1);
    public static final ModConfigSpec SPEC = BUILDER.build();
    public static CultRules rules() { return new CultRules(SPEECHES.get(), GRACE.get(), INTERVAL.get(), RADIUS.get(), RADIUS_STEP.get(), FACTOR.get(), FACTOR_STEP.get()); }
    private CultConfig() {}
}
