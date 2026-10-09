package com.kingdomsgate.cult.runtime;

import com.kingdomsgate.cult.domain.CultState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** One versioned file per dimension survives hut destruction and entity unloading. */
public final class CultSavedData extends SavedData {
    private final Map<Integer, CultState> colonies = new HashMap<>();
    public static CultSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(CultSavedData::new, CultSavedData::load, null), "minecolonies_cult");
    }
    public CultState colony(int id) { return colonies.computeIfAbsent(id, ignored -> { setDirty(); return new CultState(); }); }
    public void removeColony(int id) { if (colonies.remove(id) != null) setDirty(); }
    /** Version one memberships are all permanent; their original source cannot be inferred. */
    public static CultSavedData load(CompoundTag root, HolderLookup.Provider registries) {
        int version = root.getInt("version");
        if (version != 1 && version != 2) throw new IllegalStateException("Unsupported Kingdom's Gate save version; retain the world backup.");
        CultSavedData data = new CultSavedData();
        for (var value : root.getList("colonies", 10)) {
            CompoundTag colony = (CompoundTag) value;
            Map<UUID, CultState.Member> members = new HashMap<>();
            for (var entry : colony.getList("citizens", 10)) {
                CompoundTag citizen = (CompoundTag) entry;
                members.put(citizen.getUUID("id"), new CultState.Member(Math.max(0, citizen.getInt("speeches")), citizen.getLong("lastDay"), citizen.getBoolean("member")));
            }
            data.colonies.put(colony.getInt("id"), CultState.restore(new CultState.Snapshot(colony.getLong("time"), colony.getLong("inactiveSince"), colony.getInt("level"), colony.getLong("nextRitualDay"), version >= 2 && colony.hasUUID("leader") ? colony.getUUID("leader") : null, members)));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        root.putInt("version", 2);
        ListTag list = new ListTag();
        colonies.forEach((id, state) -> {
            var snapshot = state.snapshot();
            CompoundTag colony = new CompoundTag();
            colony.putInt("id", id); colony.putLong("time", snapshot.time()); colony.putLong("inactiveSince", snapshot.inactiveSince());
            colony.putInt("level", snapshot.level()); colony.putLong("nextRitualDay", snapshot.nextRitualDay());
            if (snapshot.leader() != null) colony.putUUID("leader", snapshot.leader());
            ListTag citizens = new ListTag();
            snapshot.citizens().forEach((uuid, member) -> {
                CompoundTag citizen = new CompoundTag(); citizen.putUUID("id", uuid);
                citizen.putInt("speeches", member.speeches()); citizen.putLong("lastDay", member.lastHeardDay()); citizen.putBoolean("member", member.joined());
                citizens.add(citizen);
            });
            colony.put("citizens", citizens); list.add(colony);
        });
        root.put("colonies", list);
        return root;
    }
}
