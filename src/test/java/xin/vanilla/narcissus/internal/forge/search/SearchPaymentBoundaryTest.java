package xin.vanilla.narcissus.internal.forge.search;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CommonSearchConfiguration;
import xin.vanilla.narcissus.config.ConfigBaselineFixture;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.forge.cost.ForgeCostPlayerFixture;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import xin.vanilla.narcissus.internal.server.NarcissusSearchService;
import xin.vanilla.narcissus.search.SearchChunkPool;
import xin.vanilla.narcissus.search.SearchCoordinator;
import xin.vanilla.narcissus.search.SearchExecutionSettings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Exercises native player identities and the production pending-ticket lifecycle.
 */
public class SearchPaymentBoundaryTest {
    private final ForgeCostPlayerFixture fixture = new ForgeCostPlayerFixture();
    private NarcissusCostService costs;
    private Map<UUID, NarcissusCostService.Ticket> pending;
    private NarcissusCostService.Ticket ticket;
    private MinecraftServer server;

    @Before
    public void setup() throws Exception {
        fixture.setup();
        server = ForgeCostPlayerFixture.allocate(DedicatedServer.class);
        set(MinecraftServer.class, server, "serverThread", Thread.currentThread());
        costs = ForgeCostPlayerFixture.allocate(NarcissusCostService.class);
        set(NarcissusCostService.class, costs, "server", server);
        pending = new HashMap<>();
        set(NarcissusCostService.class, costs, "pending", pending);
        ticket = newTicket(1);
        pending.put(fixture.player().getUUID(), ticket);
    }

    @After
    public void cleanup() throws Exception {
        if (ticket != null) ticket.cancel();
        fixture.cleanup();
    }

    private NarcissusCostService.Ticket newTicket(long id) throws Exception {
        Constructor<?> constructor = NarcissusCostService.Ticket.class.getDeclaredConstructor(NarcissusCostService.class,
                long.class, long.class, xin.vanilla.narcissus.data.cost.CostConfiguration.class, UUID.class, UUID.class,
                UUID.class, xin.vanilla.narcissus.data.TeleportRequest.class, EnumTeleportType.class);
        constructor.setAccessible(true);
        return (NarcissusCostService.Ticket) constructor.newInstance(costs, id, 1L, null,
                fixture.player().getUUID(), fixture.player().getUUID(), null, null, EnumTeleportType.TP_HOME);
    }

    private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private boolean live(ServerPlayerEntity online, ServerWorld source) {
        return NarcissusSearchService.validActor(fixture.player(), online, source, ticket.live());
    }

    @Test
    public void walkingKeepsNativeActorValid() throws Exception {
        set(net.minecraft.entity.Entity.class, fixture.player(), "position", new net.minecraft.util.math.vector.Vector3d(80, 64, 90));
        assertTrue(live(fixture.player(), fixture.player().world));
        assertEquals(10, fixture.player().totalExperience);
    }

    @Test
    public void nativeDeathInvalidatesBeforePayment() {
        fixture.player().setHealth(0);
        assertFalse(live(fixture.player(), fixture.player().world));
        assertEquals(10, fixture.player().totalExperience);
    }

    @Test
    public void sameUuidReplacementIsNotSameActor() throws Exception {
        ForgeCostPlayerFixture.RecordingPlayer replacement = ForgeCostPlayerFixture.allocate(ForgeCostPlayerFixture.RecordingPlayer.class);
        replacement.id = fixture.player().getUUID();
        assertFalse(live(replacement, fixture.player().world));
        assertEquals(10, fixture.player().totalExperience);
    }

    @Test
    public void disconnectInvalidatesBeforePayment() {
        assertFalse(live(null, fixture.player().world));
    }

    @Test
    public void sourceWorldChangeInvalidatesBeforePayment() throws Exception {
        ServerWorld source = fixture.player().world;
        fixture.player().world = ForgeCostPlayerFixture.allocate(ServerWorld.class);
        assertFalse(live(fixture.player(), source));
    }

    @Test
    public void oldTicketCannotCancelReplacement() throws Exception {
        NarcissusCostService.Ticket replacement = newTicket(2);
        pending.put(fixture.player().getUUID(), replacement);
        assertFalse(live(fixture.player(), fixture.player().world));
        ticket.cancel();
        ticket.cancel();
        assertTrue(replacement.live());
        assertEquals(10, fixture.player().totalExperience);
        replacement.cancel();
        assertTrue(pending.isEmpty());
    }

    @Test
    public void configurationCaptureFailureCancelsOriginalTicketWithoutLease() throws Exception {
        xin.vanilla.banira.common.config.ConfigHolder holder = ConfigBaselineFixture.holderWithValues(CommonConfig.class, Collections.emptyMap());
        CommonSearchConfiguration config = new CommonSearchConfiguration(holder);
        bind(ConfigBaselineFixture.holderWithValues(CommonConfig.class, Collections.emptyMap()));
        SearchChunkPool pool = pool();
        NarcissusSearchService search = service(config, pool);
        assertFalse(search.searchDestination(fixture.player(), new SafeWorldCoordinate(8, 64, 8, World.OVERWORLD),
                EnumTeleportType.TP_HOME, -1, ticket, value -> fail("unexpected completion")));
        assertFalse(ticket.live());
        assertTrue(pending.isEmpty());
        assertEquals(0, pool.leaseCount());
        search.close();
    }

    @Test
    public void requestConstructionFailureReleasesAlreadyOpenedLease() throws Exception {
        xin.vanilla.banira.common.config.ConfigHolder holder = ConfigBaselineFixture.holderWithValues(CommonConfig.class, Collections.emptyMap());
        bind(holder);
        set(MinecraftServer.class, server, "levels", Collections.singletonMap(World.OVERWORLD, fixture.player().world));
        set(ServerPlayerEntity.class, fixture.player(), "server", server);
        // A malformed native actor fails coordinate capture after the request lease is opened.
        set(net.minecraft.entity.Entity.class, fixture.player(), "level", null);
        SearchChunkPool pool = pool();
        NarcissusSearchService search = service(new CommonSearchConfiguration(holder), pool);
        assertFalse(search.searchDestination(fixture.player(), new SafeWorldCoordinate(8, 64, 8, World.OVERWORLD),
                EnumTeleportType.TP_HOME, -1, ticket, value -> fail("unexpected completion")));
        assertFalse(ticket.live());
        assertEquals(0, pool.leaseCount());
        assertEquals(0, search.activeCount());
        search.close();
    }

    private void bind(xin.vanilla.banira.common.config.ConfigHolder holder) throws Exception {
        Method method = ConfigBaselineFixture.class.getDeclaredMethod("bind", Class.class, xin.vanilla.banira.platform.BaniraConfigHandle.class);
        method.setAccessible(true);
        method.invoke(null, CommonConfig.class, holder);
        xin.vanilla.banira.platform.BaniraPlatform bound = xin.vanilla.banira.platform.BaniraPlatforms.get();
        xin.vanilla.banira.platform.BaniraPlatforms.install((xin.vanilla.banira.platform.BaniraPlatform) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{xin.vanilla.banira.platform.BaniraPlatform.class}, (p, m, a) -> {
                    if (m.getName().equals("configService")) return bound.configService();
                    if (m.getReturnType() == boolean.class) return false;
                    if (m.getReturnType() == String.class) return "fixture";
                    return null;
                }));
    }

    private SearchChunkPool pool() {
        return new SearchChunkPool(new SearchChunkPool.Backend() {
            public boolean isReady(net.minecraft.util.ResourceLocation d, int x, int z) {
                return false;
            }

            public void retain(net.minecraft.util.ResourceLocation d, int x, int z) {
            }

            public void release(net.minecraft.util.ResourceLocation d, int x, int z) {
            }
        }, server::isSameThread);
    }

    private NarcissusSearchService service(CommonSearchConfiguration config, SearchChunkPool pool) throws Exception {
        NarcissusSearchService result = ForgeCostPlayerFixture.allocate(NarcissusSearchService.class);
        set(NarcissusSearchService.class, result, "server", server);
        set(NarcissusSearchService.class, result, "configuration", config);
        set(NarcissusSearchService.class, result, "pool", pool);
        set(NarcissusSearchService.class, result, "coordinator", new SearchCoordinator(server::isSameThread,
                () -> new SearchExecutionSettings(2, 32, 30), System::nanoTime));
        return result;
    }

    @Test
    public void failedNativeReleaseDoesNotAbortRemainingServerStopCallbacks() throws Exception {
        boolean[] failRelease = {true};
        SearchChunkPool pool = new SearchChunkPool(new SearchChunkPool.Backend() {
            public boolean isReady(net.minecraft.util.ResourceLocation d, int x, int z) {
                return true;
            }

            public void retain(net.minecraft.util.ResourceLocation d, int x, int z) {
            }

            public void release(net.minecraft.util.ResourceLocation d, int x, int z) {
                if (failRelease[0]) throw new IllegalStateException("native release failed");
            }
        }, server::isSameThread);
        pool.open(World.OVERWORLD.location()).require(0, 0);
        NarcissusSearchService search = service(null, pool);
        Field current = NarcissusSearchService.class.getDeclaredField("current");
        current.setAccessible(true);
        Object previous = current.get(null);
        try {
            current.set(null, search);
            NarcissusSearchService.stop();
            ticket.cancel();
            assertTrue(pending.isEmpty());
            assertNull(NarcissusSearchService.get());
        } finally {
            failRelease[0] = false;
            pool.close();
            current.set(null, previous);
        }
    }
}
