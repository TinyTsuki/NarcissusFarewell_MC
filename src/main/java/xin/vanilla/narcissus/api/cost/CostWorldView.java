package xin.vanilla.narcissus.api.cost;

import java.util.List;
import java.util.Optional;

public interface CostWorldView {
    String dimensionId();
    long gameTime();
    long dayTime();
    boolean raining();
    boolean thundering();
    /** Chunk coordinates, not block coordinates; never loads a missing chunk. */
    boolean chunkLoaded(int chunkX, int chunkZ);
    /** Empty outside the world or in an unloaded chunk. */
    Optional<String> blockId(int x, int y, int z);
    /** Queried on demand; elements are borrowed views with this invocation's lifetime. */
    List<CostPlayerView> players();
    <T> T nativeWorld(Class<T> type);
}
