package xin.vanilla.narcissus.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

import java.util.Comparator;
import java.util.Date;

public class EventHandlerProxy {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void onServerTick() {
        MinecraftServer srv = BaniraServer.currentAs(MinecraftServer.class);
        if (srv != null) {
            TeleportCountdownTracker.tickMovementCheck(srv);
        }
        if (srv != null && srv.getTickCount() % 20 == 0) {
            long currentTimeMillis = System.currentTimeMillis();
            NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> entry.getValue().getExpireTime() < currentTimeMillis)
                    .forEach(entry -> {
                        TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(entry.getKey());
                        if (request != null) {
                            if (request.getTeleportType() == EnumTeleportType.TP_ASK) {
                                MessageUtils.sendNotification(request.getRequester(), NarcissusComponent.get().transAuto("tp_ask_expired", request.getTarget().getDisplayName().getString()), NarcissusNotificationTypes.TELEPORT_REQUEST);
                            } else if (request.getTeleportType() == EnumTeleportType.TP_HERE) {
                                MessageUtils.sendNotification(request.getRequester(), NarcissusComponent.get().transAuto("tp_here_expired", request.getTarget().getDisplayName().getString()), NarcissusNotificationTypes.TELEPORT_REQUEST);
                            }
                        }
                    });
        }
    }

    public static void onPlayerCloned(ServerPlayer original, ServerPlayer newPlayer, boolean wasDeath) {
        original.revive();
        if (wasDeath) {
            TeleportRecord record = new TeleportRecord();
            record.setTeleportTime(new Date());
            record.setTeleportType(EnumTeleportType.DEATH);
            SafeWorldCoordinate before = new SafeWorldCoordinate();
            before.x(original.getX()).y(original.getY()).z(original.getZ()).dimension(original.level().dimension());
            record.setBefore(before);
            SafeWorldCoordinate after = new SafeWorldCoordinate();
            after.x(newPlayer.getX()).y(newPlayer.getY()).z(newPlayer.getZ()).dimension(newPlayer.level().dimension());
            record.setAfter(after);
            PlayerTeleportData.getData(newPlayer).addTeleportRecords(record);
            PlayerTeleportData.syncPlayerData(newPlayer);
        }
    }

    public static void onPlayerJoinWorld(ServerPlayer player) {
        if (CommonConfig.get().base().teleportCard().teleportCard()) {
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            Date current = new Date();
            if (DateUtils.toDateInt(data.getLastCardTime()) < DateUtils.toDateInt(current)) {
                data.setLastCardTime(current);
                data.plusTeleportCard(CommonConfig.get().base().teleportCard().teleportCardDaily());
                PlayerTeleportData.syncPlayerData(player);
            }
        }
    }

    public static void onPlayerTeleport(ServerPlayer player, Vec3 previous, Vec3 target) {
        TeleportRecord record = new TeleportRecord();
        record.setTeleportTime(new Date());
        record.setTeleportType(EnumTeleportType.OTHER);
        SafeWorldCoordinate before = new SafeWorldCoordinate(player);
        before.fromVec3(previous);
        record.setBefore(before);
        SafeWorldCoordinate after = new SafeWorldCoordinate(player);
        after.fromVec3(target);
        record.setAfter(after);
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        TeleportRecord otherRecord = data.getTeleportRecords().stream().max(Comparator.comparing(o -> o.getTeleportTime().getTime())).orElse(null);
        if (otherRecord != null && otherRecord.getTeleportType() == EnumTeleportType.OTHER && otherRecord.getBefore().xyzString().equals(record.getBefore().xyzString())) {
            otherRecord.setAfter(record.getAfter());
            data.save();
            PlayerTeleportData.syncPlayerData(player);
        } else {
            data.addTeleportRecords(record);
            PlayerTeleportData.syncPlayerData(player);
        }
    }

}
