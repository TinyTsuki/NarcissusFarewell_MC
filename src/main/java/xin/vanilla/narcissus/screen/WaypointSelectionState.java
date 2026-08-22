package xin.vanilla.narcissus.screen;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** 只保存界面选择的稳定身份，不持有会在同步后失效的数据对象。 */
final class WaypointSelectionState {
    private WaypointSelectionState() {
    }

    static <T> T findMatching(Key selected, List<T> candidates, Function<T, Key> keyMapper) {
        if (selected == null || candidates == null) return null;
        for (T candidate : candidates) {
            if (selected.equals(keyMapper.apply(candidate))) return candidate;
        }
        return null;
    }

    static Key findMatching(Key selected, List<Key> candidates) {
        return findMatching(selected, candidates, value -> value);
    }

    static <T> T afterRefresh(Key previousSelection, T loadedDefault,
                              List<T> candidates, Function<T, Key> keyMapper) {
        return previousSelection == null
                ? loadedDefault
                : findMatching(previousSelection, candidates, keyMapper);
    }

    static <T> T afterDelete(T selected, T deleted) {
        return selected == deleted ? null : selected;
    }

    static final class Key {
        private final String type;
        private final String name;
        private final String dimension;
        private final Object recordTime;

        Key(String type, String name, String dimension, Object recordTime) {
            this.type = type;
            this.name = name;
            this.dimension = dimension;
            this.recordTime = recordTime;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key)) return false;
            Key that = (Key) other;
            return Objects.equals(type, that.type)
                    && Objects.equals(name, that.name)
                    && Objects.equals(dimension, that.dimension)
                    && Objects.equals(recordTime, that.recordTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name, dimension, recordTime);
        }
    }
}
