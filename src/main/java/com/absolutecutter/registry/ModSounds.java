package com.absolutecutter.registry;

import com.absolutecutter.AbsoluteCutterMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AbsoluteCutterMod.MODID);

    public static final RegistryObject<SoundEvent> SWORD_HUM = reg("sword_hum");
    public static final RegistryObject<SoundEvent> SLASH = reg("slash");
    public static final RegistryObject<SoundEvent> CHARGE = reg("charge");
    public static final RegistryObject<SoundEvent> CHARGE_COMPLETE = reg("charge_complete");
    public static final RegistryObject<SoundEvent> GETSUGA_LAUNCH = reg("getsuga_launch");
    public static final RegistryObject<SoundEvent> GETSUGA_TRAVEL = reg("getsuga_travel");
    public static final RegistryObject<SoundEvent> GETSUGA_IMPACT = reg("getsuga_impact");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbsoluteCutterMod.MODID, name)));
    }

    private ModSounds() {}
}
