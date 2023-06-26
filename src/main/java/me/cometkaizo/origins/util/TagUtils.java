package me.cometkaizo.origins.util;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;

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

    public static void putVector(CompoundNBT motionData, Vector3d motion) {
        motionData.putDouble("x", motion.x);
        motionData.putDouble("y", motion.y);
        motionData.putDouble("z", motion.z);
    }
    public static Vector3d getVector(CompoundNBT motionData) {
        return new Vector3d(motionData.getDouble("x"), motionData.getDouble("y"), motionData.getDouble("z"));
    }

}
