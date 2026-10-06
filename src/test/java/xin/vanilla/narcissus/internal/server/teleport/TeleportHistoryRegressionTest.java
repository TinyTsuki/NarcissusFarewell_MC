package xin.vanilla.narcissus.internal.server.teleport;

import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.vector.Vector3d;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.*;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ConfigBaselineFixture;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.event.EventHandlerProxy;
import xin.vanilla.narcissus.internal.forge.cost.ForgeCostPlayerFixture;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.Assert.*;

/** Exercises the real transaction and history handler; only native movement/network are isolated. */
public class TeleportHistoryRegressionTest {
    private final ForgeCostPlayerFixture fixture = new ForgeCostPlayerFixture();
    private HookPlayer player;
    private PlayerTeleportData data;
    private ConfigHolder holder;
    private boolean throwOnSync;

    @Before
    public void setup() throws Exception {
        fixture.setup();
        player = ForgeCostPlayerFixture.allocate(HookPlayer.class);
        player.id = fixture.player().getUUID();
        player.world = fixture.player().world;
        player.level = player.world;
        set(Entity.class, player, "position", Vector3d.ZERO);
        set(Entity.class, player, "passengers", com.google.common.collect.ImmutableList.of());
        data = PlayerTeleportData.getData(player);
        data.setTeleportRecords(new ArrayList<>());
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("base.teleportTogether.tpWithFollower", false);
        overrides.put("base.teleportTogether.tpWithVehicle", false);
        overrides.put("base.other.tpSound", "minecraft:missing_test_sound");
        holder = ConfigBaselineFixture.holderWithValues(CommonConfig.class, overrides);
        BaniraConfigService configs = (BaniraConfigService) proxy(BaniraConfigService.class,
                (p, m, a) -> m.getName().equals("handle") ? holder : null);
        BaniraNetworkService network = (BaniraNetworkService) proxy(BaniraNetworkService.class,
                (p, m, a) -> {
                    if (throwOnSync && m.getName().equals("sendToPlayer")) throw new IllegalStateException("sync failed");
                    return m.getReturnType() == boolean.class ? false : null;
                });
        BaniraPlatforms.install((BaniraPlatform) proxy(BaniraPlatform.class, (p, m, a) -> {
            if (m.getName().equals("configService")) return configs;
            if (m.getName().equals("networkService")) return network;
            throw new UnsupportedOperationException(m.toString());
        }));
    }

    @After
    public void cleanup() throws Exception {
        fixture.cleanup();
    }

    @Test
    public void ownCoordinateHasOnlyItsTypedRecord() throws Exception {
        assertTrue(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes(EnumTeleportType.TP_COORDINATE);
    }

    @Test
    public void ownOtherHasOnlyOneRecord() throws Exception {
        assertTrue(teleport(EnumTeleportType.OTHER, player.world));
        assertTypes(EnumTeleportType.OTHER);
    }

    @Test
    public void ownCrossDimensionKeepsTypedSourceAndDestination() throws Exception {
        ServerWorld nether = ForgeCostPlayerFixture.allocate(ServerWorld.class);
        set(World.class, nether, "dimension", World.NETHER);
        assertTrue(teleport(EnumTeleportType.TP_COORDINATE, nether));
        assertTypes(EnumTeleportType.TP_COORDINATE);
        TeleportRecord record = data.peekTeleportRecords().get(0);
        assertEquals(World.OVERWORLD, record.getBefore().dimension());
        assertEquals(World.NETHER, record.getAfter().dimension());
    }

    @Test
    public void externalTeleportStillRecordsOther() {
        player.teleportTo(player.world, 4, 70, 8, 0, 0);
        assertTypes(EnumTeleportType.OTHER);
    }

    @Test
    public void failedNativeMoveAddsNoHistoryAndDoesNotSuppressNextExternalMove() throws Exception {
        player.failMove = true;
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes();
        player.failMove = false;
        player.teleportTo(player.world, 4, 70.1, 8, 0, 0);
        assertTypes(EnumTeleportType.OTHER);
    }

    @Test
    public void throwingNativeMoveAddsNoHistoryAndDoesNotSuppressNextExternalMove() throws Exception {
        player.throwMove = true;
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes();
        player.throwMove = false;
        player.teleportTo(player.world, 4, 70.1, 8, 0, 0);
        assertTypes(EnumTeleportType.OTHER);
    }

    @Test
    public void differentExternalDestinationDuringOwnMoveStillRecordsOther() throws Exception {
        player.nested = () -> EventHandlerProxy.onPlayerTeleport(player, player.position(), new Vector3d(99, 65, 98));
        assertTrue(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes(EnumTeleportType.OTHER, EnumTeleportType.TP_COORDINATE);
        assertEquals(99, data.peekTeleportRecords().get(0).getAfter().x(), 0);
    }

    @Test
    public void nestedOwnMoveRestoresOuterOwnership() throws Exception {
        player.nested = () -> {
            try {
                assertTrue(teleport(EnumTeleportType.TP_HOME, player.world));
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        };
        assertTrue(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes(EnumTeleportType.TP_HOME, EnumTeleportType.TP_COORDINATE);
    }

    @Test
    public void otherPlayerNativeMoveKeepsOtherDuringOwnMove() throws Exception {
        HookPlayer passenger = ForgeCostPlayerFixture.allocate(HookPlayer.class);
        passenger.id = UUID.randomUUID();
        passenger.world = player.world;
        passenger.level = passenger.world;
        set(Entity.class, passenger, "position", Vector3d.ZERO);
        java.lang.reflect.Constructor<PlayerTeleportData> constructor = PlayerTeleportData.class
                .getDeclaredConstructor(net.minecraft.entity.player.PlayerEntity.class);
        constructor.setAccessible(true);
        PlayerTeleportData passengerData = constructor.newInstance((Object) null);
        passengerData.setTeleportRecords(new ArrayList<>());
        Field cache = PlayerTeleportData.class.getDeclaredField("CACHE");
        cache.setAccessible(true);
        ((Map<UUID, PlayerTeleportData>) cache.get(null)).put(passenger.id, passengerData);
        player.nested = () -> passenger.teleportTo(player.world, 4, 70.1, 8, 0, 0);
        assertTrue(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes(EnumTeleportType.TP_COORDINATE);
        assertEquals(1, passengerData.peekTeleportRecords().size());
        assertEquals(EnumTeleportType.OTHER, passengerData.peekTeleportRecords().get(0).getTeleportType());
    }

    private boolean teleport(EnumTeleportType type, ServerWorld world) throws Exception {
        return teleport(type, world, new SafeWorldCoordinate(player));
    }

    private boolean teleport(EnumTeleportType type, ServerWorld world, SafeWorldCoordinate before) throws Exception {
        SafeWorldCoordinate after = new SafeWorldCoordinate(4, 70, 8, world.dimension());
        Method method = NarcissusUtils.class.getDeclaredMethod("teleportPlayer", ServerPlayerEntity.class,
                SafeWorldCoordinate.class, EnumTeleportType.class, SafeWorldCoordinate.class, ServerWorld.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(null, player, after, type, before, world);
    }

    @Test
    public void reattachmentFailureAfterPlayerMoveKeepsOneOther() throws Exception {
        ridingFixture();
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertPartialMovement(World.OVERWORLD);
    }

    @Test
    public void crossDimensionReattachmentFailurePreservesBothWorlds() throws Exception {
        ridingFixture();
        ServerWorld nether = ForgeCostPlayerFixture.allocate(ServerWorld.class);
        set(World.class, nether, "dimension", World.NETHER);
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, nether));
        assertPartialMovement(World.NETHER);
    }

    @Test
    public void nativeExceptionAfterMovementKeepsOneOther() throws Exception {
        player.throwAfterMove = true;
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes(EnumTeleportType.OTHER);
        assertEquals(4, data.peekTeleportRecords().get(0).getAfter().x(), 0);
    }

    @Test
    public void staleRequestSourceDoesNotInventMovementOnFailure() throws Exception {
        player.failMove = true;
        SafeWorldCoordinate stale = new SafeWorldCoordinate(100, 80, 100, World.NETHER);
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, player.world, stale));
        assertTypes();
    }

    @Test
    public void lateSyncFailureCannotAddFallbackAfterTypedRecord() throws Exception {
        throwOnSync = true;
        try {
            teleport(EnumTeleportType.TP_COORDINATE, player.world);
            fail("sync failure missing");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertEquals("sync failed", expected.getCause().getMessage());
        }
        assertTypes(EnumTeleportType.TP_COORDINATE);
    }

    private void ridingFixture() throws Exception {
        holder.set("base.teleportTogether.tpWithVehicle", true);
        HookPlayer root = ForgeCostPlayerFixture.allocate(HookPlayer.class);
        root.id = UUID.randomUUID();
        root.world = player.world;
        root.level = root.world;
        set(Entity.class, root, "position", Vector3d.ZERO);
        set(Entity.class, root, "passengers", com.google.common.collect.ImmutableList.of(player));
        set(Entity.class, player, "vehicle", root);
        player.root = root;
        player.rejectAttachment = true;
        java.lang.reflect.Constructor<PlayerTeleportData> constructor = PlayerTeleportData.class
                .getDeclaredConstructor(net.minecraft.entity.player.PlayerEntity.class);
        constructor.setAccessible(true);
        PlayerTeleportData rootData = constructor.newInstance((Object) null);
        rootData.setTeleportRecords(new ArrayList<>());
        Field cache = PlayerTeleportData.class.getDeclaredField("CACHE");
        cache.setAccessible(true);
        ((Map<UUID, PlayerTeleportData>) cache.get(null)).put(root.id, rootData);
    }

    @Test
    public void nestedSuccessThenUnmovedOuterFailureDoesNotAddOther() throws Exception {
        ServerWorld nether = ForgeCostPlayerFixture.allocate(ServerWorld.class);
        set(World.class, nether, "dimension", World.NETHER);
        player.failMove = true;
        player.nested = () -> {
            player.failMove = false;
            try {
                assertTrue(teleport(EnumTeleportType.TP_HOME, nether));
            } catch (Exception error) {
                throw new IllegalStateException(error);
            } finally {
                player.failMove = true;
            }
        };
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes(EnumTeleportType.TP_HOME);
        assertEquals(World.NETHER, player.world.dimension());
    }

    @Test
    public void nestedTypedRecordBeforeSyncFailureDoesNotAddOther() throws Exception {
        ServerWorld nether = ForgeCostPlayerFixture.allocate(ServerWorld.class);
        set(World.class, nether, "dimension", World.NETHER);
        player.nested = () -> {
            throwOnSync = true;
            try {
                teleport(EnumTeleportType.TP_HOME, nether);
                fail("nested sync failure missing");
            } catch (Exception error) {
                throw new IllegalStateException(error);
            } finally {
                throwOnSync = false;
            }
        };
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes(EnumTeleportType.TP_HOME);
    }

    @Test
    public void externalRecordedDestinationDuringUnmovedOuterFailureDoesNotAddOther() throws Exception {
        ServerWorld nether = ForgeCostPlayerFixture.allocate(ServerWorld.class);
        set(World.class, nether, "dimension", World.NETHER);
        player.failMove = true;
        player.nested = () -> {
            Vector3d destination = new Vector3d(4, 70.1, 8);
            EventHandlerProxy.onPlayerTeleport(player, player.position(), destination, nether.dimension());
            player.world = nether;
            player.level = nether;
            try {
                set(Entity.class, player, "position", destination);
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        };
        assertFalse(teleport(EnumTeleportType.TP_COORDINATE, player.world));
        assertTypes(EnumTeleportType.OTHER);
    }

    private void assertPartialMovement(Object targetDimension) {
        assertTrue("RidingTransfer must attempt reattachment after moving the player", player.attachmentAttempts > 0);
        assertEquals(1, player.moves);
        assertTypes(EnumTeleportType.OTHER);
        TeleportRecord record = data.peekTeleportRecords().get(0);
        assertEquals(World.OVERWORLD, record.getBefore().dimension());
        assertEquals(0, record.getBefore().x(), 0);
        assertEquals(targetDimension, record.getAfter().dimension());
        assertEquals(4, record.getAfter().x(), 0);
        assertEquals(70.1, record.getAfter().y(), 0);
        assertEquals(8, record.getAfter().z(), 0);
    }

    private void assertTypes(EnumTeleportType... expected) {
        List<EnumTeleportType> actual = new ArrayList<>();
        for (TeleportRecord record : data.peekTeleportRecords()) actual.add(record.getTeleportType());
        assertEquals(Arrays.asList(expected), actual);
    }

    private static Object proxy(Class<?> type, java.lang.reflect.InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    public static class HookPlayer extends ServerPlayerEntity {
        UUID id;
        ServerWorld world;
        boolean failMove, throwMove, throwAfterMove, rejectAttachment;
        int moves, attachmentAttempts;
        Entity root;
        Runnable nested;

        private HookPlayer() {
            super(null, null, null, null);
        }

        @Override public UUID getUUID() { return id; }

        @Override public ServerWorld getLevel() { return world; }
        @Override public Entity getRootVehicle() { return root == null ? this : root; }
        @Override public void stopRiding() {
            Entity vehicle = getVehicle();
            if (vehicle == null) return;
            try {
                List<Entity> passengers = new ArrayList<>(vehicle.getPassengers());
                passengers.remove(this);
                set(Entity.class, vehicle, "passengers", com.google.common.collect.ImmutableList.copyOf(passengers));
                set(Entity.class, this, "vehicle", null);
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        }
        @Override public boolean startRiding(Entity vehicle, boolean force) {
            attachmentAttempts++;
            if (rejectAttachment) return false;
            try {
                set(Entity.class, this, "vehicle", vehicle);
                List<Entity> passengers = new ArrayList<>(vehicle.getPassengers());
                passengers.add(this);
                set(Entity.class, vehicle, "passengers", com.google.common.collect.ImmutableList.copyOf(passengers));
                return true;
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        }
        @Override public void sendMessage(net.minecraft.util.text.ITextComponent message, UUID sender) { }

        @Override
        public void teleportTo(ServerWorld target, double x, double y, double z, float yaw, float pitch) {
            Vector3d destination = new Vector3d(x, y, z);
            Runnable callback = nested;
            nested = null;
            if (callback != null) callback.run();
            EventHandlerProxy.onPlayerTeleport(this, position(), destination);
            if (throwMove) throw new IllegalStateException("native move failed");
            if (failMove) return;
            world = target;
            level = target;
            try {
                set(Entity.class, this, "position", destination);
                moves++;
                if (throwAfterMove) throw new IllegalStateException("native failed after moving");
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        }
    }
}
