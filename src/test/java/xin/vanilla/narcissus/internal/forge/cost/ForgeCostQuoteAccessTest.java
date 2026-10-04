package xin.vanilla.narcissus.internal.forge.cost;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.narcissus.data.*;
import xin.vanilla.narcissus.data.cost.CostQuoteTarget;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import java.util.*;
import static org.junit.Assert.*;

public class ForgeCostQuoteAccessTest {
    private final ForgeCostPlayerFixture fixture = new ForgeCostPlayerFixture();
    private PlayerTeleportData data;
    private UUID player;
    private final WorldStageData stages = new WorldStageData();
    @Before public void setup() throws Exception { fixture.setup(); data = PlayerTeleportData.getData(fixture.player()); player = fixture.player().getUUID(); }
    @After public void cleanup() throws Exception { fixture.cleanup(); }
    private SafeWorldCoordinate resolve(CostQuoteTarget target) { return ForgeCostQuoteAccess.recordedTarget(player, target, data, stages); }

    @Test public void homeUsesRealCompositeKeyWithoutTrustingAnotherOwnerOrIndex() {
        SafeWorldCoordinate first = new SafeWorldCoordinate(1, 2, 3, "minecraft:overworld");
        SafeWorldCoordinate second = new SafeWorldCoordinate(4, 5, 6, "minecraft:the_nether");
        Map<KeyValue<String, String>, SafeWorldCoordinate> homes = new LinkedHashMap<>();
        homes.put(new KeyValue<>("minecraft:overworld", "home"), first);
        homes.put(new KeyValue<>("minecraft:the_nether", "home"), second);
        data.setHomeCoordinate(homes);
        data.setDirty();
        assertEquals(4, resolve(CostQuoteTarget.home(player, "minecraft:the_nether", "home")).x(), 0);
        assertNull(resolve(CostQuoteTarget.home(UUID.randomUUID(), "minecraft:overworld", "home")));
        assertNull(resolve(CostQuoteTarget.home(player, "minecraft:overworld", "missing")));
        assertTrue(data.isDirty());
        SafeWorldCoordinate clone = resolve(CostQuoteTarget.home(player, "minecraft:overworld", "home"));
        clone.x(99); assertEquals(1, first.x(), 0);
    }

    @Test public void stageMustExistAndDeletedKeyCannotKeepOldQuoteDestination() {
        CostQuoteTarget target = CostQuoteTarget.stage("minecraft:overworld", "stage");
        assertNull(resolve(target));
        stages.addCoordinate(new KeyValue<>("minecraft:overworld", "stage"), new SafeWorldCoordinate(7, 8, 9, "minecraft:overworld"));
        assertEquals(7, resolve(target).x(), 0);
        stages.getStageCoordinate().clear(); assertNull(resolve(target));
    }

    @Test public void historyUsesExistingSecondPrecisionTypeAndCoordinateIdentity() {
        TeleportRecord record = new TeleportRecord().setTeleportTime(new Date(1_700_000_000_789L))
                .setTeleportType(EnumTeleportType.TP_HOME).setBefore(new SafeWorldCoordinate(1, 2, 3, "minecraft:overworld"));
        data.setTeleportRecords(new ArrayList<>(Collections.singletonList(record)));
        data.setDirty();
        CostQuoteTarget target = CostQuoteTarget.history(player, EnumTeleportType.TP_BACK, 1_700_000_000_000L,
                EnumTeleportType.TP_HOME, "minecraft:overworld", 1, 2, 3);
        assertEquals(1, resolve(target).x(), 0);
        assertNull(resolve(CostQuoteTarget.history(player, EnumTeleportType.TP_BACK, 1_700_000_000_000L,
                EnumTeleportType.TP_HOME, "minecraft:overworld", 2, 2, 3)));
        assertNull(resolve(CostQuoteTarget.history(player, EnumTeleportType.TP_GRAVE, 1_700_000_000_000L,
                EnumTeleportType.TP_HOME, "minecraft:overworld", 1, 2, 3)));
        data.setTeleportRecords(Collections.emptyList()); data.setDirty(); assertNull(resolve(target));
        assertTrue(data.isDirty());
    }

    @Test public void readonlyPeeksDoNotFlushDirtyRecordsAndCannotChangeCollections() {
        data.setDirty();
        try { data.peekHomeCoordinates().put(new KeyValue<>("a", "b"), new SafeWorldCoordinate()); fail("Mutable home view"); }
        catch (UnsupportedOperationException expected) { }
        try { data.peekTeleportRecords().add(new TeleportRecord()); fail("Mutable history view"); }
        catch (UnsupportedOperationException expected) { }
        assertTrue(data.isDirty());
    }

    @Test public void accessListsAreReadOnlyDuringQuoteAndRespectWhitelistAndBlacklist() {
        UUID sender = UUID.randomUUID(), stranger = UUID.randomUUID();
        PlayerAccess access = new PlayerAccess(); access.addWhiteList(sender.toString());
        data.setAccess(access); data.setDirty();
        assertTrue(data.acceptsTeleportFrom(sender)); assertFalse(data.acceptsTeleportFrom(stranger));
        access.removeWhiteList(sender.toString()); access.addBlackList(sender.toString());
        assertFalse(data.acceptsTeleportFrom(sender)); assertTrue(data.acceptsTeleportFrom(stranger));
        assertTrue(data.isDirty());
    }

    @Test public void serializedHistoryMatchesRealServerRecordWithoutPersistingNewIdentity() {
        TeleportRecord original = new TeleportRecord().setTeleportTime(new Date(1_700_000_000_789L))
                .setTeleportType(EnumTeleportType.DEATH).setBefore(new SafeWorldCoordinate(-30.5, 70, 21.25, "minecraft:overworld"));
        TeleportRecord synced = TeleportRecord.readFromNBT(original.writeToNBT());
        data.setTeleportRecords(Collections.singletonList(original));
        CostQuoteTarget target = CostQuoteTarget.history(player, EnumTeleportType.TP_GRAVE, synced.getTeleportTime().getTime(),
                synced.getTeleportType(), synced.getBefore().dimensionId(), synced.getBefore().x(), synced.getBefore().y(), synced.getBefore().z());
        assertEquals(-30.5, resolve(target).x(), 0);
    }

    @Test public void existingHistorySyncIdentitySurvivesDifferentClientTimezone() {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            TeleportRecord original = new TeleportRecord().setTeleportTime(new Date(1_700_000_000_789L))
                    .setTeleportType(EnumTeleportType.TP_HOME).setBefore(new SafeWorldCoordinate(1, 2, 3, "minecraft:overworld"));
            net.minecraft.nbt.CompoundNBT wire = original.writeToNBT();
            data.setTeleportRecords(Collections.singletonList(original));
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            TeleportRecord client = TeleportRecord.readFromNBT(wire);
            CostQuoteTarget target = CostQuoteTarget.history(player, EnumTeleportType.TP_BACK, client.getTeleportTime().getTime(),
                    client.getTeleportType(), client.getBefore().dimensionId(), client.getBefore().x(), client.getBefore().y(), client.getBefore().z());
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertNotNull(resolve(target));
        } finally { TimeZone.setDefault(previous); }
    }
}
