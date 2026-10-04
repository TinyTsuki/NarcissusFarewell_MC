package xin.vanilla.narcissus.internal.server;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.world.World;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CommonSearchConfiguration;
import xin.vanilla.narcissus.config.ConfigBaselineFixture;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.server.search.NarcissusSearchRequest;
import xin.vanilla.narcissus.search.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static org.junit.Assert.*;

public class NarcissusSearchServiceTest {
    private CommonSearchConfiguration config;
    private SearchChunkPool pool;
    private SearchCoordinator coordinator;
    private Access access;
    private long clock;
    private boolean ready;
    private int callbacks;
    private NarcissusSearchRequest resolved;
    private Object previousPlatform;
    private Set<String> retainedChunks;

    @Before public void setup() throws Exception {
        Bootstrap.bootStrap();
        java.lang.reflect.Field platform = xin.vanilla.banira.platform.BaniraPlatforms.class.getDeclaredField("platform");
        platform.setAccessible(true);
        previousPlatform = platform.get(null);
        Map<String, Object> values = new HashMap<>();
        values.put("base.safeTeleport.safeChunkRange", 1);
        values.put("base.safeTeleport.setBlockWhenSafeNotFound", true);
        values.put("base.safeTeleport.getBlockFromInventory", true);
        values.put("base.randomTeleport.tpRandomSafeNotFoundRetries", 2);
        xin.vanilla.banira.common.config.ConfigHolder holder = ConfigBaselineFixture.holderWithValues(CommonConfig.class,
                values);
        Method bind = ConfigBaselineFixture.class.getDeclaredMethod("bind", Class.class, xin.vanilla.banira.platform.BaniraConfigHandle.class);
        bind.setAccessible(true);
        bind.invoke(null, CommonConfig.class, holder);
        config = new CommonSearchConfiguration(holder);
        ready = true;
        retainedChunks = new HashSet<>();
        pool = new SearchChunkPool(new SearchChunkPool.Backend() {
            public boolean isReady(ResourceLocation d, int x, int z) { return ready; }
            public void retain(ResourceLocation d, int x, int z) { retainedChunks.add(x + "," + z); }
            public void release(ResourceLocation d, int x, int z) { retainedChunks.remove(x + "," + z); }
        }, () -> true);
        coordinator = new SearchCoordinator(() -> true, () -> new SearchExecutionSettings(10, 32, 1), () -> clock);
        access = new Access();
    }
    @After public void cleanup() throws Exception {
        coordinator.close();
        pool.close();
        java.lang.reflect.Field platform = xin.vanilla.banira.platform.BaniraPlatforms.class.getDeclaredField("platform");
        platform.setAccessible(true);
        platform.set(null, previousPlatform);
    }

    private SafeWorldCoordinate point() { return new SafeWorldCoordinate(8, 64, 8, World.OVERWORLD).safe(true).safeMode(EnumSafeMode.Y_C_OFFSET_3); }
    private NarcissusSearchRequest request(Consumer<NarcissusSearchRequest> callback) {
        NarcissusSearchRequest result = new NarcissusSearchRequest(access.id, point(), config.capture(EnumTeleportType.TP_HOME, false),
                access, pool.open(World.OVERWORLD.location()), callback);
        result.destination(point(), EnumTeleportType.TP_HOME, -1);
        return result;
    }
    private void start() {
        assertTrue(coordinator.submit(request(r -> { callbacks++; resolved = r; r.waitForCountdown(); })));
        tick();
    }
    private void tick() { pool.beginTick(); coordinator.tick(); }

    @Test public void foundWaitsAndCommitsExactlyOnce() {
        start();
        assertEquals(1, callbacks);
        assertEquals(NarcissusSearchRequest.ResultKind.FOUND, resolved.kind());
        assertEquals(1, pool.leaseCount());
        AtomicInteger payments = new AtomicInteger();
        assertTrue(resolved.complete(payments::incrementAndGet));
        assertFalse(resolved.complete(payments::incrementAndGet));
        tick();
        assertEquals(1, payments.get());
        assertEquals(0, pool.leaseCount());
        assertEquals(0, coordinator.activeCount());
    }
    @Test public void changingInputCoordinateCannotMoveCapturedSearch() {
        SafeWorldCoordinate input = point();
        NarcissusSearchRequest r = new NarcissusSearchRequest(access.id, input, config.capture(EnumTeleportType.TP_HOME, false),
                access, pool.open(World.OVERWORLD.location()), value -> { resolved = value; value.waitForCountdown(); });
        r.destination(input, EnumTeleportType.TP_HOME, -1);
        input.x(9999);
        assertTrue(coordinator.submit(r)); tick();
        assertEquals(8.5, resolved.destination().x(), 0);
    }
    @Test public void changedPlayerCancelsBeforePayment() { start(); access.live = false; assertFalse(resolved.complete(() -> fail("paid"))); tick(); assertEquals(0, pool.leaseCount()); }
    @Test public void changedPolicyCancelsBeforePayment() { start(); access.matches = false; assertFalse(resolved.complete(() -> fail("paid"))); assertEquals(0, pool.leaseCount()); }
    @Test public void changedWorldBlockCancelsFound() { start(); access.safe = false; assertFalse(resolved.complete(() -> fail("paid"))); assertEquals(0, pool.leaseCount()); }
    @Test public void timeoutNeverBecomesExhaustedFallback() {
        ready = false; start(); clock = 1000000001L; tick();
        assertEquals(0, callbacks); assertEquals(SearchTask.Failure.TIMEOUT, access.failure); assertEquals(0, pool.leaseCount());
    }
    @Test public void exhaustedKeepsOriginalFallback() {
        access.safe = false; access.air = false; start();
        assertEquals(NarcissusSearchRequest.ResultKind.EXHAUSTED_FALLBACK, resolved.kind());
        assertEquals(point().xyzString(), resolved.destination().xyzString());
        assertTrue(resolved.complete(() -> { }));
    }
    @Test public void waitingForOriginalFallbackKeepsFractionalCoordinates() {
        SafeWorldCoordinate target = new SafeWorldCoordinate(-.25, 300.75, -.25, World.OVERWORLD)
                .safe(true).safeMode(EnumSafeMode.Y_C_OFFSET_3);
        ready = false;
        NarcissusSearchRequest request = new NarcissusSearchRequest(access.id, point(),
                config.capture(EnumTeleportType.TP_HOME, false), access, pool.open(World.OVERWORLD.location()),
                value -> { resolved = value; value.waitForCountdown(); });
        request.destination(target, EnumTeleportType.TP_HOME, 0);
        assertTrue(coordinator.submit(request)); tick();
        assertEquals(SearchTask.State.WAITING_CHUNK, request.state());
        assertNull(resolved);
        ready = true; tick();
        assertNotNull(resolved);
        assertEquals(target.xyzString(), resolved.destination().xyzString());
        assertEquals(NarcissusSearchRequest.ResultKind.FOUND, resolved.kind());
        assertEquals(Collections.singleton("-1,-1"), retainedChunks);
        assertTrue(resolved.complete(() -> { })); tick();
        assertEquals(0, pool.leaseCount());
    }
    @Test public void supportMissingAtCommitCancelsWithoutPayment() {
        access.safe = false; access.air = true; access.support = Blocks.STONE.defaultBlockState(); start();
        assertEquals(NarcissusSearchRequest.ResultKind.SUPPORT_PLAN, resolved.kind());
        access.hasItem = false;
        assertFalse(resolved.complete(() -> fail("paid")));
        assertEquals(0, pool.leaseCount());
    }
    @Test public void callbackFailureAfterPaymentCannotRepeatOrReportUnpaid() {
        start(); AtomicInteger payments = new AtomicInteger();
        try { resolved.complete(() -> { payments.incrementAndGet(); throw new IllegalStateException("after payment"); }); fail(); }
        catch (IllegalStateException expected) { }
        tick(); assertEquals(1, payments.get()); assertNull(access.failure); assertEquals(0, pool.leaseCount());
    }
    @Test public void repeatedCancelAndStopReleaseOnlyOnce() {
        start(); resolved.cancel(SearchTask.Failure.CANCELLED); resolved.close(); resolved.close(); coordinator.close(); pool.close();
        assertEquals(1, access.cancels); assertEquals(0, pool.leaseCount());
    }
    @Test public void waitingChunkDoesNotLoseCandidate() {
        ready = false; start(); assertEquals(0, callbacks); ready = true; tick();
        assertEquals(1, callbacks); assertEquals(64.15, resolved.destination().y(), 0);
    }
    @Test public void resultIsCopied() { start(); resolved.destination().y(999); assertEquals(64.15, resolved.destination().y(), 0); }
    @Test public void viewSafeFlagDoesNotStartAnotherSafeSearch() {
        NarcissusSearchRequest r = new NarcissusSearchRequest(access.id, point().safeMode(EnumSafeMode.NONE),
                config.capture(EnumTeleportType.TP_VIEW, true), access, pool.open(World.OVERWORLD.location()),
                value -> { callbacks++; resolved = value; value.waitForCountdown(); });
        r.view(8, 65, 8, .75, 0, 0, 10, true);
        assertTrue(coordinator.submit(r)); tick(); assertEquals(1, callbacks); assertFalse(resolved.destination().safe());
        assertEquals(NarcissusSearchRequest.ResultKind.VIEW_ENDPOINT, resolved.kind());
        assertEquals(EnumSafeMode.Y_C_OFFSET_3, resolved.destination().safeMode());
    }
    @Test public void viewNearOriginDoesNotInvokeCompletion() {
        NarcissusSearchRequest r = request(value -> fail("resolved near origin"));
        access.motion = true; r.view(8, 64, 8, .75, 0, 0, 10, false);
        assertTrue(coordinator.submit(r)); tick(); assertEquals(0, pool.leaseCount()); assertEquals(1, access.cancels);
    }
    @Test public void viewExhaustionKeepsOriginalEndpointFallback() {
        access.safe = false;
        NarcissusSearchRequest r = request(value -> { resolved = value; value.waitForCountdown(); });
        r.view(8, 65, 8, .75, 0, 0, 10, true);
        assertTrue(coordinator.submit(r)); tick();
        assertEquals(15.5, resolved.destination().x(), 0);
        assertFalse(resolved.destination().safe());
        assertTrue(resolved.complete(() -> { }));
    }
    @Test public void safeViewHitIsRecheckedAfterCountdown() {
        NarcissusSearchRequest r = request(value -> { resolved = value; value.waitForCountdown(); });
        r.view(8, 65, 8, .75, 0, 0, 10, true);
        assertTrue(coordinator.submit(r)); tick(); access.safe = false;
        assertFalse(resolved.complete(() -> fail("paid unsafe view hit")));
    }
    @Test public void resolvedCountdownIsNotSubjectToSearchTimeout() { start(); clock = 2000000000L; tick(); assertTrue(resolved.complete(() -> { })); }
    @Test public void commitValidationExceptionCancelsAndReportsSearchFailure() {
        start(); access.throwCheck = true;
        try { resolved.complete(() -> fail("paid")); fail("expected validation failure"); } catch (IllegalStateException expected) { }
        assertEquals(SearchTask.Failure.ERROR, access.failure);
        assertEquals(0, pool.leaseCount());
    }
    @Test public void closingRequestReleasesItsCountdown() {
        start(); AtomicInteger closed = new AtomicInteger();
        resolved.onClose(closed::incrementAndGet);
        resolved.cancel(SearchTask.Failure.CANCELLED); resolved.close(); resolved.close();
        assertEquals(1, closed.get());
    }
    @Test public void closingDeadRequestStillCancelsTicket() {
        NarcissusSearchRequest r = request(value -> fail("unexpected completion"));
        access.live = false; r.close();
        assertEquals(1, access.cancels);
        assertEquals(0, pool.leaseCount());
    }
    @Test public void safetyReadThatReplacesTicketCannotCommitOldSearch() {
        start(); access.onCheck = () -> access.live = false;
        assertFalse(resolved.complete(() -> fail("paid replaced ticket")));
        assertEquals(0, pool.leaseCount());
    }
    @Test public void safetyReadThatChangesPolicyCannotCommit() {
        start(); access.onCheck = () -> access.matches = false;
        assertFalse(resolved.complete(() -> fail("paid changed policy")));
        assertEquals(0, pool.leaseCount());
    }
    @Test public void randomRetriesKeepCapturedOrigin() {
        access.safe = false; access.air = false;
        NarcissusSearchRequest r = new NarcissusSearchRequest(access.id, point(), config.capture(EnumTeleportType.TP_RANDOM, false),
                access, pool.open(World.OVERWORLD.location()), value -> { resolved = value; value.waitForCountdown(); });
        r.destination(point(), EnumTeleportType.TP_RANDOM, 16);
        assertTrue(coordinator.submit(r)); for (int i = 0; i < 50 && resolved == null; i++) tick();
        assertNotNull(resolved);
        assertEquals(config.capture(EnumTeleportType.TP_RANDOM, false).randomRetries(), access.retries);
        assertEquals(point().xyzString(), access.randomOrigin.xyzString());
    }

    @Test public void negativeFractionFoundRetainsActualBlockChunk() {
        for (int coordinate : new int[]{-1, -16, -17, -32}) {
            resolved = null;
            SafeWorldCoordinate target = point();
            target.x(coordinate).z(coordinate);
            NarcissusSearchRequest r = new NarcissusSearchRequest(access.id, point(), config.capture(EnumTeleportType.TP_HOME, false),
                    access, pool.open(World.OVERWORLD.location()), value -> { resolved = value; value.waitForCountdown(); });
            r.destination(target, EnumTeleportType.TP_HOME, 0);
            assertTrue(coordinator.submit(r)); tick();
            assertNull(access.failure);
            assertNotNull("negative half-block destination must resolve", resolved);
            assertEquals(coordinate + .5, resolved.destination().x(), 0);
            assertEquals(coordinate + .5, resolved.destination().z(), 0);
            assertTrue(resolved.complete(() -> { })); tick();
            assertEquals(0, pool.leaseCount());
            access.live = true;
        }
    }
    @Test public void negativeFractionViewRetainsActualBlockChunk() {
        NarcissusSearchRequest r = request(value -> { resolved = value; value.waitForCountdown(); });
        r.view(.25, 65, .25, -.75, 0, -.75, 22, false);
        assertTrue(coordinator.submit(r)); tick();
        assertNull(access.failure);
        assertNotNull(resolved);
        assertEquals(-16.25, resolved.destination().x(), 0);
        assertEquals(-16.25, resolved.destination().z(), 0);
        assertEquals(Collections.singleton("-2,-2"), retainedChunks);
        assertTrue(resolved.complete(() -> { })); tick();
        assertEquals(0, pool.leaseCount());
    }

    @Test public void unsupportedRadialSearchResolvesInOneTickWithOriginalFallback() {
        access.safe = false;
        access.heightFilter = (chunk, min, max) -> Long.MAX_VALUE;
        SafeWorldCoordinate target = point().safeMode(EnumSafeMode.NONE);
        NarcissusSearchRequest r = new NarcissusSearchRequest(access.id, target, config.capture(EnumTeleportType.TP_HOME, false),
                access, pool.open(World.OVERWORLD.location()), value -> { resolved = value; value.waitForCountdown(); });
        r.destination(target, EnumTeleportType.TP_HOME, 0);
        assertTrue(coordinator.submit(r)); tick();
        assertNotNull("No-support proof must bypass the per-block tick floor", resolved);
        assertEquals(NarcissusSearchRequest.ResultKind.EXHAUSTED_FALLBACK, resolved.kind());
        assertEquals(target.xyzString(), resolved.destination().xyzString());
        assertEquals(1, access.safeChecks);
        assertTrue(resolved.complete(() -> { })); tick();
        assertEquals(0, pool.leaseCount());
    }

    private static final class Access implements NarcissusSearchRequest.Access {
        final UUID id = UUID.randomUUID();
        boolean live = true, matches = true, safe = true, air, hasItem = true, motion, throwCheck;
        int cancels, retries;
        BlockState support;
        SafeWorldCoordinate randomOrigin;
        SearchTask.Failure failure;
        Runnable onCheck;
        SafeCandidateCursor.YFilter heightFilter;
        int safeChecks;
        public boolean live() { return live; }
        public boolean policyMatches() { return matches; }
        public void beginSlice() { }
        public SafeCandidateCursor.YFilter heightFilter(SearchBox box, boolean belowAir) { return belowAir ? null : heightFilter; }
        public boolean safe(BlockPos pos, boolean belowAir) {
            safeChecks++;
            if (throwCheck) throw new IllegalStateException("check failed");
            if (onCheck != null) onCheck.run();
            return belowAir ? air : safe;
        }
        public boolean blocksMotion(BlockPos pos) { return motion; }
        public int minY() { return 0; }
        public int maxY() { return 255; }
        public BlockState supportState() { return support; }
        public boolean hasSupportItem(ItemStack item) { return hasItem; }
        public SafeWorldCoordinate random(SafeWorldCoordinate origin, int range) { retries++; randomOrigin = origin.clone(); return pointOf(origin); }
        private SafeWorldCoordinate pointOf(SafeWorldCoordinate origin) { SafeWorldCoordinate c = origin.clone(); c.x(8).y(64).z(8); return c; }
        public void cancelTicket() { cancels++; live = false; }
        public void failed(SearchTask.Failure reason) { failure = reason; }
        public void viewNotFound(boolean safe) { }
    }
}
