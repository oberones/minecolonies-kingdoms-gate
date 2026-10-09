package com.kingdomsgate.cult.mixin;

import com.kingdomsgate.cult.runtime.CultService;
import com.kingdomsgate.cult.runtime.RitualState;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact-version bridge: keep MineColonies' state machine and last-state bookkeeping in control. */
@Mixin(value = CitizenAI.class, remap = false)
public abstract class CitizenAIMixin {
    @Shadow @Final private EntityCitizen citizen;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void cult$register(EntityCitizen citizen, CallbackInfo callback) {
        citizen.getCitizenAI().addTransition(new AITarget<IState>(RitualState.RITUAL, () -> true, () -> {
            CultService.participantTick(citizen); return null;
        }, 1));
    }
    @Inject(method = "calculateNextState", at = @At("HEAD"), cancellable = true)
    private void cult$choose(CallbackInfoReturnable<IState> callback) {
        if (CultService.attending(citizen)) callback.setReturnValue(RitualState.RITUAL);
    }
}
