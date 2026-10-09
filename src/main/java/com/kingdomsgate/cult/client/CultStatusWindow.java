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
        findPaneOfTypeByID("citizens", ScrollingList.class).setDataProvider(() -> moduleView.status.citizens().size(), (index, row) -> {
            var citizen = moduleView.status.citizens().get(index);
            row.findPaneOfTypeByID("name", Text.class).setText(Component.literal(citizen.name()));
            row.findPaneOfTypeByID("faith", Text.class).setText(citizen.leader() ? Component.translatable("minecolonies_cult.status.leader_role")
                    : citizen.member() ? Component.translatable("minecolonies_cult.status.member")
                    : Component.translatable("minecolonies_cult.status.progress", citizen.speeches(), moduleView.status.required()));
        });
        updateLabels();
    }
    @Override public void onUpdate() { super.onUpdate(); updateLabels(); }
    private void updateLabels() {
        var status = moduleView.status;
        findPaneOfTypeByID("members", Text.class).setText(Component.translatable("minecolonies_cult.status.members", status.recruits(), status.capacity()));
        findPaneOfTypeByID("leader", Text.class).setText(status.leader().isEmpty() ? Component.translatable("minecolonies_cult.status.no_leader")
                : Component.translatable("minecolonies_cult.status.leader", status.leader()));
        findPaneOfTypeByID("next", Text.class).setText(status.nextDay() < 0 ? Component.translatable("minecolonies_cult.status.not_started")
                : Component.translatable("minecolonies_cult.status.next", Math.max(0, status.nextDay() - status.today())));
        findPaneOfTypeByID("condition", Text.class).setText(Component.translatable("minecolonies_cult.status." + status.conditionKey(),
                (status.grace() + 23999) / 24000));
    }
}
