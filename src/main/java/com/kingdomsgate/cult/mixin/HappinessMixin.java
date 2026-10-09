package com.kingdomsgate.cult.mixin;

import com.kingdomsgate.cult.runtime.CultService;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import com.minecolonies.api.colony.ICitizenData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Install before persisted/client modifiers are read; the pinned handler otherwise filters unknown IDs. */
@Mixin(value = CitizenHappinessHandler.class, remap = false)
public abstract class HappinessMixin {
    @Inject(method = "<init>()V", at = @At("RETURN"))
    private void cult$initializeClient(CallbackInfo callback) {
        ((CitizenHappinessHandler)(Object)this).addModifier(CultService.modifier());
    }
    @Inject(method = "<init>(Lcom/minecolonies/api/colony/ICitizenData;)V", at = @At("RETURN"))
    private void cult$initializeServer(ICitizenData data, CallbackInfo callback) {
        ((CitizenHappinessHandler)(Object)this).addModifier(CultService.modifier());
    }
}
