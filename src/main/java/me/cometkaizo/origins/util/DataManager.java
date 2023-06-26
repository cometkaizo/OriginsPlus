package me.cometkaizo.origins.util;

import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class DataManager implements INBTSerializable<INBT> {

    private final Map<String, Entry<?>> entries = new HashMap<>(1);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void registerSaved(@Nonnull DataKey<Integer> key, int defaultValue, ResourceLocation namespace) {
        registerSaved(key, defaultValue, namespace, new INBTSerializer<Integer>() {
            @Override
            public INBT serialize(Integer object) {
                return IntNBT.valueOf(object);
            }
            @Override
            public Integer deserialize(INBT nbt) {
                return ((IntNBT) nbt).getInt();
            }
        });
    }

    public void registerSaved(@Nonnull DataKey<Float> key, float defaultValue, ResourceLocation namespace) {
        registerSaved(key, defaultValue, namespace, new INBTSerializer<Float>() {
            @Override
            public INBT serialize(Float object) {
                return FloatNBT.valueOf(object);
            }
            @Override
            public Float deserialize(INBT nbt) {
                return ((FloatNBT) nbt).getFloat();
            }
        });
    }

    public void registerSaved(@Nonnull DataKey<Double> key, double defaultValue, ResourceLocation namespace) {
        registerSaved(key, defaultValue, namespace, new INBTSerializer<Double>() {
            @Override
            public INBT serialize(Double object) {
                return DoubleNBT.valueOf(object);
            }
            @Override
            public Double deserialize(INBT nbt) {
                return ((DoubleNBT) nbt).getDouble();
            }
        });
    }

    public void registerSaved(@Nonnull DataKey<Boolean> key, boolean defaultValue, ResourceLocation namespace) {
        registerSaved(key, defaultValue, namespace, new INBTSerializer<Boolean>() {
            @Override
            public INBT serialize(Boolean object) {
                return ByteNBT.valueOf(object);
            }
            @Override
            public Boolean deserialize(INBT nbt) {
                return ((ByteNBT) nbt).getByte() != 0;
            }
        });
    }

    public <T extends INBTSerializable<INBT>> void registerSaved(@Nonnull DataKey<T> key, T defaultValue, ResourceLocation namespace) {
        registerSaved(key, defaultValue, namespace, new INBTSerializer<T>() {
            @Override
            public INBT serialize(T object) {
                return object.serializeNBT();
            }
            @Override
            public T deserialize(INBT nbt) {
                defaultValue.deserializeNBT(nbt);
                return defaultValue;
            }
        });
    }

    public <T> void registerSaved(@Nonnull DataKey<T> key, T defaultValue, ResourceLocation namespace, INBTSerializer<T> serializer) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (entries.containsKey(key.getId())) return;
        addSavedEntry(key.getId(), key, defaultValue, namespace, serializer);
    }

    public <T> void register(@Nonnull DataKey<T> key, T defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        throwIfDuplicateId(key);
        addEntry(key.getId(), key, defaultValue);
    }

    private void throwIfDuplicateId(DataKey<?> key) {
        String id = key.getId();
        Entry<?> duplicate = entries.get(id);
        if (duplicate != null) {
            String keyTypeName = key.getType().getName();
            throw new IllegalStateException(keyTypeName + " key has ID " + id + " that is already used for value: " + duplicate.value);
        }
    }

    private <T> void addEntry(String id, @Nonnull DataKey<T> key, T value) {
        Entry<T> entry = new Entry<>(key, value);
        addEntry(id, entry);
    }

    private <T> void addSavedEntry(String id, @Nonnull DataKey<T> key, T value, ResourceLocation namespace, INBTSerializer<T> serializer) {
        Entry<T> entry = new SavedEntry<>(key, value, namespace, serializer);
        addEntry(id, entry);
    }

    private void addEntry(String id, Entry<?> entry) {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(entry, "Entry cannot be null");
        lock.writeLock().lock();
        entries.put(id, entry);
        lock.writeLock().unlock();
    }

    @SuppressWarnings("unchecked")
    private <T> Entry<T> getEntry(DataKey<T> key) {
        this.lock.readLock().lock();
        Entry<?> entry;

        try {
            entry = entries.get(key.getId());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            this.lock.readLock().unlock();
        }

        throwIfIllegalEntry(key, entry);
        return (Entry<T>) entry;
    }

    private void throwIfIllegalEntry(DataKey<?> key, Entry<?> entry) {
        if (key == null)
            throw new IllegalStateException("Key cannot be null");
        if (entry == null)
            throw new NoSuchElementException("There is no mapping for ID " + key.getId() + " type '" + key.getType().getName() + "'; available mappings are: " + this);
        if (!entry.key.equals(key))
            throw new IllegalStateException("Incorrect key has matching ID " + key.getId() + " for value: " + entry.value);
        if (entry.value != null && !key.getType().isAssignableFrom(entry.value.getClass()))
            throw new IllegalStateException("Key type '" + key.getType().getName() + "' is not compatible with value type '" + entry.value.getClass().getName() + '\'');
    }

    public <T> T get(DataKey<T> key) {
        return getEntry(key).value;
    }

    public <T> void set(DataKey<T> key, T value) {
        Entry<T> entry = getEntry(key);
        if (!Objects.equals(value, entry.value))
            entry.value = value;
    }

    public void increase(DataKey<Integer> key, int value) {
        set(key, get(key) + value);
    }
    public void increase(DataKey<Long> key, long value) {
        set(key, get(key) + value);
    }
    public void increase(DataKey<Double> key, double value) {
        set(key, get(key) + value);
    }
    public void increase(DataKey<Float> key, float value) {
        set(key, get(key) + value);
    }

    public void decrease(DataKey<Integer> key, int value) {
        set(key, get(key) - value);
    }
    public void decrease(DataKey<Long> key, long value) {
        set(key, get(key) - value);
    }
    public void decrease(DataKey<Double> key, double value) {
        set(key, get(key) - value);
    }
    public void decrease(DataKey<Float> key, float value) {
        set(key, get(key) - value);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        for (Map.Entry<String, Entry<?>> data : entries.entrySet()) {
            Entry<?> entry = data.getValue();
            if (entry instanceof SavedEntry) {
                SavedEntry<?> savedEntry = (SavedEntry<?>) entry;
                nbt.put(savedEntry.namespace.toString(), savedEntry.serializeNBT());
            }
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(INBT inbt) {
        CompoundNBT compound = (CompoundNBT) inbt;
        for (String key : compound.keySet()) {
            INBT entryData = compound.get(key);
            SavedEntry<?> savedEntry = getSavedEntry(ResourceLocation.create(key, ':'));
            savedEntry.deserializeNBT(entryData);
        }
    }

    private SavedEntry<?> getSavedEntry(ResourceLocation namespace) {
        return (SavedEntry<?>) entries.values().stream()
                .filter(e -> e instanceof SavedEntry && ((SavedEntry<?>) e).namespace.equals(namespace))
                .findAny()
                .orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public String toString() {
        return entries.entrySet().stream().map(e ->
                "ID " + e.getKey() +
                        " type '" + e.getValue().key.getType().getName() +
                        "' : " + e.getValue().value
                ).collect(Collectors.joining(", \n", "[", "]"));
    }

    private static class Entry<T> {
        @Nonnull DataKey<T> key;
        T value;

        Entry(@Nonnull DataKey<T> key, T value) {
            Objects.requireNonNull(key, "Key cannot be null");
            this.value = value;
            this.key = key;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '{' +
                    "key=" + key +
                    ", value=" + value +
                    '}';
        }
    }

    private static class SavedEntry<T> extends Entry<T> implements INBTSerializable<INBT> {
        ResourceLocation namespace;
        INBTSerializer<T> serializer;

        SavedEntry(@Nonnull DataKey<T> key, T value, ResourceLocation namespace, INBTSerializer<T> serializer) {
            super(key, value);
            this.namespace = namespace;
            this.serializer = serializer;
        }

        @Override
        public INBT serializeNBT() {
            return serializer.serialize(value);
        }

        @Override
        public void deserializeNBT(INBT nbt) {
            value = serializer.deserialize(nbt);
        }
    }
}
