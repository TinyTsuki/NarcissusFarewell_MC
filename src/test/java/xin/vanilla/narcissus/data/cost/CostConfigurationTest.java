package xin.vanilla.narcissus.data.cost;

import org.junit.Test;
import xin.vanilla.narcissus.api.cost.CostParameters;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.EnumMap;

import static org.junit.Assert.*;

public class CostConfigurationTest {
    @Test public void distanceLimitsTreatCrossDimensionAndUnlimitedDistanceSeparately() {
        CostConfiguration defaults = CostConfiguration.defaults();
        assertEquals(10000, defaults.distance(12000, false), 0);
        assertEquals(10000, defaults.distance(20, true), 0);
        CostConfiguration unlimited = new CostConfiguration(defaults.groups(), defaults.cards(), 0, 500);
        assertEquals(12000, unlimited.distance(12000, false), 0);
        assertEquals(500, unlimited.distance(20, true), 0);
        for (double value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            try { unlimited.distance(value, false); fail("Invalid distance"); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void snapshotDoesNotFollowEditsAndRejectsMissingGroups() {
        CostConfiguration defaults = CostConfiguration.defaults();
        EnumMap<EnumTeleportType, CostParameters> mutable = new EnumMap<>(EnumTeleportType.class);
        mutable.putAll(defaults.groups());
        CostConfiguration snapshot = new CostConfiguration(mutable, defaults.cards(), 10000, 10000);
        mutable.remove(EnumTeleportType.TP_HOME);
        assertSame(CostParameters.free(), snapshot.parameters(EnumTeleportType.TP_HOME));
        try { new CostConfiguration(mutable, defaults.cards(), 10000, 10000); fail("Missing group"); }
        catch (NullPointerException expected) { }
        try { snapshot.groups().clear(); fail("Mutable snapshot"); }
        catch (UnsupportedOperationException expected) { }
    }
}
