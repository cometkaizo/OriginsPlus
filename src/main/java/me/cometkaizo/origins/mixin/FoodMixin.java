package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.cometkaizo.origins.origin.FoxOriginType.Property.IGNORE_HUNGER_FOR_SWEET_BERRIES;

public final class FoodMixin {

    @Mixin(Item.class)
    public static abstract class MixedItem {

        @Inject(method = "onItemRightClick",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;canEat(Z)Z"),
                cancellable = true)
        protected void modifyCanEat(World world,
                                    PlayerEntity player,
                                    Hand hand,
                                    CallbackInfoReturnable<ActionResult<ItemStack>> info) {
            Origin origin = Origin.getOrigin(player);
            if (origin != null) {
                ItemStack itemStack = player.getHeldItem(hand);
                if (origin.hasProperty(IGNORE_HUNGER_FOR_SWEET_BERRIES) && itemStack.getItem() == Items.SWEET_BERRIES) {
                    player.setActiveHand(hand);
                    info.setReturnValue(ActionResult.resultConsume(itemStack));
                }
            }
        }

    }

}
