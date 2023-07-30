package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.origin.Origin;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientPhantomOriginType {
    public static boolean shouldPhase(Origin origin) {
        ClientPlayerEntity player = (ClientPlayerEntity) origin.getPlayer();
        return player.movementInput.sneaking && player.collidedVertically;
    }

    public static boolean isPhasing(Origin origin) {
        ClientPlayerEntity player = (ClientPlayerEntity) origin.getPlayer();
        return player.collidedVertically;
    }
}
