package org.hdhmc.hyperwallpapermonet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class ReflectionUtils {
    private ReflectionUtils() {
    }

    static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(type.getName() + "#" + name);
    }

    static Object getField(Object target, String name) throws ReflectiveOperationException {
        return findField(target.getClass(), name).get(target);
    }

    static void setBooleanField(Object target, String name, boolean value) throws ReflectiveOperationException {
        findField(target.getClass(), name).setBoolean(target, value);
    }

    static int getIntField(Object target, String name, int fallback) {
        try {
            return findField(target.getClass(), name).getInt(target);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    static Object callNoArg(Object target, String name) throws ReflectiveOperationException {
        return findMethod(target.getClass(), name).invoke(target);
    }

    static Object callBoolean(Object target, String name, boolean value)
            throws ReflectiveOperationException {
        return findMethod(target.getClass(), name, Boolean.TYPE).invoke(target, value);
    }
}
