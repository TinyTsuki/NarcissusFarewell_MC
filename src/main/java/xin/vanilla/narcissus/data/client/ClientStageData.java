package xin.vanilla.narcissus.data.client;

import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.WaypointOrder;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public final class ClientStageData {
    private static final Map<KeyValue<String, String>, SafeWorldCoordinate> STAGE_COORDINATE = Collections.synchronizedMap(new LinkedHashMap<>());

    private ClientStageData() {
    }

    /**
     * 清空并替换为新的驿站数据
     */
    public static void setStageCoordinate(@Nonnull Map<KeyValue<String, String>, SafeWorldCoordinate> data) {
        STAGE_COORDINATE.clear();
        STAGE_COORDINATE.putAll(data);
    }

    /**
     * 添加驿站
     */
    public static void addStage(String dimension, String name, SafeWorldCoordinate safeWorldCoordinate) {
        KeyValue<String, String> key = new KeyValue<>(dimension, name);
        synchronized (STAGE_COORDINATE) {
            LinkedHashMap<KeyValue<String, String>, SafeWorldCoordinate> reordered =
                    WaypointOrder.prepend(key, safeWorldCoordinate, STAGE_COORDINATE);
            STAGE_COORDINATE.clear();
            STAGE_COORDINATE.putAll(reordered);
        }
    }

    /**
     * 移除驿站
     */
    public static void removeStage(String dimension, String name) {
        STAGE_COORDINATE.remove(new KeyValue<>(dimension, name));
    }

    /**
     * 获取所有驿站
     */
    @Nonnull
    public static Map<KeyValue<String, String>, SafeWorldCoordinate> getStageCoordinate() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(STAGE_COORDINATE));
    }

    public static void clear() {
        STAGE_COORDINATE.clear();
    }
}
