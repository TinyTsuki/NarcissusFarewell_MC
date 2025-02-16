package xin.vanilla.narcissus.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.datasync.DataParameter;
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
     * 设置 类中声明的私有 target 字段值
     *
     * @param clazz     类
     * @param instance  实例
     * @param fieldName 字段名称
     * @param value     字段值
     */
    public static void setPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to set private field {} from {}", fieldName, clazz.getName(), e);
        }
    }

    private static String LANGUAGE_FIELD_NAME;

    /**
     * 获取玩家语言字段名称
     */
    public static String getPlayerLanguageFieldName(EntityPlayerMP player) {
        if (StringUtils.isNotNullOrEmpty(LANGUAGE_FIELD_NAME)) return LANGUAGE_FIELD_NAME;
        try {
            for (String field : FieldUtils.getPrivateFieldNames(EntityPlayerMP.class, String.class)) {
                String lang = (String) FieldUtils.getPrivateFieldValue(EntityPlayerMP.class, player, field);
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
            Class<?> clazz = EntityLivingBase.class;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(DataParameter.class) && field.getGenericType().getTypeName().contains("Float")) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        HEALTH_FIELD_NAME = field.getName();
                    }
                }
            }
        } catch (Exception e) {
            HEALTH_FIELD_NAME = "HEALTH";
            LOGGER.error("Failed to get entity health field name", e);
        }
        return HEALTH_FIELD_NAME;
    }

    private static final String[] structStartNames = new String[]{"Mineshaft", "Village", "Fortress", "Stronghold", "Temple", "Monument", "EndCity", "Mansion"};

    private static String START_NAME_TO_CLASS_MAP_FIELD_NAME;

    @SuppressWarnings("unchecked")
    public static String getStartNameToClassMapFieldName() {
        if (StringUtils.isNotNullOrEmpty(START_NAME_TO_CLASS_MAP_FIELD_NAME)) {
            return START_NAME_TO_CLASS_MAP_FIELD_NAME;
        }
        try {
            for (Field field : MapGenStructureIO.class.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Map<String, Class<? extends StructureStart>> value = (Map<String, Class<? extends StructureStart>>) field.get(null);
                    if (Arrays.stream(structStartNames).anyMatch(key -> value != null && value.containsKey(key))) {
                        return START_NAME_TO_CLASS_MAP_FIELD_NAME = field.getName();
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get start name to class map field name", e);
        }
        return START_NAME_TO_CLASS_MAP_FIELD_NAME = "startNameToClassMap";
    }
}
