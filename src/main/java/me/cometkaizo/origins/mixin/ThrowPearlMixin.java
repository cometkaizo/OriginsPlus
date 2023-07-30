package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.network.C2SThrowEnderianPearl;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.EnderianOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.SoundUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.Random;

public final class ThrowPearlMixin {
    @Mixin(Minecraft.class)
    public static abstract class MixedMinecraft {
        @Unique
        private static final Random originsPlus$RANDOM = new Random();
        @Shadow @Nullable public ClientPlayerEntity player;

        @Inject(method = "rightClickMouse",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 2),
                locals = LocalCapture.CAPTURE_FAILHARD)
        protected void throwPearlIfEmptyHand(CallbackInfo info,
                                             Hand[] hands,
                                             int var2, int var3,
                                             Hand hand,
                                             InputEvent.ClickInputEvent inputEvent,
                                             ItemStack itemstack) {
            if (hand == Hand.OFF_HAND && itemstack.isEmpty() &&
                    player != null && player.getHeldItem(Hand.MAIN_HAND).isEmpty()) originsPlus$tryThrowPearl();
        }

        @Inject(method = "rightClickMouse",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ActionResultType;isSuccessOrConsume()Z", ordinal = 3),
                locals = LocalCapture.CAPTURE_FAILHARD)
        protected void throwPearlIfNothingElseHappened(CallbackInfo ci,
                                                       Hand[] hands, int var2, int var3,
                                                       Hand hand,
                                                       InputEvent.ClickInputEvent inputEvent,
                                                       ItemStack itemstack,
                                                       ActionResultType itemActionResultType) {
            if (hand == Hand.OFF_HAND && itemActionResultType == ActionResultType.PASS &&
                    player != null && player.getHeldItem(Hand.MAIN_HAND).isEmpty())
                originsPlus$tryThrowPearl();
        }

        @Unique
        private void originsPlus$tryThrowPearl() {
            Origin origin = Origin.getOrigin(player);
            if (origin != null && origin.hasLabel(EnderianOriginType.Label.THROW_ENDER_PEARL)) {
                if (EnderianOriginType.hasPearlCooldown(origin)) return;

                SoundUtils.playSound(player, SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (originsPlus$RANDOM.nextFloat() * 0.4F + 0.8F));
                player.getCooldownTracker().setCooldown(Items.ENDER_PEARL, 20);
                Packets.sendToServer(new C2SThrowEnderianPearl());
            }
        }
    }
}
