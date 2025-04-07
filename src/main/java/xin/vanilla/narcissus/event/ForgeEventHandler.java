package xin.vanilla.narcissus.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.PlayerDataAttachment;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EI18nType;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.network.packet.ClientModLoadedNotice;
import xin.vanilla.narcissus.util.DateUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.Date;

/**
 * Forge 事件处理
 */
@Mod.EventBusSubscriber(modid = NarcissusFarewell.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        LOGGER.debug("Client: Player logged in.");
        PacketDistributor.SERVER.noArg().send(new ClientModLoadedNotice());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LOGGER.debug("Client: Player logged out.");
    }

    /**
     * 同步客户端服务端数据
     */
    @SubscribeEvent
    public static void playerTickEvent(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            if (event.player instanceof ServerPlayer player) {
                // 仅给安装了mod的玩家发送数据包
                if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getUUID().toString())
                        && !NarcissusFarewell.getPlayerCapabilityStatus().get(player.getStringUUID())) {
                    // 如果玩家还活着则同步玩家传送数据到客户端
                    if (player.isAlive()) {
                        try {
                            PlayerDataAttachment.syncPlayerData(player);
                        } catch (Exception e) {
                            LOGGER.error("Failed to sync player data to client", e);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (event.haveTime()) {
                if (NarcissusFarewell.getServerInstance().getTickCount() % 20 == 0) {
                    long currentTimeMillis = System.currentTimeMillis();
                    NarcissusFarewell.getTeleportRequest().entrySet().stream()
                            .filter(entry -> entry.getValue().getExpireTime() < currentTimeMillis)
                            .forEach(entry -> {
                                TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(entry.getKey());
                                if (request != null) {
                                    if (request.getTeleportType() == ETeleportType.TP_ASK) {
                                        NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_expired"), request.getTarget().getDisplayName().getString());
                                    } else if (request.getTeleportType() == ETeleportType.TP_HERE) {
                                        NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_expired"), request.getTarget().getDisplayName().getString());
                                    }
                                }
                            });
                }
            }
        }
    }

    /**
     * 玩家死亡后重生或者从末地回主世界
     */
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        ServerPlayer original = (ServerPlayer) event.getOriginal();
        ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
        NarcissusUtils.clonePlayerLanguage(original, newPlayer);
        original.revive();
        PlayerTeleportData oldDataCap = PlayerDataAttachment.getData(original);
        PlayerTeleportData newDataCap = PlayerDataAttachment.getData(newPlayer);
        newDataCap.copyFrom(oldDataCap);
        if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(newPlayer.getUUID().toString())) {
            NarcissusFarewell.getPlayerCapabilityStatus().put(newPlayer.getStringUUID(), false);
        }
        // 如果是死亡，则记录死亡记录
        if (event.isWasDeath()) {
            TeleportRecord record = new TeleportRecord();
            record.setTeleportTime(new Date());
            record.setTeleportType(ETeleportType.DEATH);
            record.setBefore(new Coordinate().setX(original.getX()).setY(original.getY()).setZ(original.getZ()).setDimension(original.level().dimension()));
            record.setAfter(new Coordinate().setX(newPlayer.getX()).setY(newPlayer.getY()).setZ(newPlayer.getZ()).setDimension(newPlayer.level().dimension()));
            PlayerDataAttachment.getData(newPlayer).addTeleportRecords(record);
        }
    }

    /**
     * 玩家进入维度
     */
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 初始化能力同步状态
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getStringUUID())) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), false);
            }
            // 给予传送卡
            if (ServerConfig.TELEPORT_CARD.get()) {
                PlayerTeleportData data = PlayerDataAttachment.getData(player);
                Date current = new Date();
                if (DateUtils.toDateInt(data.getLastCardTime()) < DateUtils.toDateInt(current)) {
                    data.setLastCardTime(current);
                    data.plusTeleportCard(ServerConfig.TELEPORT_CARD_DAILY.get());
                }
            }
        }
    }

    /**
     * 同维度传送事件
     */
    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportRecord record = new TeleportRecord();
            record.setTeleportTime(new Date());
            record.setTeleportType(ETeleportType.OTHER);
            record.setBefore(new Coordinate(player).fromVec3(event.getPrev()));
            record.setAfter(new Coordinate(player).fromVec3(event.getTarget()));
            PlayerTeleportData data = PlayerDataAttachment.getData(player);
            TeleportRecord otherRecord = data.getTeleportRecords().stream().max(Comparator.comparing(o -> o.getTeleportTime().getTime())).orElse(null);
            if (otherRecord != null && otherRecord.getTeleportType() == ETeleportType.OTHER && otherRecord.getBefore().toXyzString().equals(record.getBefore().toXyzString())) {
                otherRecord.setAfter(record.getAfter());
            } else {
                data.addTeleportRecords(record);
            }
        }
    }

    /**
     * 玩家登出事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 玩家退出服务器时移除键(移除mod安装状态)
        if (event.getEntity() instanceof ServerPlayer) {
            NarcissusFarewell.getPlayerCapabilityStatus().remove(event.getEntity().getStringUUID());
        }
    }
}
