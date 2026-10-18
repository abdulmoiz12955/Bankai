package com.absolutecutter;

import com.absolutecutter.config.CutterConfig;
import com.absolutecutter.network.ModNetwork;
import com.absolutecutter.registry.ModEntities;
import com.absolutecutter.registry.ModItems;
import com.absolutecutter.registry.ModParticles;
import com.absolutecutter.registry.ModSounds;
import com.absolutecutter.registry.ModTabs;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AbsoluteCutterMod.MODID)
public class AbsoluteCutterMod {
    public static final String MODID = "absolute_cutter";

    public AbsoluteCutterMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModTabs.TABS.register(bus);

        bus.addListener(this::commonSetup);
        bus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CutterConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CutterConfig.CLIENT_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.ABSOLUTE_CUTTER);
        }
    }
}
