package me.cometkaizo.origins.util;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraftforge.common.util.INBTSerializable;
import org.apache.commons.lang3.SerializationUtils;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class DataManager implements INBTSerializable<INBT> {

    private final Map<String, Entry<?>> entries = new HashMap<>(1);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public <T extends Serializable> void registerSaved(@Nonnull DataKey<T> key, T defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (entries.containsKey(key.getId())) return;
        addSavedEntry(key.getId(), key, defaultValue);
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

    private <T extends Serializable> void addSavedEntry(String id, @Nonnull DataKey<T> key, T value) {
        Entry<T> entry = new SavedEntry<>(key, value);
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
            throw new IllegalArgumentException("There is no mapping for ID " + key.getId() + " type '" + key.getType().getName() + "'; available mappings are: " + this);
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
            String id = data.getKey();
            Entry<?> entry = data.getValue();
            if (entry instanceof SavedEntry) {
                nbt.putByteArray(id, SerializationUtils.serialize((SavedEntry<?>) entry));
            }
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(INBT inbt) {
        CompoundNBT nbt = (CompoundNBT) inbt;
        for (String key : nbt.keySet()) {
            byte[] bytes = nbt.getByteArray(key);
            SavedEntry<?> entry = SerializationUtils.deserialize(bytes);
            addEntry(key, entry);
        }
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

    private static class SavedEntry<T extends Serializable> extends Entry<T> implements Serializable {
        SavedEntry(@Nonnull DataKey<T> key, T value) {
            super(key, value);
        }
    }
}
