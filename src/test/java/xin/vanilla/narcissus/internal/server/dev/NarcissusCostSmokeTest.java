package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.narcissus.internal.dev.NarcissusCostSmokeState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;

public class NarcissusCostSmokeTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void rejectsUnknownPhasesAndMissingOrAmbiguousPaths() {
        Properties values = settings();
        values.setProperty("narcissus.costSmoke.phase", "other");
        Properties unknown = values;
        rejected(() -> NarcissusCostSmokeState.from(unknown, "server", 0));
        values = settings();
        values.remove("narcissus.costSmoke.status");
        Properties missing = values;
        rejected(() -> NarcissusCostSmokeState.from(missing, "server", 0));
        values = settings();
        values.setProperty("narcissus.costSmoke.peer", values.getProperty("narcissus.costSmoke.status"));
        Properties ambiguous = values;
        rejected(() -> NarcissusCostSmokeState.from(ambiguous, "server", 0));
    }

    @Test
    public void timeoutIsWallClockAndCannotBeExtendedByProgress() {
        NarcissusCostSmokeState state = NarcissusCostSmokeState.from(settings(), "client", 10);
        state.checkDeadline(10 + 299_999_999_999L);
        rejected(() -> state.checkDeadline(10 + 300_000_000_000L));
    }

    @Test
    public void requiresExactMarkersAndPropagatesPeerFailure() throws Exception {
        Properties values = settings();
        Path status = temporary.getRoot().toPath().resolve("client.status");
        Path peer = status.resolveSibling("server.status");
        values.setProperty("narcissus.costSmoke.status", status.toString());
        values.setProperty("narcissus.costSmoke.peer", peer.toString());
        NarcissusCostSmokeState state = NarcissusCostSmokeState.from(values, "client", 0);
        assertFalse(state.peerHas("PASS quote"));
        Files.write(peer, Arrays.asList("PASS quote-extra", "PASS quote suffix"), StandardCharsets.UTF_8);
        assertFalse(state.peerHas("PASS quote"));
        Files.write(peer, Arrays.asList("PASS quote"), StandardCharsets.UTF_8);
        assertTrue(state.peerHas("PASS quote"));
        state.append("PASS roundtrip");
        assertEquals(Arrays.asList("PASS roundtrip"), Files.readAllLines(status, StandardCharsets.UTF_8));
        Files.write(peer, Arrays.asList("PASS quote", "FAIL unexpected disconnect"), StandardCharsets.UTF_8);
        rejected(() -> state.peerHas("PASS quote"));
        rejected(() -> state.append("PASS first\nPASS forged"));
    }

    @Test
    public void waitsForNativeLogoutEvenAfterThePeerAcknowledgement() throws Exception {
        Properties values = settings();
        NarcissusCostSmokeState state = NarcissusCostSmokeState.from(values, "server", 0);
        Path peer = java.nio.file.Paths.get(values.getProperty("narcissus.costSmoke.peer"));
        assertFalse(state.peerDisconnected(false));
        Files.write(peer, Arrays.asList("PASS client-disconnected"), StandardCharsets.UTF_8);
        assertFalse(state.peerDisconnected(true));
        assertTrue(state.peerDisconnected(false));
    }

    private Properties settings() {
        Properties values = new Properties();
        values.setProperty("narcissus.costSmoke.phase", "integration");
        values.setProperty("narcissus.costSmoke.status", temporary.getRoot().toPath().resolve("self.status").toString());
        values.setProperty("narcissus.costSmoke.peer", temporary.getRoot().toPath().resolve("peer.status").toString());
        return values;
    }

    @Test
    public void nativeHelperSourcesCompileThroughTheRealScriptSession() throws Exception {
        java.util.Map<String, String> files = NarcissusCostSmokeServerRunner.formulaSources("getY");
        assertEquals(2, files.size());
        java.util.concurrent.ExecutorService owner = java.util.concurrent.Executors.newSingleThreadExecutor();
        xin.vanilla.banira.api.script.ScriptSession<xin.vanilla.banira.api.script.ScriptFactory<xin.vanilla.narcissus.api.cost.CostFormula>> session =
                xin.vanilla.banira.api.script.BaniraScripts.openFactorySession("narcissus_farewell",
                        xin.vanilla.narcissus.api.cost.CostFormula.class, "1",
                        new xin.vanilla.banira.api.script.ScriptLimits(262144, 8388608, 128), owner);
        try {
            xin.vanilla.banira.api.script.PreparedScripts<xin.vanilla.banira.api.script.ScriptFactory<xin.vanilla.narcissus.api.cost.CostFormula>> prepared =
                    session.prepareGroups(java.util.Collections.singletonList(new xin.vanilla.banira.api.script.ScriptSourceGroup(
                            "native-helper", "xin.vanilla.banira.generated.cost.SmokeFee", files))).get(15, java.util.concurrent.TimeUnit.SECONDS);
            xin.vanilla.narcissus.api.cost.CostFormula formula = owner.submit(() -> prepared.scripts().get("native-helper").create()).get(15, java.util.concurrent.TimeUnit.SECONDS);
            Class<?> helper = formula.getClass().getClassLoader().loadClass("xin.vanilla.banira.generated.cost.helpers.SmokeNative");
            assertEquals(double.class, helper.getMethod("value", xin.vanilla.narcissus.api.cost.CostContext.class).getReturnType());
        } finally {
            session.close();
            owner.shutdownNow();
            assertTrue(owner.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS));
        }
        rejected(() -> NarcissusCostSmokeServerRunner.formulaSources("notAMethod();"));
    }

    private static void rejected(Runnable action) {
        try {
            action.run();
            fail("Expected rejection");
        } catch (IllegalArgumentException | IllegalStateException expected) {
        }
    }
}
