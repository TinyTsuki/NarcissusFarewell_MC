package xin.vanilla.narcissus.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.LogicalSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CustomConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataProvider;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.DateUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.Date;

public class EventHandlerProxy {
    private static final Logger LOGGER = LogManager.getLogger();

    private static long lastSaveConfTime = System.currentTimeMillis();
    private static long lastReadConfTime = System.currentTimeMillis();

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player;
        if (event.side == LogicalSide.SERVER) {
            // 仅给安装了mod的玩家发送数据包
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getUUID().toString())
                    && !NarcissusFarewell.getPlayerCapabilityStatus().get(player.getStringUUID())) {
                // 如果玩家还活着则同步玩家传送数据到客户端
                if (player.isAlive()) {
                    try {
                        PlayerTeleportDataCapability.syncPlayerData((ServerPlayer) player);
                    } catch (Exception e) {
                        LOGGER.error("Failed to sync player data to client", e);
                    }
                }
            }
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (NarcissusFarewell.getServerInstance().getTickCount() % 20 == 0) {
            long currentTimeMillis = System.currentTimeMillis();
            NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> entry.getValue().getExpireTime() < currentTimeMillis)
                    .forEach(entry -> {
                        TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(entry.getKey());
                        if (request != null) {
                            if (request.getTeleportType() == EnumTeleportType.TP_ASK) {
                                NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_expired"), request.getTarget().getDisplayName().getString());
                            } else if (request.getTeleportType() == EnumTeleportType.TP_HERE) {
                                NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_expired"), request.getTarget().getDisplayName().getString());
                            }
                        }
                    });
        }

        // 保存通用配置
        if (System.currentTimeMillis() - lastSaveConfTime >= 10 * 1000) {
            lastSaveConfTime = System.currentTimeMillis();
            CustomConfig.saveCustomConfig();
        }
        // 读取通用配置
        else if (System.currentTimeMillis() - lastReadConfTime >= 2 * 60 * 1000) {
            lastReadConfTime = System.currentTimeMillis();
            CustomConfig.loadCustomConfig(true);
        }
    }

    public static void registerCaps(RegisterCapabilitiesEvent event) {
        // 注册 PlayerDataCapability
        event.register(IPlayerTeleportData.class);
    }

    public static void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(NarcissusFarewell.createResource("player_teleport_data"), new PlayerTeleportDataProvider());
        }
    }

    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer original = (ServerPlayer) event.getOriginal();
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
            original.revive();
            NarcissusUtils.clonePlayerLanguage(original, newPlayer);
            LazyOptional<IPlayerTeleportData> oldDataCap = original.getCapability(PlayerTeleportDataCapability.PLAYER_DATA);
            LazyOptional<IPlayerTeleportData> newDataCap = newPlayer.getCapability(PlayerTeleportDataCapability.PLAYER_DATA);
            oldDataCap.ifPresent(oldData -> newDataCap.ifPresent(newData -> newData.copyFrom(oldData)));
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(newPlayer.getUUID().toString())) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(newPlayer.getStringUUID(), false);
            }
            // 如果是死亡，则记录死亡记录
            if (event.isWasDeath()) {
                TeleportRecord record = new TeleportRecord();
                record.setTeleportTime(new Date());
                record.setTeleportType(EnumTeleportType.DEATH);
                record.setBefore(new Coordinate().setX(original.getX()).setY(original.getY()).setZ(original.getZ()).setDimension(original.level().dimension()));
                record.setAfter(new Coordinate().setX(newPlayer.getX()).setY(newPlayer.getY()).setZ(newPlayer.getZ()).setDimension(newPlayer.level().dimension()));
                PlayerTeleportDataCapability.getData(newPlayer).addTeleportRecords(record);
            }
        }
    }

    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 初始化能力同步状态
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getStringUUID())) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), false);
            }
            // 给予传送卡
            if (CommonConfig.TELEPORT_CARD.get()) {
                IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
                Date current = new Date();
                if (DateUtils.toDateInt(data.getLastCardTime()) < DateUtils.toDateInt(current)) {
                    data.setLastCardTime(current);
                    data.plusTeleportCard(CommonConfig.TELEPORT_CARD_DAILY.get());
                }
            }
        }
    }

    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportRecord record = new TeleportRecord();
            record.setTeleportTime(new Date());
            record.setTeleportType(EnumTeleportType.OTHER);
            record.setBefore(new Coordinate(player).fromVec3(event.getPrev()));
            record.setAfter(new Coordinate(player).fromVec3(event.getTarget()));
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            TeleportRecord otherRecord = data.getTeleportRecords().stream().max(Comparator.comparing(o -> o.getTeleportTime().getTime())).orElse(null);
            if (otherRecord != null && otherRecord.getTeleportType() == EnumTeleportType.OTHER && otherRecord.getBefore().toXyzString().equals(record.getBefore().toXyzString())) {
                otherRecord.setAfter(record.getAfter());
            } else {
                data.addTeleportRecords(record);
            }
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 玩家退出服务器时移除键(移除mod安装状态)
        if (event.getEntity() instanceof ServerPlayer) {
            NarcissusFarewell.getPlayerCapabilityStatus().remove(event.getEntity().getStringUUID());
        }
    }
}
