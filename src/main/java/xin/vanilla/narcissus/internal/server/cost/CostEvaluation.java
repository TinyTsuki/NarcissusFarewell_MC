package xin.vanilla.narcissus.internal.server.cost;

import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.CostContextInput;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.*;

/** One invocation identity; never reactivate a context retained by external code. */
public final class CostEvaluation implements AutoCloseable {
    private static final ThreadLocal<CostEvaluation> ACTIVE = new ThreadLocal<>();
    private final Thread owner = Thread.currentThread();
    private final CostEvaluation parent;
    private final CostContext context = new Context();
    private CostContextInput input;
    private List<Borrowed<?>> borrowed;
    private Player player;
    private Player payer;
    private Player requester;
    private Player targetPlayer;
    private World sourceWorld;
    private World targetWorld;
    private Server server;
    private boolean closed;

    private CostEvaluation(CostContextInput input) {
        this.input = Objects.requireNonNull(input, "input");
        Objects.requireNonNull(input.phase(), "phase");
        Objects.requireNonNull(input.teleportType(), "teleportType");
        Objects.requireNonNull(input.parameters(), "parameters");
        Objects.requireNonNull(input.cardSettings(), "cardSettings");
        Objects.requireNonNull(input.player(), "player");
        Objects.requireNonNull(input.payer(), "payer");
        Objects.requireNonNull(input.source(), "source");
        Objects.requireNonNull(input.sourceWorld(), "sourceWorld");
        Objects.requireNonNull(input.server(), "server");
        if (input.operationId() < 1 || input.generationId() < 1 || input.cooldownSeconds() < 0
                || input.countdownSeconds() < 0 || !Double.isFinite(input.rawDistance()) || input.rawDistance() < 0
                || !Double.isFinite(input.distance()) || input.distance() < 0) {
            throw new IllegalArgumentException("Invalid cost invocation");
        }
        parent = ACTIVE.get();
        ACTIVE.set(this);
    }

    public static CostEvaluation open(CostContextInput input) { return new CostEvaluation(input); }

    public CostContext context() { check(); return context; }

    private void check() {
        if (closed || Thread.currentThread() != owner || ACTIVE.get() != this) {
            throw new IllegalStateException("Cost context expired, suspended or accessed off its owner thread");
        }
    }

    @Override public void close() {
        if (closed) return;
        check();
        closed = true;
        if (parent == null) ACTIVE.remove(); else ACTIVE.set(parent);
        input = null;
        if (borrowed != null) {
            for (Borrowed<?> view : borrowed) view.delegate = null;
            borrowed.clear();
        }
    }

    private abstract class Borrowed<T> {
        private T delegate;
        Borrowed(T delegate) {
            this.delegate = Objects.requireNonNull(delegate, "view");
            if (borrowed == null) borrowed = new ArrayList<>();
            borrowed.add(this);
        }
        T read() { check(); return delegate; }
    }

    private List<CostPlayerView> players(List<CostPlayerView> raw) {
        List<CostPlayerView> result = new ArrayList<>(raw.size());
        for (CostPlayerView value : raw) result.add(new Player(value));
        return Collections.unmodifiableList(result);
    }

    private final class Player extends Borrowed<CostPlayerView> implements CostPlayerView {
        Player(CostPlayerView player) { super(player); }
        public UUID uuid() { return read().uuid(); }
        public String name() { return read().name(); }
        public CostPosition position() { return read().position(); }
        public int experiencePoints() { return read().experiencePoints(); }
        public int experienceLevels() { return read().experienceLevels(); }
        public float health() { return read().health(); }
        public float maxHealth() { return read().maxHealth(); }
        public int foodLevel() { return read().foodLevel(); }
        public int teleportCards() { return read().teleportCards(); }
        public boolean alive() { return read().alive(); }
        public boolean removed() { return read().removed(); }
        public boolean creative() { return read().creative(); }
        public boolean spectator() { return read().spectator(); }
        public <T> T nativePlayer(Class<T> type) { return Objects.requireNonNull(type, "type").cast(read().nativePlayer(type)); }
        public <T> T nativeTeleportData(Class<T> type) { return Objects.requireNonNull(type, "type").cast(read().nativeTeleportData(type)); }
    }

    private final class World extends Borrowed<CostWorldView> implements CostWorldView {
        World(CostWorldView world) { super(world); }
        public String dimensionId() { return read().dimensionId(); }
        public long gameTime() { return read().gameTime(); }
        public long dayTime() { return read().dayTime(); }
        public boolean raining() { return read().raining(); }
        public boolean thundering() { return read().thundering(); }
        public boolean chunkLoaded(int x, int z) { return read().chunkLoaded(x, z); }
        public Optional<String> blockId(int x, int y, int z) { return read().blockId(x, y, z); }
        public List<CostPlayerView> players() { return CostEvaluation.this.players(read().players()); }
        public <T> T nativeWorld(Class<T> type) { return Objects.requireNonNull(type, "type").cast(read().nativeWorld(type)); }
    }

    private final class Server extends Borrowed<CostServerView> implements CostServerView {
        Server(CostServerView server) { super(server); }
        public long tick() { return read().tick(); }
        public int onlinePlayerCount() { return read().onlinePlayerCount(); }
        public Optional<CostPlayerView> player(UUID id) { return read().player(id).map(Player::new); }
        public List<CostPlayerView> players() { return CostEvaluation.this.players(read().players()); }
        public Optional<CostWorldView> world(String id) { return read().world(id).map(World::new); }
        public <T> T nativeServer(Class<T> type) { return Objects.requireNonNull(type, "type").cast(read().nativeServer(type)); }
    }

    private final class Context implements CostContext {
        public long operationId() { check(); return input.operationId(); }
        public long generationId() { check(); return input.generationId(); }
        public CostPhase phase() { check(); return input.phase(); }
        public EnumTeleportType teleportType() { check(); return input.teleportType(); }
        public EnumCostType costType() { return parameters().type(); }
        public CostParameters parameters() { check(); return input.parameters(); }
        public CostCardSettings cardSettings() { check(); return input.cardSettings(); }
        public CostPosition source() { check(); return input.source(); }
        public Optional<CostPosition> destination() { check(); return Optional.ofNullable(input.destination()); }
        public double rawDistance() { check(); return input.rawDistance(); }
        public double distance() { check(); return input.distance(); }
        public boolean crossDimension() { check(); return input.destination() != null && !input.source().dimensionId().equals(input.destination().dimensionId()); }
        public int cooldownSeconds() { check(); return input.cooldownSeconds(); }
        public int countdownSeconds() { check(); return input.countdownSeconds(); }
        public Optional<CostRequestInfo> request() { check(); return Optional.ofNullable(input.request()); }
        public CostPlayerView player() { check(); if (player == null) player = new Player(input.player()); return player; }
        public CostPlayerView payer() {
            check();
            if (input.player() == input.payer()) return player();
            if (payer == null) payer = new Player(input.payer());
            return payer;
        }
        public Optional<CostPlayerView> requester() {
            check();
            if (input.requester() == null) return Optional.empty();
            if (requester == null) requester = new Player(input.requester());
            return Optional.of(requester);
        }
        public Optional<CostPlayerView> targetPlayer() {
            check();
            if (input.targetPlayer() == null) return Optional.empty();
            if (targetPlayer == null) targetPlayer = new Player(input.targetPlayer());
            return Optional.of(targetPlayer);
        }
        public CostWorldView sourceWorld() {
            check(); if (sourceWorld == null) sourceWorld = new World(input.sourceWorld()); return sourceWorld;
        }
        public Optional<CostWorldView> targetWorld() {
            check();
            if (input.targetWorld() == null) return Optional.empty();
            if (targetWorld == null) targetWorld = new World(input.targetWorld());
            return Optional.of(targetWorld);
        }
        public CostServerView server() { check(); if (server == null) server = new Server(input.server()); return server; }
    }
}
