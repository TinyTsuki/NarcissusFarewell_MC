package xin.vanilla.narcissus.internal.server.search;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CommonSearchConfiguration;
import xin.vanilla.narcissus.config.ConfigBaselineFixture;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.search.SearchChunkPool;
import xin.vanilla.narcissus.search.SearchTask;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

public class TeleportCoordinateGeometryTest {
    private Field platform;
    private Object previousPlatform;
    private CommonSearchConfiguration config;
    private SearchChunkPool pool;
    private NarcissusSearchRequest request;
    private final Set<String> retainedChunks = new HashSet<>();
    private final Access access = new Access();
    private int callbacks;

    @Before
    public void setup() throws Exception {
        net.minecraft.server.Bootstrap.bootStrap();
        platform = BaniraPlatforms.class.getDeclaredField("platform");
        platform.setAccessible(true);
        previousPlatform = platform.get(null);
        Map<String, Object> values = new HashMap<>();
        values.put("base.safeTeleport.safeChunkRange", 1);
        values.put("base.safeTeleport.setBlockWhenSafeNotFound", false);
        ConfigHolder holder = ConfigBaselineFixture.holderWithValues(CommonConfig.class, values);
        Method bind = ConfigBaselineFixture.class.getDeclaredMethod("bind", Class.class, BaniraConfigHandle.class);
        bind.setAccessible(true);
        bind.invoke(null, CommonConfig.class, holder);
        config = new CommonSearchConfiguration(holder);
        pool = new SearchChunkPool(new SearchChunkPool.Backend() {
            public boolean isReady(ResourceLocation dimension, int x, int z) {
                return true;
            }

            public void retain(ResourceLocation dimension, int x, int z) {
                retainedChunks.add(x + "," + z);
            }

            public void release(ResourceLocation dimension, int x, int z) {
                retainedChunks.remove(x + "," + z);
            }
        }, () -> true);
    }

    @After
    public void cleanup() throws Exception {
        try {
            if (request != null) request.close();
            if (pool != null) pool.close();
        } finally {
            if (platform != null) platform.set(null, previousPlatform);
        }
    }

    @Test
    public void negativeFractionalXSearchesContainingCell() {
        assertFound(-.5, 64.25, 8.25, EnumSafeMode.Y_C_OFFSET_3, -.5, 64.15, 8.5, "-1,0");
    }

    @Test
    public void negativeFractionalZSearchesContainingCell() {
        assertFound(8.25, 64.25, -.5, EnumSafeMode.Y_C_OFFSET_3, 8.5, 64.15, -.5, "0,-1");
    }

    @Test
    public void negativeFractionalXSearchesContainingChunk() {
        assertFound(-16.1, 64.25, 8.25, EnumSafeMode.NONE, -16.5, 64.15, 8.5, "-2,0");
    }

    @Test
    public void negativeFractionalZSearchesContainingChunk() {
        assertFound(8.25, 64.25, -16.1, EnumSafeMode.NONE, 8.5, 64.15, -16.5, "0,-2");
    }

    @Test
    public void mixedNegativeCellAndChunkResolveInOriginalContainingCell() {
        assertFound(-.5, 64.25, -16.1, EnumSafeMode.NONE, -.5, 64.15, -16.5, "-1,-2");
    }

    @Test
    public void upwardColumnStartsAtContainingNegativeYCell() {
        assertFound(8.25, -.5, 8.25, EnumSafeMode.Y_C_TO_T, 8.5, -.85, 8.5, "0,0");
    }

    @Test
    public void offsetSearchStartsAtContainingNegativeYCell() {
        assertFound(8.25, -16.1, 8.25, EnumSafeMode.Y_C_OFFSET_3, 8.5, -16.85, 8.5, "0,0");
    }

    @Test
    public void exactNegativeIntegerBoundariesStayInTheirOwnCell() {
        assertFound(-16, -1, -16, EnumSafeMode.NONE, -15.5, -.85, -15.5, "-1,-1");
    }

    @Test
    public void positiveFractionsKeepContainingCellAndChunk() {
        assertFound(15.9, 64.9, 16.1, EnumSafeMode.NONE, 15.5, 64.15, 16.5, "0,1");
    }

    @Test
    public void fractionsImmediatelyBelowZeroAndNegativeChunkEdgeStayOnNegativeSide() {
        assertFound(-.0001, 64.25, -16.0001, EnumSafeMode.NONE, -.5, 64.15, -16.5, "-1,-2");
    }

    @Test
    public void viewEndpointTwoCellsAcrossZeroIsNotDiscardedAsAdjacent() {
        open(point(-.5, 64, 8.5, EnumSafeMode.NONE), EnumTeleportType.TP_VIEW, true);
        request.view(-.5, 64, 8.5, .75, 0, 0, 2, false);
        advance();
        assertEquals(SearchTask.State.RESOLVED, request.state());
        assertEquals(NarcissusSearchRequest.ResultKind.VIEW_ENDPOINT, request.kind());
        assertPosition(request.destination(), 1, 64, 8.5);
        assertEquals(Collections.singleton("0,0"), retainedChunks);
        assertEquals(1, callbacks);
        assertFalse(access.viewNotFound);
        assertTrue(request.complete(() -> {}));
        assertTrue(retainedChunks.isEmpty());
    }

    @Test
    public void viewEndpointInAdjacentCellAcrossZeroIsStillDiscarded() {
        open(point(-.5, 64, 8.5, EnumSafeMode.NONE), EnumTeleportType.TP_VIEW, true);
        request.view(-.5, 64, 8.5, .75, 0, 0, 1, false);
        advance();
        assertEquals(SearchTask.State.CANCELLED, request.state());
        assertNull(request.destination());
        assertEquals(0, callbacks);
        assertTrue(access.viewNotFound);
    }

    @Test
    public void exhaustedSearchPreservesExactFractionalDestination() {
        access.safe = false;
        SafeWorldCoordinate target = point(-.5, -16.1, -16.1, EnumSafeMode.Y_C_OFFSET_3);
        open(target, EnumTeleportType.TP_COORDINATE, false);
        request.destination(target, EnumTeleportType.TP_COORDINATE, 0);
        advance();
        assertEquals(SearchTask.State.RESOLVED, request.state());
        assertEquals(NarcissusSearchRequest.ResultKind.EXHAUSTED_FALLBACK, request.kind());
        assertPosition(request.destination(), -.5, -16.1, -16.1);
        assertEquals(Collections.singleton("-1,-2"), retainedChunks);
        assertTrue(request.complete(() -> {}));
        assertTrue(retainedChunks.isEmpty());
    }

    private void assertFound(double x, double y, double z, EnumSafeMode mode,
                             double expectedX, double expectedY, double expectedZ, String chunk) {
        SafeWorldCoordinate target = point(x, y, z, mode);
        open(target, EnumTeleportType.TP_HOME, false);
        request.destination(target, EnumTeleportType.TP_HOME, 0);
        advance();
        assertEquals(SearchTask.State.RESOLVED, request.state());
        assertEquals(NarcissusSearchRequest.ResultKind.FOUND, request.kind());
        assertPosition(request.destination(), expectedX, expectedY, expectedZ);
        assertEquals(Collections.singleton(chunk), retainedChunks);
        assertEquals(1, callbacks);
        assertTrue(request.complete(() -> {}));
        assertTrue(retainedChunks.isEmpty());
        assertEquals(0, pool.leaseCount());
    }

    private static void assertPosition(SafeWorldCoordinate coordinate, double x, double y, double z) {
        assertNotNull(coordinate);
        assertEquals(x, coordinate.x(), 1e-12);
        assertEquals(y, coordinate.y(), 1e-12);
        assertEquals(z, coordinate.z(), 1e-12);
        assertEquals(Level.OVERWORLD, coordinate.dimension());
    }

    private static SafeWorldCoordinate point(double x, double y, double z, EnumSafeMode mode) {
        return new SafeWorldCoordinate(x, y, z, Level.OVERWORLD).safe(true).safeMode(mode);
    }

    private void open(SafeWorldCoordinate origin, EnumTeleportType type, boolean view) {
        request = new NarcissusSearchRequest(UUID.randomUUID(), origin, config.capture(type, view), access,
                pool.open(Level.OVERWORLD.location()), resolved -> callbacks++);
    }

    private void advance() {
        for (int slices = 0; slices < 32; slices++) {
            if (request.state() == SearchTask.State.RESOLVED || request.state() == SearchTask.State.CANCELLED) return;
            pool.beginTick();
            request.step(64);
        }
        fail("Coordinate search did not terminate within the bounded test budget");
    }

    // World reads are controlled; coordinate conversion, cursors and leases remain real.
    private static final class Access implements NarcissusSearchRequest.Access {
        boolean safe = true;
        boolean viewNotFound;

        public boolean live() { return true; }
        public boolean policyMatches() { return true; }
        public void beginSlice() {}
        public boolean safe(BlockPos pos, boolean belowAir) { return safe; }
        public boolean blocksMotion(BlockPos pos) { return false; }
        public int minY() { return -64; }
        public int maxY() { return 319; }
        public BlockState supportState() { return null; }
        public boolean hasSupportItem(ItemStack item) { return false; }
        public SafeWorldCoordinate random(SafeWorldCoordinate origin, int range) {
            throw new AssertionError("This geometry test must not retry a random destination");
        }
        public void cancelTicket() {}
        public void failed(SearchTask.Failure reason) { fail("Unexpected search failure: " + reason); }
        public void viewNotFound(boolean safe) { viewNotFound = true; }
    }
}
