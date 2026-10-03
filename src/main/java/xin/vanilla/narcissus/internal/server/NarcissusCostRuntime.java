package xin.vanilla.narcissus.internal.server;

import xin.vanilla.banira.api.script.*;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;
import xin.vanilla.narcissus.service.cost.CostCalculator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Server-owned generations of trusted local code; this is not a Java sandbox. */
public final class NarcissusCostRuntime implements AutoCloseable {
    private static final int SOURCE_BYTES = 262144, BATCH_BYTES = 8388608, SOURCE_COUNT = 128;
    private static final String NAMESPACE = "xin.vanilla.banira.generated.cost.";
    private static final String BUNDLE = "_NarcissusCostBundle";
    private final Thread ownerThread = Thread.currentThread();
    private final Path sourceRoot;
    private final LiveConfiguration live;
    private final Executor worker, owner;
    private final Consumer<Throwable> diagnostics;
    private final CostCalculator calculator = new CostCalculator();
    private final List<Runnable> unregister = new ArrayList<>();
    private volatile boolean closed;
    private Request pending;
    private Generation active;
    private long sequence;
    private boolean reportedConfigurationFailure;

    /** Implement with generated views: each charged operation reads just one group. */
    public interface LiveConfiguration {
        CostParameters parameters(EnumTeleportType type);
        CostCardSettings cards();
        int maxDistance();
        int crossDimensionDistance();

        default CostConfiguration snapshot() {
            EnumMap<EnumTeleportType, CostParameters> groups = new EnumMap<>(EnumTeleportType.class);
            for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) groups.put(type, parameters(type));
            return new CostConfiguration(groups, cards(), maxDistance(), crossDimensionDistance());
        }
    }

    public NarcissusCostRuntime(Path sourceRoot, LiveConfiguration live, Executor worker, Executor owner,
                                Consumer<Throwable> diagnostics) {
        this.sourceRoot = Objects.requireNonNull(sourceRoot, "sourceRoot").toAbsolutePath().normalize();
        this.live = Objects.requireNonNull(live, "live");
        this.worker = Objects.requireNonNull(worker, "worker");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public void watch(ConfigHolder holder) {
        checkOwner();
        if (closed) throw new IllegalStateException("Cost runtime is closed");
        Consumer<Set<String>> changed = paths -> {
            if (closed || paths.stream().noneMatch(path -> path.startsWith("cost."))) return;
            dispatch(() -> {
                if (!closed) {
                    try { prepare(live.snapshot()); }
                    catch (RuntimeException error) { invalidate(); report(error); }
                }
            }, null);
        };
        unregister.add(holder.onSaved(changed));
        unregister.add(holder.onReloaded(changed));
    }

    public CompletableFuture<Boolean> prepare(CostConfiguration configuration) {
        checkOwner();
        Objects.requireNonNull(configuration, "configuration");
        if (closed) return CompletableFuture.completedFuture(false);
        if (pending != null && !pending.result.isDone() && same(pending.configuration, configuration)) return pending.result;
        Request previous = pending;
        Request request = new Request(configuration);
        pending = request;
        reportedConfigurationFailure = false;
        request.result.whenComplete((result, failure) -> {
            if (request.result.isCancelled()) dispatch(() -> {
                if (pending == request) { pending = null; request.release(); }
            }, request);
        });
        invalidate();
        if (previous != null) { previous.release(); previous.result.complete(false); }
        try {
            worker.execute(() -> {
                try {
                    if (closed || request.result.isDone()) return;
                    Sources sources = Sources.load(sourceRoot, configuration, Collections.emptyMap());
                    if (closed || request.result.isDone()) return;
                    dispatch(() -> compile(request, sources), request);
                } catch (Exception | LinkageError error) { reject(request, error); }
            });
        } catch (RuntimeException error) { reject(request, error); }
        return request.result;
    }

    private void compile(Request request, Sources sources) {
        checkOwner();
        if (!current(request)) return;
        request.sources = sources;
        if (sources.group == null) { finish(request, null); return; }
        request.session = BaniraScripts.openFactorySession("narcissus_farewell", CostFormula.class, "1",
                new ScriptLimits(SOURCE_BYTES, BATCH_BYTES, SOURCE_COUNT), owner);
        request.session.prepareGroups(Collections.singletonList(sources.group)).whenComplete((prepared, error) -> {
            dispatch(() -> {
                if (!current(request)) return;
                sources.group = null;
                if (error != null) { fail(request, error); return; }
                CostFormula formula;
                try { formula = prepared.scripts().get("cost").create(); }
                catch (RuntimeException | LinkageError failure) { fail(request, failure); return; }
                finally { request.closeSession(); }
                if (!current(request)) return;
                try {
                    worker.execute(() -> {
                        try {
                            sources.verify(sourceRoot);
                            dispatch(() -> finish(request, formula), request);
                        } catch (Exception | LinkageError failure) { reject(request, failure); }
                    });
                } catch (RuntimeException failure) { fail(request, failure); }
            }, request);
        });
    }

    private void finish(Request request, CostFormula formula) {
        checkOwner();
        if (!current(request)) return;
        try {
            if (!same(request.configuration, live.snapshot())) throw new IllegalStateException("Cost configuration changed during preparation");
            if (!request.sources.fingerprints.isEmpty()) request.sources.verify(sourceRoot);
            if (!same(request.configuration, live.snapshot())) throw new IllegalStateException("Cost configuration changed before publication");
            active = new Generation(++sequence, request.configuration, formula);
            pending = null;
            request.release();
            request.result.complete(true);
        } catch (IOException | RuntimeException error) { fail(request, error); }
    }

    public long generationId() { checkOwner(); return active == null ? 0 : active.id; }

    /** Default arithmetic does not need player/world views or an invocation context. */
    public CostCalculation calculate(EnumTeleportType type, long generationId, double rawDistance, boolean crossDimension) {
        checkOwner();
        if (closed) return unavailable();
        CostParameters parameters = currentParameters(type);
        if (parameters == null || closed) return unavailable();
        if (parameters.type() == EnumCostType.NONE) return CostCalculation.FREE;
        Generation generation = active;
        if (generation == null || generation.id != generationId || !parameters.customFile().isEmpty()
                || !selectionMatches(generation, type, parameters)) return unavailable();
        if (!Double.isFinite(rawDistance) || rawDistance < 0) return CostCalculation.failed(EnumCostFailure.INVALID_DISTANCE);
        return calculator.calculate(parameters, generation.configuration.distance(rawDistance, crossDimension));
    }

    public CostCalculation calculate(CostContextInput input) {
        checkOwner();
        try (CostEvaluation evaluation = CostEvaluation.open(input)) { return calculate(evaluation.context()); }
    }

    public CostCalculation calculate(CostContext context) {
        checkOwner();
        if (closed) return unavailable();
        EnumTeleportType type = context.teleportType();
        CostParameters parameters = currentParameters(type);
        if (parameters == null || closed) return unavailable();
        if (parameters.type() == EnumCostType.NONE) return CostCalculation.FREE;
        Generation generation = active;
        if (!matches(generation, context, parameters)) return unavailable();
        generation.leases++;
        try {
            CostCalculation result = calculator.calculate(parameters, context,
                    parameters.customFile().isEmpty() ? null : generation.formula);
            // Trusted code can re-enter, stop the world, or change configuration while evaluating.
            if (!parameters.customFile().isEmpty() && !matches(generation, context, currentParameters(type))) return unavailable();
            return result;
        } finally { generation.release(); }
    }

    private boolean matches(Generation generation, CostContext context, CostParameters parameters) {
        if (closed || generation == null || generation != active || generation.retired
                || generation.id != context.generationId() || !selectionMatches(generation, context.teleportType(), parameters)
                || !parameters.equals(context.parameters())
                || !generation.configuration.cards().equals(context.cardSettings())) return false;
        return Double.compare(context.distance(), generation.configuration.distance(context.rawDistance(), context.crossDimension())) == 0;
    }

    private boolean selectionMatches(Generation generation, EnumTeleportType type, CostParameters parameters) {
        try {
            return generation.configuration.parameters(type).equals(parameters) && generation.configuration.cards().equals(live.cards())
                    && generation.configuration.maxDistance() == live.maxDistance()
                    && generation.configuration.crossDimensionDistance() == live.crossDimensionDistance();
        } catch (RuntimeException failure) { configurationFailure(failure); return false; }
    }

    private CostParameters currentParameters(EnumTeleportType type) {
        Objects.requireNonNull(type, "type");
        if (type == EnumTeleportType.DEATH || type == EnumTeleportType.OTHER) return CostParameters.free();
        try { return Objects.requireNonNull(live.parameters(type), "Current cost parameters"); }
        catch (RuntimeException failure) { configurationFailure(failure); return null; }
    }

    private void configurationFailure(RuntimeException failure) {
        if (!reportedConfigurationFailure) { reportedConfigurationFailure = true; report(failure); }
    }

    @Override public void close() {
        checkOwner();
        if (closed) return;
        closed = true;
        for (Runnable remove : unregister) remove.run();
        unregister.clear();
        invalidate();
        Request request = pending;
        pending = null;
        if (request != null) { request.release(); request.result.complete(false); }
    }

    private void invalidate() {
        Generation previous = active;
        active = null;
        if (previous != null) { previous.retired = true; previous.clearIfUnused(); }
    }

    private boolean current(Request request) { return !closed && pending == request && !request.result.isDone(); }

    private void reject(Request request, Throwable error) { dispatch(() -> fail(request, error), request); }

    private void fail(Request request, Throwable error) {
        checkOwner();
        if (!current(request)) return;
        pending = null;
        request.release();
        report(error);
        request.result.complete(false);
    }

    private void dispatch(Runnable action, Request request) {
        try { owner.execute(action); }
        catch (RuntimeException rejected) {
            // A stopped owner cannot accept cleanup; completion still must not hang the caller.
            if (request != null) { request.release(); request.result.complete(false); }
        }
    }

    private void report(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
        try { diagnostics.accept(error); } catch (RuntimeException ignored) { }
    }

    private void checkOwner() {
        if (Thread.currentThread() != ownerThread) throw new IllegalStateException("Cost runtime must run on its server owner");
    }

    private static CostCalculation unavailable() { return CostCalculation.failed(EnumCostFailure.CONFIGURATION_UNAVAILABLE); }

    private static boolean same(CostConfiguration left, CostConfiguration right) {
        return left.groups().equals(right.groups()) && left.cards().equals(right.cards())
                && left.maxDistance() == right.maxDistance() && left.crossDimensionDistance() == right.crossDimensionDistance();
    }

    /** Cold startup validation before Forge corrects the old TOML; never executes constructors. */
    public static void preflight(CostConfiguration configuration, Path sourceRoot, Map<String, String> migratedSources) throws IOException {
        ThreadPoolExecutor preflight = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1), task -> new Thread(task, "narcissus-cost-preflight"));
        ScriptSession<ScriptFactory<CostFormula>> session = BaniraScripts.openFactorySession("narcissus_farewell",
                CostFormula.class, "1", new ScriptLimits(SOURCE_BYTES, BATCH_BYTES, SOURCE_COUNT), preflight);
        try {
            Sources sources = Sources.load(sourceRoot.toAbsolutePath().normalize(), configuration, migratedSources);
            if (sources.group != null) session.prepareGroups(Collections.singletonList(sources.group)).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new IOException("Cost preflight interrupted", interrupted);
        } catch (ExecutionException | TimeoutException | RuntimeException failure) {
            throw new IOException("Cost preflight failed", failure);
        } finally {
            session.close(); preflight.shutdownNow();
            try { preflight.awaitTermination(5, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }
    }

    private static final class Request {
        final CostConfiguration configuration;
        final CompletableFuture<Boolean> result = new CompletableFuture<>();
        volatile Sources sources;
        private volatile ScriptSession<ScriptFactory<CostFormula>> session;
        Request(CostConfiguration configuration) { this.configuration = configuration; }
        synchronized void closeSession() { if (session != null) { session.close(); session = null; } }
        void release() { sources = null; closeSession(); }
    }

    private final class Generation {
        final long id;
        CostConfiguration configuration;
        CostFormula formula;
        int leases;
        boolean retired;
        boolean reportedFailure;
        Generation(long id, CostConfiguration configuration, CostFormula compiled) {
            this.id = id; this.configuration = configuration;
            if (compiled != null) formula = context -> {
                try { return compiled.calculate(context); }
                catch (RuntimeException | LinkageError failure) {
                    if (!reportedFailure) { reportedFailure = true; report(failure); }
                    throw new IllegalStateException("Cost formula failed", failure);
                }
            };
        }
        void release() { leases--; clearIfUnused(); }
        void clearIfUnused() { if (retired && leases == 0) { configuration = null; formula = null; } }
    }

    private static final class Sources {
        ScriptSourceGroup group;
        final Map<String, String> fingerprints;
        final Set<String> helpers;
        Sources(ScriptSourceGroup group, Map<String, String> fingerprints, Set<String> helpers) {
            this.group = group; this.fingerprints = fingerprints; this.helpers = helpers;
        }

        static Sources load(Path root, CostConfiguration configuration, Map<String, String> overlays) throws IOException {
            Map<String, Integer> entries = new LinkedHashMap<>();
            for (CostParameters parameters : configuration.groups().values()) {
                if (parameters.type() != EnumCostType.NONE && !parameters.customFile().isEmpty())
                    entries.computeIfAbsent(parameters.customFile(), name -> entries.size());
            }
            if (entries.isEmpty()) return new Sources(null, Collections.emptyMap(), Collections.emptySet());
            Set<String> helpers = helpers(root);
            Map<String, String> files = new LinkedHashMap<>(), fingerprints = new LinkedHashMap<>();
            Set<String> names = new TreeSet<>(helpers); names.addAll(entries.keySet());
            int bytes = 0;
            for (String name : names) {
                if (name.equals(BUNDLE + ".java")) throw new IOException("Reserved cost filename");
                className(name);
                byte[] code = overlays.containsKey(name) ? overlays.get(name).getBytes(StandardCharsets.UTF_8) : read(local(root, name));
                if (code.length > SOURCE_BYTES || (bytes += code.length) > BATCH_BYTES) throw new IOException("Cost sources exceed byte limits");
                files.put(name, StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(code)).toString());
                fingerprints.put(name, hash(code));
            }
            files.put(BUNDLE + ".java", bundle(configuration, entries));
            if (files.size() > SOURCE_COUNT || bytes + files.get(BUNDLE + ".java").getBytes(StandardCharsets.UTF_8).length > BATCH_BYTES)
                throw new IOException("Cost source count or bytes exceed limit");
            return new Sources(new ScriptSourceGroup("cost", NAMESPACE + BUNDLE, files), fingerprints, helpers);
        }

        void verify(Path root) throws IOException {
            if (!helpers.equals(helpers(root))) throw new IOException("Cost helper file selection changed during preparation");
            for (Map.Entry<String, String> file : fingerprints.entrySet()) {
                if (!file.getValue().equals(hash(read(local(root, file.getKey())))))
                    throw new IOException("Cost source changed during preparation: " + file.getKey());
            }
        }

        private static String bundle(CostConfiguration configuration, Map<String, Integer> entries) throws IOException {
            StringBuilder java = new StringBuilder("package xin.vanilla.banira.generated.cost; public final class ").append(BUNDLE)
                    .append(" implements xin.vanilla.narcissus.api.cost.CostFormula {");
            for (Map.Entry<String, Integer> entry : entries.entrySet()) {
                java.append("private final xin.vanilla.narcissus.api.cost.CostFormula formula").append(entry.getValue())
                        .append(" = new ").append(className(entry.getKey())).append("();");
            }
            java.append("public double calculate(xin.vanilla.narcissus.api.cost.CostContext ctx) { switch(ctx.teleportType()) {");
            for (Map.Entry<EnumTeleportType, CostParameters> entry : configuration.groups().entrySet()) {
                if (entry.getValue().type() != EnumCostType.NONE && !entry.getValue().customFile().isEmpty()) {
                    java.append("case ").append(entry.getKey().name()).append(": return formula")
                            .append(entries.get(entry.getValue().customFile())).append(".calculate(ctx);");
                }
            }
            return java.append("default: throw new IllegalArgumentException(\"No custom cost formula for this type\"); } } }").toString();
        }

        private static String className(String file) throws IOException {
            if (!file.endsWith(".java")) throw new IOException("Expected Java cost source");
            String suffix = file.substring(0, file.length() - 5).replace('/', '.');
            if (!suffix.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) throw new IOException("Invalid cost Java class path: " + file);
            return NAMESPACE + suffix;
        }

        private static Set<String> helpers(Path root) throws IOException {
            Path directory = local(root, "helpers");
            Set<String> files = new TreeSet<>();
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return files;
            Files.walkFileTree(directory, Collections.emptySet(), 16, new SimpleFileVisitor<Path>() {
                int visited;
                private void inspect(Path path) throws IOException {
                    if (++visited > 512) throw new IOException("Cost helper tree exceeds limit");
                    checkAncestors(path);
                }
                @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
                    inspect(dir); return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    inspect(file);
                    if (attributes.isDirectory()) throw new IOException("Cost helper tree exceeds depth limit");
                    if (file.getFileName().toString().endsWith(".java")) {
                        files.add(root.relativize(file).toString().replace('\\', '/'));
                        if (files.size() >= SOURCE_COUNT) throw new IOException("Too many cost helper sources");
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return files;
        }

        private static Path local(Path root, String name) throws IOException {
            if (name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.contains(":")) throw new IOException("Invalid cost source path");
            for (String part : name.split("/", -1)) if (part.isEmpty() || part.equals(".") || part.equals("..")) throw new IOException("Invalid cost source path");
            Path path = root.resolve(name).normalize();
            if (!path.startsWith(root)) throw new IOException("Cost source leaves its root");
            checkAncestors(path); return path;
        }

        private static void checkAncestors(Path path) throws IOException {
            Path current = path.getRoot();
            for (Path part : path) {
                current = current.resolve(part);
                if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) continue;
                BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (attributes.isSymbolicLink() || attributes.isOther() || !current.toRealPath().equals(current.toAbsolutePath().normalize()))
                    throw new IOException("Linked cost paths are not allowed: " + current);
            }
        }

        private static byte[] read(Path file) throws IOException {
            checkAncestors(file);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing cost source: " + file);
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                if (channel.size() > SOURCE_BYTES) throw new IOException("Cost source exceeds byte limit");
                ByteArrayOutputStream result = new ByteArrayOutputStream(); ByteBuffer buffer = ByteBuffer.allocate(8192);
                while (channel.read(buffer) != -1) {
                    buffer.flip();
                    if (result.size() + buffer.remaining() > SOURCE_BYTES) throw new IOException("Cost source grew beyond limit");
                    result.write(buffer.array(), 0, buffer.remaining()); buffer.clear();
                }
                return result.toByteArray();
            }
        }

        private static String hash(byte[] bytes) {
            try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        }
    }
}
