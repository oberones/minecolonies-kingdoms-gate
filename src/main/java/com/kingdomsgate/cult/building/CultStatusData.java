package com.kingdomsgate.cult.building;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Shared status payload keeps the server writer and client reader on the same contract. */
public record CultStatusData(int recruits, int capacity, int required, long nextDay, long today,
                             long grace, boolean operational, String leader, List<Citizen> citizens) {
    public record Citizen(String name, boolean member, boolean leader, int speeches) {}
    public CultStatusData { citizens = List.copyOf(citizens); }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(recruits); buffer.writeInt(capacity); buffer.writeInt(required);
        buffer.writeLong(nextDay); buffer.writeLong(today); buffer.writeLong(grace);
        buffer.writeBoolean(operational); buffer.writeUtf(leader); buffer.writeVarInt(citizens.size());
        for (var citizen : citizens) {
            buffer.writeUtf(citizen.name()); buffer.writeBoolean(citizen.member());
            buffer.writeBoolean(citizen.leader()); buffer.writeInt(citizen.speeches());
        }
    }

    public static CultStatusData read(RegistryFriendlyByteBuf buffer) {
        int recruits = buffer.readInt(), capacity = buffer.readInt(), required = buffer.readInt();
        long next = buffer.readLong(), today = buffer.readLong(), grace = buffer.readLong();
        boolean operational = buffer.readBoolean();
        String leader = buffer.readUtf();
        int count = buffer.readVarInt();
        if (count < 0 || count > 10000) throw new IllegalArgumentException("Invalid cult citizen count");
        List<Citizen> citizens = new ArrayList<>();
        for (int i = 0; i < count; i++)
            citizens.add(new Citizen(buffer.readUtf(), buffer.readBoolean(), buffer.readBoolean(), buffer.readInt()));
        return new CultStatusData(recruits, capacity, required, next, today, grace, operational, leader, citizens);
    }

    /** Capacity never hides an inactive Gate or its faith countdown. */
    public String conditionKey() {
        if (grace >= 0) return "grace";
        if (!operational) return "inactive";
        if (recruits > capacity) return "over_capacity";
        return recruits == capacity ? "full" : "active";
    }
}
