package com.absolutecutter.combat;

import com.absolutecutter.config.CutterConfig;
import com.absolutecutter.registry.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Client-side particle effects for the sword (aura + charge). Only calls Level#addParticle, which is a no-op on a
 * server level, so it is safe to reference from common code. Positions are approximations of where the blade is.
 */
public final class AuraFx {
    public static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.03F, 0.03F, 0.05F), 1.2F);
    public static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.18F), 1.0F);

    private AuraFx() {}

    /** Point along the blade, t = 0 (guard) .. 1 (tip). */
    public static Vec3 bladePoint(LivingEntity e, double t) {
        float yaw = e.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        double side = e.getMainArm() == HumanoidArm.RIGHT ? 1.0D : -1.0D;
        Vec3 hand = new Vec3(e.getX() + fx * 0.45 + rx * 0.4 * side,
                e.getY() + e.getBbHeight() * 0.58,
                e.getZ() + fz * 0.45 + rz * 0.4 * side);
        Vec3 dir = e.getLookAngle().scale(0.75).add(0.0, 0.65, 0.0).normalize();
        return hand.add(dir.scale(0.3 + t * 1.3));
    }

    private static double rnd(RandomSource r) {
        return (r.nextDouble() - 0.5D) * 2.0D;
    }

    /** Called every client tick for every player holding the sword. */
    public static void idle(Level level, LivingEntity e) {
        if (!CutterConfig.CLIENT.idleAura.get()) return;
        double d = CutterConfig.CLIENT.auraDensity.get();
        RandomSource r = level.random;

        if (r.nextDouble() < 0.55D * d) {
            Vec3 p = bladePoint(e, r.nextDouble());
            level.addParticle(ModParticles.SILVER_MOTE.get(), p.x + rnd(r) * 0.05, p.y, p.z + rnd(r) * 0.05,
                    rnd(r) * 0.004, 0.012 + r.nextDouble() * 0.014, rnd(r) * 0.004);
        }
        if (r.nextDouble() < 0.10D * d) {                       // tiny black motes mixed in
            Vec3 p = bladePoint(e, r.nextDouble());
            level.addParticle(BLACK_DUST, p.x, p.y, p.z, rnd(r) * 0.004, 0.01, rnd(r) * 0.004);
        }
        if (r.nextDouble() < 0.18D * d) {                       // faint shimmer at the edge
            Vec3 p = bladePoint(e, r.nextDouble());
            level.addParticle(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 0.0, 0.0, 0.0);
        }
        if (e.tickCount % 70 == 0) {                            // brighter pulse every few seconds
            for (int i = 0; i < (int) Math.ceil(10 * d); i++) {
                Vec3 p = bladePoint(e, r.nextDouble());
                level.addParticle(ModParticles.SILVER_MOTE.get(), p.x, p.y, p.z, rnd(r) * 0.03, 0.02 + r.nextDouble() * 0.03, rnd(r) * 0.03);
            }
        }
        if (e.isSprinting() && r.nextDouble() < 0.8D * d) {
            Vec3 p = bladePoint(e, r.nextDouble());
            level.addParticle(ModParticles.SILVER_MOTE.get(), p.x, p.y, p.z, rnd(r) * 0.02, 0.01, rnd(r) * 0.02);
        }
        if (e.swinging && r.nextDouble() < 0.9D) {
            for (int i = 0; i < Math.max(1, (int) Math.round(3 * d)); i++) {
                Vec3 p = bladePoint(e, r.nextDouble());
                level.addParticle(ModParticles.SILVER_MOTE.get(), p.x, p.y, p.z, rnd(r) * 0.05, rnd(r) * 0.03, rnd(r) * 0.05);
            }
        }
    }

    /** Called every tick while charging (both sides tick it; only the client branch spawns particles). */
    public static void charge(Level level, LivingEntity e, int ticks, int full) {
        RandomSource r = level.random;
        double d = Math.max(0.3D, CutterConfig.CLIENT.auraDensity.get());
        float frac = Mth.clamp(ticks / (float) full, 0.0F, 1.0F);

        Vec3 a = bladePoint(e, 0.0), b = bladePoint(e, 1.0);
        Vec3 dir = b.subtract(a).normalize();
        Vec3 u = dir.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1.0E-4) u = new Vec3(1, 0, 0);
        u = u.normalize();
        Vec3 v = dir.cross(u).normalize();

        // 0-1 s: thin silver aura
        if (r.nextDouble() < 0.8D * d) {
            Vec3 p = bladePoint(e, r.nextDouble());
            level.addParticle(ModParticles.SILVER_MOTE.get(), p.x, p.y, p.z, rnd(r) * 0.01, 0.02, rnd(r) * 0.01);
        }
        // 1-3 s: silver coils around the blade + black particles
        if (ticks >= 20) {
            for (int i = 0; i < 2; i++) {
                double t = ((ticks * 0.045D) + i * 0.5D) % 1.0D;
                double ang = ticks * 0.6D + i * Math.PI;
                Vec3 c = bladePoint(e, t).add(u.scale(Math.cos(ang) * 0.28)).add(v.scale(Math.sin(ang) * 0.28));
                level.addParticle(ModParticles.SILVER_MOTE.get(), c.x, c.y, c.z, 0.0, 0.01, 0.0);
            }
            if (r.nextDouble() < 0.5D * d) {
                Vec3 p = bladePoint(e, r.nextDouble());
                level.addParticle(BLACK_DUST, p.x + rnd(r) * 0.15, p.y + rnd(r) * 0.15, p.z + rnd(r) * 0.15, 0.0, 0.01, 0.0);
            }
        }
        // 3-5 s: rotating black + golden ring around the weapon, brighter tip
        if (ticks >= 60) {
            Vec3 m = bladePoint(e, 0.5);
            double radius = 0.7D + 0.35D * frac;
            for (int k = 0; k < 3; k++) {
                double ang = ticks * 0.4D + k * (Math.PI * 2.0D / 3.0D);
                Vec3 p = m.add(u.scale(Math.cos(ang) * radius)).add(v.scale(Math.sin(ang) * radius));
                Vec3 tan = u.scale(-Math.sin(ang)).add(v.scale(Math.cos(ang))).scale(0.06);
                if ((k + ticks) % 2 == 0) level.addParticle(BLACK_DUST, p.x, p.y, p.z, tan.x, tan.y, tan.z);
                else level.addParticle(ModParticles.GETSUGA_EMBER.get(), p.x, p.y, p.z, tan.x, tan.y, tan.z);
            }
            if (r.nextFloat() < frac * 0.6F) {
                level.addParticle(ParticleTypes.END_ROD, b.x + rnd(r) * 0.1, b.y + rnd(r) * 0.1, b.z + rnd(r) * 0.1, rnd(r) * 0.03, rnd(r) * 0.03, rnd(r) * 0.03);
            }
        }
        if (ticks == full) {                                    // maximum power: strong flash
            level.addParticle(ParticleTypes.FLASH, b.x, b.y, b.z, 0, 0, 0);
            for (int i = 0; i < Math.ceil(40 * d); i++) {
                Vec3 s = new Vec3(rnd(r), rnd(r), rnd(r)).normalize().scale(0.15 + r.nextDouble() * 0.25);
                level.addParticle(i % 2 == 0 ? ModParticles.SILVER_MOTE.get() : ModParticles.GETSUGA_EMBER.get(), b.x, b.y, b.z, s.x, s.y, s.z);
            }
        }
    }
}
