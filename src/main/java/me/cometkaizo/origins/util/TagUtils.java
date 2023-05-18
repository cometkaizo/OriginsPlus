package me.cometkaizo.origins.util;

import net.minecraft.nbt.CompoundNBT;

@SuppressWarnings("unused")
public class TagUtils {

    public static void appendOrCreate(CompoundNBT data, String key, int... elements) {
        if (!data.contains(key))
            data.putIntArray(key, elements);
        else
            data.putIntArray(key, CollUtils.append(data.getIntArray(key), elements));
    }

    public static void appendOrCreate(CompoundNBT data, String key, long... elements) {
        if (!data.contains(key))
            data.putLongArray(key, elements);
        else
            data.putLongArray(key, CollUtils.append(data.getLongArray(key), elements));
    }

}
