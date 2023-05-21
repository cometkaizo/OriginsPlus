package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.property.ReachProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class EntityReachMixin {

    @OnlyIn(Dist.CLIENT)
    @Mixin(PlayerController.class)
    public static abstract class MixedPlayerController {
        @Shadow @Final private Minecraft mc;

        @Inject(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/GameType;isCreative()Z",
                shift = At.Shift.AFTER),
                method = "extendedReach",
                cancellable = true
        )
        protected void test(CallbackInfoReturnable<Boolean> info) {
            Origin origin = Origin.getOrigin(mc.player);
            if (origin != null) {
                for (ReachProperty property : origin.getProperties(ReachProperty.class)) {
                    if (!property.hasCreativeEntityReach()) return;
                }
            }
            info.setReturnValue(true);
        }
    }
}
