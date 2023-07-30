package me.cometkaizo.origins.util;

import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import java.io.Serializable;
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
    public <T extends Serializable> void registerSaved(@Nonnull DataKey<T> key, T defaultValue, ResourceLocation namespace) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (contains(key)) return;
        addSavedEntry(key.getId(), key, defaultValue, namespace, INBTSerializer.getSerializer());
    }

    public <N extends INBT, T extends INBTSerializable<N>> void registerSaved(@Nonnull DataKey<T> key, T defaultValue, ResourceLocation namespace) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (contains(key)) return;
        addSavedEntry(key.getId(), key, defaultValue, namespace, INBTSerializer.getSerializer(defaultValue));
    }

    public <T> void registerSaved(@Nonnull DataKey<T> key, T defaultValue, ResourceLocation namespace, INBTSerializer<T> serializer) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (contains(key)) return;
        addSavedEntry(key.getId(), key, defaultValue, namespace, serializer);
    }

    public <T extends Serializable> void registerSynced(@Nonnull DataKey<T> key, T defaultValue, ResourceLocation namespace) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (contains(key)) return;
        addSyncedEntry(key.getId(), key, defaultValue, namespace, INBTSerializer.getSerializer());
    }

    public <N extends INBT, T extends INBTSerializable<N>> void registerSynced(@Nonnull DataKey<T> key, T defaultValue, ResourceLocation namespace) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (contains(key)) return;
        addSyncedEntry(key.getId(), key, defaultValue, namespace, INBTSerializer.getSerializer(defaultValue));
    }

    public <T> void registerSynced(@Nonnull DataKey<T> key, T defaultValue, ResourceLocation namespace, INBTSerializer<T> serializer) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (contains(key)) return;
        addSyncedEntry(key.getId(), key, defaultValue, namespace, serializer);
    }

    public <T> void register(@Nonnull DataKey<T> key, T defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        throwIfDuplicateId(key);
        addEntry(key.getId(), key, defaultValue);
    }

    private void throwIfDuplicateId(DataKey<?> key) {
        Entry<?> duplicate = getEntryRaw(key);
        if (duplicate != null) {
            String keyTypeName = key.getType().getName();
            throw new IllegalArgumentException(keyTypeName + " key has ID " + key.getId() + " that is already used for value: " + duplicate.value);
        }
    }

    private <T> void addEntry(String id, @Nonnull DataKey<T> key, T value) {
        Entry<T> entry = new Entry<>(key, value);
        addEntry(id, entry);
    }

    private <T> void addSyncedEntry(String id, @Nonnull DataKey<T> key, T value, ResourceLocation namespace, INBTSerializer<T> serializer) {
        Entry<T> entry = new SerializableEntry<>(key, value, namespace, serializer, false);
        addEntry(id, entry);
    }

    private <T> void addSavedEntry(String id, @Nonnull DataKey<T> key, T value, ResourceLocation namespace, INBTSerializer<T> serializer) {
        Entry<T> entry = new SerializableEntry<>(key, value, namespace, serializer, true);
        addEntry(id, entry);
    }

    private void addEntry(String id, Entry<?> entry) {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(entry, "Entry cannot be null");
        lock.writeLock().lock();
        try {
            entries.put(id, entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Entry<T> getEntryRaw(DataKey<T> key) {
        this.lock.readLock().lock();

        try {
            return (Entry<T>) entries.get(key.getId());
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private <T> Entry<T> getEntry(DataKey<T> key) {
        Entry<T> entry = getEntryRaw(key);
        throwIfIllegalEntry(key, entry);
        return entry;
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
        final Entry<T> entry = getEntry(key);
        if (!Objects.equals(value, entry.value)) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (entry) {
                entry.value = value;
            }
        }
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


    public boolean contains(DataKey<?> key) {
        this.lock.readLock().lock();

        try {
            return entries.containsKey(key.getId());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            this.lock.readLock().unlock();
        }
    }


    public CompoundNBT serializeSynced() {
        lock.readLock().lock();

        try {
            CompoundNBT nbt = new CompoundNBT();
            for (Map.Entry<String, Entry<?>> data : entries.entrySet()) {
                Entry<?> entry = data.getValue();
                if (entry instanceof SerializableEntry) {
                    SerializableEntry<?> serializableEntry = (SerializableEntry<?>) entry;
                    nbt.put(serializableEntry.namespace.toString(), serializableEntry.serializeNBT());
                }
            }
            return nbt;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void deserializeSynced(CompoundNBT nbt) {
        lock.writeLock().lock();

        try {
            for (String key : nbt.keySet()) {
                INBT entryData = nbt.get(key);
                SerializableEntry<?> serializableEntry = getSerializableEntry(ResourceLocation.create(key, ':'));
                serializableEntry.deserializeNBT(entryData);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public CompoundNBT serializeNBT() {
        lock.readLock().lock();

        try {
            CompoundNBT nbt = new CompoundNBT();
            for (Map.Entry<String, Entry<?>> data : entries.entrySet()) {
                Entry<?> entry = data.getValue();
                if (entry instanceof SerializableEntry) {
                    SerializableEntry<?> serializableEntry = (SerializableEntry<?>) entry;
                    if (!serializableEntry.save) continue;
                    nbt.put(serializableEntry.namespace.toString(), serializableEntry.serializeNBT());
                }
            }
            return nbt;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deserializeNBT(INBT inbt) {
        lock.writeLock().lock();

        try {
            CompoundNBT compound = (CompoundNBT) inbt;
            for (String key : compound.keySet()) {
                INBT entryData = compound.get(key);
                SerializableEntry<?> serializableEntry = getSerializableEntry(ResourceLocation.create(key, ':'));
                serializableEntry.deserializeNBT(entryData);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private SerializableEntry<?> getSerializableEntry(ResourceLocation namespace) {
        return (SerializableEntry<?>) entries.values().stream()
                .filter(e -> e instanceof SerializableEntry)
                .filter(e -> ((SerializableEntry<?>) e).namespace.equals(namespace))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No saved entry with namespace '" + namespace + "'; available entries: " + entries));
    }

    @Override
    public String toString() {
        return entries.entrySet().stream().map(e ->
                "ID " + e.getKey() +
                        " type '" + e.getValue().key.getType().getName() +
                        "' : " + e.getValue().value
                ).collect(Collectors.joining(", \n", "[", "]"));
    }

    protected static class Entry<T> {
        protected @Nonnull DataKey<T> key;
        protected T value;

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

    protected static class SerializableEntry<T> extends Entry<T> implements INBTSerializable<INBT> {
        protected ResourceLocation namespace;
        protected INBTSerializer<T> serializer;
        protected boolean save;

        SerializableEntry(@Nonnull DataKey<T> key, T value, ResourceLocation namespace, INBTSerializer<T> serializer, boolean save) {
            super(key, value);
            this.namespace = namespace;
            this.serializer = serializer;
            this.save = save;
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
