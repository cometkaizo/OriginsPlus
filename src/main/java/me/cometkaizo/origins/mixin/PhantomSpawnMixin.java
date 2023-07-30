package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.PhantomOriginType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.spawner.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

public class PhantomSpawnMixin {

    @Mixin(PhantomSpawner.class)
    public static abstract class MixedPhantomSpawner {
        @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z"))
        protected boolean shouldNotSpawnOn(PlayerEntity player) {
            if (player.isSpectator()) return true;
            return Origin.hasLabel(player, PhantomOriginType.Label.NO_INSOMNIA);
        }
    }

}
