package com.absolutecutter.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** COMMON = gameplay (per world/server).  CLIENT = visuals only. */
public final class CutterConfig {
    public enum EarlyRelease { CANCEL, WEAK_VERSION }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        var c = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = c.getLeft();
        COMMON_SPEC = c.getRight();
        var k = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = k.getLeft();
        CLIENT_SPEC = k.getRight();
    }

    private CutterConfig() {}

    /** Safe even before the config file is loaded (item attributes are queried very early). */
    public static double baseDamage() {
        return COMMON_SPEC.isLoaded() ? COMMON.baseDamage.get() : 50.0D;
    }

    public static int fullChargeTicks() {
        double s = COMMON_SPEC.isLoaded() ? COMMON.fullChargeSeconds.get() : 5.0D;
        return Math.max(1, (int) Math.round(s * 20.0D));
    }

    public static class Common {
        public final ForgeConfigSpec.DoubleValue baseDamage;
        public final ForgeConfigSpec.DoubleValue getsugaDamage;
        public final ForgeConfigSpec.DoubleValue fullChargeSeconds;
        public final ForgeConfigSpec.EnumValue<EarlyRelease> earlyRelease;
        public final ForgeConfigSpec.DoubleValue minWeakChargeSeconds;
        public final ForgeConfigSpec.IntValue cooldownTicks;

        public final ForgeConfigSpec.DoubleValue projectileSpeed;
        public final ForgeConfigSpec.DoubleValue projectileWidth;
        public final ForgeConfigSpec.DoubleValue projectileRange;
        public final ForgeConfigSpec.IntValue projectileLifetime;
        public final ForgeConfigSpec.BooleanValue entityDamage;
        public final ForgeConfigSpec.BooleanValue friendlyFire;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> excludedEntities;

        public final ForgeConfigSpec.BooleanValue terrainDestruction;
        public final ForgeConfigSpec.DoubleValue impactRadius;
        public final ForgeConfigSpec.IntValue terrainBlockBudget;

        public final ForgeConfigSpec.DoubleValue cutRadius;
        public final ForgeConfigSpec.IntValue maxBlocksPerSwing;
        public final ForgeConfigSpec.DoubleValue coneAngle;
        public final ForgeConfigSpec.IntValue swingCooldownTicks;

        public final ForgeConfigSpec.BooleanValue allowTerrainMode;
        public final ForgeConfigSpec.DoubleValue terrainRadius;
        public final ForgeConfigSpec.IntValue terrainMaxBlocks;
        public final ForgeConfigSpec.DoubleValue terrainMaxHardness;

        public final ForgeConfigSpec.BooleanValue flightEnabled;
        public final ForgeConfigSpec.DoubleValue flightSpeed;
        public final ForgeConfigSpec.BooleanValue invincibleInInventory;
        public final ForgeConfigSpec.BooleanValue speedInInventory;
        public final ForgeConfigSpec.BooleanValue dodgeEnabled;
        public final ForgeConfigSpec.IntValue dodgeCooldownTicks;
        public final ForgeConfigSpec.DoubleValue dodgeDistance;

        Common(ForgeConfigSpec.Builder b) {
            b.push("combat");
            baseDamage = b.comment("Base attack damage of a normal hit (health points, 2 = 1 heart). Default 50 = 25 hearts.")
                    .defineInRange("baseDamage", 50.0D, 1.0D, 1_000_000.0D);
            b.pop();

            b.push("getsuga");
            getsugaDamage = b.comment("Damage of a FULLY charged Getsuga to each primary target (default 800 = 400 hearts).",
                            "Partial charges scale linearly; the value can never exceed this setting.")
                    .defineInRange("fullDamage", 800.0D, 1.0D, 10_000_000.0D);
            fullChargeSeconds = b.comment("Seconds of continuous right-click needed for full power.")
                    .defineInRange("fullChargeSeconds", 5.0D, 0.5D, 30.0D);
            earlyRelease = b.comment("CANCEL = releasing early does nothing. WEAK_VERSION = fires a scaled-down Getsuga (after the minimum charge).")
                    .defineEnum("earlyRelease", EarlyRelease.WEAK_VERSION);
            minWeakChargeSeconds = b.comment("Minimum charge time for WEAK_VERSION to fire.")
                    .defineInRange("minWeakChargeSeconds", 1.0D, 0.0D, 30.0D);
            cooldownTicks = b.comment("Cooldown (ticks) after firing. Applied AFTER the shot, so it never shortens the charge. 40 = 2 s.")
                    .defineInRange("cooldownTicks", 40, 0, 12000);
            projectileSpeed = b.comment("Projectile speed in blocks per tick.")
                    .defineInRange("projectileSpeed", 1.6D, 0.2D, 6.0D);
            projectileWidth = b.comment("Size of the crescent in blocks (visual size and hit-box edge length at full charge).")
                    .defineInRange("projectileWidth", 5.0D, 0.5D, 24.0D);
            projectileRange = b.comment("Maximum travel distance in blocks.")
                    .defineInRange("projectileRange", 80.0D, 4.0D, 512.0D);
            projectileLifetime = b.comment("Maximum lifetime in ticks.")
                    .defineInRange("projectileLifetime", 100, 5, 1200);
            entityDamage = b.comment("If false the Getsuga never damages entities (visual only).")
                    .define("entityDamage", true);
            friendlyFire = b.comment("If false, the Getsuga skips your tamed pets and entities on your scoreboard team. The caster is ALWAYS excluded.")
                    .define("friendlyFire", false);
            excludedEntities = b.comment("Entity type ids that the Getsuga ignores, e.g. [\"minecraft:villager\", \"minecraft:iron_golem\"].")
                    .defineListAllowEmpty(List.of("excludedEntities"), List::of, o -> o instanceof String);
            b.pop();

            b.push("getsuga_terrain");
            terrainDestruction = b.comment("The Getsuga destroys blocks along its path and at impact (up to maxBlocksDestroyed).")
                    .define("destroyBlocks", true);
            impactRadius = b.comment("Sphere radius (blocks) destroyed at impact when destroyBlocks is on.")
                    .defineInRange("impactRadius", 3.0D, 0.0D, 8.0D);
            terrainBlockBudget = b.comment("Hard cap on blocks a single Getsuga may destroy in total.")
                    .defineInRange("maxBlocksDestroyed", 20, 0, 4096);
            b.pop();

            b.push("cutting");
            cutRadius = b.comment("Reach (blocks) of the foliage/tree cutting cone when you swing.")
                    .defineInRange("cutRadius", 6.0D, 1.0D, 16.0D);
            maxBlocksPerSwing = b.comment("Maximum blocks processed per swing.")
                    .defineInRange("maxBlocksPerSwing", 64, 1, 1024);
            coneAngle = b.comment("Half-angle (degrees) of the cutting cone in front of you.")
                    .defineInRange("coneAngle", 40.0D, 5.0D, 89.0D);
            swingCooldownTicks = b.comment("Minimum ticks between cutting swings per player (anti-spam / anti-abuse).")
                    .defineInRange("swingCooldownTicks", 5, 1, 100);
            b.pop();

            b.push("terrain_mode");
            allowTerrainMode = b.comment("Allow the optional terrain-cutting mode (hold the 'Terrain Cutting Modifier' key while swinging).")
                    .define("allowTerrainMode", true);
            terrainRadius = b.comment("Reach of terrain mode.").defineInRange("terrainRadius", 4.0D, 1.0D, 8.0D);
            terrainMaxBlocks = b.comment("Maximum blocks per terrain-mode swing.").defineInRange("terrainMaxBlocks", 24, 1, 256);
            terrainMaxHardness = b.comment("Blocks harder than this (obsidian = 50) are never cut. Unbreakable blocks are never cut.")
                    .defineInRange("terrainMaxHardness", 10.0D, 0.0D, 100.0D);
            b.pop();

            b.push("inventory_powers");
            flightEnabled = b.comment("While the sword is ANYWHERE in your inventory you can fly (double-tap jump). Survival/adventure only.")
                    .define("flightEnabled", true);
            flightSpeed = b.comment("Flying speed value. Vanilla creative = 0.05. Sprinting while flying doubles it.",
                            "0.3 is roughly 65 blocks/s (130 sprinting), far faster than elytra. Above ~0.45 the server may reject the movement ('moved too quickly').")
                    .defineInRange("flightSpeed", 0.3D, 0.05D, 0.6D);
            invincibleInInventory = b.comment("Take no damage while the sword is in your inventory (void and /kill still work so you can't get stuck).")
                    .define("invincibleInInventory", true);
            speedInInventory = b.comment("Speed II effect for as long as the sword is in your inventory.")
                    .define("speedInInventory", true);
            b.pop();

            b.push("hand_powers");
            dodgeEnabled = b.comment("While HOLDING the sword in your main hand, incoming attacks teleport you away instead of hitting you.")
                    .define("dodgeEnabled", true);
            dodgeCooldownTicks = b.comment("Minimum ticks between dodges (10 = half a second).")
                    .defineInRange("dodgeCooldownTicks", 10, 0, 200);
            dodgeDistance = b.comment("How far (blocks) a dodge teleports you.")
                    .defineInRange("dodgeDistance", 5.0D, 2.0D, 16.0D);
            b.pop();
        }
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue idleAura;
        public final ForgeConfigSpec.DoubleValue auraDensity;
        public final ForgeConfigSpec.BooleanValue screenDistortion;
        public final ForgeConfigSpec.BooleanValue chargeSlowdownFix;

        Client(ForgeConfigSpec.Builder b) {
            idleAura = b.comment("Silver aura while the sword is held. Disable for performance.")
                    .define("idleAura", true);
            auraDensity = b.comment("Particle density multiplier for aura and charge effects.")
                    .defineInRange("auraDensity", 1.0D, 0.1D, 3.0D);
            screenDistortion = b.comment("Subtle FOV squeeze + camera tremor while charging past 3 seconds.")
                    .define("screenDistortion", true);
            chargeSlowdownFix = b.comment("EXPERIMENTAL: cancel vanilla's 80% walk slowdown while charging. Leave false unless you test it.")
                    .define("chargeSlowdownFix", false);
        }
    }
}
