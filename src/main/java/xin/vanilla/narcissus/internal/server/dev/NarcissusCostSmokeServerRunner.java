package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.api.BaniraDataPaths;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.dev.NarcissusCostSmokeState;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static xin.vanilla.narcissus.internal.dev.NarcissusCostSmokeState.require;

/**
 * Narrow opt-in integration proof using the production dispatcher, runtime and native actors.
 */
public final class NarcissusCostSmokeServerRunner {
    private static NarcissusCostSmokeServerRunner instance;
    private final NarcissusCostSmokeState state = NarcissusCostSmokeState.from(System.getProperties(), "server", System.nanoTime());
    private int step, ticks, records;
    private boolean ready, stopping, failed;
    private boolean cancelExperience, experienceEventObserved;
    private CompletableFuture<Boolean> reload;
    private ServerPlayerEntity player, target;
    private String requestId;

    private NarcissusCostSmokeServerRunner() {
    }

    public static void register() {
        if (!NarcissusCostSmokeState.enabled()) return;
        instance = new NarcissusCostSmokeServerRunner();
        MinecraftForge.EVENT_BUS.addListener(instance::tick);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerXpEvent.XpChange event) -> {
            if (instance.cancelExperience && event.getPlayer() == instance.player && event.getAmount() < 0) {
                instance.experienceEventObserved = true;
                event.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.fml.event.server.FMLServerStoppedEvent event) -> {
            if (instance.stopping && !instance.failed) instance.state.append("PASS server-stopped");
        });
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failed) return;
        MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
        if (server == null || !server.isRunning()) return;
        try {
            state.checkDeadline(System.nanoTime());
            if (!ready) {
                ready = true;
                state.append("PASS server-ready");
            }
            if (stopping) {
                if (++ticks > 20) server.halt(false);
                return;
            }
            if (step == 20) {
                if (state.peerDisconnected(server.getPlayerList().getPlayerByName("CostSmoke") != null)) {
                    state.append("PASS disconnect");
                    stopping = true;
                    ticks = 0;
                }
                return;
            }
            player = server.getPlayerList().getPlayerByName("CostSmoke");
            if (player == null) return;
            if (step == 0) {
                server.getPlayerList().op(player.getGameProfile());
                if (state.phase().equals("restart")) {
                    require(CommonConfig.get().cost().home().fixedAmount() == 7, "Reloaded fee was not persisted");
                    require(PlayerTeleportData.getData(player).peekHomeCoordinates().containsKey(homeKey()), "Home was not persisted");
                    state.append("PASS restart");
                    step = 20;
                    return;
                }
                require(CommonConfig.get().cost().home().fixedAmount() == 3, "Legacy fixed fee migration mismatch");
                Path root = BaniraDataPaths.configPath().resolve(NarcissusFarewell.MODID);
                require(Files.isRegularFile(root.resolve("cost/migration.json")), "Missing migration journal");
                state.append("PASS migration");
                setup(server);
                reload = NarcissusCostService.get().reload();
                step = 1;
                return;
            }
            if (step == 1 && loaded()) {
                state.append("READY quote-home");
                step = 2;
                return;
            }
            if (step == 2 && state.peerHas("PASS quote-home")) {
                require(player.totalExperience == 100, "Quote charged experience");
                records = records();
                command(player, "home CostHome false");
                step = 3;
                return;
            }
            if (step == 3 && records() == records + 1) {
                require(player.totalExperience == 97 && Math.abs(player.getX() - 8.5) < .01, "Home did not charge exactly3 at the destination");
                state.append("PASS home-payment");
                cancelExperience = true;
                records = records();
                command(player, "home CostHome false");
                ticks = 0;
                step = 17;
                return;
            }
            if (step == 17 && ++ticks >= 10) {
                require(experienceEventObserved && records() == records && player.totalExperience == 97,
                        "Cancelled Forge XP event charged experience or teleported");
                cancelExperience = false;
                state.append("PASS forge-xp-cancellation");
                PlayerTeleportData.getData(player).setTeleportCard(0);
                CommonConfig.get().cost().cards().enabled(true).mode(EnumCardType.REQUIRE_ONE_WITH_COST);
                reload = NarcissusCostService.get().reload();
                step = 4;
                return;
            }
            if (step == 4 && loaded()) {
                records = records();
                command(player, "home CostHome false");
                ticks = 0;
                step = 5;
                return;
            }
            if (step == 5 && ++ticks >= 10) {
                require(records() == records && player.totalExperience == 97 && PlayerTeleportData.getData(player).peekTeleportCard() == 0,
                        "Card shortage mutated resources or teleported");
                state.append("PASS card-shortage");
                CommonConfig.get().cost().cards().enabled(false);
                CommonConfig.get().cost().home().type(EnumCostType.COMMAND).command("scoreboard players add @s CostFee {amount}");
                command(player, "scoreboard objectives add CostFee dummy");
                reload = NarcissusCostService.get().reload();
                step = 6;
                return;
            }
            if (step == 6 && loaded()) {
                records = records();
                command(player, "home CostHome false");
                step = 7;
                return;
            }
            if (step == 7 && records() == records + 1) {
                int score = server.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), server.getScoreboard().getObjective("CostFee")).getScore();
                require(score == 3, "Cost command executed more than once or not at all");
                state.append("PASS command-once");
                CommonConfig.get().cost().here().type(EnumCostType.EXP_POINT).fixedAmount(4).perBlockAmount(0);
                reload = NarcissusCostService.get().reload();
                step = 8;
                return;
            }
            if (step == 8 && loaded()) {
                target = spawnTarget(server);
                PlayerTeleportData.getData(target).setTeleportCountdownSeconds(EnumTeleportType.TP_HERE, 0);
                target.giveExperiencePoints(50);
                command(player, NarcissusUtils.getCommandPrefix() + " " + CommonConfig.get().command().tpHere().commandTpHere() + " " + target.getGameProfile().getName());
                TeleportRequest request = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(r -> r.getRequester() == player && r.getTarget() == target).findFirst().orElseThrow(() -> new IllegalStateException("HERE request missing"));
                requestId = request.getRequestId();
                command(target, NarcissusUtils.getCommandPrefix() + " " + CommonConfig.get().command().tpHere().commandTpHereYes() + " " + requestId);
                step = 9;
                return;
            }
            if (step == 9 && target.getLevel().dimension().equals(World.OVERWORLD)) {
                require(player.totalExperience == 93 && target.totalExperience == 50, "HERE charged the moving target");
                require(!NarcissusFarewell.getTeleportRequest().containsKey(requestId), "Completed HERE request retained");
                state.append("PASS cross-here");
                writeFormula(false);
                CommonConfig.get().cost().home().type(EnumCostType.EXP_POINT).custom().file("SmokeFee.java");
                reload = NarcissusCostService.get().reload();
                step = 10;
                return;
            }
            if (step == 10 && loaded()) {
                state.append("READY quote-custom");
                step = 11;
                return;
            }
            if (step == 11 && state.peerHas("PASS quote-custom")) {
                records = records();
                command(player, "home CostHome false");
                step = 12;
                return;
            }
            if (step == 12 && records() == records + 1) {
                require(player.totalExperience == 86, "Warm Java fee/native context mismatch");
                state.append("PASS custom-payment");
                writeFormula(true);
                reload = NarcissusCostService.get().reload();
                step = 13;
                return;
            }
            if (step == 13 && reload.isDone()) {
                require(!reload.join(), "Invalid Java reload accepted");
                records = records();
                command(player, "home CostHome false");
                ticks = 0;
                step = 14;
                return;
            }
            if (step == 14 && ++ticks >= 10) {
                require(player.totalExperience == 86 && records() == records, "Failed reload continued charging/teleporting");
                state.append("PASS reload-blocked");
                writeFormula(false);
                CommonConfig.get().cost().home().custom().file("");
                CommonConfig.get().cost().home().fixedAmount(7);
                BaniraConfigs.holder(CommonConfig.class).save();
                reload = NarcissusCostService.get().reload();
                step = 15;
                return;
            }
            if (step == 15 && loaded()) {
                state.append("READY quote-reloaded");
                step = 16;
                return;
            }
            if (step == 16 && state.peerHas("PASS quote-reloaded")) {
                PlayerTeleportData.getData(player).saveEx();
                if (target != null)
                    target.connection.disconnect(new net.minecraft.util.text.StringTextComponent("Cost smoke complete"));
                state.append("PASS reload");
                state.append("FINISHED integration");
                step = 20;
                return;
            }
        } catch (Throwable error) {
            failed = true;
            state.append("FAIL server " + error);
            server.halt(false);
            org.apache.logging.log4j.LogManager.getLogger().error("Cost smoke failed", error);
        }
    }

    private void setup(MinecraftServer server) {
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        server.getLevel(World.OVERWORLD).setBlock(new BlockPos(8, 199, 8), Blocks.STONE.defaultBlockState(), 2);
        player.teleportTo(server.getLevel(World.OVERWORLD), 4.5, 200.1, 8.5, 0, 0);
        require(player.totalExperience == 0, "Fresh fixture had experience");
        player.giveExperiencePoints(100);
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        data.getHomeCoordinate().put(homeKey(), new SafeWorldCoordinate(8.5, 200.1, 8.5, World.OVERWORLD).safe(false));
        data.setDirty();
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes())
            data.setTeleportCountdownSeconds(type, 0);
        CommonConfig.get().cooldown().cooldownTpHome(0).cooldownTpHere(0);
        CommonConfig.get().base().teleportLimit().teleportAcrossDimension(true);
        CommonConfig.get().base().teleportRequest().teleportRequestCooldown(0);
        CommonConfig.get().cost().cards().enabled(false).dailyGrant(0);
        PlayerTeleportData.syncPlayerData(player);
    }

    private boolean loaded() {
        if (!reload.isDone()) return false;
        require(reload.join(), "Valid fee reload rejected");
        return true;
    }

    private int records() {
        return PlayerTeleportData.getData(player).peekTeleportRecords().size();
    }

    private void command(ServerPlayerEntity actor, String command) {
        try {
            actor.server.getCommands().getDispatcher().execute(command, actor.createCommandSourceStack());
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw new IllegalStateException("Cost smoke command rejected: " + command, error);
        }
    }

    private static KeyValue<String, String> homeKey() {
        return new KeyValue<>("minecraft:overworld", "CostHome");
    }

    private ServerPlayerEntity spawnTarget(MinecraftServer server) throws ReflectiveOperationException {
        Class<?> type = Class.forName("net.cjsah.mod.carpet.patch.EntityPlayerMPFake");
        ServerPlayerEntity result = (ServerPlayerEntity) type.getMethod("createFake", String.class, MinecraftServer.class,
                        double.class, double.class, double.class, double.class, double.class, RegistryKey.class, GameType.class, boolean.class)
                .invoke(null, "CostTarget", server, 8.5, 200.1, 8.5, 0.0, 0.0, World.NETHER, GameType.SURVIVAL, true);
        require(result != null && server.getPlayerList().getPlayer(result.getUUID()) == result, "Carpet target did not join");
        result.setNoGravity(true);
        server.getPlayerList().op(result.getGameProfile());
        return result;
    }

    private void writeFormula(boolean broken) throws java.io.IOException {
        Path source = BaniraDataPaths.configPath().resolve(NarcissusFarewell.MODID + "/cost/sources/SmokeFee.java");
        Files.createDirectories(source.getParent());
        if (broken) {
            Files.write(source, "invalid Java".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String nativeY;
        try {
            net.minecraft.entity.Entity.class.getMethod("func_226278_cu_");
            nativeY = "func_226278_cu_";
        } catch (NoSuchMethodException mappedDevelopment) {
            nativeY = "getY";
        }
        for (java.util.Map.Entry<String, String> file : formulaSources(nativeY).entrySet()) {
            Path destination = source.getParent().resolve(file.getKey());
            Files.createDirectories(destination.getParent());
            Files.write(destination, file.getValue().getBytes(StandardCharsets.UTF_8));
        }
        state.append("PASS native-helper-source " + nativeY);
    }

    static java.util.Map<String, String> formulaSources(String nativeY) {
        require(nativeY.equals("getY") || nativeY.equals("func_226278_cu_"), "Unknown native Y accessor");
        String text = "package xin.vanilla.banira.generated.cost; public class SmokeFee implements xin.vanilla.narcissus.api.cost.CostFormula { "
                + "public double calculate(xin.vanilla.narcissus.api.cost.CostContext ctx) { "
                + "if (ctx.nativePlayer(net.minecraft.entity.player.ServerPlayerEntity.class) == null || ctx.nativeServer(net.minecraft.server.MinecraftServer.class) == null "
                + "|| !ctx.destination().isPresent() || !ctx.targetWorld().isPresent() || !ctx.payer().alive()) throw new IllegalStateException(\"native context\"); "
                + "return xin.vanilla.banira.generated.cost.helpers.SmokeNative.value(ctx); } }";
        String helper = "package xin.vanilla.banira.generated.cost.helpers; public final class SmokeNative { "
                + "public static double value(xin.vanilla.narcissus.api.cost.CostContext ctx) { "
                + "net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity) ctx.nativePlayer(net.minecraft.entity.Entity.class); "
                + "double nativeY = entity." + nativeY + "(); "
                + "if (nativeY != ctx.source().y()) throw new IllegalStateException(\"native helper mismatch\"); return 7; } }";
        java.util.Map<String, String> files = new java.util.LinkedHashMap<>();
        files.put("SmokeFee.java", text);
        files.put("helpers/SmokeNative.java", helper);
        return files;
    }
}
