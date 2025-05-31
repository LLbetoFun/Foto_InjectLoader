package com.fun.utils.unsafe;

import com.fun.inject.mapper.Mapper;
import com.fun.utils.asm.ASMUtils;
import lombok.Getter;
import lombok.val;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author TIMER_err
 * since 2025/3/28 21:17
 * IntelliJ IDEA
 */
@Getter
public class UnsafeField {
    private static final Unsafe unsafe;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {

            throw new RuntimeException("Failed to get unsafe instance");
        }
    }

    private final Field field;
    private final Class<?> type;
    private final Object base;
    private final long offset;
    private final boolean isStatic;

    public UnsafeField(Class<?> owner, String name) {
        try {
            field = owner.getDeclaredField(Mapper.map(name,Mapper.getMappedClass(ASMUtils.slash(owner.getName())), null));
            type = field.getType();
            val modifiers = field.getModifiers();
            isStatic = Modifier.isStatic(modifiers);

            if (isStatic) {
                base = unsafe.staticFieldBase(field);
                offset = unsafe.staticFieldOffset(field);
            } else {
                base = null;
                offset = unsafe.objectFieldOffset(field);
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAccess(Object obj) {
        if (!isStatic && obj == null) {
            throw new NullPointerException("Cannot access instance field with null object");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        if (obj == null) return null;
        if (type == int.class) {
            return (T) Integer.valueOf(unsafe.getInt(obj, offset));
        } else if (type == long.class) {
            return (T) Long.valueOf(unsafe.getLong(obj, offset));
        } else if (type == boolean.class) {
            return (T) Boolean.valueOf(unsafe.getBoolean(obj, offset));
        } else if (type == byte.class) {
            return (T) Byte.valueOf(unsafe.getByte(obj, offset));
        } else if (type == char.class) {
            return (T) Character.valueOf(unsafe.getChar(obj, offset));
        } else if (type == short.class) {
            return (T) Short.valueOf(unsafe.getShort(obj, offset));
        } else if (type == float.class) {
            return (T) Float.valueOf(unsafe.getFloat(obj, offset));
        } else if (type == double.class) {
            return (T) Double.valueOf(unsafe.getDouble(obj, offset));
        } else {
            return (T) unsafe.getObject(obj, offset);
        }
    }

    public int getInt(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return obj != null ? unsafe.getInt(obj, offset) : 0;
    }

    public long getLong(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return obj != null ? unsafe.getLong(obj, offset) : 0;
    }

    public boolean getBoolean(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return obj != null && unsafe.getBoolean(obj, offset);
    }

    public byte getByte(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return obj != null ? unsafe.getByte(obj, offset) : 0;
    }

    public char getChar(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return obj != null ? unsafe.getChar(obj, offset) : 0;
    }

    public short getShort(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return obj != null ? unsafe.getShort(obj, offset) : 0;
    }

    public float getFloat(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return obj != null ? unsafe.getFloat(obj, offset) : 0;
    }

    public double getDouble(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return obj != null ? unsafe.getDouble(obj, offset) : 0;
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(Object obj) {
        if (isStatic) {
            obj = base;
        } else {
            checkAccess(obj);
        }
        return (T) unsafe.getObject(obj, offset);
    }

    public void set(Object obj, Object value) {
        if (type == int.class) {
            unsafe.putInt(obj, offset, (int) value);
        } else if (type == long.class) {
            unsafe.putLong(obj, offset, (long) value);
        } else if (type == boolean.class) {
            unsafe.putBoolean(obj, offset, (boolean) value);
        } else if (type == byte.class) {
            unsafe.putByte(obj, offset, (byte) value);
        } else if (type == char.class) {
            unsafe.putChar(obj, offset, (char) value);
        } else if (type == short.class) {
            unsafe.putShort(obj, offset, (short) value);
        } else if (type == float.class) {
            unsafe.putFloat(obj, offset, (float) value);
        } else if (type == double.class) {
            unsafe.putDouble(obj, offset, (double) value);
        } else {
            unsafe.putObject(obj, offset, value);
        }
    }
}
