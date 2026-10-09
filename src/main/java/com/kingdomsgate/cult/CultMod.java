package com.kingdomsgate.cult;

import com.kingdomsgate.cult.runtime.CultService;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CultMod.ID)
public final class CultMod {
    public static final String ID = "minecolonies_cult";
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    public CultMod(IEventBus bus, ModContainer container) {
        CultRegistry.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, CultConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(CultService::tick);
        NeoForge.EVENT_BUS.addListener(CultService::unload);
    }
}
