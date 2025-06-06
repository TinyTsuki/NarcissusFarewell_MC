package xin.vanilla.narcissus.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.LogicalSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataProvider;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.DateUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Date;

public class EventHandlerProxy {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            // 仅给安装了mod的玩家发送数据包
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getUUID().toString())
                    && !NarcissusFarewell.getPlayerCapabilityStatus().get(player.getStringUUID())) {
                // 如果玩家还活着则同步玩家传送数据到客户端
                if (player.isAlive()) {
                    try {
                        PlayerTeleportDataCapability.syncPlayerData((ServerPlayerEntity) player);
                    } catch (Exception e) {
                        LOGGER.error("Failed to sync player data to client", e);
                    }
                }
            }
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (NarcissusFarewell.getServerInstance().getTickCount() % 20 == 0) {
                long currentTimeMillis = System.currentTimeMillis();
                NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> entry.getValue().getExpireTime() < currentTimeMillis)
                        .forEach(entry -> {
                            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(entry.getKey());
                            if (request != null) {
                                if (request.getTeleportType() == ETeleportType.TP_ASK) {
                                    NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_expired"), request.getTarget().getDisplayName().getString());
                                } else if (request.getTeleportType() == ETeleportType.TP_HERE) {
                                    NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_expired"), request.getTarget().getDisplayName().getString());
                                }
                            }
                        });
            }
        }
    }

    public static void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            event.addCapability(NarcissusFarewell.createResource("player_teleport_data"), new PlayerTeleportDataProvider());
        }
    }

    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity original = (ServerPlayerEntity) event.getOriginal();
            ServerPlayerEntity newPlayer = (ServerPlayerEntity) event.getPlayer();
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
                record.setTeleportType(ETeleportType.DEATH);
                record.setBefore(new Coordinate().setX(original.x).setY(original.y).setZ(original.z).setDimension(original.level.dimension.getType()));
                record.setAfter(new Coordinate().setX(newPlayer.x).setY(newPlayer.z).setZ(newPlayer.z).setDimension(newPlayer.level.dimension.getType()));
                PlayerTeleportDataCapability.getData(newPlayer).addTeleportRecords(record);
            }
        }
    }

    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
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

    /**
     * 玩家登出事件
     */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 玩家退出服务器时移除键(移除mod安装状态)
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            NarcissusFarewell.getPlayerCapabilityStatus().remove(event.getPlayer().getStringUUID());
        }
    }
}
