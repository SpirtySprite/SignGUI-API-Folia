package me.kirug.gui.internal.reflect;

import me.kirug.gui.internal.version.ServerVersion;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// Resolve classes by name (Mojang first, then Spigot), but read members by type instead of
// their obfuscated names. A field typed BlockPos is a BlockPos whatever it's called.
public final class Reflect {

    private Reflect() {
    }

    public static Class<?> nms(String subPackage, String mojangName, String spigotName) {
        ServerVersion v = ServerVersion.current();

        // 1.16.x keeps everything in one versioned package under Spigot names.
        if (v.legacyVersionedPackage() && !v.craftbukkitTag().isEmpty()) {
            Class<?> legacy = tryClass("net.minecraft.server." + v.craftbukkitTag() + "." + spigotName);
            if (legacy != null) {
                return legacy;
            }
        }

        String primary = v.mojangMapped() ? mojangName : spigotName;
        String secondary = v.mojangMapped() ? spigotName : mojangName;

        Class<?> c = tryClass("net.minecraft." + subPackage + "." + primary);
        if (c == null) {
            c = tryClass("net.minecraft." + subPackage + "." + secondary);
        }
        if (c == null) {
            throw new IllegalStateException("Could not locate NMS class " + mojangName + "/" + spigotName
                    + " for server " + v);
        }
        return c;
    }

    public static Class<?> tryClass(String fqn) {
        try {
            return Class.forName(fqn);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Object handle(Object craftObject) {
        try {
            Method getHandle = craftObject.getClass().getMethod("getHandle");
            getHandle.setAccessible(true);
            return getHandle.invoke(craftObject);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("getHandle() failed on " + craftObject.getClass(), e);
        }
    }

    public static Field fieldOfType(Class<?> owner, Class<?> type) {
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == type) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        throw new IllegalStateException("No field of type " + type.getName() + " on " + owner.getName());
    }

    public static Field fieldAssignableFrom(Class<?> owner, Class<?> type) {
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType().isAssignableFrom(type) && f.getType() != Object.class) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        throw new IllegalStateException("No field assignable from " + type.getName() + " on " + owner.getName());
    }

    public static Object read(Field field, Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed reading field " + field, e);
        }
    }

    public static Method methodTaking(Class<?> owner, Class<?> paramType) {
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(paramType)) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        throw new IllegalStateException("No single-arg method taking " + paramType.getName() + " on " + owner.getName());
    }

    public static Constructor<?> constructor(Class<?> owner, Class<?>... paramTypes) {
        try {
            Constructor<?> ctor = owner.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No constructor " + owner.getSimpleName() + "(" + describe(paramTypes) + ")", e);
        }
    }

    public static Constructor<?> constructorWithArgCount(Class<?> owner, int argCount) {
        for (Constructor<?> ctor : owner.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == argCount) {
                ctor.setAccessible(true);
                return ctor;
            }
        }
        throw new IllegalStateException("No " + argCount + "-arg constructor on " + owner.getName());
    }

    public static Object newInstance(Constructor<?> ctor, Object... args) {
        try {
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Constructor invocation failed: " + ctor, e);
        }
    }

    public static Object invoke(Method method, Object instance, Object... args) {
        try {
            return method.invoke(instance, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Method invocation failed: " + method, e);
        }
    }

    private static String describe(Class<?>[] types) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(types[i].getSimpleName());
        }
        return sb.toString();
    }
}
