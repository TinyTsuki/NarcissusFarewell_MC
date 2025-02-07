package xin.vanilla.narcissus.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class FieldUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 获取 类中声明的私有 target 字段名称
     */
    public static List<String> getPrivateFieldNames(Class<?> clazz, Class<?> target) {
        List<String> fieldNames = new ArrayList<>();

        // 获取 类的所有声明字段（不包含父类字段）
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            // 检查字段是否为私有且类型为 target
            if (Modifier.isPrivate(field.getModifiers()) && field.getType() == target) {
                fieldNames.add(field.getName());
            }
        }
        return fieldNames;
    }

    public static Object getPrivateField(Class<?> clazz, Object instance, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to get private field {} from {}", fieldName, clazz.getName(), e);
        }
        return null;
    }
}
