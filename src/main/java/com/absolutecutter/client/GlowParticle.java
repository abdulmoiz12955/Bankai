package com.absolutecutter.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Soft glowing mote used for the silver aura (silver) and Getsuga embers (gold). */
public class GlowParticle extends TextureSheetParticle {
    private final float startSize;

    protected GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                           SpriteSet sprites, float r, float g, float b, float size, int life) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.92F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = life + this.random.nextInt(Math.max(1, life / 2));
        this.quadSize = size * (0.8F + this.random.nextFloat() * 0.5F);
        this.startSize = this.quadSize;
        this.setColor(r, g, b);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float f = (float) this.age / (float) this.lifetime;
        this.alpha = Math.max(0.0F, 1.0F - f * f);
        this.quadSize = this.startSize * (1.0F - 0.4F * f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;    // fullbright
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float r, g, b, size;
        private final int life;

        public Provider(SpriteSet sprites, float r, float g, float b, float size, int life) {
            this.sprites = sprites;
            this.r = r;
            this.g = g;
            this.b = b;
            this.size = size;
            this.life = life;
        }

        @Nullable
        @Override
        public net.minecraft.client.particle.Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                                     double vx, double vy, double vz) {
            return new GlowParticle(level, x, y, z, vx, vy, vz, sprites, r, g, b, size, life);
        }
    }
}
