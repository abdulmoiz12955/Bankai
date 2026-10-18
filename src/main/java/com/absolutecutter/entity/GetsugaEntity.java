package com.absolutecutter.entity;

import com.absolutecutter.combat.AuraFx;
import com.absolutecutter.combat.CutHandler;
import com.absolutecutter.config.CutterConfig;
import com.absolutecutter.registry.ModParticles;
import com.absolutecutter.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative Getsuga. The server moves it, sweeps it against blocks and entities every tick, and applies damage.
 * The client only mirrors the motion and draws particles; the crescent mesh is drawn by GetsugaRenderer.
 * Position() is the CENTRE of the crescent (not the feet).
 */
public class GetsugaEntity extends Projectile {
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(GetsugaEntity.class, EntityDataSerializers.FLOAT);

    private float damage = 800.0F;
    private double range = 80.0D;
    private int lifetime = 100;
    private int budget = 0;
    private double travelled = 0.0D;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public GetsugaEntity(EntityType<? extends GetsugaEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_WIDTH, 5.0F);
    }

    public void configure(float damage, double speed, float width, double range, int lifetime, int terrainBudget) {
        this.damage = damage;
        this.range = range;
        this.lifetime = lifetime;
        this.budget = CutterConfig.COMMON.terrainDestruction.get() ? terrainBudget : 0;
        this.entityData.set(DATA_WIDTH, width);
    }

    public float getCrescentWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    /** Point the projectile along {@code dir}. */
    public void aim(Vec3 dir, double speed) {
        Vec3 d = dir.normalize();
        this.setDeltaMovement(d.scale(speed));
        float yaw = (float) (Mth.atan2(-d.x, d.z) * (180.0D / Math.PI));
        float pitch = (float) (-(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * (180.0D / Math.PI)));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    // -------------------------------------------------------------------- rendering helpers (plain Entity methods, no client classes)
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(this.getCrescentWidth() * 1.2D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 160.0D * 160.0D;
    }

    // -------------------------------------------------------------------- tick
    @Override
    public void tick() {
        super.tick();
        Vec3 start = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 end = start.add(motion);

        if (this.level().isClientSide) {
            clientTrail(start, motion);
            this.setPos(end.x, end.y, end.z);
            return;
        }

        ServerLevel level = (ServerLevel) this.level();
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blocked = blockHit.getType() != HitResult.Type.MISS;
        Vec3 reach = blocked ? blockHit.getLocation() : end;      // swept: entities are tested along the whole segment

        if (CutterConfig.COMMON.entityDamage.get()) {
            hitEntities(level, start, reach);
        }
        if (budget > 0 && this.getOwner() instanceof ServerPlayer sp) {
            carve(level, sp, reach, Math.min(6.0D, this.getCrescentWidth() * 0.3D));
        }
        this.travelled += start.distanceTo(reach);

        if (blocked) {
            impact(level, reach);
            this.discard();
            return;
        }
        this.setPos(end.x, end.y, end.z);

        if (this.tickCount % 8 == 1) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.GETSUGA_TRAVEL.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
        }
        if (this.travelled >= this.range || this.tickCount >= this.lifetime) {
            fizzle(level);
            this.discard();
        }
    }

    private void clientTrail(Vec3 pos, Vec3 motion) {
        var r = this.level().random;
        double w = this.getCrescentWidth() * 0.35D;
        for (int i = 0; i < 4; i++) {
            Vec3 p = pos.add((r.nextDouble() - 0.5D) * 2 * w, (r.nextDouble() - 0.5D) * 2 * w, (r.nextDouble() - 0.5D) * 2 * w).subtract(motion.scale(r.nextDouble() * 1.5D));
            this.level().addParticle(AuraFx.BLACK_DUST, p.x, p.y, p.z, 0.0, 0.01, 0.0);
        }
        for (int i = 0; i < 3; i++) {
            Vec3 p = pos.add((r.nextDouble() - 0.5D) * 2 * w, (r.nextDouble() - 0.5D) * 2 * w, (r.nextDouble() - 0.5D) * 2 * w);
            this.level().addParticle(ModParticles.GETSUGA_EMBER.get(), p.x, p.y, p.z, (r.nextDouble() - 0.5D) * 0.05, (r.nextDouble() - 0.5D) * 0.05, (r.nextDouble() - 0.5D) * 0.05);
        }
        if (r.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 0.0, 0.02, 0.0);
        }
    }

    // -------------------------------------------------------------------- hit detection / damage
    private boolean canHit(Entity e) {
        if (!e.isPickable() || e.isSpectator() || !e.isAlive()) return false;
        Entity owner = this.getOwner();
        if (owner != null) {
            if (e == owner || e.isPassengerOfSameVehicle(owner)) return false;     // the caster is ALWAYS excluded
            if (!CutterConfig.COMMON.friendlyFire.get()) {
                if (e.isAlliedTo(owner)) return false;
                if (e instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
            }
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return id == null || !CutterConfig.COMMON.excludedEntities.get().contains(id.toString());
    }

    private void hitEntities(ServerLevel level, Vec3 from, Vec3 to) {
        double h = this.getCrescentWidth() * 0.5D;
        AABB sweep = new AABB(from, to).inflate(h);
        for (Entity e : level.getEntities(this, sweep, this::canHit)) {
            Entity target = e instanceof EnderDragonPart part ? part.parentMob : e;     // a dragon counts once, not once per part
            if (!this.alreadyHit.add(target.getUUID())) continue;                       // once per target per projectile
            DamageSource src = level.damageSources().thrown(this, this.getOwner());
            e.hurt(src, this.damage);                                                   // normal pipeline: armor, i-frames, pvp rules
            Vec3 dir = this.getDeltaMovement().normalize();
            if (e instanceof LivingEntity le) {
                le.knockback(0.8D, -dir.x, -dir.z);
            }
            level.sendParticles(ParticleTypes.FLASH, e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(), 1, 0, 0, 0, 0.0);
            level.sendParticles(ModParticles.GETSUGA_EMBER.get(), e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(), 20, 0.4, 0.5, 0.4, 0.2);
            level.sendParticles(AuraFx.BLACK_DUST, e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(), 14, 0.4, 0.5, 0.4, 0.0);
            level.playSound(null, e.getX(), e.getY(), e.getZ(), ModSounds.GETSUGA_IMPACT.get(), SoundSource.PLAYERS, 1.2F, 1.1F);
        }
    }

    // -------------------------------------------------------------------- optional, bounded terrain destruction (off by default)
    private void carve(ServerLevel level, ServerPlayer sp, Vec3 center, double radius) {
        if (radius < 0.5D) return;
        int r = (int) Math.ceil(radius);
        BlockPos c = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            if (this.budget <= 0) return;
            if (pos.distToCenterSqr(center) > radius * radius) continue;
            BlockPos im = pos.immutable();
            if (CutHandler.canCutTerrain(level, im) && CutHandler.destroy(sp, im)) {   // goes through the break event => respects protection
                this.budget--;
            }
        }
    }

    private void impact(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0.0);
        level.sendParticles(ModParticles.GETSUGA_EMBER.get(), at.x, at.y, at.z, 60, 1.2, 1.2, 1.2, 0.3);
        level.sendParticles(AuraFx.BLACK_DUST, at.x, at.y, at.z, 40, 1.2, 1.2, 1.2, 0.0);
        level.sendParticles(ModParticles.SILVER_MOTE.get(), at.x, at.y, at.z, 30, 1.0, 1.0, 1.0, 0.3);
        level.playSound(null, at.x, at.y, at.z, ModSounds.GETSUGA_IMPACT.get(), SoundSource.PLAYERS, 2.0F, 0.9F);
        if (this.budget > 0 && this.getOwner() instanceof ServerPlayer sp) {
            carve(level, sp, at.add(this.getDeltaMovement().normalize().scale(0.6D)), CutterConfig.COMMON.impactRadius.get());
        }
    }

    private void fizzle(ServerLevel level) {
        level.sendParticles(ModParticles.GETSUGA_EMBER.get(), getX(), getY(), getZ(), 30, 1.0, 1.0, 1.0, 0.15);
        level.sendParticles(AuraFx.BLACK_DUST, getX(), getY(), getZ(), 20, 1.0, 1.0, 1.0, 0.0);
    }

    // not persisted (EntityType is noSave), but Entity requires these
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
