package me.cometkaizo.origins.util;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

public class ClassUtils {

    public static Object getFieldOrThrow(String fieldName, @Nonnull Object instance) {
        Objects.requireNonNull(instance, "Reference object cannot be null");
        try {
            Field enchantmentField = instance.getClass().getDeclaredField(fieldName);
            enchantmentField.setAccessible(true);
            return enchantmentField.get(instance);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException("No field named '" + fieldName + "' in " + instance.getClass(), e);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not access field '" + fieldName + "' in " + instance.getClass(), e);
        }
    }
    public static Object getFieldOrThrow(String fieldName, @Nonnull Class<?> clazz) {
        try {
            Field enchantmentField = clazz.getDeclaredField(fieldName);
            enchantmentField.setAccessible(true);
            return enchantmentField.get(null);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException("No field named '" + fieldName + "' in " + clazz, e);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not access field '" + fieldName + "' in " + clazz, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldOfType(Class<T> fieldType, @Nonnull Object instance) {
        try {
            for (Field field : instance.getClass().getDeclaredFields()) {
                if (field.getType() == fieldType) {
                    field.setAccessible(true);
                    return (T) field.get(instance);
                }
            }
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not access field of type " + fieldType + " in " + instance.getClass(), e);
        }
        throw new UnsupportedOperationException("No field of type " + fieldType + " in " + instance.getClass() + ", all fields: " + Arrays.toString(instance.getClass().getDeclaredFields()));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldOfType(Class<T> fieldType, @Nonnull Class<?> clazz) {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == fieldType) {
                    field.setAccessible(true);
                    return (T) field.get(null);
                }
            }
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not access field of type " + fieldType + " in " + clazz, e);
        }
        throw new UnsupportedOperationException("No field of type " + fieldType + " in " + clazz + ", all fields: " + Arrays.toString(clazz.getDeclaredFields()));
    }
}
