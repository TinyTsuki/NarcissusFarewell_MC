package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.SafeBlockChecker;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Real mod-created players; preparation and asynchronous completion are measured separately. */
final class NarcissusMeasuredTeleports {
    static final int PLAYER_COUNT = 4;
    static final int HOMES = 48;
    private static final int Y = 200;
    private static final int VIEW_RANGE = 512;
    private static final int RANDOM_RANGE = 16;
    private static final String FAKE_CLASS = "dev.dubhe.curtain.features.player.patches.EntityPlayerMPFake";
    private static final String CURTAIN_COORDINATE = "maven.modrinth:curtain:twFTNdtl";
    private static boolean providerVerified;
    private final MinecraftServer server;
    private final List<ServerPlayer> actors = new ArrayList<>();
    private final List<UUID> pets = new ArrayList<>();
    private final List<net.minecraft.nbt.ListTag> observedRecords = new ArrayList<>();
    private final Map<ServerLevel, List<ChunkPos>> ownedTickets = new LinkedHashMap<>();
    private final Map<String, NarcissusNetworkSmokeTimings> timings = new LinkedHashMap<>();
    private final NarcissusTeleportSmokeWorkload plan = new NarcissusTeleportSmokeWorkload();
    private final long[] submittedAt = new long[PLAYER_COUNT];
    private final SafeWorldCoordinate[] targets = new SafeWorldCoordinate[PLAYER_COUNT];
    private final int[] recordsBefore = new int[PLAYER_COUNT];
    private final boolean[] observed = new boolean[PLAYER_COUNT];
    private final ServerPlayer realPlayer;
    private final net.minecraft.nbt.CompoundTag realRecordSnapshot;
    private String operation;
    private int completedTeleports;
    private long nextDispatchAt;

    NarcissusMeasuredTeleports(ServerPlayer realPlayer) {
        this.realPlayer = realPlayer;
        this.server = realPlayer.getServer();
        long started = System.nanoTime();
        CommonConfig.get().base().teleportTogether().tpWithFollower(true).tpWithFollowerRange(32);
        CommonConfig.get().base().teleportLimit().teleportAcrossDimension(true);
        for (ResourceKey<Level> key : Arrays.asList(Level.OVERWORLD, Level.NETHER)) {
            ServerLevel world = server.getLevel(key);
            require(world != null, "Missing fixture dimension " + key);
            world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, server);
            List<ChunkPos> owned = new ArrayList<>();
            ownedTickets.put(world, owned);
            for (int cx = -2; cx <= 26; cx++) for (int cz = -2; cz <= 4; cz++) {
                if (cx > 4 && (cz < 0 || cz > 2)) continue;
                if (!world.getForcedChunks().contains(ChunkPos.asLong(cx, cz))) {
                    world.setChunkForced(cx, cz, true);
                    owned.add(new ChunkPos(cx, cz));
                }
                world.getChunk(cx, cz);
            }
            for (int x = -32; x <= 410; x++) {
                int lo = x <= 64 ? -32 : 0;
                int hi = x <= 64 ? 64 : 36;
                for (int z = lo; z <= hi; z++) {
                    world.setBlock(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState(), 2);
                    for (int y = Y; y <= Y + 3; y++) {
                        world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        realPlayer.setGameMode(GameType.CREATIVE);
        realPlayer.setNoGravity(true);
        NarcissusUtils.teleportTo(realPlayer,
                new SafeWorldCoordinate(8.5, Y, 8.5, Level.OVERWORLD), EnumTeleportType.OTHER);
        List<TeleportRecord> realRecords = PlayerTeleportData.getData(realPlayer).getTeleportRecords();
        require(realRecords.size() == 1, "Fixture teleport did not produce exactly one real player record");
        realRecordSnapshot = realRecords.get(0).writeToNBT();
        Map<KeyValue<String, String>, SafeWorldCoordinate> homes = new LinkedHashMap<>();
        for (int i = 0; i < HOMES; i++) homes.put(new KeyValue<>("minecraft:overworld", "Smoke home " + i),
                new SafeWorldCoordinate(i + .5, Y, 8.5, Level.OVERWORLD));
        PlayerTeleportData.getData(realPlayer).setHomeCoordinate(homes);
        PlayerTeleportData.syncPlayerData(realPlayer);
        spawnActors();
        for (String name : new String[]{"random-submit", "random-complete", "view-search", "view-submit",
                "view-complete", "cross-submit", "cross-complete"}) {
            timings.put(name, new NarcissusNetworkSmokeTimings(PLAYER_COUNT * NarcissusTeleportSmokeWorkload.REQUIRED_CYCLES));
        }
        NarcissusNetworkSmokeStatus.append("PASS measured-fixture preparation-ns=" + (System.nanoTime() - started)
                + " players=" + PLAYER_COUNT + " homes=" + HOMES + " view-steps=" + VIEW_RANGE + " random-range=" + RANDOM_RANGE);
    }

    private void spawnActors() {
        try {
            Class<?> type = Class.forName(FAKE_CLASS);
            Method spawn = type.getMethod("createFakePlayer", String.class, MinecraftServer.class,
                    double.class, double.class, double.class, double.class, double.class,
                    ResourceKey.class, GameType.class, boolean.class);
            for (int i = 0; i < PLAYER_COUNT; i++) {
                String name = "NarcissusSmoke" + i;
                require(server.getPlayerList().getPlayerByName(name) == null, "Fixture name already in use");
                ServerPlayer actor = createActor(spawn, name, i);
                require(actor != null && type.isInstance(actor)
                        && server.getPlayerList().getPlayer(actor.getUUID()) == actor, "Curtain player did not join");
                actor.setNoGravity(true);
                actors.add(actor);
                observedRecords.add(new net.minecraft.nbt.ListTag());
                require(PlayerTeleportData.getData(actor).getTeleportRecords().isEmpty(), "Nonempty isolated fake player data");
                Wolf pet = EntityType.WOLF.create(actor.getLevel());
                require(pet != null, "Could not create pet");
                pet.setOwnerUUID(actor.getUUID());
                pet.setNoAi(true);
                pet.setNoGravity(true);
                pet.moveTo(actor.getX() + 1, Y, actor.getZ(), 0, 0);
                require(actor.getLevel().addFreshEntity(pet), "Could not add pet");
                pets.add(pet.getUUID());
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Curtain could not create smoke players", error);
        }
        NarcissusNetworkSmokeStatus.append("PASS curtain-fake-players-ready count=" + actors.size() + " class=" + FAKE_CLASS);
    }

    private ServerPlayer createActor(Method spawn, String name, int index) throws ReflectiveOperationException {
        Field offline = Class.forName("dev.dubhe.curtain.CurtainRules", true,
                spawn.getDeclaringClass().getClassLoader()).getField("allowSpawningOfflinePlayers");
        boolean previous = offline.getBoolean(null);
        try {
            offline.setBoolean(null, true);
            return (ServerPlayer) spawn.invoke(null, name, server, 16.5, (double) Y, 4.5 + index * 8,
                    -90.0, 0.0, Level.OVERWORLD, GameType.CREATIVE, true);
        } finally {
            offline.setBoolean(null, previous);
        }
    }

    static void verifyProvider() {
        if (!NarcissusNetworkSmokeStatus.enabled()) throw new IllegalStateException("Smoke disabled");
        if (providerVerified) return;
        try {
            require(CURTAIN_COORDINATE.equals(System.getProperty("narcissus.networkSmoke.curtainCoordinate")),
                    "Unexpected Curtain coordinate");
            Class<?> type = Class.forName(FAKE_CLASS);
            require(ServerPlayer.class.isAssignableFrom(type), "Curtain actor is not a real ServerPlayer");
            net.minecraftforge.fml.ModContainer container = ModList.get().getModContainerById("curtain")
                    .orElseThrow(() -> new IllegalStateException("Curtain mod container unavailable"));
            require("1.2.2".equals(container.getModInfo().getVersion().toString()), "Unexpected Curtain version");
            Object mod = container.getMod();
            String source = type.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
            require(mod != null && type.getClassLoader() == mod.getClass().getClassLoader()
                    && source.equals(mod.getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm()),
                    "Curtain actor does not belong to the loaded Curtain mod");
            Path path = ModList.get().getModFileById("curtain").getFile().getFilePath().toAbsolutePath();
            require(Files.isRegularFile(path), "Curtain mod file unavailable");
            byte[] bytes = Files.readAllBytes(path);
            StringBuilder hash = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) hash.append(String.format("%02x", b & 255));
            String expectedHash = System.getProperty("narcissus.networkSmoke.curtainSha256", "");
            require(expectedHash.equals(hash.toString()), "Loaded Curtain JAR differs from parent runtime fingerprint");
            NarcissusNetworkSmokeStatus.append("RUNTIME " + FAKE_CLASS + " provider=curtain coordinate=" + CURTAIN_COORDINATE
                    + " version=1.2.2 codeSource=" + source + " loader-match=curtain-entrypoint sha256=" + hash
                    + " size=" + bytes.length + " path=" + path);
            providerVerified = true;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot fingerprint loaded Curtain", error);
        }
    }

    void tick(BooleanSupplier active) {
        if (complete()) return;
        require(active.getAsBoolean(), "Sampling ended before teleport workload completed");
        if (operation != null) {
            boolean all = true;
            for (int i = 0; i < PLAYER_COUNT; i++) {
                if (observed[i]) continue;
                ServerPlayer actor = actors.get(i);
                require(System.nanoTime() - submittedAt[i] < 10_000_000_000L, "Teleport timed out " + operation + " actor=" + i);
                List<TeleportRecord> records = PlayerTeleportData.getData(actor).getTeleportRecords();
                if (records.size() == recordsBefore[i]) { all = false; continue; }
                require(records.size() == recordsBefore[i] + 1, "Unexpected teleport record count");
                SafeWorldCoordinate target = targets[i];
                require(actor.level.dimension().equals(target.dimension())
                        && actor.distanceToSqr(target.x(), target.y(), target.z()) < 9.0,
                        "Wrong landing for " + operation + " actor=" + i);
                require(new SafeBlockChecker(actor.level).isSafeBlock(actor.blockPosition(), false), "Unsafe landing");
                Entity pet = actor.getLevel().getEntity(pets.get(i));
                require(pet instanceof Wolf && actor.getUUID().equals(((Wolf) pet).getOwnerUUID())
                        && pet.distanceToSqr(actor) < 16, "Pet did not follow " + operation + " actor=" + i);
                if ("cross".equals(operation)) {
                    ServerLevel previous = server.getLevel(actor.level.dimension().equals(Level.OVERWORLD) ? Level.NETHER : Level.OVERWORLD);
                    require(previous.getEntity(pets.get(i)) == null, "Pet duplicated across dimensions");
                }
                require(active.getAsBoolean(), "Completion fell outside the sample window");
                timings.get(operation + "-complete").record(System.nanoTime() - submittedAt[i]);
                observedRecords.get(i).add(records.get(records.size() - 1).writeToNBT());
                observed[i] = true;
                completedTeleports++;
            }
            if (all) {
                operation = null;
                plan.completeCurrent();
                nextDispatchAt = System.nanoTime() + 700_000_000L;
            }
            return;
        }
        if (System.nanoTime() < nextDispatchAt) return;
        operation = plan.current() == NarcissusTeleportSmokeWorkload.Step.SAFE_RANDOM ? "random"
                : plan.current() == NarcissusTeleportSmokeWorkload.Step.VIEW_END ? "view" : "cross";
        for (int i = 0; i < PLAYER_COUNT; i++) {
            ServerPlayer actor = actors.get(i);
            SafeWorldCoordinate target;
            EnumTeleportType type;
            if ("cross".equals(operation)) {
                target = new SafeWorldCoordinate(16.5, Y, 4.5 + i * 8,
                        actor.level.dimension().equals(Level.OVERWORLD) ? Level.NETHER : Level.OVERWORLD);
                type = EnumTeleportType.OTHER;
            } else {
                // Reposition only the fixture actors, not the real client that is rendering the UI.
                actor.teleportTo(actor.getLevel(), 16.5, Y, 4.5 + i * 8, -90, 0);
                Entity pet = actor.getLevel().getEntity(pets.get(i));
                require(pet != null, "Pet absent before dispatch");
                pet.moveTo(actor.getX() + 1, Y, actor.getZ(), 0, 0);
                if ("random".equals(operation)) {
                    target = SafeWorldCoordinate.random(actor, RANDOM_RANGE);
                    target.y(Y + 8);
                    target.safe(true);
                    type = EnumTeleportType.TP_RANDOM;
                } else {
                    long start = System.nanoTime();
                    target = NarcissusUtils.findViewEndCandidate(actor, true, VIEW_RANGE);
                    require(active.getAsBoolean(), "View search exceeded sampling window");
                    timings.get("view-search").record(System.nanoTime() - start);
                    require(target != null && actor.distanceToSqr(target.x(), target.y(), target.z()) >= 350 * 350,
                            "View search did not traverse a long distance");
                    target.safe(true);
                    type = EnumTeleportType.TP_VIEW;
                }
            }
            targets[i] = target.clone();
            if ("random".equals(operation)) targets[i].y(Y);
            recordsBefore[i] = PlayerTeleportData.getData(actor).getTeleportRecords().size();
            observed[i] = false;
            require(active.getAsBoolean(), "Dispatch fell outside sampling window");
            submittedAt[i] = System.nanoTime();
            NarcissusUtils.teleportTo(actor, target, type, RANDOM_RANGE);
            require(active.getAsBoolean(), "Dispatch exceeded sampling window");
            timings.get(operation + "-submit").record(System.nanoTime() - submittedAt[i]);
        }
    }

    boolean complete() { return plan.current() == NarcissusTeleportSmokeWorkload.Step.COMPLETE; }

    void finish() {
        require(complete() && completedTeleports == PLAYER_COUNT * 3 * NarcissusTeleportSmokeWorkload.REQUIRED_CYCLES,
                "Incomplete sustained workload");
        for (Map.Entry<String, NarcissusNetworkSmokeTimings> entry : timings.entrySet()) {
            require(entry.getValue().count() == PLAYER_COUNT * NarcissusTeleportSmokeWorkload.REQUIRED_CYCLES,
                    "Incomplete timings " + entry.getKey());
            NarcissusNetworkSmokeStatus.append("PASS operation " + entry.getKey() + " " + entry.getValue().summary());
        }
        Properties checkpoint = new Properties();
        checkpoint.setProperty("cycles", Integer.toString(plan.cycles()));
        checkpoint.setProperty("teleports", Integer.toString(completedTeleports));
        checkpoint.setProperty("realPlayer", realPlayer.getUUID().toString());
        checkpoint.setProperty("realRecord", realRecordSnapshot.toString());
        checkpoint.setProperty("homes", Integer.toString(HOMES));
        for (int i = 0; i < actors.size(); i++) {
            ServerPlayer actor = actors.get(i);
            require(PlayerTeleportData.getData(actor).getTeleportRecords().size() == 60, "Final actor record mismatch");
            PlayerTeleportData.getData(actor).save();
            checkpoint.setProperty("actor." + i, actor.getUUID().toString());
            checkpoint.setProperty("pet." + i, pets.get(i).toString());
            checkpoint.setProperty("records." + i, observedRecords.get(i).toString());
        }
        try {
            Path path = checkpointPath();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) { checkpoint.store(writer, "final measured cycle"); }
        } catch (java.io.IOException error) { throw new IllegalStateException("Could not write checkpoint", error); }
        NarcissusNetworkSmokeStatus.append("PASS safe-random-teleport count=80");
        NarcissusNetworkSmokeStatus.append("PASS view-end-teleport count=80 min-distance=350");
        NarcissusNetworkSmokeStatus.append("PASS cross-dimension-follower-teleport count=80");
        NarcissusNetworkSmokeStatus.append("PASS sustained-teleport-workload cycles=" + plan.cycles() + " teleports=" + completedTeleports);
        NarcissusNetworkSmokeStatus.append("PASS final-checkpoint cycles=" + plan.cycles() + " homes=" + HOMES + " actors=" + PLAYER_COUNT);
        for (ServerPlayer actor : actors) {
            int result = server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                    "player " + actor.getGameProfile().getName() + " kill");
            require(result > 0, "Could not disconnect Curtain actor");
        }
        ownedTickets.forEach((world, tickets) -> tickets.forEach(pos -> world.setChunkForced(pos.x, pos.z, false)));
    }

    boolean actorsRemoved() {
        return actors.stream().noneMatch(actor -> server.getPlayerList().getPlayer(actor.getUUID()) != null);
    }

    static void verifyRestart(ServerPlayer player) {
        try {
            Properties p = new Properties();
            try (Reader reader = Files.newBufferedReader(checkpointPath(), StandardCharsets.UTF_8)) { p.load(reader); }
            require("20".equals(p.getProperty("cycles")) && "240".equals(p.getProperty("teleports"))
                    && "48".equals(p.getProperty("homes")) && player.getUUID().toString().equals(p.getProperty("realPlayer")),
                    "Checkpoint did not describe the final workload");
            Map<KeyValue<String, String>, SafeWorldCoordinate> homes = PlayerTeleportData.getData(player).getHomeCoordinate();
            require(homes.size() == HOMES, "Homes did not persist");
            for (int i = 0; i < HOMES; i++) {
                SafeWorldCoordinate home = homes.get(new KeyValue<>("minecraft:overworld", "Smoke home " + i));
                require(home != null && home.dimension().equals(Level.OVERWORLD)
                        && home.x() == i + .5 && home.y() == Y && home.z() == 8.5, "Home content did not persist " + i);
            }
            require(PlayerTeleportData.getData(player).getTeleportRecords().size() == 1, "Real player record did not persist");
            require(net.minecraft.nbt.TagParser.parseTag(p.getProperty("realRecord")).equals(
                    PlayerTeleportData.getData(player).getTeleportRecords().get(0).writeToNBT()),
                    "Real player record content changed after restart");
            for (int i = 0; i < PLAYER_COUNT; i++) {
                net.minecraft.nbt.CompoundTag data = xin.vanilla.banira.api.BaniraPlayerData.getOrCreate(
                        UUID.fromString(p.getProperty("actor." + i)), "narcissus_farewell", net.minecraft.nbt.CompoundTag.class);
                net.minecraft.nbt.ListTag records = data.getList("teleportRecords", 10);
                require(records.size() == 60, "Fake player records did not persist actor=" + i);
                net.minecraft.nbt.ListTag expected = net.minecraft.nbt.TagParser.parseTag(
                        "{records:" + p.getProperty("records." + i) + "}").getList("records", 10);
                require(expected.size() == 60 && expected.equals(records), "Full ordered teleport records changed actor=" + i);
                int random = 0, view = 0, cross = 0;
                for (int r = 0; r < records.size(); r++) {
                    TeleportRecord record = TeleportRecord.readFromNBT(records.getCompound(r));
                    SafeWorldCoordinate after = record.getAfter();
                    require(Math.abs(after.y() - Y) < .3, "Persisted landing height mismatch");
                    if (record.getTeleportType() == EnumTeleportType.TP_RANDOM) random++;
                    else if (record.getTeleportType() == EnumTeleportType.TP_VIEW) {
                        view++;
                        require(after.x() >= 366, "Persisted view destination was not long distance");
                    } else if (record.getTeleportType() == EnumTeleportType.OTHER) {
                        cross++;
                        require(!record.getBefore().dimension().equals(after.dimension())
                                && after.x() == 16.5 && after.z() == 4.5 + i * 8, "Persisted cross-dimension destination mismatch");
                    } else throw new IllegalStateException("Unexpected persisted teleport type");
                }
                require(random == 20 && view == 20 && cross == 20, "Persisted operation counts mismatch");
            }
            NarcissusNetworkSmokeStatus.append("PASS persisted-final-checkpoint cycles=20 teleports=240 homes=48 records-per-actor=60");
        } catch (java.io.IOException | com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw new IllegalStateException("Cannot read checkpoint", error);
        }
    }

    private static Path checkpointPath() {
        String path = System.getProperty("narcissus.networkSmoke.checkpoint", "").trim();
        require(!path.isEmpty(), "Missing independent checkpoint path");
        return Paths.get(path).toAbsolutePath();
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
