package com.kingdomsgate.cult.client;

import com.kingdomsgate.cult.CultMod;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CultStatusView extends AbstractBuildingModuleView {
    public record Citizen(String name, boolean member, int speeches) {}
    int members, required;
    long nextDay, today, grace;
    boolean operational;
    final List<Citizen> citizens = new ArrayList<>();
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) {
        members = buffer.readInt(); required = buffer.readInt(); nextDay = buffer.readLong(); today = buffer.readLong();
        grace = buffer.readLong(); operational = buffer.readBoolean(); citizens.clear();
        int count = buffer.readVarInt();
        if (count < 0 || count > 10000) throw new IllegalArgumentException("Invalid cult citizen count");
        for (int i = 0; i < count; i++) citizens.add(new Citizen(buffer.readUtf(), buffer.readBoolean(), buffer.readInt()));
    }
    @Override public BOWindow getWindow() { return new CultStatusWindow(this); }
    @Override public Component getDesc() { return Component.translatable("minecolonies_cult.status.title"); }
    @Override public ResourceLocation getIconResourceLocation() { return CultMod.id("textures/gui/cult_icon.png"); }
}
