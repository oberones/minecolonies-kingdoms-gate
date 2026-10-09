package com.kingdomsgate.cult.client;

import com.kingdomsgate.cult.CultMod;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.network.chat.Component;

public final class CultStatusWindow extends AbstractModuleWindow<CultStatusView> {
    public CultStatusWindow(CultStatusView view) { super(view, CultMod.id("gui/cult_status.xml")); }
    @Override public void onOpened() {
        super.onOpened();
        findPaneOfTypeByID("citizens", ScrollingList.class).setDataProvider(() -> moduleView.citizens.size(), (index, row) -> {
            var citizen = moduleView.citizens.get(index);
            row.findPaneOfTypeByID("name", Text.class).setText(Component.literal(citizen.name()));
            row.findPaneOfTypeByID("faith", Text.class).setText(citizen.member() ? Component.translatable("minecolonies_cult.status.member")
                    : Component.translatable("minecolonies_cult.status.progress", citizen.speeches(), moduleView.required));
        });
        updateLabels();
    }
    @Override public void onUpdate() { super.onUpdate(); updateLabels(); }
    private void updateLabels() {
        findPaneOfTypeByID("members", Text.class).setText(Component.translatable("minecolonies_cult.status.members", moduleView.members));
        findPaneOfTypeByID("next", Text.class).setText(moduleView.nextDay < 0 ? Component.translatable("minecolonies_cult.status.not_started")
                : Component.translatable("minecolonies_cult.status.next", Math.max(0, moduleView.nextDay - moduleView.today)));
        findPaneOfTypeByID("condition", Text.class).setText(moduleView.grace >= 0
                ? Component.translatable("minecolonies_cult.status.grace", (moduleView.grace + 23999) / 24000)
                : Component.translatable(moduleView.operational ? "minecolonies_cult.status.active" : "minecolonies_cult.status.inactive"));
    }
}
