package xin.vanilla.narcissus.data;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 传送点顺序操作，不接触坐标内容本身。 */
public final class WaypointOrder {
    private WaypointOrder() {
    }

    public static <K, V> LinkedHashMap<K, V> prepend(K key, V value, Map<K, V> source) {
        LinkedHashMap<K, V> reordered = new LinkedHashMap<>();
        reordered.put(key, value);
        source.forEach(reordered::putIfAbsent);
        return reordered;
    }

    public static <K, V> LinkedHashMap<K, V> validated(Map<K, V> source, List<K> requested) {
        if (source.size() != requested.size()
                || new HashSet<>(source.keySet()).size() != source.size()
                || !new HashSet<>(source.keySet()).equals(new HashSet<>(requested))) {
            return null;
        }
        LinkedHashMap<K, V> reordered = new LinkedHashMap<>();
        for (K key : requested) {
            V value = source.get(key);
            if (value == null || reordered.put(key, value) != null) {
                return null;
            }
        }
        return reordered;
    }
}
