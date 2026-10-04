package xin.vanilla.narcissus.internal.forge.cost;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.narcissus.api.cost.CostPhase;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;
import xin.vanilla.narcissus.service.cost.CostQuoteService;
import xin.vanilla.narcissus.util.NarcissusUtils;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

public final class ForgeCostQuoteAccess implements CostQuoteService.QuoteAccess {
    private final Supplier<MinecraftServer> server;
    private final Function<EnumTeleportType, CostConfiguration> configuration;
    private final ToIntBiFunction<ServerPlayer, EnumTeleportType> cooldown;

    public ForgeCostQuoteAccess(Supplier<MinecraftServer> server, Supplier<CostConfiguration> configuration,
                                 ToIntBiFunction<ServerPlayer, EnumTeleportType> cooldown) {
        this(server, type -> configuration.get(), cooldown);
    }
    public ForgeCostQuoteAccess(Supplier<MinecraftServer> server, Function<EnumTeleportType, CostConfiguration> configuration,
                                 ToIntBiFunction<ServerPlayer, EnumTeleportType> cooldown) {
        this.server = Objects.requireNonNull(server); this.configuration = Objects.requireNonNull(configuration);
        this.cooldown = Objects.requireNonNull(cooldown);
    }
    private MinecraftServer currentServer() {
        MinecraftServer current = Objects.requireNonNull(server.get(), "server");
        if (!current.isSameThread()) throw new IllegalStateException("Quote resolution requires server owner thread");
        return current;
    }
    private ServerPlayer player(UUID id) { return currentServer().getPlayerList().getPlayer(id); }
    private boolean live(ServerPlayer player) {
        return player != null && player.server == currentServer() && !player.hasDisconnected() && !player.isRemoved() && player.isAlive();
    }
    @Override public boolean connected(UUID id) { return live(player(id)); }
    @Override public boolean permitted(UUID id, CostQuoteTarget target) {
        ServerPlayer player = player(id);
        return live(player) && NarcissusUtils.isCommandEnabled(target.teleportType().toCommandType())
                && NarcissusUtils.hasCommandPermission(player.createCommandSourceStack(), target.teleportType().toCommandType());
    }
    @Override public CostEvaluation open(UUID id, CostQuoteRequest request) {
        CostQuoteTarget target = request.target();
        if (!permitted(id, target)) throw new SecurityException("Quote permission revoked");
        ServerPlayer paying = player(id), moving = paying, other = null;
        SafeWorldCoordinate destination;
        switch (target.kind()) {
            case HOME:
            case STAGE:
            case HISTORY:
                destination = recordedTarget(id, target, PlayerTeleportData.getData(paying), target.kind() == CostQuoteTarget.Kind.STAGE
                        ? WorldStageData.get(currentServer().getAllLevels().iterator().next()) : null);
                break;
            case PLAYER:
                other = player(target.owner());
                if (!live(other) || other == paying || !PlayerTeleportData.getData(other).acceptsTeleportFrom(id)) {
                    throw new SecurityException("Quote target player unavailable or disallows requests");
                }
                moving = target.teleportType() == EnumTeleportType.TP_HERE ? other : paying;
                destination = new SafeWorldCoordinate(target.teleportType() == EnumTeleportType.TP_HERE ? paying : other);
                break;
            case COORDINATE:
                destination = new SafeWorldCoordinate(target.x(), target.y(), target.z(), target.dimension());
                break;
            default:
                return null;
        }
        if (destination == null || currentServer().getLevel(destination.dimension()) == null) return null;
        if (!moving.level.dimension().equals(destination.dimension())
                && (!CommonConfig.get().base().teleportLimit().teleportAcrossDimension()
                || !NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(paying, target.teleportType()))) {
            throw new SecurityException("Cross-dimension quote not permitted");
        }
        return ForgeCostContext.open(request.requestId(), request.generationId(), CostPhase.PREVIEW, target.teleportType(),
                configuration.apply(target.teleportType()), moving, paying, other == null ? null : paying, other,
                destination, null, cooldown.applyAsInt(paying, target.teleportType()));
    }

    static SafeWorldCoordinate recordedTarget(UUID connectionPlayer, CostQuoteTarget target, PlayerTeleportData playerData, WorldStageData stages) {
        SafeWorldCoordinate result = null;
        switch (target.kind()) {
            case HOME:
                if (connectionPlayer.equals(target.owner())) result = playerData.peekHomeCoordinates().get(new KeyValue<>(target.dimension(), target.name()));
                break;
            case STAGE:
                if (stages != null) result = stages.getStageCoordinate().get(new KeyValue<>(target.dimension(), target.name()));
                break;
            case HISTORY:
                if (!connectionPlayer.equals(target.owner()) || target.teleportType() == EnumTeleportType.TP_GRAVE && target.historyType() != EnumTeleportType.DEATH) break;
                // Existing NBT and player sync use second precision; no new persistence IDs.
                for (TeleportRecord record : playerData.peekTeleportRecords()) {
                    SafeWorldCoordinate before = record.getBefore();
                    if (before != null && record.getTeleportType() == target.historyType()
                            && before.dimensionId().equals(target.dimension())
                            && before.x() == target.x() && before.y() == target.y() && before.z() == target.z()
                            && DateUtils.toDateTimeString(record.getTeleportTime()).equals(target.historyTime())) {
                        result = before; break;
                    }
                }
                break;
            default: break;
        }
        return result == null ? null : result.clone();
    }
}
