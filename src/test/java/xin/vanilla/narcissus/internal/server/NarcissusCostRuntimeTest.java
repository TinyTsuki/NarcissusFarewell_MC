package xin.vanilla.narcissus.internal.server;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.api.script.ScriptSession;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueStore;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.config.migration.CostConfigMigration;
import xin.vanilla.narcissus.config.migration.CostMigrationPlan;
import xin.vanilla.narcissus.data.cost.CostCalculation;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.data.cost.CostContextInput;
import xin.vanilla.narcissus.enums.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class NarcissusCostRuntimeTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    public static final class Probe {
        public static final AtomicInteger constructions = new AtomicInteger();
        public static CostContext retained;
        public static Thread constructorThread;
        public static Runnable nested;

        public static void construct() {
            constructions.incrementAndGet();
            constructorThread = Thread.currentThread();
        }

        public static double inspect(CostContext context) {
            retained = context;
            if (nested != null) nested.run();
            return context.payer().health() + context.parameters().fixedAmount();
        }
    }

    @Test
    public void routesAllSixteenGroupsAndNeverCompilesDisabledFiles() throws Exception {
        try (Fixture f = new Fixture()) {
            int index = 0;
            for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
                f.values.put(type, parameters(++index, ""));
            }
            assertTrue(f.prepare());
            index = 0;
            for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
                assertEquals(++index, f.calculate(type).amount());
            }
            f.values.put(EnumTeleportType.TP_HOME, new CostParameters(EnumCostType.NONE, 0, 0, 0, 20, "", "", "Missing.java"));
            assertTrue(f.prepare());
            assertEquals(0, f.calculate(EnumTeleportType.TP_HOME).amount());
            assertEquals(0, f.calculate(EnumTeleportType.DEATH).amount());
            assertEquals(0, f.calculate(EnumTeleportType.OTHER).amount());
            assertTrue(f.errors.isEmpty());
        }
    }

    @Test
    public void generatedLegacyFormulaUsesActualBorrowedContextAndSharedFileIsConstructedOnce() throws Exception {
        Probe.constructions.set(0);
        try (Fixture f = new Fixture()) {
            f.source("Shared.java", source("Shared", "Probe.construct();", "return Probe.inspect(ctx);"));
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Shared.java"));
            f.values.put(EnumTeleportType.TP_STAGE, parameters(3, "Shared.java"));
            assertTrue(f.prepare());
            assertSame(Thread.currentThread(), Probe.constructorThread);
            assertEquals(1, Probe.constructions.get());
            assertEquals(12, f.calculate(EnumTeleportType.TP_HOME).amount());
            assertEquals(13, f.calculate(EnumTeleportType.TP_STAGE).amount());
            assertEquals(1, Probe.constructions.get());
            rejected(Probe.retained::phase);
            Map<String, Object> legacy = new LinkedHashMap<>();
            Map<String, Object> home = new LinkedHashMap<>();
            home.put("costTpHomeType", "EXP_POINT");
            home.put("costTpHomeNum", 2);
            home.put("costTpHomeRate", .002);
            home.put("costTpHomeExp", "num + sqrt(distance)");
            legacy.put("cost", Collections.singletonMap("tpHome", home));
            CostMigrationPlan plan = CostConfigMigration.plan(legacy);
            for (Map.Entry<String, String> file : plan.sources().entrySet()) f.source(file.getKey(), file.getValue());
            f.set(plan.configuration());
            assertTrue(f.prepare());
            assertEquals(12, f.calculate(EnumTeleportType.TP_HOME).amount());
        }
    }

    @Test
    public void constructorAndCalculationFailuresNeverFallBackToDefaultPricing() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Broken.java"));
            f.source("Broken.java", source("Broken", "throw new IllegalArgumentException(\"construction\");", "return 1;"));
            assertFalse(f.prepare());
            assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
            assertEquals(0, f.calculate(EnumTeleportType.TP_STAGE).amount());
            f.source("Broken.java", source("Broken", "", "throw new IllegalArgumentException(\"calculation\");"));
            assertTrue(f.prepare());
            assertEquals(EnumCostFailure.FORMULA_FAILED, f.calculate(EnumTeleportType.TP_HOME).failure());
            assertFalse(f.errors.isEmpty());
        }
    }

    @Test
    public void unsavedRelatedEditsInvalidateOnlyTheirCostDependencies() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, ""));
            f.values.put(EnumTeleportType.TP_STAGE, parameters(3, ""));
            assertTrue(f.prepare());
            f.reads.set(0);
            assertEquals(2, f.calculate(EnumTeleportType.TP_HOME).amount());
            assertEquals(1, f.reads.get());
            f.values.put(EnumTeleportType.TP_STAGE, parameters(4, ""));
            assertEquals(2, f.calculate(EnumTeleportType.TP_HOME).amount());
            assertFalse(f.calculate(EnumTeleportType.TP_STAGE).isSuccess());
            f.cards = new CostCardSettings(true, 2, EnumCardType.OFFSET_COST);
            assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
            assertEquals(0, f.calculate(EnumTeleportType.TP_BACK).amount());
        }
    }

    @Test
    public void contextMustMatchGenerationParametersCardsAndDistance() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, ""));
            assertTrue(f.prepare());
            assertFalse(f.runtime.calculate(f.input(EnumTeleportType.TP_HOME).generationId(99).build()).isSuccess());
            assertFalse(f.runtime.calculate(f.input(EnumTeleportType.TP_HOME).parameters(parameters(3, "")).build()).isSuccess());
            assertFalse(f.runtime.calculate(f.input(EnumTeleportType.TP_HOME).distance(101).build()).isSuccess());
            assertFalse(f.runtime.calculate(f.input(EnumTeleportType.TP_HOME)
                    .cardSettings(new CostCardSettings(true, 0, EnumCardType.REQUIRE_ONE_WITH_COST)).build()).isSuccess());
        }
    }

    @Test
    public void sourceEditedDuringPreparationAndFailedReloadCannotKeepOldCodeCharging() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.source("Formula.java", source("Formula", "", "return 7;"));
            assertTrue(f.prepare());
            assertEquals(7, f.calculate(EnumTeleportType.TP_HOME).amount());
            f.source("Formula.java", "not Java");
            assertFalse(f.prepare());
            assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
            assertEquals(0, f.runtime.generationId());
            f.source("Formula.java", source("Formula", "Probe.construct();", "return 9;"));
            CompletableFuture<Boolean> pending = f.runtime.prepare(f.configuration());
            f.next().run();
            // Factory construction happens here; verification must still catch this edit.
            f.source("Formula.java", source("Formula", "", "return 11;"));
            assertFalse(f.await(pending));
            assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
        }
    }

    @Test
    public void repeatedRequestsCoalesceAndLateCandidatesCannotPublishAfterStop() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.source("Formula.java", source("Formula", "Probe.construct();", "return 7;"));
            CompletableFuture<Boolean> first = f.runtime.prepare(f.configuration());
            assertSame(first, f.runtime.prepare(f.configuration()));
            f.runtime.close();
            assertFalse(first.get(5, TimeUnit.SECONDS));
            while (!f.owner.isEmpty()) f.owner.poll().run();
            assertEquals(0, f.runtime.generationId());
            assertFalse(f.runtime.prepare(f.configuration()).get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void changedConfigurationSupersedesPendingCandidate() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, ""));
            CompletableFuture<Boolean> first = f.runtime.prepare(f.configuration());
            f.values.put(EnumTeleportType.TP_HOME, parameters(3, ""));
            CompletableFuture<Boolean> second = f.runtime.prepare(f.configuration());
            assertFalse(first.get(5, TimeUnit.SECONDS));
            assertTrue(f.await(second));
            assertEquals(3, f.calculate(EnumTeleportType.TP_HOME).amount());
        }
    }

    @Test
    public void nestedFormulaFailureRestoresOuterContextAndReleasesBothLeases() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.values.put(EnumTeleportType.TP_STAGE, parameters(2, "Throwing.java"));
            f.source("Formula.java", source("Formula", "", "return Probe.inspect(ctx);"));
            f.source("Throwing.java", source("Throwing", "", "throw new IllegalArgumentException(\"nested\");"));
            assertTrue(f.prepare());
            Probe.nested = () -> assertEquals(EnumCostFailure.FORMULA_FAILED, f.calculate(EnumTeleportType.TP_STAGE).failure());
            try {
                assertEquals(12, f.calculate(EnumTeleportType.TP_HOME).amount());
            } finally {
                Probe.nested = null;
            }
            rejected(Probe.retained::phase);
            assertTrue(f.prepare());
            assertEquals(12, f.calculate(EnumTeleportType.TP_HOME).amount());
        }
    }

    @Test
    public void closingInsideInvocationRejectsItsResultAndStillExpiresContext() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.source("Formula.java", source("Formula", "", "return Probe.inspect(ctx);"));
            assertTrue(f.prepare());
            Probe.nested = f.runtime::close;
            try {
                assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
            } finally {
                Probe.nested = null;
            }
            rejected(Probe.retained::phase);
            assertEquals(0, f.runtime.generationId());
        }
    }

    @Test
    public void sharedHelperCompilesOnceWithMultipleEntrypoints() throws Exception {
        try (Fixture f = new Fixture()) {
            f.source("helpers/Numbers.java", "package xin.vanilla.banira.generated.cost.helpers; public class Numbers { public static double amount() { return 6; } }");
            f.source("First.java", source("First", "", "return xin.vanilla.banira.generated.cost.helpers.Numbers.amount();"));
            f.source("Second.java", source("Second", "", "return xin.vanilla.banira.generated.cost.helpers.Numbers.amount() + 1;"));
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "First.java"));
            f.values.put(EnumTeleportType.TP_STAGE, parameters(2, "Second.java"));
            assertTrue(f.prepare());
            assertEquals(6, f.calculate(EnumTeleportType.TP_HOME).amount());
            assertEquals(7, f.calculate(EnumTeleportType.TP_STAGE).amount());
        }
    }

    @Test
    public void preflightValidatesMigratedSourcesWithoutConstructingThemOrInstallingFiles() throws Exception {
        Probe.constructions.set(0);
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            Map<String, String> sources = Collections.singletonMap("Formula.java", source("Formula", "Probe.construct();", "return 1;"));
            NarcissusCostRuntime.preflight(f.configuration(), f.root, sources);
            assertEquals(0, Probe.constructions.get());
            assertFalse(Files.exists(f.root.resolve("Formula.java")));
            try {
                NarcissusCostRuntime.preflight(f.configuration(), f.root, Collections.singletonMap("Formula.java", "invalid"));
                fail("Invalid source accepted");
            } catch (java.io.IOException expected) {
            }
            assertEquals(0, f.runtime.generationId());
        }
    }

    @Test
    public void completedOrRejectedCandidateDoesNotKeepPreparedFactoriesAndDefaultPathNeedsNoContext() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.source("Formula.java", source("Formula", "", "return 7;"));
            assertTrue(f.prepare());
            assertNoPreparedCandidate(f.runtime);
            f.source("Formula.java", source("Formula", "throw new IllegalStateException();", "return 7;"));
            assertFalse(f.prepare());
            assertNoPreparedCandidate(f.runtime);
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, ""));
            assertTrue(f.prepare());
            assertNoPreparedCandidate(f.runtime);
            assertEquals(2, f.runtime.calculate(EnumTeleportType.TP_HOME, f.runtime.generationId(), 100, false).amount());
            assertFalse(f.runtime.calculate(EnumTeleportType.TP_HOME, 99, 100, false).isSuccess());
            assertFalse(f.runtime.calculate(EnumTeleportType.TP_HOME, f.runtime.generationId(), Double.NaN, false).isSuccess());
            assertEquals(0, f.runtime.calculate(EnumTeleportType.TP_BACK, 0, Double.NaN, false).amount());
        }
    }

    @Test
    public void canceledRequestIsNotReusedAndFormulaFailureReportsOnlyOncePerGeneration() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.source("Formula.java", source("Formula", "", "throw new IllegalArgumentException(\"failure\");"));
            CompletableFuture<Boolean> first = f.runtime.prepare(f.configuration());
            first.cancel(false);
            CompletableFuture<Boolean> second = f.runtime.prepare(f.configuration());
            assertNotSame(first, second);
            assertTrue(f.await(second));
            int before = f.errors.size();
            for (int i = 0; i < 20; i++) assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
            assertEquals(before + 1, f.errors.size());
        }
    }

    @Test
    public void savedReloadedAndUnrelatedChangesUseActualHolderAndStopUnregistersListeners() throws Exception {
        try (Fixture f = new Fixture()) {
            MapStore store = new MapStore();
            ConfigHolder holder = ConfigHolder.create("narcissus_farewell", "test", ConfigScope.COMMON, store,
                    Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
            f.runtime.watch(holder);
            assertTrue(f.prepare());
            long initial = f.runtime.generationId();
            holder.set("other", 1);
            holder.save();
            assertTrue(f.owner.isEmpty());
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, ""));
            holder.set("cost.home.fixedAmount", 2);
            holder.save();
            f.next().run();
            assertTrue(f.await(f.runtime.prepare(f.configuration())));
            assertTrue(f.runtime.generationId() > initial);
            assertEquals(2, f.calculate(EnumTeleportType.TP_HOME).amount());
            f.values.put(EnumTeleportType.TP_HOME, parameters(3, ""));
            store.values.put("cost.home.fixedAmount", 3);
            holder.acceptExternalReload();
            f.next().run();
            assertTrue(f.await(f.runtime.prepare(f.configuration())));
            assertEquals(3, f.calculate(EnumTeleportType.TP_HOME).amount());
            f.runtime.close();
            holder.set("cost.home.fixedAmount", 4);
            holder.save();
            store.values.put("cost.home.fixedAmount", 5);
            holder.acceptExternalReload();
            assertTrue(f.owner.isEmpty());
            for (String name : Arrays.asList("savedListeners", "reloadedListeners")) {
                java.lang.reflect.Field listeners = ConfigHolder.class.getDeclaredField(name);
                listeners.setAccessible(true);
                assertTrue(((Collection<?>) listeners.get(holder)).isEmpty());
            }
        }
    }

    @Test
    public void rejectsOversizedMalformedSourcesAndDoesNotCompileOtherDisabledEntrypoints() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.values.put(EnumTeleportType.TP_STAGE, new CostParameters(EnumCostType.NONE, 0, 0, 0, 20, "", "", "Disabled.java"));
            f.source("Disabled.java", "invalid disabled code");
            f.source("Formula.java", source("Formula", "", "return 7;"));
            assertTrue(f.prepare());
            byte[] oversized = new byte[262145];
            Arrays.fill(oversized, (byte) ' ');
            Files.write(f.root.resolve("Formula.java"), oversized);
            assertFalse(f.prepare());
            Files.write(f.root.resolve("Formula.java"), new byte[]{(byte) 0xc3, 0x28});
            assertFalse(f.prepare());
            f.source("Formula.java", source("Formula", "", "return 7;"));
            f.source("helpers/Invalid.java", "package outsider; public class Invalid { }");
            assertFalse(f.prepare());
            Files.delete(f.root.resolve("helpers/Invalid.java"));
            assertTrue(f.prepare());
        }
    }

    @Test
    public void helperLimitIncludesAllFilesButPermitsMoreThanSixteenHelperFiles() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.source("Formula.java", source("Formula", "", "return xin.vanilla.banira.generated.cost.helpers.Helper0.amount();"));
            for (int i = 0; i < 18; i++)
                f.source("helpers/Helper" + i + ".java",
                        "package xin.vanilla.banira.generated.cost.helpers; public class Helper" + i + " { public static double amount() { return 7; } }");
            assertTrue(f.prepare());
            assertEquals(7, f.calculate(EnumTeleportType.TP_HOME).amount());
            for (int i = 18; i < 128; i++)
                f.source("helpers/Helper" + i + ".java",
                        "package xin.vanilla.banira.generated.cost.helpers; public class Helper" + i + " { }");
            assertFalse(f.prepare());
        }
    }

    @Test
    public void ownerRejectionCompletesRequestAndOffThreadAccessIsRejected() throws Exception {
        try (Fixture f = new Fixture()) {
            NarcissusCostRuntime rejected = new NarcissusCostRuntime(f.root, f, f.worker,
                    action -> {
                        throw new RejectedExecutionException("stopped owner");
                    }, error -> {
            });
            try {
                assertFalse(rejected.prepare(f.configuration()).get(5, TimeUnit.SECONDS));
                assertFalse(rejected.prepare(f.configuration()).get(5, TimeUnit.SECONDS));
                Future<?> read = f.worker.submit(rejected::generationId);
                try {
                    read.get(5, TimeUnit.SECONDS);
                    fail("Off-thread runtime access accepted");
                } catch (ExecutionException expected) {
                    assertTrue(expected.getCause() instanceof IllegalStateException);
                }
            } finally {
                rejected.close();
            }
        }
    }

    @Test
    public void cancellationReleasesRequestEvenWithoutAnotherReload() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.source("Formula.java", source("Formula", "", "return 7;"));
            CompletableFuture<Boolean> first = f.runtime.prepare(f.configuration());
            first.cancel(false);
            while (!f.owner.isEmpty()) f.next().run();
            assertNoPreparedCandidate(f.runtime);
        }
    }

    @Test
    public void sourceChangedAfterWorkerVerificationCannotPublishAtOwnerBoundary() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            f.source("Formula.java", source("Formula", "", "return 7;"));
            CompletableFuture<Boolean> pending = f.runtime.prepare(f.configuration());
            f.next().run(); // Hand immutable sources to the compiler.
            f.next().run(); // Bind the factory without initializing its class.
            f.next().run(); // Construct on the owner and queue worker verification.
            Runnable publication = f.next();
            f.source("Formula.java", source("Formula", "", "return 11;"));
            publication.run();
            assertFalse(f.await(pending));
            assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
        }
    }

    @Test
    public void temporarilyInvalidLiveValuesBlockChargingWithoutEscapingToGameLoop() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, ""));
            assertTrue(f.prepare());
            f.invalidParameters = true;
            assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
            assertFalse(f.runtime.calculate(EnumTeleportType.TP_HOME, f.runtime.generationId(), 100, false).isSuccess());
            assertEquals(0, f.calculate(EnumTeleportType.TP_BACK).amount());
            f.invalidParameters = false;
            f.invalidCards = true;
            assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
            assertEquals(0, f.calculate(EnumTeleportType.TP_BACK).amount());
            f.invalidCards = false;
            assertEquals(2, f.calculate(EnumTeleportType.TP_HOME).amount());
            assertEquals(1, f.errors.size());
        }
    }

    @Test
    public void repeatedReloadsReleaseSourceAndFactoryPayloadsAndLinkedRootCannotLoadCode() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            for (int i = 0; i < 6; i++) {
                f.source("Formula.java", source("Formula", "", "return " + i + ";") + "/*" + String.join("", Collections.nCopies(40000, "x")) + "*/");
                assertTrue(f.prepare());
                assertNoPreparedCandidate(f.runtime);
                assertEquals(i, f.calculate(EnumTeleportType.TP_HOME).amount());
            }
            Path outside = temporary.newFolder().toPath();
            Files.write(outside.resolve("Formula.java"), source("Formula", "", "return 7;").getBytes(StandardCharsets.UTF_8));
            Path link = f.root.resolve("linked");
            if (System.getProperty("os.name").startsWith("Windows")) {
                Process process = new ProcessBuilder("cmd.exe", "/d", "/c", "mklink", "/J", link.toString(), outside.toString())
                        .redirectErrorStream(true).start();
                try {
                    assertTrue(process.waitFor(15, TimeUnit.SECONDS));
                    assertEquals(0, process.exitValue());
                } finally {
                    if (process.isAlive()) process.destroyForcibly();
                }
            } else Files.createSymbolicLink(link, outside);
            try {
                f.values.put(EnumTeleportType.TP_HOME, parameters(2, "linked/Formula.java"));
                assertFalse(f.prepare());
                assertFalse(f.calculate(EnumTeleportType.TP_HOME).isSuccess());
            } finally {
                Files.delete(link);
            }
        }
    }

    @Test
    public void preflightRejectsWrongContractAndConstructorSignatureWithoutChangingLegacyFiles() throws Exception {
        try (Fixture f = new Fixture()) {
            f.values.put(EnumTeleportType.TP_HOME, parameters(2, "Formula.java"));
            for (String code : Arrays.asList(
                    "package xin.vanilla.banira.generated.cost; public class Formula { }",
                    "package xin.vanilla.banira.generated.cost; public abstract class Formula implements xin.vanilla.narcissus.api.cost.CostFormula { }",
                    "package xin.vanilla.banira.generated.cost; public class Formula implements xin.vanilla.narcissus.api.cost.CostFormula { private Formula() { } public double calculate(xin.vanilla.narcissus.api.cost.CostContext ctx) { return 1; } }")) {
                try {
                    NarcissusCostRuntime.preflight(f.configuration(), f.root, Collections.singletonMap("Formula.java", code));
                    fail("Invalid entrypoint accepted");
                } catch (java.io.IOException expected) {
                }
                assertFalse(Files.exists(f.root.resolve("Formula.java")));
            }
        }
    }

    private static void assertNoPreparedCandidate(NarcissusCostRuntime runtime) throws Exception {
        // Check actual compiler references, not GC timing or a source-text search.
        for (java.lang.reflect.Field field : NarcissusCostRuntime.class.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(runtime);
            if (value instanceof ScriptSession) {
                java.lang.reflect.Field candidate = value.getClass().getDeclaredField("candidate");
                candidate.setAccessible(true);
                assertNull("Completed runtime retains a prepared factory candidate", candidate.get(value));
            }
        }
        java.lang.reflect.Field pending = NarcissusCostRuntime.class.getDeclaredField("pending");
        pending.setAccessible(true);
        assertNull("Completed runtime retains request/source payload", pending.get(runtime));
    }

    private static final class MapStore implements ConfigValueStore {
        final Map<String, Object> values = new LinkedHashMap<>();

        MapStore() {
            values.put("cost.home.fixedAmount", 0);
            values.put("other", 0);
        }

        public Set<String> paths() {
            return values.keySet();
        }

        public Object get(String path) {
            return values.get(path);
        }

        public void set(String path, Object value) {
            values.put(path, value);
        }

        public Class<?> valueClass(String path) {
            return Integer.class;
        }

        public Object defaultValue(String path) {
            return 0;
        }

        public boolean validate(String path, Object value) {
            return value instanceof Integer;
        }

        public void save() {
        }
    }

    private static CostParameters parameters(double amount, String file) {
        return new CostParameters(EnumCostType.EXP_POINT, amount, 0, 0, 20, "", "", file);
    }

    private static String source(String name, String constructor, String body) {
        return "package xin.vanilla.banira.generated.cost; import xin.vanilla.narcissus.api.cost.*; "
                + "import xin.vanilla.narcissus.internal.server.NarcissusCostRuntimeTest.Probe; public final class " + name
                + " implements CostFormula { public " + name + "() { " + constructor + " } public double calculate(CostContext ctx) { " + body + " } }";
    }

    private static void rejected(Runnable read) {
        try {
            read.run();
            fail("Expired context accepted");
        } catch (IllegalStateException expected) {
        }
    }

    private final class Fixture implements AutoCloseable, NarcissusCostRuntime.LiveConfiguration {
        final Path root = temporary.newFolder().toPath();
        final BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        final List<Throwable> errors = new ArrayList<>();
        final AtomicInteger reads = new AtomicInteger();
        final EnumMap<EnumTeleportType, CostParameters> values = new EnumMap<>(EnumTeleportType.class);
        CostCardSettings cards = CostConfiguration.defaults().cards();
        int maxDistance = 10000, crossDistance = 10000;
        boolean invalidParameters, invalidCards;
        final NarcissusCostRuntime runtime;
        final CostPosition position = new CostPosition("minecraft:overworld", 0, 64, 0, 0, 0, false, EnumSafeMode.NONE);
        final CostPlayerView player = new CostPlayerView() {
            public UUID uuid() {
                return new UUID(0, 1);
            }

            public String name() {
                return "payer";
            }

            public CostPosition position() {
                return position;
            }

            public int experiencePoints() {
                return 20;
            }

            public int experienceLevels() {
                return 2;
            }

            public float health() {
                return 10;
            }

            public float maxHealth() {
                return 20;
            }

            public int foodLevel() {
                return 10;
            }

            public int teleportCards() {
                return 2;
            }

            public boolean alive() {
                return true;
            }

            public boolean removed() {
                return false;
            }

            public boolean creative() {
                return false;
            }

            public boolean spectator() {
                return false;
            }

            public <T> T nativePlayer(Class<T> type) {
                return type.cast(this);
            }

            public <T> T nativeTeleportData(Class<T> type) {
                return type.cast(this);
            }
        };
        final CostWorldView world = new CostWorldView() {
            public String dimensionId() {
                return position.dimensionId();
            }

            public long gameTime() {
                return 100;
            }

            public long dayTime() {
                return 100;
            }

            public boolean raining() {
                return false;
            }

            public boolean thundering() {
                return false;
            }

            public boolean chunkLoaded(int x, int z) {
                return false;
            }

            public Optional<String> blockId(int x, int y, int z) {
                return Optional.empty();
            }

            public List<CostPlayerView> players() {
                return Collections.singletonList(player);
            }

            public <T> T nativeWorld(Class<T> type) {
                return type.cast(this);
            }
        };
        final CostServerView server = new CostServerView() {
            public long tick() {
                return 50;
            }

            public int onlinePlayerCount() {
                return 1;
            }

            public Optional<CostPlayerView> player(UUID id) {
                return Optional.of(player);
            }

            public List<CostPlayerView> players() {
                return Collections.singletonList(player);
            }

            public Optional<CostWorldView> world(String id) {
                return Optional.of(world);
            }

            public <T> T nativeServer(Class<T> type) {
                return type.cast(this);
            }
        };

        Fixture() throws Exception {
            values.putAll(CostConfiguration.defaults().groups());
            runtime = new NarcissusCostRuntime(root, this, worker, owner::add, errors::add);
        }

        public CostParameters parameters(EnumTeleportType type) {
            reads.incrementAndGet();
            if (invalidParameters && type == EnumTeleportType.TP_HOME)
                throw new IllegalArgumentException("Incomplete cost edit");
            return values.getOrDefault(type, CostParameters.free());
        }

        public CostCardSettings cards() {
            if (invalidCards) throw new IllegalStateException("Card configuration rebinding");
            return cards;
        }

        public int maxDistance() {
            return maxDistance;
        }

        public int crossDimensionDistance() {
            return crossDistance;
        }

        CostConfiguration configuration() {
            return new CostConfiguration(values, cards, maxDistance, crossDistance);
        }

        void set(CostConfiguration configuration) {
            values.clear();
            values.putAll(configuration.groups());
            cards = configuration.cards();
            maxDistance = configuration.maxDistance();
            crossDistance = configuration.crossDimensionDistance();
        }

        void source(String name, String text) throws Exception {
            Path path = root.resolve(name);
            Files.createDirectories(path.getParent());
            Files.write(path, text.getBytes(StandardCharsets.UTF_8));
        }

        boolean prepare() throws Exception {
            return await(runtime.prepare(configuration()));
        }

        Runnable next() throws Exception {
            Runnable next = owner.poll(15, TimeUnit.SECONDS);
            assertNotNull("Owner callback timed out", next);
            return next;
        }

        boolean await(CompletableFuture<Boolean> result) throws Exception {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (!result.isDone()) {
                Runnable next = owner.poll(Math.max(1, end - System.nanoTime()), TimeUnit.NANOSECONDS);
                assertNotNull("Owner callback timed out", next);
                next.run();
            }
            return result.get(1, TimeUnit.SECONDS);
        }

        CostCalculation calculate(EnumTeleportType type) {
            return runtime.calculate(input(type).build());
        }

        CostContextInput.CostContextInputBuilder input(EnumTeleportType type) {
            return CostContextInput.builder().operationId(1).generationId(Math.max(1, runtime.generationId()))
                    .phase(CostPhase.CHECK).teleportType(type).parameters(values.getOrDefault(type, CostParameters.free()))
                    .cardSettings(cards).player(player).payer(player).source(position).sourceWorld(world).server(server)
                    .rawDistance(100).distance(100);
        }

        public void close() throws Exception {
            runtime.close();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(15, TimeUnit.SECONDS));
            Probe.retained = null;
            Probe.nested = null;
        }
    }
}
