package xin.vanilla.narcissus.internal.neoforge.search;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import xin.vanilla.narcissus.search.SafeBlockPolicy;
import xin.vanilla.narcissus.search.SafeCandidateCursor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SectionSearchPruner {
    public interface ChunkAccess {
        boolean ready(int chunkX, int chunkZ);
        LevelChunkSection section(int chunkX, int chunkZ, int sectionY);
    }

    private static final int FOOT = 1, SUPPORT = 2, AIR_SUPPORT = 4;
    private final ChunkAccess access;
    private final SafeBlockPolicy policy;
    private final Map<Long, Map<Integer, Integer>> flags = new HashMap<>();

    public SectionSearchPruner(ChunkAccess access, SafeBlockPolicy policy) {
        this.access = Objects.requireNonNull(access);
        this.policy = Objects.requireNonNull(policy);
    }
    public void beginSlice() { flags.clear(); }
    public SafeCandidateCursor.YFilter filter(int minZ, int maxZ, boolean belowAir) {
        int firstZ = minZ >> 4, lastZ = maxZ >> 4;
        return (chunkX, minY, maxY) -> next(chunkX, firstZ, lastZ, minY, maxY, belowAir);
    }

    private long next(int x, int firstZ, int lastZ, long minY, long maxY, boolean air) {
        // Missing chunks cannot prove absence. This never requests or generates terrain.
        for (int z = firstZ; z <= lastZ; z++) if (!access.ready(x, z)) return minY;
        int supports = SUPPORT | (air ? AIR_SUPPORT : 0);
        for (long y = minY; y <= maxY;) {
            int sectionY = (int) (y >> 4);
            long bottom = (long) sectionY << 4;
            long end = Math.min(maxY, bottom + 15);
            long possible = Long.MAX_VALUE;
            for (int z = firstZ; z <= lastZ; z++) {
                int current = flags(x, z, sectionY);
                if ((current & FOOT) == 0) continue;
                if (y == bottom && (flags(x, z, sectionY - 1) & supports) != 0) return y;
                long interior = Math.max(y, bottom + 1);
                if (interior <= end && (current & supports) != 0) possible = interior;
            }
            if (possible != Long.MAX_VALUE) return possible;
            y = end + 1;
        }
        return Long.MAX_VALUE;
    }

    private int flags(int x, int z, int sectionY) {
        Map<Integer, Integer> column = flags.computeIfAbsent(ChunkPos.asLong(x, z), k -> new HashMap<>());
        Integer cached = column.get(sectionY);
        if (cached != null) return cached;
        LevelChunkSection section = access.section(x, z, sectionY);
        int value;
        if (section == null || section.hasOnlyAir()) {
            value = (canStandIn(Blocks.AIR.defaultBlockState()) ? FOOT : 0) | AIR_SUPPORT;
        } else {
            // maybeHas may include stale entries or the global palette: false positives are harmless.
            value = (section.maybeHas(this::canStandIn) ? FOOT : 0)
                    | (section.maybeHas(this::canSupport) ? SUPPORT : 0)
                    | (section.maybeHas(s -> s.is(Blocks.AIR) || s.is(Blocks.CAVE_AIR)) ? AIR_SUPPORT : 0);
        }
        column.put(sectionY, value);
        return value;
    }

    private boolean canStandIn(BlockState state) {
        return !policy.unsafe(state);
    }
    private boolean canSupport(BlockState state) {
        // Shape and entity-specific support is rechecked at the actual candidate.
        return !state.isAir() && !policy.unsafe(state);
    }
}
