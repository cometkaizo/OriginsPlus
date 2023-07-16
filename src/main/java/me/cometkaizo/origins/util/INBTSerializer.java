package me.cometkaizo.origins.util;

import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.INBT;
import net.minecraftforge.common.util.INBTSerializable;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public interface INBTSerializer<T> {

    INBT serialize(T object);
    T deserialize(INBT nbt);

    static <V extends Serializable> INBTSerializer<V> getSerializer() {
        return new INBTSerializer<V>() {
            @Override
            public INBT serialize(V object) {
                return new ByteArrayNBT(SerializationUtils.serialize(object));
            }
            @Override
            public V deserialize(INBT nbt) {
                return SerializationUtils.deserialize(((ByteArrayNBT) nbt).getByteArray());
            }
        };
    }

    static <N extends INBT, V extends INBTSerializable<N>> INBTSerializer<V> getSerializer(V serializable) {
        return new INBTSerializer<V>() {
            @Override
            public N serialize(V object) {
                return object.serializeNBT(); //xn: might cause issues? if so change back to serializable.serializeNBT()
            }

            @Override
            public V deserialize(INBT nbt) {
                serializable.deserializeNBT((N) nbt);
                return serializable;
            }
        };
    }
}
