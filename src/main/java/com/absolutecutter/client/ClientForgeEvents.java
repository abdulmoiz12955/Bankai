package com.absolutecutter.client;

import com.absolutecutter.AbsoluteCutterMod;
import com.absolutecutter.combat.AuraFx;
import com.absolutecutter.config.CutterConfig;
import com.absolutecutter.item.AbsoluteCutterItem;
import com.absolutecutter.network.ModNetwork;
import com.absolutecutter.network.SwingPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbsoluteCutterMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    private static boolean holdsSword(net.minecraft.world.entity.LivingEntity e) {
        return e.getMainHandItem().getItem() instanceof AbsoluteCutterItem;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;

        for (AbstractClientPlayer p : level.players()) {
            if (holdsSword(p) && !p.isUsingItem()) {      // while charging, AuraFx.charge() (called from the item) takes over
                AuraFx.idle(level, p);
            }
        }
        LocalPlayer me = mc.player;
        if (me != null && holdsSword(me) && ClientKeys.TERRAIN_MODE.isDown() && me.tickCount % 10 == 0) {
            me.displayClientMessage(Component.translatable("message.absolute_cutter.terrain_mode").withStyle(ChatFormatting.RED), true);
        }
    }

    /** Left-click (swing) -> ask the server to run the cutting cone. The server re-validates everything. */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !holdsSword(p)) return;
        ModNetwork.CHANNEL.sendToServer(new SwingPacket(ClientKeys.TERRAIN_MODE.isDown()));
    }

    /** 0 = no effect, 1 = full distortion (ramps in between 3 s and full charge). */
    private static float distortion() {
        if (!CutterConfig.CLIENT.screenDistortion.get()) return 0.0F;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !p.isUsingItem() || !(p.getUseItem().getItem() instanceof AbsoluteCutterItem)) return 0.0F;
        int ticks = AbsoluteCutterItem.USE_DURATION - p.getUseItemRemainingTicks();
        int full = CutterConfig.fullChargeTicks();
        return Mth.clamp((ticks - 60) / (float) Math.max(1, full - 60), 0.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float k = distortion();
        if (k > 0.0F && event.getPlayer() == Minecraft.getInstance().player) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F - 0.10F * k));
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float k = distortion();
        LocalPlayer p = Minecraft.getInstance().player;
        if (k > 0.0F && p != null) {
            float t = p.tickCount + (float) event.getPartialTick();
            event.setRoll(event.getRoll() + Mth.sin(t * 1.9F) * 0.7F * k);
            event.setPitch(event.getPitch() + Mth.sin(t * 2.7F) * 0.25F * k);
        }
    }

    /** Experimental (off by default): undo vanilla's use-item slowdown while charging. */
    @SubscribeEvent
    public static void onMove(MovementInputUpdateEvent event) {
        if (!CutterConfig.CLIENT.chargeSlowdownFix.get()) return;
        var p = event.getEntity();
        if (p.isUsingItem() && !p.isPassenger() && p.getUseItem().getItem() instanceof AbsoluteCutterItem) {
            event.getInput().leftImpulse *= 5.0F;
            event.getInput().forwardImpulse *= 5.0F;
        }
    }
}
