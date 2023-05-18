package me.cometkaizo.origins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class ClientUtils {
    public static World getClientLevel() {
        return Minecraft.getInstance().world;
    }

    public static PlayerEntity getClientPlayer() {
        return Minecraft.getInstance().player;
    }
}
