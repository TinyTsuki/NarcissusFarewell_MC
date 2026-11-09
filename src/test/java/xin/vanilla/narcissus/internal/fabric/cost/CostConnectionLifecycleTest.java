package xin.vanilla.narcissus.internal.fabric.cost;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.entity.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.narcissus.api.cost.CostContext;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.server.NarcissusCostRuntime;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;
import xin.vanilla.narcissus.service.cost.CostPaymentService;
import xin.vanilla.narcissus.service.cost.CostQuoteService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class CostConnectionLifecycleTest {
    private final FabricCostPlayerFixture fixture = new FabricCostPlayerFixture();
    private MinecraftServer server;
    private FabricCostPlayerFixture.RecordingPlayer player;
    private Map<UUID, net.minecraft.server.level.ServerPlayer> players;
    private FabricCostQuoteAccess access;
    private CostQuoteService quotes;
    private NarcissusCostService service;
    private Map<UUID, UUID> registrations;
    private int opens, payments;

    @Before
    public void setup() throws Exception {
        fixture.setup();
        player = fixture.player();
        server = FabricCostPlayerFixture.allocate(DedicatedServer.class);
        set(server, "serverThread", Thread.currentThread());
        DedicatedPlayerList list = FabricCostPlayerFixture.allocate(DedicatedPlayerList.class);
        players = new HashMap<>();
        players.put(player.getUUID(), player);
        set(list, "playersByUUID", players);
        set(server, "playerList", list);
        set(player.world, "server", server);
        set(player, "server", server);
        access = new FabricCostQuoteAccess(() -> server, () -> null, (p, t) -> 0);
        quotes = quotes(true);
        service = FabricCostPlayerFixture.allocate(NarcissusCostService.class);
        registrations = new HashMap<>();
        set(service, "server", server);
        set(service, "connections", registrations);
        set(service, "pending", new HashMap<>());
        set(service, "quotes", quotes);
        NarcissusCostRuntime runtime = FabricCostPlayerFixture.allocate(NarcissusCostRuntime.class);
        set(runtime, "ownerThread", Thread.currentThread());
        set(service, "runtime", runtime);
    }

    @After
    public void cleanup() throws Exception {
        fixture.cleanup();
    }

    private CostQuoteService quotes(boolean connected) {
        return new CostQuoteService(() -> 0, () -> 1, new CostQuoteService.QuoteAccess() {
            public boolean connected(UUID id) {
                return connected && access.connected(id);
            }

            public boolean permitted(UUID id, CostQuoteTarget target) {
                return player.isAlive();
            }

            public CostEvaluation open(UUID id, CostQuoteRequest request) {
                opens++;
                return null;
            }
        }, context -> CostCalculation.success(0, 0), new CostPaymentService.PaymentAccess() {
            public EnumCostFailure validate(CostContext context) {
                return EnumCostFailure.NONE;
            }

            public EnumCostFailure check(CostContext context, CostPaymentPlan plan) {
                return EnumCostFailure.NONE;
            }

            public EnumCostFailure pay(CostContext context, CostPaymentPlan plan) {
                payments++;
                return EnumCostFailure.NONE;
            }
        });
    }

    private CostQuoteRequest request(long id) {
        return new CostQuoteRequest(registrations.get(player.getUUID()), id, 1,
                CostQuoteTarget.home(player.getUUID(), "minecraft:overworld", "home"));
    }

    @Test
    public void deathDoesNotTurnARegisteredConnectionIntoADisconnect() {
        player.setHealth(0);
        assertTrue(access.connected(player.getUUID()));
        assertFalse(access.permitted(player.getUUID(), CostQuoteTarget.home(player.getUUID(), "minecraft:overworld", "home")));
        service.connect(player);
        assertEquals(1, registrations.size());
        assertEquals(1, quotes.connectionCount());
    }

    @Test
    public void deadQuoteIsRejectedWithoutDeletingSessionAndRespawnNeedsNoNewHandshake() {
        service.connect(player);
        UUID session = registrations.get(player.getUUID());
        player.setHealth(0);
        assertEquals(CostQuote.Status.REJECTED, quotes.quote(player.getUUID(), request(1)).status());
        assertEquals(1, quotes.connectionCount());
        assertEquals(0, opens);
        player.setHealth(20);
        assertEquals(CostQuote.Status.UNKNOWN, quotes.quote(player.getUUID(), request(2)).status());
        assertEquals(session, registrations.get(player.getUUID()));
        assertEquals(1, opens);
        assertEquals(0, payments);
    }

    @Test
    public void disconnectedCallbackDoesNotCreateHalfRegisteredState() {
        player.disconnected = true;
        assertFalse(access.connected(player.getUUID()));
        connectWithoutFailure();
        assertTrue(registrations.isEmpty());
        assertEquals(0, quotes.connectionCount());
    }

    @Test
    public void removedBodyKeepsTransportButCannotResolveQuotes() throws Exception {
        set(player, "removalReason", Entity.RemovalReason.KILLED);
        assertTrue(access.connected(player.getUUID()));
        assertFalse(access.permitted(player.getUUID(), CostQuoteTarget.home(player.getUUID(), "minecraft:overworld", "home")));
        service.connect(player);
        assertEquals(1, registrations.size());
        assertEquals(1, quotes.connectionCount());
    }

    @Test
    public void playerRemovedFromThePlayerListCannotRegister() {
        players.remove(player.getUUID());
        assertFalse(access.connected(player.getUUID()));
        connectWithoutFailure();
        assertTrue(registrations.isEmpty());
        assertEquals(0, quotes.connectionCount());
    }

    @Test
    public void oldPlayerCallbackCannotCreateOrReplaceTheNewPlayersSession() throws Exception {
        FabricCostPlayerFixture.RecordingPlayer old = player;
        FabricCostPlayerFixture.RecordingPlayer replacement = FabricCostPlayerFixture.allocate(FabricCostPlayerFixture.RecordingPlayer.class);
        replacement.id = old.id;
        replacement.world = old.world;
        set(replacement, "server", server);
        replacement.health = 20;
        players.put(old.id, replacement);
        service.connect(old);
        assertTrue(registrations.isEmpty());
        service.connect(replacement);
        UUID session = registrations.get(old.id);
        service.connect(old);
        assertEquals(session, registrations.get(old.id));
        assertEquals(1, quotes.connectionCount());
        assertEquals(0, payments);
    }

    @Test
    public void failedOpenDoesNotPublishRegistration() throws Exception {
        set(service, "quotes", quotes(false));
        try {
            service.connect(player);
            fail("Expected strict connection rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(registrations.isEmpty());
        }
    }

    @Test
    public void duplicateHandshakeDoesNotResetRequestSequenceOrRateLimit() {
        service.connect(player);
        UUID session = registrations.get(player.getUUID());
        for (int i = 1; i <= 8; i++) {
            assertEquals(CostQuote.Status.UNKNOWN, quotes.quote(player.getUUID(), request(i)).status());
            service.connect(player);
            assertEquals(session, registrations.get(player.getUUID()));
        }
        assertEquals(CostQuote.Status.RATE_LIMITED, quotes.quote(player.getUUID(), request(9)).status());
        assertEquals(8, opens);
        assertEquals(0, payments);
    }

    @Test
    public void logoutClosesBothRegistrationLayers() {
        service.connect(player);
        service.disconnect(player.getUUID());
        assertTrue(registrations.isEmpty());
        assertEquals(0, quotes.connectionCount());
    }

    @Test
    public void callbacksOnOtherThreadsAreRejectedBeforePublishingState() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                service.connect(player);
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        worker.start();
        worker.join(2000);
        assertFalse(worker.isAlive());
        assertTrue(failure.get() instanceof IllegalStateException);
        assertTrue(registrations.isEmpty());
    }

    private static void set(Object target, String name, Object value) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                if (name.equals("playersByUUID")) {
                    try {
                        Field field = type.getDeclaredField("uuidToPlayerMap");
                        field.setAccessible(true);
                        field.set(target, value);
                        return;
                    } catch (NoSuchFieldException absent) {
                    }
                }
            }
        }
        throw new NoSuchFieldException(name);
    }

    private void connectWithoutFailure() {
        try {
            service.connect(player);
        } catch (RuntimeException failure) {
            throw new AssertionError("Obsolete callback must not create a session", failure);
        }
    }
}
