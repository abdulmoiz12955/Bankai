package com.absolutecutter.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ClientKeys {
    public static final KeyMapping TERRAIN_MODE = new KeyMapping("key.absolute_cutter.terrain_mode",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.absolute_cutter");

    private ClientKeys() {}
}
