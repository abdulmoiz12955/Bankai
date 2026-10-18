package com.absolutecutter.registry;

import com.absolutecutter.AbsoluteCutterMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, AbsoluteCutterMod.MODID);

    public static final RegistryObject<SimpleParticleType> SILVER_MOTE = PARTICLES.register("silver_mote", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GETSUGA_EMBER = PARTICLES.register("getsuga_ember", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
