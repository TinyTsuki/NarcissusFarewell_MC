package xin.vanilla.narcissus.config;

import net.minecraft.world.level.block.Blocks;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.narcissus.search.SearchExecutionSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static xin.vanilla.narcissus.enums.EnumTeleportType.*;

public class CommonSearchConfigurationTest {
    private Object previousPlatform;
    private ConfigBaselineFixture fixture;
    private CommonSearchConfiguration live;

    @BeforeClass
    public static void bootstrap() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void setup() throws Exception {
        java.lang.reflect.Field field = xin.vanilla.banira.platform.BaniraPlatforms.class.getDeclaredField("platform");
        field.setAccessible(true);
        previousPlatform = field.get(null);
        fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        live = new CommonSearchConfiguration(fixture.holder);
    }

    @After
    public void restorePlatform() throws Exception {
        java.lang.reflect.Field field = xin.vanilla.banira.platform.BaniraPlatforms.class.getDeclaredField("platform");
        field.setAccessible(true);
        field.set(null, previousPlatform);
    }

    @Test
    public void defaultsAndUnchangedInputsReuseSnapshotsAndPolicy() {
        SearchExecutionSettings execution = live.execution();
        assertEquals(2, execution.timeBudgetMs(), 0);
        assertEquals(32, execution.maxConcurrentSearches());
        assertEquals(30, execution.timeoutSeconds());
        assertSame(execution, live.execution());
        CommonSearchConfiguration.Snapshot home = live.capture(TP_HOME, false);
        assertSame(home, live.capture(TP_HOME, false));
        assertSame(home.policy(), live.capture(TP_RANDOM, true).policy());
        assertEquals(1, home.safeChunkRange());
        assertFalse(home.setBlockWhenSafeNotFound());
        assertTrue(home.getBlockFromInventory());
        assertTrue(home.matches());
        assertEquals(0, fixture.saves);
    }

    @Test
    public void unsavedChangesInvalidateEachSafetyDependency() {
        List<String> paths = Arrays.asList("unsafeBlocks", "suffocatingBlocks", "safeBlocks", "safeChunkRange",
                "setBlockWhenSafeNotFound", "getBlockFromInventory");
        List<Object> edits = Arrays.asList(Arrays.asList("minecraft:stone"), Arrays.asList("minecraft:stone"),
                Arrays.asList("minecraft:stone"), 2, true, false);
        for (int i = 0; i < paths.size(); i++) {
            CommonSearchConfiguration.Snapshot before = live.capture(TP_HOME, false);
            fixture.holder.set("base.safeTeleport." + paths.get(i), edits.get(i));
            assertFalse(paths.get(i), before.matches());
            CommonSearchConfiguration.Snapshot after = live.capture(TP_HOME, false);
            assertNotSame(before, after);
            assertTrue(after.matches());
            assertSame(after, live.capture(TP_HOME, false));
        }
        assertEquals(0, fixture.saves);
    }

    @Test
    public void externallyMutableListInvalidatesMatchButCannotMutateCapturedPolicy() {
        List<String> unsafe = new ArrayList<>(Arrays.asList("minecraft:lava"));
        fixture.values.put("base.safeTeleport.unsafeBlocks", unsafe);
        CommonSearchConfiguration.Snapshot first = live.capture(TP_HOME, false);
        assertFalse(first.policy().unsafe(Blocks.STONE.defaultBlockState()));
        unsafe.add("minecraft:stone");
        assertFalse(first.matches());
        assertFalse(first.policy().unsafe(Blocks.STONE.defaultBlockState()));
        CommonSearchConfiguration.Snapshot second = live.capture(TP_HOME, false);
        assertNotSame(first.policy(), second.policy());
        assertTrue(second.policy().unsafe(Blocks.STONE.defaultBlockState()));
        assertThrows(UnsupportedOperationException.class, () -> second.policy().supportStates().clear());
    }

    @Test
    public void onlyRelevantRandomAndViewFieldsInvalidateSnapshots() {
        CommonSearchConfiguration.Snapshot home = live.capture(TP_HOME, false);
        CommonSearchConfiguration.Snapshot random = live.capture(TP_RANDOM, false);
        CommonSearchConfiguration.Snapshot view = live.capture(TP_VIEW, true);
        fixture.holder.set("base.randomTeleport.teleportRandomDistanceLimit", 999);
        fixture.holder.set("base.randomTeleport.tpRandomSafeNotFoundRetries", 2);
        assertTrue(home.matches());
        assertTrue(view.matches());
        assertFalse(random.matches());
        random = live.capture(TP_RANDOM, false);
        assertEquals(999, random.randomDistanceLimit());
        assertEquals(2, random.randomRetries());
        fixture.holder.set("base.teleportLimit.teleportViewDistanceLimit", 256);
        assertTrue(home.matches());
        assertTrue(random.matches());
        assertFalse(view.matches());
        assertEquals(256, live.capture(TP_VIEW, true).viewDistanceLimit());
        assertSame(home, live.capture(TP_HOME, false));
        assertSame(home.policy(), random.policy());
    }

    @Test
    public void executionEditsDoNotCancelSafetySnapshotsOrReparsePolicy() {
        CommonSearchConfiguration.Snapshot safety = live.capture(TP_RANDOM, true);
        SearchExecutionSettings old = live.execution();
        fixture.holder.set("base.safeTeleport.search.timeBudgetMs", .5D);
        fixture.holder.set("base.safeTeleport.search.maxConcurrentSearches", 4);
        fixture.holder.set("base.safeTeleport.search.timeoutSeconds", 5);
        assertTrue(safety.matches());
        assertSame(safety, live.capture(TP_RANDOM, true));
        SearchExecutionSettings current = live.execution();
        assertNotSame(old, current);
        assertEquals(.5, current.timeBudgetMs(), 0);
        assertEquals(4, current.maxConcurrentSearches());
        assertEquals(5, current.timeoutSeconds());
        assertSame(current, live.execution());
    }

    @Test
    public void reboundHolderRejectsOldSnapshotsAndRuntime() throws Exception {
        CommonSearchConfiguration.Snapshot before = live.capture(TP_HOME, false);
        live.execution();
        ConfigBaselineFixture other = new ConfigBaselineFixture(CommonConfig.class);
        other.bind(CommonConfig.class);
        assertFalse(before.matches());
        assertThrows(IllegalStateException.class, live::execution);
        assertThrows(IllegalStateException.class, () -> live.capture(TP_HOME, false));
        assertTrue(new CommonSearchConfiguration(other.holder).capture(TP_HOME, false).matches());
    }

    @Test
    public void invalidStoredValuesCannotHideBehindGeneratedDefaultsOrCachedSnapshot() {
        CommonSearchConfiguration.Snapshot before = live.capture(TP_HOME, false);
        fixture.values.put("base.safeTeleport.safeChunkRange", 0);
        assertFalse(before.matches());
        assertThrows(IllegalArgumentException.class, () -> live.capture(TP_HOME, false));
        fixture.values.put("base.safeTeleport.safeChunkRange", 1);
        assertSame(before, live.capture(TP_HOME, false));
        live.execution();
        for (Object bad : new Object[]{Double.NaN, 0D, 11D, null, "garbage"}) {
            fixture.values.put("base.safeTeleport.search.timeBudgetMs", bad);
            assertThrows(IllegalArgumentException.class, live::execution);
        }
        fixture.values.put("base.safeTeleport.search.timeBudgetMs", 2D);
        fixture.values.put("base.safeTeleport.unsafeBlocks", null);
        assertThrows(IllegalArgumentException.class, () -> live.capture(TP_HOME, false));
    }

    @Test
    public void invalidUnrelatedInputsDoNotCancelHomeSearch() {
        CommonSearchConfiguration.Snapshot home = live.capture(TP_HOME, false);
        fixture.values.put("base.randomTeleport.teleportRandomDistanceLimit", -1);
        fixture.values.put("base.teleportLimit.teleportViewDistanceLimit", -1);
        assertTrue(home.matches());
        assertSame(home, live.capture(TP_HOME, false));
        assertThrows(IllegalArgumentException.class, () -> live.capture(TP_RANDOM, false));
        assertThrows(IllegalArgumentException.class, () -> live.capture(TP_VIEW, true));
    }
}
