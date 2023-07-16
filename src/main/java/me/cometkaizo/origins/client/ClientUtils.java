package me.cometkaizo.origins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class ClientUtils {
    public static World getClientLevel() {
        return Minecraft.getInstance().world;
    }
    public static Screen getClientScreen() {
        return Minecraft.getInstance().currentScreen;
    }
    public static PlayerEntity getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }
}
