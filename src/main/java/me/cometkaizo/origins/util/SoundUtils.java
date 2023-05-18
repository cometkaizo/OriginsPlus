package me.cometkaizo.origins.util;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class SoundUtils {

    public static void playSound(PlayerEntity player, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        PlayerEntity target = player instanceof ClientPlayerEntity ? player : null;
        player.world.playSound(target, player.getPosX(), player.getPosY(), player.getPosZ(),
                sound, category, volume, pitch);
    }

    public static void playSound(PlayerEntity player, SoundEvent sound, SoundCategory category) {
        playSound(player, sound, category, 1, 1);
    }

    public static void playMovingSound(PlayerEntity player, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        PlayerEntity target = player instanceof ClientPlayerEntity ? player : null;
        player.world.playMovingSound(target, player,
                sound, category, volume, pitch);
    }

    public static void playMovingSound(PlayerEntity player, SoundEvent sound, SoundCategory category) {
        playMovingSound(player, sound, category, 1, 1);
    }

}
