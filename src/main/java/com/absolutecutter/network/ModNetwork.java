package com.absolutecutter.network;

import com.absolutecutter.AbsoluteCutterMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AbsoluteCutterMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, SwingPacket.class, SwingPacket::encode, SwingPacket::decode, SwingPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private ModNetwork() {}
}
