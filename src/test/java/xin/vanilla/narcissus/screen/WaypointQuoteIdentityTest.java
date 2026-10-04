package xin.vanilla.narcissus.screen;

import org.junit.Test;
import org.junit.BeforeClass;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.cost.CostQuoteTarget;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.Collections;

import static org.junit.Assert.*;

public class WaypointQuoteIdentityTest {
    @BeforeClass
    public static void bootstrap() {
        try { xin.vanilla.narcissus.test.ForgeUnitTestBootstrap.bootstrap(); } catch (Exception error) { throw new IllegalStateException(error); }
    }

    @Test
    public void oversizedStoredNameKeepsTheEntryWithoutConstructingAnInvalidQuote() {
        String name = String.join("", Collections.nCopies(300, "x"));
        SafeWorldCoordinate coordinate = new SafeWorldCoordinate(1, 64, 2, "minecraft:overworld");
        WaypointScreen.WaypointEntry entry = new WaypointScreen.WaypointEntry(
                WaypointScreen.WaypointEntry.Type.STAGE, name, coordinate, true, "", null);
        assertEquals(name, entry.name);
        assertSame(coordinate, entry.safeWorldCoordinate);
        assertTrue(entry.canTeleport);
        assertEquals(CostQuoteTarget.unknown(EnumTeleportType.TP_STAGE), entry.quoteTarget);
        assertThrows(IllegalArgumentException.class, () -> CostQuoteTarget.stage("minecraft:overworld", name));
    }

    @Test
    public void validStoredNameRetainsItsExactQuoteIdentity() {
        WaypointScreen.WaypointEntry entry = new WaypointScreen.WaypointEntry(
                WaypointScreen.WaypointEntry.Type.STAGE, "stage", new SafeWorldCoordinate(1, 64, 2, "minecraft:overworld"), true, "", null);
        assertEquals(CostQuoteTarget.stage("minecraft:overworld", "stage"), entry.quoteTarget);
    }
}
