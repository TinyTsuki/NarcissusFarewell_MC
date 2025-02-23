package xin.vanilla.narcissus.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.enums.EOperationType;

import java.util.*;
import java.util.stream.Collectors;

public class VirtualPermissionManager {

    public static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().create();

    private static final Map<String, Set<ECommandType>> OP_MAP = deserialize();

    /**
     * 添加权限（合并原有权限）
     */
    public static void addVirtualPermission(Player player, ECommandType... types) {
        modifyPermissions(player.getStringUUID(), EOperationType.ADD, types);
    }

    /**
     * 设置权限（覆盖原有权限）
     */
    public static void setVirtualPermission(Player player, ECommandType... types) {
        modifyPermissions(player.getStringUUID(), EOperationType.SET, types);
    }

    /**
     * 删除权限
     */
    public static void delVirtualPermission(Player player, ECommandType... types) {
        modifyPermissions(player.getStringUUID(), EOperationType.REMOVE, types);
    }

    /**
     * 清空所有权限
     */
    public static void clearVirtualPermission(Player player) {
        modifyPermissions(player.getStringUUID(), EOperationType.CLEAR);
    }

    /**
     * 获取当前权限列表
     */
    public static Set<ECommandType> getVirtualPermission(Player player) {
        return getExistingPermissions(player.getStringUUID());
    }

    public static String buildPermissionsString(ECommandType... types) {
        return Arrays.stream(types)
                .filter(ECommandType::isOp)
                .sorted(Comparator.comparingInt(ECommandType::getSort))
                .map(ECommandType::name)
                .collect(Collectors.joining(","));
    }

    public static String buildPermissionsString(Set<ECommandType> types) {
        return types.stream()
                .filter(ECommandType::isOp)
                .sorted(Comparator.comparingInt(ECommandType::getSort))
                .map(ECommandType::name)
                .collect(Collectors.joining(","));
    }

    private static void modifyPermissions(String stringUUID, EOperationType operation, ECommandType... types) {
        Set<ECommandType> newTypes = processOperation(getExistingPermissions(stringUUID), new HashSet<>(Arrays.asList(types)), operation);
        updateRuleList(stringUUID, newTypes);
    }

    /**
     * 查找现有规则
     */
    private static Set<ECommandType> getExistingPermissions(String uuid) {
        return OP_MAP.getOrDefault(uuid, new HashSet<>());
    }

    /**
     * 处理权限操作
     */
    private static Set<ECommandType> processOperation(Set<ECommandType> existing, Set<ECommandType> input, EOperationType operation) {
        Set<ECommandType> result = new LinkedHashSet<>(existing);
        switch (operation) {
            case ADD:
                result.addAll(input);
                break;
            case SET:
                result.clear();
                result.addAll(input);
                break;
            case DEL:
            case REMOVE:
                input.forEach(result::remove);
                break;
            case CLEAR:
                result.clear();
                break;
        }
        return result.stream().filter(ECommandType::isOp).collect(Collectors.toSet());
    }

    /**
     * 更新规则列表
     */
    private static void updateRuleList(String stringUUID, Set<ECommandType> types) {
        OP_MAP.put(stringUUID, types);
        ServerConfig.OP_LIST.set(serialize().toString());
        ServerConfig.OP_LIST.save();
    }

    private static JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        OP_MAP.forEach((uuid, types) -> {
            JsonArray jsonArray = new JsonArray();
            types.stream().map(ECommandType::name).forEach(jsonArray::add);
            jsonObject.add(uuid, jsonArray);
        });
        return jsonObject;
    }

    private static Map<String, Set<ECommandType>> deserialize(JsonObject jsonObject) {
        Map<String, Set<ECommandType>> map = new HashMap<>();
        jsonObject.entrySet().forEach(entry -> {
            Set<ECommandType> types = new HashSet<>();
            entry.getValue().getAsJsonArray().forEach(jsonElement -> types.add(ECommandType.valueOf(jsonElement.getAsString())));
            map.put(entry.getKey(), types);
        });
        return map;
    }

    private static Map<String, Set<ECommandType>> deserialize() {
        Map<String, Set<ECommandType>> result;
        try {
            result = deserialize(GSON.fromJson(ServerConfig.OP_LIST.get(), JsonObject.class));
        } catch (Exception e) {
            result = new HashMap<>();
        }
        return result;
    }

}
