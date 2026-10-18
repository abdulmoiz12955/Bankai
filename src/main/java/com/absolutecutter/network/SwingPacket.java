package com.absolutecutter.network;

import com.absolutecutter.combat.CutHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: "I swung the Absolute Cutter". The server re-validates EVERYTHING (held item, cooldown, config). */
public class SwingPacket {
    private final boolean terrainKeyDown;

    public SwingPacket(boolean terrainKeyDown) {
        this.terrainKeyDown = terrainKeyDown;
    }

    public static void encode(SwingPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.terrainKeyDown);
    }

    public static SwingPacket decode(FriendlyByteBuf buf) {
        return new SwingPacket(buf.readBoolean());
    }

    public static void handle(SwingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            if (player != null) {
                CutHandler.handleSwing(player, msg.terrainKeyDown);
            }
        });
        c.setPacketHandled(true);
    }
}
