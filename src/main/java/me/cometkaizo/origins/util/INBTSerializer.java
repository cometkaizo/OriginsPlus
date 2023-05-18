package me.cometkaizo.origins.util;

import net.minecraft.nbt.INBT;
import net.minecraftforge.common.util.INBTSerializable;

public interface INBTSerializer<T> {
    INBT serialize(T object);
    T deserialize(INBT nbt);

    static <V extends INBTSerializable<INBT>> INBTSerializer<V> getSerializer(V serializable) {
        return new INBTSerializer<V>() {
            @Override
            public INBT serialize(V object) {
                return serializable.serializeNBT();
            }

            @Override
            public V deserialize(INBT nbt) {
                serializable.deserializeNBT(nbt);
                return serializable;
            }
        };
    }
}
