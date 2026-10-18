package com.absolutecutter.combat;

import com.absolutecutter.AbsoluteCutterMod;
import com.absolutecutter.config.CutterConfig;
import com.absolutecutter.item.AbsoluteCutterItem;
import com.absolutecutter.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two power sets, all decided on the server:
 *  - IN INVENTORY (anywhere): flight, Speed II, invincibility.
 *  - IN MAIN HAND: dodge-teleport away from incoming attacks.
 */
@Mod.EventBusSubscriber(modid = AbsoluteCutterMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PowerEvents {
    private static final String TAG_FLIGHT = "absolute_cutter_flight";
    private static final Map<UUID, Long> LAST_DODGE = new ConcurrentHashMap<>();

    private PowerEvents() {}

    private static boolean hasSword(ServerPlayer p) {
        var inv = p.getInventory();
        for (ItemStack s : inv.items) if (s.getItem() instanceof AbsoluteCutterItem) return true;
        for (ItemStack s : inv.offhand) if (s.getItem() instanceof AbsoluteCutterItem) return true;
        for (ItemStack s : inv.armor) if (s.getItem() instanceof AbsoluteCutterItem) return true;
        return false;
    }

    // ------------------------------------------------------------------ flight + speed (inventory)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 5 != 0) return;

        boolean has = hasSword(p);
        CompoundTag data = p.getPersistentData();
        boolean granted = data.getBoolean(TAG_FLIGHT);
        Abilities ab = p.getAbilities();
        boolean normalMode = !p.isCreative() && !p.isSpectator();

        if (has && CutterConfig.COMMON.flightEnabled.get()) {
            if (normalMode) {
                boolean changed = false;
                float speed = CutterConfig.COMMON.flightSpeed.get().floatValue();
                if (!ab.mayfly) { ab.mayfly = true; changed = true; }
                if (ab.getFlyingSpeed() != speed) { ab.setFlyingSpeed(speed); changed = true; }
                data.putBoolean(TAG_FLIGHT, true);
                if (changed) p.onUpdateAbilities();
            }
        } else if (granted) {                       // sword gone (or flight disabled): take our flight back
            if (normalMode) {
                ab.mayfly = false;
                ab.flying = false;
                ab.setFlyingSpeed(0.05F);
                p.onUpdateAbilities();
            }
            data.putBoolean(TAG_FLIGHT, false);
        }

        if (has && CutterConfig.COMMON.speedInInventory.get()) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, true, false, true));   // Speed II, no particles
        }
    }

    // ------------------------------------------------------------------ invincibility (inventory) + dodge (hand)
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        DamageSource src = event.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;          // void and /kill still work

        boolean holding = p.getMainHandItem().getItem() instanceof AbsoluteCutterItem;
        if (holding && CutterConfig.COMMON.dodgeEnabled.get() && (src.getEntity() != null || src.getDirectEntity() != null)) {
            if (tryDodge(p)) {
                event.setCanceled(true);
                return;
            }
        }
        if (CutterConfig.COMMON.invincibleInInventory.get() && hasSword(p)) {
            event.setCanceled(true);
        }
    }

    private static boolean tryDodge(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        Long last = LAST_DODGE.get(p.getUUID());
        if (last != null && now - last < CutterConfig.COMMON.dodgeCooldownTicks.get()) return false;

        RandomSource r = p.getRandom();
        double dist = CutterConfig.COMMON.dodgeDistance.get();
        Vec3 from = p.position();
        for (int i = 0; i < 20; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0D;
            double d = dist * (0.6D + 0.5D * r.nextDouble());
            double dx = Math.cos(ang) * d, dz = Math.sin(ang) * d, dy = r.nextInt(3) - 1;
            AABB box = p.getBoundingBox().move(dx, dy, dz);
            if (!level.noCollision(p, box) || level.containsAnyLiquid(box) || !level.getWorldBorder().isWithinBounds(box)) continue;

            burst(level, from.add(0, 1.0, 0));
            p.connection.teleport(from.x + dx, from.y + dy, from.z + dz, p.getYRot(), p.getXRot());
            burst(level, p.position().add(0, 1.0, 0));
            level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.4F);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.4F);
            LAST_DODGE.put(p.getUUID(), now);
            return true;
        }
        return false;       // nowhere safe to land: the normal rules apply
    }

    private static void burst(ServerLevel level, Vec3 at) {
        level.sendParticles(ModParticles.SILVER_MOTE.get(), at.x, at.y, at.z, 20, 0.3, 0.5, 0.3, 0.12);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 20, 0.3, 0.5, 0.3, 0.1);
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_DODGE.remove(event.getEntity().getUUID());
    }
}
