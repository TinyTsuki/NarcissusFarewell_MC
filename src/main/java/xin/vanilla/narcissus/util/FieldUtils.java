package xin.vanilla.narcissus.util;

import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureStart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FieldUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    private static String[] structStartNames = new String[]{"Mineshaft", "Village", "Fortress", "Stronghold", "Temple", "Monument", "EndCity", "Mansion"};

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

    public static String START_NAME_TO_CLASS_MAP_FIELD_NAME;

    public static String getStartNameToClassMapFieldName() {
        if (StringUtils.isNotNullOrEmpty(START_NAME_TO_CLASS_MAP_FIELD_NAME)) {
            return START_NAME_TO_CLASS_MAP_FIELD_NAME;
        }
        for (Field field : MapGenStructureIO.class.getDeclaredFields()) {
            LOGGER.info(field.getName());
            try {
                field.setAccessible(true);
                Map<String, Class<? extends StructureStart>> value = (Map<String, Class<? extends StructureStart>>) field.get(null);
                if (Arrays.stream(structStartNames).anyMatch(key -> value != null && value.containsKey(key))) {
                    return START_NAME_TO_CLASS_MAP_FIELD_NAME = field.getName();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get start name to class map field name", e);
            }
        }
        return START_NAME_TO_CLASS_MAP_FIELD_NAME = "startNameToClassMap";
    }
}
