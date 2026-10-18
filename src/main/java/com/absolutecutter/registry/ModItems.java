package com.absolutecutter.registry;

import com.absolutecutter.AbsoluteCutterMod;
import com.absolutecutter.item.AbsoluteCutterItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AbsoluteCutterMod.MODID);

    public static final RegistryObject<Item> ABSOLUTE_CUTTER = ITEMS.register("absolute_cutter",
            () -> new AbsoluteCutterItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    private ModItems() {}
}
