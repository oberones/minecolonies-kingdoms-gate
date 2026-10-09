package com.kingdomsgate.cult.client;

import com.kingdomsgate.cult.CultMod;
import com.kingdomsgate.cult.building.CultStatusData;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CultStatusView extends AbstractBuildingModuleView {
    CultStatusData status = new CultStatusData(0, 0, 6, -1, 0, -1, false, "", List.of());
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) {
        status = CultStatusData.read(buffer);
    }
    @Override public BOWindow getWindow() { return new CultStatusWindow(this); }
    @Override public Component getDesc() { return Component.translatable("minecolonies_cult.status.title"); }
    @Override public ResourceLocation getIconResourceLocation() { return CultMod.id("textures/gui/cult_icon.png"); }
}
