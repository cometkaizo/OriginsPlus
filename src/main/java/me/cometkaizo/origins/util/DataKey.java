package me.cometkaizo.origins.util;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class DataKey<T> implements Serializable {
    private static final AtomicLong keyCount = new AtomicLong(Long.MIN_VALUE);
    private final String id;
    private final Class<T> type;

    private DataKey(Class<T> type, String id) {
        this.type = type;
        this.id = id;
    }

    public static <K> DataKey<K> create(Class<K> type) {
        return new DataKey<>(type, getNextId());
    }

    private static String getNextId() {
        return String.valueOf(keyCount.getAndIncrement());
    }

    public String getId() {
        return id;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "DataKey{" +
                "id='" + id + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataKey<?> dataKey = (DataKey<?>) o;
        return Objects.equals(id, dataKey.id) && Objects.equals(type, dataKey.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
