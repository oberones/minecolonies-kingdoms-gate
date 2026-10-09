package com.kingdomsgate.cult.client;

import com.kingdomsgate.cult.CultMod;
import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.client.render.modeltype.IModelType;
import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CultMod.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CultClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> IModelTypeRegistry.getInstance().register(new IModelType() {
            @Override public ResourceLocation getName() { return CultMod.id("cult_leader"); }
            @Override public ResourceLocation getTexture(AbstractEntityCitizen citizen) { return CultMod.id("textures/entity/cult_leader.png"); }
            @Override public CitizenModel<AbstractEntityCitizen> getMaleModel() { return ModModelTypes.CITIZEN.getMaleModel(); }
            @Override public CitizenModel<AbstractEntityCitizen> getFemaleModel() { return ModModelTypes.CITIZEN.getFemaleModel(); }
        }));
    }
    private CultClient() {}
}
