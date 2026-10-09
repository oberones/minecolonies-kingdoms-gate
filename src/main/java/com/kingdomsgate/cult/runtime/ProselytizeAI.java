package com.kingdomsgate.cult.runtime;

import com.kingdomsgate.cult.building.CultLeaderJob;
import com.kingdomsgate.cult.building.KingdomsGateBuilding;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** MineColonies owns the working-hours decision; the coordinator samples hearing on every server tick. */
public final class ProselytizeAI extends AbstractEntityAIBasic<CultLeaderJob, KingdomsGateBuilding> {
    private long lastWorkTick = Long.MIN_VALUE;
    public ProselytizeAI(CultLeaderJob job) {
        super(job);
        registerTargets(
                new AITarget(AIWorkerState.IDLE, () -> AIWorkerState.START_WORKING, 1),
                new AITarget(AIWorkerState.START_WORKING, () -> AIWorkerState.START_WORKING, 1));
    }
    @Override public Class<KingdomsGateBuilding> getExpectedBuildingClass() { return KingdomsGateBuilding.class; }
    /** Run native request waiting/pickup before allowing the coordinator to own movement. */
    @Override public void tick() {
        lastWorkTick = Long.MIN_VALUE;
        super.tick();
        if (getState() == AIWorkerState.IDLE || getState() == AIWorkerState.START_WORKING) {
            ensureBook();
            if (hasBook()) lastWorkTick = world.getGameTime();
        }
    }

    /** Native helper checks carried/hut stock and deduplicates open and completed requests. */
    public void ensureBook() { checkIfRequestForItemExistOrCreate(new ItemStack(Items.BOOK)); }

    public boolean hasBook() { return bookSlot() >= 0; }

    /** Called only after a new permanent conversion, once per listener. */
    public void consumeBook() {
        int slot = bookSlot();
        if (slot < 0) throw new IllegalStateException("Conversion requires a carried book");
        getInventory().extractItem(slot, 1, false);
        ensureBook();
    }

    private int bookSlot() {
        for (int slot = 0; slot < getInventory().getSlots(); slot++)
            if (getInventory().getStackInSlot(slot).is(Items.BOOK)) return slot;
        return -1;
    }
    public boolean isWorking(long tick) { return lastWorkTick != Long.MIN_VALUE && tick - lastWorkTick <= 5; }
    @Override public void resetAI() { super.resetAI(); lastWorkTick = Long.MIN_VALUE; }
    @Override public void onRemoval() { lastWorkTick = Long.MIN_VALUE; super.onRemoval(); }
}
