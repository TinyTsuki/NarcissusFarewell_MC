package xin.vanilla.narcissus.event;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.notification.NarcissusNotificationSend;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

import java.util.Comparator;
import java.util.Date;

public class EventHandlerProxy {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer srv = BaniraCodex.serverInstance().key();
            if (srv != null) {
                TeleportCountdownTracker.tickMovementCheck(srv);
            }
            if (BaniraCodex.serverInstance().key().getTickCount() % 20 == 0) {
                long currentTimeMillis = System.currentTimeMillis();
                NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> entry.getValue().getExpireTime() < currentTimeMillis)
                        .forEach(entry -> {
                            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(entry.getKey());
                            if (request != null) {
                                if (request.getTeleportType() == EnumTeleportType.TP_ASK) {
                                    NarcissusNotificationSend.send(request.getRequester(), NarcissusComponent.get().transAuto("tp_ask_expired", request.getTarget().getDisplayName().getString()), NarcissusNotificationTypes.TELEPORT_REQUEST);
                                } else if (request.getTeleportType() == EnumTeleportType.TP_HERE) {
                                    NarcissusNotificationSend.send(request.getRequester(), NarcissusComponent.get().transAuto("tp_here_expired", request.getTarget().getDisplayName().getString()), NarcissusNotificationTypes.TELEPORT_REQUEST);
                                }
                            }
                        });
            }
        }
    }

    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity original = (ServerPlayerEntity) event.getOriginal();
            ServerPlayerEntity newPlayer = (ServerPlayerEntity) event.getPlayer();
            original.revive();

            // 如果是死亡，则记录死亡记录
            if (event.isWasDeath()) {
                TeleportRecord record = new TeleportRecord();
                record.setTeleportTime(new Date());
                record.setTeleportType(EnumTeleportType.DEATH);
                SafeWorldCoordinate before = new SafeWorldCoordinate();
                before.x(original.getX()).y(original.getY()).z(original.getZ()).dimension(original.level.dimension());
                record.setBefore(before);
                SafeWorldCoordinate after = new SafeWorldCoordinate();
                after.x(newPlayer.getX()).y(newPlayer.getY()).z(newPlayer.getZ()).dimension(newPlayer.level.dimension());
                record.setAfter(after);
                PlayerTeleportData.getData(newPlayer).addTeleportRecords(record);
                PlayerTeleportData.syncPlayerData(newPlayer);
            }
        }
    }

    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            // 给予传送卡
            if (CommonConfig.get().base().teleportCard()) {
                PlayerTeleportData data = PlayerTeleportData.getData(player);
                Date current = new Date();
                if (DateUtils.toDateInt(data.getLastCardTime()) < DateUtils.toDateInt(current)) {
                    data.setLastCardTime(current);
                    data.plusTeleportCard(CommonConfig.get().base().teleportCardDaily());
                    PlayerTeleportData.syncPlayerData(player);
                }
            }
        }
    }

    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            TeleportRecord record = new TeleportRecord();
            record.setTeleportTime(new Date());
            record.setTeleportType(EnumTeleportType.OTHER);
            SafeWorldCoordinate before = new SafeWorldCoordinate(player);
            before.fromVector3d(event.getPrev());
            record.setBefore(before);
            SafeWorldCoordinate after = new SafeWorldCoordinate(player);
            after.fromVector3d(event.getTarget());
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

}
