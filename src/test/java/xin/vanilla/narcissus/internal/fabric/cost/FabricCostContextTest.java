package xin.vanilla.narcissus.internal.fabric.cost;

import org.junit.Test;
import xin.vanilla.banira.api.script.*;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;
import xin.vanilla.narcissus.service.cost.CostCalculator;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class FabricCostContextTest {
    private static final CostParameters PARAMETERS = new CostParameters(EnumCostType.EXP_POINT, 2, .002, 0, 20, "", "", "custom.java");

    @Test public void distinguishesMovingPlayerFromPayerAndUsesActualSource() {
        Fixture fixture = new Fixture();
        try (CostEvaluation evaluation = CostEvaluation.open(fixture.input())) {
            CostContext context = evaluation.context();
            assertEquals("moving", context.player().name());
            assertEquals("paying", context.payer().name());
            assertEquals("moving", context.targetPlayer().get().name());
            assertEquals("minecraft:the_nether", context.source().dimensionId());
            assertEquals("minecraft:overworld", context.destination().get().dimensionId());
            assertTrue(context.crossDimension());
            assertEquals(10000, context.distance(), 0);
            assertEquals(500, context.rawDistance(), 0);
            assertEquals(2, context.parameters().fixedAmount(), 0);
            assertEquals(CostPhase.PREVIEW, context.phase());
        }
    }

    @Test public void borrowedViewsExpireAndCannotReadTheNextPlayer() {
        Fixture fixture = new Fixture();
        CostContext expired;
        CostPlayerView player;
        try (CostEvaluation first = CostEvaluation.open(fixture.input())) {
            expired = first.context(); player = expired.player();
        }
        try (CostEvaluation second = CostEvaluation.open(fixture.input())) {
            rejected(expired::phase);
            rejected(player::name);
            assertEquals("moving", second.context().player().name());
        }
    }

    @Test public void nestedEvaluationSuspendsOuterViewsAndRestoresThem() {
        Fixture fixture = new Fixture();
        CostContext inner;
        try (CostEvaluation outer = CostEvaluation.open(fixture.input())) {
            CostPlayerView outerPlayer = outer.context().player();
            try (CostEvaluation nested = CostEvaluation.open(fixture.input())) {
                inner = nested.context();
                rejected(outerPlayer::name);
                assertEquals("moving", inner.player().name());
            }
            rejected(inner::phase);
            assertEquals("moving", outerPlayer.name());
        }
    }

    @Test public void exceptionInNestedEvaluationRestoresTheOuterContext() {
        Fixture fixture = new Fixture();
        CostContext inner = null;
        try (CostEvaluation outer = CostEvaluation.open(fixture.input())) {
            try (CostEvaluation nested = CostEvaluation.open(fixture.input())) {
                inner = nested.context();
                throw new IllegalArgumentException("Nested formula failed");
            } catch (IllegalArgumentException expected) {
                assertEquals("moving", outer.context().player().name());
                rejected(inner::phase);
            }
        }
    }

    @Test public void invalidNestedInputDoesNotSuspendTheOuterContext() {
        Fixture fixture = new Fixture();
        try (CostEvaluation outer = CostEvaluation.open(fixture.input())) {
            try { CostEvaluation.open(fixture.inputBuilder().operationId(0).build()); fail("Invalid operation"); }
            catch (IllegalArgumentException expected) { }
            assertEquals("moving", outer.context().player().name());
        }
    }

    @Test public void unknownDestinationHasNoArtificialWorldAndSnapshotsSurviveClosure() {
        Fixture fixture = new Fixture();
        CostPosition position;
        CostParameters parameters;
        CostWorldView world;
        try (CostEvaluation evaluation = CostEvaluation.open(fixture.inputBuilder().destination(null)
                .targetWorld(null).targetPlayer(null).requester(null).request(null).build())) {
            CostContext context = evaluation.context();
            assertFalse(context.destination().isPresent());
            assertFalse(context.targetWorld().isPresent());
            assertFalse(context.targetPlayer().isPresent());
            assertFalse(context.requester().isPresent());
            assertFalse(context.request().isPresent());
            assertEquals(0, fixture.worldQueries);
            position = context.source(); parameters = context.parameters(); world = context.sourceWorld();
        }
        assertEquals("minecraft:the_nether", position.dimensionId());
        assertEquals(2, parameters.fixedAmount(), 0);
        rejected(world::dimensionId);
    }

    @Test public void crossThreadAccessAndOutOfOrderCloseAreRejected() throws Exception {
        Fixture fixture = new Fixture();
        try (CostEvaluation outer = CostEvaluation.open(fixture.input())) {
            CostContext context = outer.context();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread reader = new Thread(() -> {
                try { context.phase(); } catch (Throwable error) { failure.set(error); }
            });
            reader.start(); reader.join();
            assertTrue(failure.get() instanceof IllegalStateException);
            try (CostEvaluation inner = CostEvaluation.open(fixture.input())) { rejected(outer::close); }
            assertEquals("moving", context.player().name());
        }
    }

    @Test public void defaultCalculationDoesNotQueryAnyWorldOrPlayer() {
        Fixture fixture = new Fixture();
        assertEquals(5, new CostCalculator().calculate(PARAMETERS, 1500).amount());
        assertEquals(0, fixture.worldQueries);
        assertEquals(0, fixture.playerQueries);
    }

    @Test public void listsAreLazyReadOnlyAndTheirElementsExpire() {
        Fixture fixture = new Fixture();
        CostPlayerView element;
        try (CostEvaluation evaluation = CostEvaluation.open(fixture.input())) {
            CostContext context = evaluation.context();
            assertEquals(0, fixture.worldQueries);
            List<CostPlayerView> players = context.sourceWorld().players();
            assertEquals(1, fixture.worldQueries);
            try { players.clear(); fail("Mutable list"); } catch (UnsupportedOperationException expected) { }
            element = players.get(0);
            assertEquals("moving", element.name());
            assertFalse(context.server().world("missing:world").isPresent());
            assertFalse(context.sourceWorld().blockId(100, 64, 100).isPresent());
        }
        rejected(element::name);
    }

    @Test public void nativeAccessChecksTypeAndExpiresButCannotRevokeReturnedReferences() {
        Fixture fixture = new Fixture();
        CostPlayerView player;
        Object nativeObject;
        try (CostEvaluation evaluation = CostEvaluation.open(fixture.input())) {
            player = evaluation.context().player();
            nativeObject = player.nativePlayer(Object.class);
            assertSame(fixture.moving, nativeObject);
            try { player.nativePlayer(String.class); fail("Wrong native type"); } catch (ClassCastException expected) { }
        }
        rejected(() -> player.nativePlayer(Object.class));
        assertSame(fixture.moving, nativeObject);
    }

    @Test public void formulaFailuresBecomeFailuresAndEvaluationStillCloses() {
        Fixture fixture = new Fixture();
        CostContext saved;
        try (CostEvaluation evaluation = CostEvaluation.open(fixture.input())) {
            saved = evaluation.context();
            CostCalculation result = new CostCalculator().calculate(PARAMETERS, saved, ctx -> { throw new IllegalArgumentException("bad formula"); });
            assertFalse(result.isSuccess());
            for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1}) {
                assertFalse(new CostCalculator().calculate(PARAMETERS, saved, ctx -> invalid).isSuccess());
            }
        }
        rejected(saved::phase);
    }

    @Test public void actualJaninoFormulaUsesRichPublicContext() throws Exception {
        Fixture fixture = new Fixture();
        ExecutorService owner = Executors.newSingleThreadExecutor();
        ScriptSession<ScriptFactory<CostFormula>> session = BaniraScripts.openFactorySession(
                "narcissus-cost-test", CostFormula.class, "1", ScriptLimits.defaults(), owner);
        try {
            String source = "package xin.vanilla.banira.generated.cost; public final class QuoteFormula implements xin.vanilla.narcissus.api.cost.CostFormula { "
                    + "public double calculate(xin.vanilla.narcissus.api.cost.CostContext ctx) { "
                    + "return ctx.payer().health() + ctx.parameters().fixedAmount() "
                    + "+ (ctx.player().position().dimensionId().equals(\"minecraft:the_nether\") ? 1 : 0) "
                    + "+ (ctx.targetWorld().isPresent() ? 1 : 0); } }";
            PreparedScripts<ScriptFactory<CostFormula>> prepared = session.prepare(Collections.singletonList(
                    new ScriptSource("quote", "xin.vanilla.banira.generated.cost.QuoteFormula", "QuoteFormula.java", source))).get(15, TimeUnit.SECONDS);
            Future<Integer> result = owner.submit(() -> {
                CostFormula formula = prepared.scripts().get("quote").create();
                try (CostEvaluation evaluation = CostEvaluation.open(fixture.input())) {
                    return new CostCalculator().calculate(PARAMETERS, evaluation.context(), formula).amount();
                }
            });
            assertEquals(Integer.valueOf(14), result.get(15, TimeUnit.SECONDS));
        } finally {
            session.close(); owner.shutdownNow(); assertTrue(owner.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    private static void rejected(Runnable read) {
        try { read.run(); fail("Expected expired or suspended access rejection"); }
        catch (IllegalStateException expected) { }
    }

    private static final class Fixture {
        int worldQueries;
        int playerQueries;
        final FakePlayer moving = new FakePlayer("moving", "minecraft:the_nether");
        final FakePlayer paying = new FakePlayer("paying", "minecraft:overworld");
        final CostWorldView source = world("minecraft:the_nether");
        final CostWorldView destination = world("minecraft:overworld");
        final CostServerView server = new CostServerView() {
            public long tick() { return 50; }
            public int onlinePlayerCount() { return 2; }
            public Optional<CostPlayerView> player(UUID id) { return moving.uuid().equals(id) ? Optional.of(moving) : Optional.empty(); }
            public List<CostPlayerView> players() { playerQueries++; return Arrays.asList(moving, paying); }
            public Optional<CostWorldView> world(String id) { return id.equals(destination.dimensionId()) ? Optional.of(destination) : Optional.empty(); }
            public <T> T nativeServer(Class<T> type) { return type.cast(this); }
        };

        CostContextInput input() {
            return inputBuilder().build();
        }

        CostContextInput.CostContextInputBuilder inputBuilder() {
            return CostContextInput.builder().operationId(7).generationId(1).phase(CostPhase.PREVIEW)
                    .teleportType(EnumTeleportType.TP_HERE).parameters(PARAMETERS)
                    .cardSettings(new CostCardSettings(true, 0, EnumCardType.WAIVE_COST))
                    .player(moving).payer(paying).requester(paying).targetPlayer(moving)
                    .source(moving.position()).destination(paying.position()).rawDistance(500).distance(10000)
                    .sourceWorld(source).targetWorld(destination).server(server).cooldownSeconds(30).countdownSeconds(3)
                    .request(new CostRequestInfo("request", 1, 1000, true, false));
        }

        CostWorldView world(String id) {
            return new CostWorldView() {
                public String dimensionId() { return id; }
                public long gameTime() { return 100; }
                public long dayTime() { return 100; }
                public boolean raining() { return false; }
                public boolean thundering() { return false; }
                public boolean chunkLoaded(int x, int z) { worldQueries++; return false; }
                public Optional<String> blockId(int x, int y, int z) { worldQueries++; return Optional.empty(); }
                public List<CostPlayerView> players() { worldQueries++; return Collections.singletonList(moving); }
                public <T> T nativeWorld(Class<T> type) { return type.cast(this); }
            };
        }
    }

    private static final class FakePlayer implements CostPlayerView {
        private final String name;
        private final String dimension;
        FakePlayer(String name, String dimension) { this.name = name; this.dimension = dimension; }
        public UUID uuid() { return UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        public String name() { return name; }
        public CostPosition position() { return new CostPosition(dimension, 0, 64, 0, 0, 0, false, EnumSafeMode.NONE); }
        public int experiencePoints() { return 20; }
        public int experienceLevels() { return 2; }
        public float health() { return 10; }
        public float maxHealth() { return 20; }
        public int foodLevel() { return 10; }
        public int teleportCards() { return 2; }
        public boolean alive() { return true; }
        public boolean removed() { return false; }
        public boolean creative() { return false; }
        public boolean spectator() { return false; }
        public <T> T nativePlayer(Class<T> type) { return type.cast(this); }
        public <T> T nativeTeleportData(Class<T> type) { return type.cast(this); }
    }
}
