package me.cometkaizo.origins.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;

public class PlayerItemReceivedEvent extends PlayerEvent {
    private final ItemStack stackLeft;
    private final int slot;

    public PlayerItemReceivedEvent(PlayerEntity player, ItemStack stackLeft, int slot) {
        super(player);
        this.stackLeft = stackLeft;
        this.slot = slot;
    }

    public ItemStack getItemStack() {
        return slot >= 0 ? getPlayer().inventory.getStackInSlot(slot) : null;
    }

    @Nullable
    public ItemStack getItemStackLeft() {
        return stackLeft;
    }

    public int getSlot() {
        return slot;
    }
}
