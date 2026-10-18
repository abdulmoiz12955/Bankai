package com.absolutecutter.client;

import com.absolutecutter.AbsoluteCutterMod;
import com.absolutecutter.registry.ModEntities;
import com.absolutecutter.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbsoluteCutterMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GETSUGA.get(), GetsugaRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SILVER_MOTE.get(), s -> new GlowParticle.Provider(s, 0.88F, 0.94F, 1.0F, 0.14F, 24));
        event.registerSpriteSet(ModParticles.GETSUGA_EMBER.get(), s -> new GlowParticle.Provider(s, 1.0F, 0.82F, 0.25F, 0.16F, 20));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientKeys.TERRAIN_MODE);
    }
}
