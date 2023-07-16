package me.cometkaizo.origins.mixin;

import com.mojang.authlib.GameProfile;
import me.cometkaizo.origins.Main;
import net.minecraft.server.management.OpList;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

public final class PlayerJoinMixin {
    @Mixin(PlayerList.class)
    public static abstract class MixedPlayerList {
        @Shadow @Final private OpList ops;

        @Inject(method = "canPlayerLogin", at = @At(value = "RETURN", ordinal = 3), cancellable = true)
        protected void checkAccessAndVersion(SocketAddress socketAddress, GameProfile profile, CallbackInfoReturnable<ITextComponent> info) {
            if (info.getReturnValue() == null) {
                if (Main.CLOSED_FOR_MAINTENANCE && !profile.getName().equals("CometKaizo") &&
                        ops.getEntry(profile) == null) {
                    info.setReturnValue(new TranslationTextComponent("multiplayer.disconnect.closed_for_maintenance"));
                }
            }
        }
    }
}
