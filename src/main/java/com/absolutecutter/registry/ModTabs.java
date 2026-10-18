package com.absolutecutter.registry;

import com.absolutecutter.AbsoluteCutterMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AbsoluteCutterMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + AbsoluteCutterMod.MODID))
            .icon(() -> new ItemStack(ModItems.ABSOLUTE_CUTTER.get()))
            .displayItems((params, output) -> output.accept(ModItems.ABSOLUTE_CUTTER.get()))
            .build());

    private ModTabs() {}
}
