package com.absolutecutter.combat;

import com.absolutecutter.AbsoluteCutterMod;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbsoluteCutterMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CombatEvents {
    private CombatEvents() {}

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CutHandler.forget(event.getEntity().getUUID());
    }
}
