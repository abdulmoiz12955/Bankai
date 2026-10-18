package com.absolutecutter.item;

import com.absolutecutter.combat.AuraFx;
import com.absolutecutter.combat.CutHandler;
import com.absolutecutter.config.CutterConfig;
import com.absolutecutter.entity.GetsugaEntity;
import com.absolutecutter.registry.ModEntities;
import com.absolutecutter.registry.ModParticles;
import com.absolutecutter.registry.ModSounds;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AbsoluteCutterItem extends SwordItem {
    /** "Use duration" is just an upper bound; the real charge is USE_DURATION - remainingTicks, computed by the server. */
    public static final int USE_DURATION = 72000;

    public AbsoluteCutterItem(Properties props) {
        super(Tiers.NETHERITE, 3, -2.0F, props);
    }

    // ------------------------------------------------------------------ damage (attribute ONLY - no attack event adds damage)
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        // player base attack damage is 1.0, so the modifier is (configured - 1) => total = exactly the configured value (50)
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", CutterConfig.baseDamage() - 1.0D, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.0D, AttributeModifier.Operation.ADDITION));
        return b.build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // damage was already applied by vanilla's normal attack path; this is cosmetic only (and the blade never wears out)
        if (attacker.level() instanceof ServerLevel level) {
            double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.5D, z = target.getZ();
            level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0.0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0.0);
            level.sendParticles(ModParticles.SILVER_MOTE.get(), x, y, z, 24, 0.3, 0.4, 0.3, 0.15);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 8, 0.25, 0.35, 0.25, 0.08);
        }
        return true;
    }

    // ------------------------------------------------------------------ mining feel
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return CutHandler.isFoliage(state) ? 30.0F : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return true;    // blocks it breaks always drop their loot
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return true;    // vanilla swords refuse in creative; this one must be able to cut there too
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // ------------------------------------------------------------------ right-click charge (server-authoritative)
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);                       // vanilla tracks the hold; switching item / dying / disconnecting ends it
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        int ticks = USE_DURATION - remaining;
        int full = CutterConfig.fullChargeTicks();
        if (level.isClientSide) {
            AuraFx.charge(level, living, ticks, full);
            return;
        }
        // server: audio cues so the stage is audible without looking at the screen
        if (ticks == 0 || ticks % 10 == 0) {
            if (ticks <= full) {
                float frac = Mth.clamp(ticks / (float) full, 0.0F, 1.0F);
                float pitch = ticks < 60 ? 0.9F - 0.4F * (ticks / 60.0F) : 0.5F + 1.1F * Mth.clamp((ticks - 60) / (float) Math.max(1, full - 60), 0.0F, 1.0F);
                level.playSound(null, living.getX(), living.getY(), living.getZ(), ModSounds.CHARGE.get(), SoundSource.PLAYERS, 0.5F + 0.6F * frac, pitch);
            }
        }
        if (ticks == full) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), ModSounds.CHARGE_COMPLETE.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) return;
        int charge = USE_DURATION - timeLeft;
        int full = CutterConfig.fullChargeTicks();
        float frac = Math.min(1.0F, charge / (float) full);        // clamped: charge can never exceed the configured full damage
        if (frac < 1.0F) {
            if (CutterConfig.COMMON.earlyRelease.get() == CutterConfig.EarlyRelease.CANCEL) return;
            if (charge < Math.round(CutterConfig.COMMON.minWeakChargeSeconds.get() * 20.0D)) return;
        }
        fire(serverLevel, player, frac);
        int cd = CutterConfig.COMMON.cooldownTicks.get();
        if (cd > 0) player.getCooldowns().addCooldown(this, cd);
    }

    private void fire(ServerLevel level, ServerPlayer player, float frac) {
        CutterConfig.Common cfg = CutterConfig.COMMON;
        double speed = cfg.projectileSpeed.get();
        float damage = (float) (cfg.getsugaDamage.get() * frac);
        float width = (float) (cfg.projectileWidth.get() * (0.5D + 0.5D * frac));

        GetsugaEntity g = new GetsugaEntity(ModEntities.GETSUGA.get(), level);
        g.setOwner(player);
        g.configure(damage, speed, width, cfg.projectileRange.get(), cfg.projectileLifetime.get(), cfg.terrainBlockBudget.get());
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.getEyePosition().add(look.scale(1.6D)).add(0.0D, -0.25D, 0.0D);
        g.setPos(pos.x, pos.y, pos.z);
        g.aim(look, speed);
        level.addFreshEntity(g);

        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.GETSUGA_LAUNCH.get(), SoundSource.PLAYERS, 2.0F, 0.8F + 0.2F * frac);
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.0);
        level.sendParticles(ModParticles.GETSUGA_EMBER.get(), pos.x, pos.y, pos.z, 30, 0.6, 0.6, 0.6, 0.2);
        level.sendParticles(AuraFx.BLACK_DUST, pos.x, pos.y, pos.z, 25, 0.6, 0.6, 0.6, 0.0);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.absolute_cutter.line1"));
        tooltip.add(Component.translatable("tooltip.absolute_cutter.line2"));
        tooltip.add(Component.translatable("tooltip.absolute_cutter.line3", String.valueOf(CutterConfig.fullChargeTicks() / 20.0D)));
    }
}
