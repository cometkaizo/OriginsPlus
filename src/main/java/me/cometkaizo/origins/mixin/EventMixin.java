package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.event.PlayerItemReceivedEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

public final class EventMixin {

    @Mixin(PlayerInventory.class)
    public static abstract class MixedPlayerInventory {

        @Shadow @Final public PlayerEntity player;

        @Shadow @Final private List<NonNullList<ItemStack>> allInventories;

        @Inject(method = "add", at = @At("RETURN"))
        protected void triggerEvent(int slot, ItemStack stackLeft, CallbackInfoReturnable<Boolean> info) {
            if (info.getReturnValueZ()) MinecraftForge.EVENT_BUS.post(new PlayerItemReceivedEvent(player, stackLeft, slot));
        }

        @Inject(method = "setInventorySlotContents",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/util/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER),
                locals = LocalCapture.CAPTURE_FAILHARD)
        protected void triggerEvent1(int index, ItemStack stack, CallbackInfo info, NonNullList<ItemStack> inventoryList) {
            int slot = index;
            int invIndex = allInventories.indexOf(inventoryList);
            for (int i = 0; i < invIndex; i ++) slot += allInventories.get(i).size();

            MinecraftForge.EVENT_BUS.post(new PlayerItemReceivedEvent(player, stack, slot));
        }

    }

}
