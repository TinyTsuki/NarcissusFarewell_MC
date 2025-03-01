package xin.vanilla.narcissus.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class FieldUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access Unsafe instance", e);
        }
    }

    /**
     * 获取 类中声明的私有 target 字段名称
     *
     * @param clazz  类
     * @param target 字段类型
     * @return 字段名称
     */
    public static List<String> getPrivateFieldNames(Class<?> clazz, Class<?> target) {
        List<String> fieldNames = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isPrivate(field.getModifiers()) && field.getType() == target) {
                fieldNames.add(field.getName());
            }
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
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to get private field {} from {}", fieldName, clazz.getName(), e);
        }
        return null;
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
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            if (Modifier.isStatic(field.getModifiers())) {
                if (isJava8()) {
                    setStaticFieldByUnsafe(field, value);
                } else {
                    try {
                        setStaticFieldByVarHandle(clazz, fieldName, value);
                    } catch (Throwable t) {
                        setStaticFieldByUnsafe(field, value);
                    }
                }
            } else {
                setInstanceFieldByUnsafe(instance, field, value);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to set private field {} from {}", fieldName, clazz.getName(), e);
        }
    }

    private static boolean isJava8() {
        String version = System.getProperty("java.version");
        return version.startsWith("1.8");
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

    private static void setStaticFieldByVarHandle(Class<?> clazz, String fieldName, Object value) throws Throwable {
        VarHandle handle = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup())
                .findStaticVarHandle(clazz, fieldName, value.getClass());
        handle.set(value);
    }

    private static String LANGUAGE_FIELD_NAME;

    /**
     * 获取玩家语言字段名称
     */
    public static String getPlayerLanguageFieldName(ServerPlayer player) {
        if (StringUtils.isNotNullOrEmpty(LANGUAGE_FIELD_NAME)) return LANGUAGE_FIELD_NAME;
        try {
            for (String field : FieldUtils.getPrivateFieldNames(ServerPlayer.class, String.class)) {
                String lang = (String) FieldUtils.getPrivateFieldValue(ServerPlayer.class, player, field);
                if (StringUtils.isNotNullOrEmpty(lang) && lang.matches("^[a-zA-Z]{2}_[a-zA-Z]{2}$")) {
                    LANGUAGE_FIELD_NAME = field;
                }
            }
        } catch (Exception e) {
            LANGUAGE_FIELD_NAME = "language";
            LOGGER.error("Failed to get player language field name", e);
        }
        return LANGUAGE_FIELD_NAME;
    }

    private static String HEALTH_FIELD_NAME;

    /**
     * 获取实体生命字段名称
     */
    public static String getEntityHealthFieldName() {
        if (StringUtils.isNotNullOrEmpty(HEALTH_FIELD_NAME)) return HEALTH_FIELD_NAME;
        try {
            Class<?> clazz = LivingEntity.class;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(EntityDataAccessor.class) && field.getGenericType().getTypeName().contains("Float")) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        HEALTH_FIELD_NAME = field.getName();
                    }
                }
            }
        } catch (Exception e) {
            HEALTH_FIELD_NAME = "DATA_HEALTH_ID";
            LOGGER.error("Failed to get entity health field name", e);
        }
        return HEALTH_FIELD_NAME;
    }
}
