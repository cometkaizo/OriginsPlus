package me.cometkaizo.origins.util;

import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;

@SuppressWarnings("unused")
public class AttributeUtils {

    public static ModifiableAttributeInstance getModifiableAttribute(PlayerEntity player, Attribute attribute) {
        return player.getAttributeManager().createInstanceIfAbsent(attribute);
    }

    public static boolean setAttribute(PlayerEntity player, Attribute attribute, double value) {
        ModifiableAttributeInstance modifiableAttribute = getModifiableAttribute(player, attribute);
        if (modifiableAttribute != null)
            modifiableAttribute.setBaseValue(value);
        return modifiableAttribute != null;
    }

}
