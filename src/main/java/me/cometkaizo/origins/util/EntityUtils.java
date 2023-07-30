package me.cometkaizo.origins.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class EntityUtils {

    public static boolean isWearingArmor(LivingEntity entity) {
        Iterable<ItemStack> armorList = entity.getArmorInventoryList();
        for (ItemStack i : armorList) {
            if (!i.isEmpty()) return true;
        }
        return false;
    }

    public static boolean isHoldingItem(LivingEntity entity) {
        Iterable<ItemStack> heldEquipment = entity.getHeldEquipment();
        for (ItemStack i : heldEquipment) {
            if (!i.isEmpty()) return true;
        }
        return false;
    }

}
