package com.absolutecutter.combat;

import com.absolutecutter.config.CutterConfig;
import com.absolutecutter.item.AbsoluteCutterItem;
import com.absolutecutter.registry.ModParticles;
import com.absolutecutter.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side "Absolute Cutter" logic: validates a swing, finds blocks in a cone, breaks them through the normal player break path. */
public final class CutHandler {
    private static final Map<UUID, Long> LAST_SWING = new ConcurrentHashMap<>();
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.1F), 1.0F);

    private CutHandler() {}

    public static void forget(UUID id) {
        LAST_SWING.remove(id);
    }

    public static void handleSwing(ServerPlayer p, boolean terrainKeyDown) {
        if (!p.isAlive() || p.isSpectator()) return;
        if (!(p.getMainHandItem().getItem() instanceof AbsoluteCutterItem)) return;      // server decides, never the client
        long now = p.level().getGameTime();
        Long last = LAST_SWING.get(p.getUUID());
        if (last != null && now - last < CutterConfig.COMMON.swingCooldownTicks.get()) return;
        LAST_SWING.put(p.getUUID(), now);

        boolean terrain = terrainKeyDown && CutterConfig.COMMON.allowTerrainMode.get();
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.SLASH.get(), SoundSource.PLAYERS, 0.9F, 0.9F + level.random.nextFloat() * 0.2F);
        slashFx(level, p, terrain);
        cut(p, terrain);
    }

    private static void slashFx(ServerLevel level, ServerPlayer p, boolean terrain) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        double half = Math.toRadians(CutterConfig.COMMON.coneAngle.get());
        for (int i = -6; i <= 6; i++) {
            double a = i / 6.0D * half;
            Vec3 pt = eye.add(look.scale(Math.cos(a) * 2.4D)).add(right.scale(Math.sin(a) * 2.4D)).add(0, -0.3, 0);
            if (terrain) level.sendParticles(RED_DUST, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0.0);
            level.sendParticles(ModParticles.SILVER_MOTE.get(), pt.x, pt.y, pt.z, 1, 0, 0, 0, 0.0);
        }
        Vec3 c = eye.add(look.scale(1.8D));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y - 0.2, c.z, 1, 0, 0, 0, 0.0);
    }

    public static int cut(ServerPlayer p, boolean terrain) {
        ServerLevel level = p.serverLevel();
        CutterConfig.Common cfg = CutterConfig.COMMON;
        double radius = terrain ? cfg.terrainRadius.get() : cfg.cutRadius.get();
        int limit = terrain ? cfg.terrainMaxBlocks.get() : cfg.maxBlocksPerSwing.get();
        double cosHalf = Math.cos(Math.toRadians(cfg.coneAngle.get()));
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle().normalize();
        int r = (int) Math.ceil(radius);
        BlockPos origin = BlockPos.containing(eye);

        List<BlockPos> targets = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    Vec3 to = Vec3.atCenterOf(pos).subtract(eye);
                    double dist = to.length();
                    if (dist > radius || dist < 0.3D) continue;
                    double dot = to.dot(look);
                    if (dot <= 0.0D || dot / dist < cosHalf) continue;                  // narrow cone only
                    if (!canCut(level, pos, terrain)) continue;
                    targets.add(pos.immutable());
                }
            }
        }
        targets.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(eye)));

        int broken = 0;
        for (BlockPos pos : targets) {
            if (broken >= limit) break;
            if (canCut(level, pos, terrain) && destroy(p, pos)) broken++;
        }
        return broken;
    }

    /** Breaks via ServerPlayerGameMode, so BlockEvent.BreakEvent (protection mods), drops, loot tables and game-mode rules all apply. */
    public static boolean destroy(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        BlockState before = level.getBlockState(pos);
        if (before.isAir()) return false;
        boolean ok = p.gameMode.destroyBlock(pos);
        if (ok) {
            // other players get the break effect from playerWillDestroy; the breaker needs it sent explicitly
            p.connection.send(new ClientboundLevelEventPacket(2001, pos, Block.getId(before), false));
        }
        return ok;
    }

    public static boolean canCut(ServerLevel level, BlockPos pos, boolean terrain) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || st.getBlock() instanceof LiquidBlock) return false;
        if (st.getDestroySpeed(level, pos) < 0.0F) return false;                      // bedrock, barriers, etc.
        if (st.is(BlockTags.WITHER_IMMUNE)) return false;                             // command blocks, end portal frames...
        if (level.getBlockEntity(pos) != null) return false;                          // chests, spawners, ...
        if (isFoliage(st)) return true;
        return terrain && st.getDestroySpeed(level, pos) <= CutterConfig.COMMON.terrainMaxHardness.get();
    }

    /** Used by the Getsuga's optional terrain destruction (same safeguards as terrain mode). */
    public static boolean canCutTerrain(ServerLevel level, BlockPos pos) {
        return canCut(level, pos, true);
    }

    public static boolean isFoliage(BlockState st) {
        Block b = st.getBlock();
        if (b instanceof CropBlock || b instanceof NetherWartBlock) return false;     // leave farms alone
        return st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || st.is(BlockTags.FLOWERS) || st.is(BlockTags.SAPLINGS)
                || b instanceof BushBlock || b instanceof VineBlock || b instanceof GrowingPlantBlock
                || b instanceof WebBlock || b instanceof HugeMushroomBlock;
    }
}
