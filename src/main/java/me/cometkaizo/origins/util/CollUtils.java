package me.cometkaizo.origins.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;

@SuppressWarnings("unused")
public class CollUtils {

    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        return ImmutableList.<T>builder().add(elements).build();
    }

    public static <T> List<T> copyOf(Collection<? extends T> collection) {
        return ImmutableList.copyOf(collection);
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        return ImmutableSet.<T>builder().add(elements).build();
    }

    public static int[] append(int[] array, int... elements) {
        if (elements.length == 0) return array;
        int[] result = new int[array.length + elements.length];
        System.arraycopy(elements, 0, result, array.length, elements.length);
        return result;
    }
    public static float[] append(float[] array, float... elements) {
        if (elements.length == 0) return array;
        float[] result = new float[array.length + elements.length];
        System.arraycopy(elements, 0, result, array.length, elements.length);
        return result;
    }
    public static double[] append(double[] array, double... elements) {
        if (elements.length == 0) return array;
        double[] result = new double[array.length + elements.length];
        System.arraycopy(elements, 0, result, array.length, elements.length);
        return result;
    }
    public static long[] append(long[] array, long... elements) {
        if (elements.length == 0) return array;
        long[] result = new long[array.length + elements.length];
        System.arraycopy(elements, 0, result, array.length, elements.length);
        return result;
    }

    public static <K, V> V getFirstKey(List<Map.Entry<K, V>> list, K key) {
        for (Map.Entry<K, V> entry : list) {
            if (Objects.equals(entry.getKey(), key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
