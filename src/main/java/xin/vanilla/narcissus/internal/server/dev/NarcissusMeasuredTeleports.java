package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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
    private static final String FAKE_CLASS = "net.cjsah.mod.carpet.patch.EntityPlayerMPFake";
    private final MinecraftServer server;
    private final List<ServerPlayerEntity> actors = new ArrayList<>();
    private final List<UUID> pets = new ArrayList<>();
    private final List<net.minecraft.nbt.ListNBT> observedRecords = new ArrayList<>();
    private final Map<ServerWorld, List<ChunkPos>> ownedTickets = new LinkedHashMap<>();
    private final Map<String, NarcissusNetworkSmokeTimings> timings = new LinkedHashMap<>();
    private final NarcissusTeleportSmokeWorkload plan = new NarcissusTeleportSmokeWorkload();
    private final long[] submittedAt = new long[PLAYER_COUNT];
    private final SafeWorldCoordinate[] targets = new SafeWorldCoordinate[PLAYER_COUNT];
    private final int[] recordsBefore = new int[PLAYER_COUNT];
    private final boolean[] observed = new boolean[PLAYER_COUNT];
    private final ServerPlayerEntity realPlayer;
    private final net.minecraft.nbt.CompoundNBT realRecordSnapshot;
    private String operation;
    private int completedTeleports;
    private long nextDispatchAt;

    NarcissusMeasuredTeleports(ServerPlayerEntity realPlayer) {
        this.realPlayer = realPlayer;
        this.server = realPlayer.getServer();
        long started = System.nanoTime();
        CommonConfig.get().base().teleportTogether().tpWithFollower(true).tpWithFollowerRange(32);
        CommonConfig.get().base().teleportLimit().teleportAcrossDimension(true);
        for (RegistryKey<World> key : Arrays.asList(World.OVERWORLD, World.NETHER)) {
            ServerWorld world = server.getLevel(key);
            require(world != null, "Missing fixture dimension " + key);
            world.getGameRules().getRule(net.minecraft.world.GameRules.RULE_DOMOBSPAWNING).set(false, server);
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
                new SafeWorldCoordinate(8.5, Y, 8.5, World.OVERWORLD), EnumTeleportType.OTHER);
        List<TeleportRecord> realRecords = PlayerTeleportData.getData(realPlayer).getTeleportRecords();
        require(realRecords.size() == 1, "Fixture teleport did not produce exactly one real player record");
        realRecordSnapshot = realRecords.get(0).writeToNBT();
        Map<KeyValue<String, String>, SafeWorldCoordinate> homes = new LinkedHashMap<>();
        for (int i = 0; i < HOMES; i++) homes.put(new KeyValue<>("minecraft:overworld", "Smoke home " + i),
                new SafeWorldCoordinate(i + .5, Y, 8.5, World.OVERWORLD));
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
            Method spawn = type.getMethod("createFake", String.class, MinecraftServer.class,
                    double.class, double.class, double.class, double.class, double.class,
                    RegistryKey.class, GameType.class, boolean.class);
            for (int i = 0; i < PLAYER_COUNT; i++) {
                String name = "NarcissusSmoke" + i;
                require(server.getPlayerList().getPlayerByName(name) == null, "Fixture name already in use");
                ServerPlayerEntity actor = (ServerPlayerEntity) spawn.invoke(null, name, server,
                        16.5, (double) Y, 4.5 + i * 8, -90.0, 0.0, World.OVERWORLD, GameType.CREATIVE, true);
                require(actor != null && type.isInstance(actor)
                        && server.getPlayerList().getPlayer(actor.getUUID()) == actor, "Forge Carpet player did not join");
                actor.setNoGravity(true);
                actors.add(actor);
                observedRecords.add(new net.minecraft.nbt.ListNBT());
                require(PlayerTeleportData.getData(actor).getTeleportRecords().isEmpty(), "Nonempty isolated fake player data");
                WolfEntity pet = EntityType.WOLF.create(actor.getLevel());
                require(pet != null, "Could not create pet");
                pet.setOwnerUUID(actor.getUUID());
                pet.setNoAi(true);
                pet.setNoGravity(true);
                pet.moveTo(actor.getX() + 1, Y, actor.getZ(), 0, 0);
                require(actor.getLevel().addFreshEntity(pet), "Could not add pet");
                pets.add(pet.getUUID());
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Forge Carpet could not create smoke players", error);
        }
        NarcissusNetworkSmokeStatus.append("PASS carpet-fake-players-ready count=" + actors.size() + " class=" + FAKE_CLASS);
    }

    void tick(BooleanSupplier active) {
        if (complete()) return;
        require(active.getAsBoolean(), "Sampling ended before teleport workload completed");
        if (operation != null) {
            boolean all = true;
            for (int i = 0; i < PLAYER_COUNT; i++) {
                if (observed[i]) continue;
                ServerPlayerEntity actor = actors.get(i);
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
                require(pet instanceof WolfEntity && actor.getUUID().equals(((WolfEntity) pet).getOwnerUUID())
                        && pet.distanceToSqr(actor) < 16, "Pet did not follow " + operation + " actor=" + i);
                if ("cross".equals(operation)) {
                    ServerWorld previous = server.getLevel(actor.level.dimension().equals(World.OVERWORLD) ? World.NETHER : World.OVERWORLD);
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
            ServerPlayerEntity actor = actors.get(i);
            SafeWorldCoordinate target;
            EnumTeleportType type;
            if ("cross".equals(operation)) {
                target = new SafeWorldCoordinate(16.5, Y, 4.5 + i * 8,
                        actor.level.dimension().equals(World.OVERWORLD) ? World.NETHER : World.OVERWORLD);
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
            ServerPlayerEntity actor = actors.get(i);
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
        for (ServerPlayerEntity actor : actors) {
            int result = server.getCommands().performCommand(server.createCommandSourceStack().withPermission(4),
                    "player " + actor.getGameProfile().getName() + " kill");
            require(result > 0, "Could not disconnect Forge Carpet actor");
        }
        ownedTickets.forEach((world, tickets) -> tickets.forEach(pos -> world.setChunkForced(pos.x, pos.z, false)));
    }

    boolean actorsRemoved() {
        return actors.stream().noneMatch(actor -> server.getPlayerList().getPlayer(actor.getUUID()) != null);
    }

    static void verifyRestart(ServerPlayerEntity player) {
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
                require(home != null && home.dimension().equals(World.OVERWORLD)
                        && home.x() == i + .5 && home.y() == Y && home.z() == 8.5, "Home content did not persist " + i);
            }
            require(PlayerTeleportData.getData(player).getTeleportRecords().size() == 1, "Real player record did not persist");
            require(net.minecraft.nbt.JsonToNBT.parseTag(p.getProperty("realRecord")).equals(
                    PlayerTeleportData.getData(player).getTeleportRecords().get(0).writeToNBT()),
                    "Real player record content changed after restart");
            for (int i = 0; i < PLAYER_COUNT; i++) {
                net.minecraft.nbt.CompoundNBT data = xin.vanilla.banira.api.BaniraPlayerData.getOrCreate(
                        UUID.fromString(p.getProperty("actor." + i)), "narcissus_farewell", net.minecraft.nbt.CompoundNBT.class);
                net.minecraft.nbt.ListNBT records = data.getList("teleportRecords", 10);
                require(records.size() == 60, "Fake player records did not persist actor=" + i);
                net.minecraft.nbt.ListNBT expected = net.minecraft.nbt.JsonToNBT.parseTag(
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
