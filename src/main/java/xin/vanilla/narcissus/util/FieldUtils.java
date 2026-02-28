package xin.vanilla.narcissus.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;


@SuppressWarnings("sunapi")
public final class FieldUtils {
    private FieldUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String UNSAFE_FIELD_NAME;
    private static final Unsafe UNSAFE;


    /**
     * 缓存字段
     */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    static {
        try {
            List<String> names = getPrivateFieldNames(Unsafe.class, Unsafe.class);
            if (CollectionUtils.isNotNullOrEmpty(names)) {
                UNSAFE_FIELD_NAME = names.get(0);
            } else {
                UNSAFE_FIELD_NAME = "theUnsafe";
            }
            UNSAFE = (Unsafe) getPrivateFieldValue(Unsafe.class, null, UNSAFE_FIELD_NAME);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access Unsafe instance", e);
        }
    }

    public static Class<?> getClass(Object o) {
        return o == null ? null : o.getClass();
    }

    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to get class {}", className, e);
        }
        return null;
    }

    /**
     * 获取 类中声明的私有 target 字段名称（仅当前类）
     *
     * @param clazz  类
     * @param target 字段类型
     * @return 字段名称
     */
    public static List<String> getPrivateFieldNames(Class<?> clazz, Class<?> target) {
        return getPrivateFieldNames(clazz, target, false, false, false);
    }

    /**
     * 获取 类中声明的私有 target 字段名称
     *
     * @param clazz  类
     * @param target 字段类型
     * @return 字段名称
     */
    public static List<String> getPrivateFieldNames(Class<?> clazz, Class<?> target, boolean parent, boolean targetFrom, boolean targetInstance) {
        List<String> fieldNames = new ArrayList<>();
        Class<?> cur = clazz;
        try {
            do {
                Field[] fields = cur.getDeclaredFields();
                for (Field field : fields) {
                    if ((Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers()))
                            && ((field.getType() == target)
                            || (targetFrom && target.isAssignableFrom(field.getType()))
                            || (targetInstance && target.isAssignableFrom(field.getType())))
                    ) {
                        fieldNames.add(field.getName());
                    }
                }
                cur = cur.getSuperclass();
            } while (parent && cur != Object.class);
        } catch (Exception e) {
            LOGGER.error("Failed to get private field names from {}", cur.getName(), e);
        }
        return fieldNames;
    }

    /**
     * 获取所有(含父类)字段名称，允许自定义过滤条件
     *
     * @param clazz          起始类
     * @param includeParent  是否遍历父类
     * @param fieldPredicate 字段过滤器
     */
    public static List<String> getFieldNames(Class<?> clazz, boolean includeParent, Predicate<Field> fieldPredicate) {
        List<String> fieldNames = new ArrayList<>();
        if (clazz == null || fieldPredicate == null) return fieldNames;
        Class<?> cur = clazz;
        try {
            do {
                for (Field field : cur.getDeclaredFields()) {
                    if (fieldPredicate.test(field)) {
                        fieldNames.add(field.getName());
                    }
                }
                cur = cur.getSuperclass();
            } while (includeParent && cur != null && cur != Object.class);
        } catch (Exception e) {
            LOGGER.error("Failed to get field names from {}", cur.getName(), e);
        }
        return fieldNames;
    }

    /**
     * 获取 类中声明的私有 target 字段值
     *
     * @param clazz     类
     * @param instance  实例
     * @param fieldName 字段名称
     */
    public static Object getPrivateFieldValue(Class<?> clazz, Object instance, String fieldName) {
        return getPrivateFieldValue(clazz, instance, fieldName, false);
    }

    /**
     * 获取 类中声明的私有 target 字段值
     *
     * @param clazz     类
     * @param instance  实例
     * @param fieldName 字段名称
     */
    public static Object getPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, boolean parent) {
        Field field = findField(clazz, fieldName, parent);
        if (field == null) return null;
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to get private field {} from {}", fieldName, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 类型安全的字段读取（支持父类）
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, boolean parent, Class<T> type) {
        Object value = getPrivateFieldValue(clazz, instance, fieldName, parent);
        if (value == null) return null;
        if (type != null && !type.isInstance(value)) {
            LOGGER.error("Field {} in {} is not of expected type {}", fieldName, clazz.getName(), type.getName());
            return null;
        }
        return (T) value;
    }

    /**
     * 类型安全的字段读取（自动从实例类型推断 & 遍历父类）
     */
    public static <T> T getPrivateFieldValue(Object instance, String fieldName, Class<T> type) {
        Objects.requireNonNull(instance, "instance");
        return getPrivateFieldValue(instance.getClass(), instance, fieldName, true, type);
    }

    /**
     * 设置 类中声明的私有 target 字段值 (支持private+final+static)
     *
     * @param clazz     类
     * @param instance  实例 (若为static字段应传null)
     * @param fieldName 字段名称
     * @param value     字段值
     */
    public static void setPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, Object value) {
        setPrivateFieldValue(clazz, instance, fieldName, value, false);
    }

    /**
     * 设置 类中声明的私有 target 字段值 (支持private+final+static) 并可选向上查找父类
     */
    public static void setPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, Object value, boolean parent) {
        Field field = findField(clazz, fieldName, parent);
        if (field == null) {
            LOGGER.error("Failed to locate private field {} on {}", fieldName, clazz.getName());
            return;
        }
        try {
            field.setAccessible(true);

            if (!Modifier.isStatic(field.getModifiers())
                    && (clazz.isRecord() || field.getDeclaringClass().isRecord())) {
                field.set(instance, value);
                return;
            }

            if (Modifier.isStatic(field.getModifiers())) {
                setStaticFieldByUnsafe(field, value);
            } else {
                setInstanceFieldByUnsafe(instance, field, value);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to set private field {} from {}", fieldName, clazz.getName(), e);
        }
    }

    /**
     * 根据 Class + 字段名 查找字段（向上查找父类），并带有简单缓存
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        return findField(clazz, fieldName, true);
    }

    /**
     * 根据 Class + 字段名 查找字段，可选是否向上查找父类，并带有简单缓存
     */
    private static Field findField(Class<?> clazz, String fieldName, boolean parent) {
        if (clazz == null || fieldName == null) return null;
        Map<String, Field> byName = FIELD_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        Field cached = byName.get(fieldName);
        if (cached != null) return cached;

        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            try {
                Field f = cur.getDeclaredField(fieldName);
                f.setAccessible(true);
                byName.put(fieldName, f);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
            if (!parent) break;
            cur = cur.getSuperclass();
        }
        return null;
    }

    private static void setInstanceFieldByUnsafe(Object instance, Field field, Object value) {
        long offset = UNSAFE.objectFieldOffset(field);
        UNSAFE.putObject(instance, offset, value);
    }

    private static void setStaticFieldByUnsafe(Field field, Object value) {
        Object base = UNSAFE.staticFieldBase(field);
        long offset = UNSAFE.staticFieldOffset(field);
        UNSAFE.putObject(base, offset, value);
    }

    public static Object newInstanceFromClassName(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Failed to create instance of class {}", className, e);
        }
        return null;
    }

    /**
     * 查找方法（支持父类查找，带参数类型兼容性检查）
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param args       参数数组（用于参数类型匹配）
     * @return 找到的方法，如果未找到则返回null
     */
    public static Method findMethod(Class<?> clazz, String methodName, Object[] args) {
        if (clazz == null || methodName == null) {
            return null;
        }

        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            Method[] methods = cur.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                    // 简单的参数类型检查
                    Class<?>[] paramTypes = method.getParameterTypes();
                    boolean match = true;
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (args[i] != null && !isCompatibleType(args[i].getClass(), paramTypes[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            cur = cur.getSuperclass();
        }

        return null;
    }

    /**
     * 智能查找方法
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param args       参数数组
     */
    public static MethodMatchResult findMethodWithTypeConversion(Class<?> clazz, String methodName, Object[] args) {
        if (clazz == null || methodName == null || args == null) {
            return new MethodMatchResult(null, null);
        }
        Method directMatch = findMethod(clazz, methodName, args);
        if (directMatch != null) {
            return new MethodMatchResult(directMatch, args);
        }
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            Method[] methods = cur.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] convertedArgs = convertArgsToMatchTypes(args, paramTypes);
                    if (convertedArgs != null) {
                        method.setAccessible(true);
                        return new MethodMatchResult(method, convertedArgs);
                    }
                }
            }
            cur = cur.getSuperclass();
        }

        return new MethodMatchResult(null, null);
    }

    /**
     * 方法匹配结果
     */
    public static class MethodMatchResult {
        public final Method method;
        public final Object[] args;

        public MethodMatchResult(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

    /**
     * 转换参数数组以匹配目标类型数组
     */
    private static Object[] convertArgsToMatchTypes(Object[] args, Class<?>[] targetTypes) {
        if (args.length != targetTypes.length) {
            return null;
        }

        Object[] converted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object convertedArg = convertToType(args[i], targetTypes[i]);
            if (convertedArg == null && args[i] != null) {
                // 转换失败且原始参数不为null
                return null;
            }
            converted[i] = convertedArg;
        }
        return converted;
    }

    /**
     * 将值转换为目标类型
     */
    private static Object convertToType(Object value, Class<?> targetType) {
        if (value == null) {
            if (!targetType.isPrimitive()) {
                return null;
            }
            if (targetType == boolean.class) return false;
            if (targetType == byte.class) return (byte) 0;
            if (targetType == short.class) return (short) 0;
            if (targetType == int.class) return 0;
            if (targetType == long.class) return 0L;
            if (targetType == float.class) return 0.0f;
            if (targetType == double.class) return 0.0;
            if (targetType == char.class) return '\u0000';
            return null;
        }
        Class<?> valueClass = value.getClass();
        if (targetType.isAssignableFrom(valueClass)) {
            return value;
        }
        if (value instanceof Number) {
            return convertNumber((Number) value, targetType);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) return value;
            if (value instanceof String) return Boolean.parseBoolean((String) value);
        }
        if (targetType == char.class || targetType == Character.class) {
            if (value instanceof Character) return value;
            if (value instanceof String) {
                String str = (String) value;
                return str.isEmpty() ? '\u0000' : str.charAt(0);
            }
        }
        if (targetType == String.class) {
            return value.toString();
        }

        return null;
    }

    /**
     * 转换数值类型
     */
    private static Object convertNumber(Number number, Class<?> targetType) {
        if (targetType == Byte.class || targetType == byte.class) {
            return number.byteValue();
        }
        if (targetType == Short.class || targetType == short.class) {
            return number.shortValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return number.intValue();
        }
        if (targetType == Long.class || targetType == long.class) {
            return number.longValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return number.floatValue();
        }
        if (targetType == Double.class || targetType == double.class) {
            return number.doubleValue();
        }
        return null;
    }

    /**
     * 类型兼容性检查
     */
    private static boolean isCompatibleType(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (to == int.class && (from == Integer.class || from == Double.class || from == Float.class)) {
            return true;
        }
        if (to == double.class && (from == Double.class || from == Integer.class || from == Float.class)) {
            return true;
        }
        if (to == float.class && (from == Float.class || from == Double.class || from == Integer.class)) {
            return true;
        }
        if (to == boolean.class && from == Boolean.class) {
            return true;
        }
        if (to == long.class && (from == Long.class || from == Double.class || from == Float.class || from == Integer.class)) {
            return true;
        }
        if (to == char.class && (from == Character.class || from == Integer.class)) {
            return true;
        }
        if (to == byte.class && (from == Byte.class || from == Integer.class)) {
            return true;
        }
        if (to == short.class && (from == Short.class || from == Integer.class)) {
            return true;
        }
        if (to == String.class && from == String.class) {
            return true;
        }
        return false;
    }

}
