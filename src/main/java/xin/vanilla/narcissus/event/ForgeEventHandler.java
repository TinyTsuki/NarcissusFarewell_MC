package xin.vanilla.narcissus.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.capability.TeleportRecord;
import xin.vanilla.narcissus.capability.player.IPlayerTeleportData;
import xin.vanilla.narcissus.capability.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.capability.player.PlayerTeleportDataProvider;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.enums.EI18nType;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.network.ClientModLoadedNotice;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.util.DateUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Date;

/**
 * Forge 事件处理
 */
@Mod.EventBusSubscriber(modid = NarcissusFarewell.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        LOGGER.debug("Client: Player logged in.");
        // 同步客户端配置到服务器
        ModNetworkHandler.INSTANCE.sendToServer(new ClientModLoadedNotice());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        LOGGER.debug("Client: Player logged out.");
    }

    /**
     * 同步客户端服务端数据
     */
    @SubscribeEvent
    public static void playerTickEvent(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            // 不用给未安装mod的玩家发送数据包
            if (!NarcissusFarewell.getPlayerCapabilityStatus().getOrDefault(player.getUUID().toString(), true)) {
                // 同步玩家传送数据到客户端
                PlayerTeleportDataCapability.syncPlayerData((ServerPlayerEntity) player);
            }
        }
    }

    @SubscribeEvent
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
                                    NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_expired"), request.getTarget().getDisplayName().getString());
                                } else if (request.getTeleportType() == ETeleportType.TP_HERE) {
                                    NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_expired"), request.getTarget().getDisplayName().getString());
                                }
                            }
                        });
            }
        }
    }

    /**
     * 当 AttachCapabilitiesEvent 事件发生时，此方法会为玩家实体附加自定义的能力
     * 在 Minecraft 中，实体可以拥有多种能力，这是一种扩展游戏行为的强大机制
     * 此处我们利用这个机制，为玩家实体附加一个用于传送的数据管理能力
     *
     * @param event 事件对象，包含正在附加能力的实体信息
     */
    @SubscribeEvent
    public static void onAttachCapabilityEvent(AttachCapabilitiesEvent<Entity> event) {
        // 检查事件对象是否为玩家实体，因为我们的目标是为玩家附加能力
        if (event.getObject() instanceof PlayerEntity) {
            // 为玩家实体附加一个名为 "player_teleport_data" 的能力
            // 这个能力由 PlayerTeleportDataProvider 提供，用于管理玩家的传送数据
            event.addCapability(new ResourceLocation(NarcissusFarewell.MODID, "player_teleport_data"), new PlayerTeleportDataProvider());
        }
    }

    /**
     * 玩家死亡后重生或者从末地回主世界
     */
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        ServerPlayerEntity original = (ServerPlayerEntity) event.getOriginal();
        ServerPlayerEntity newPlayer = (ServerPlayerEntity) event.getPlayer();
        newPlayer.updateOptions(NarcissusUtils.getCClientSettingsPacket(original));
        original.revive();
        LazyOptional<IPlayerTeleportData> oldDataCap = original.getCapability(PlayerTeleportDataCapability.PLAYER_DATA);
        LazyOptional<IPlayerTeleportData> newDataCap = newPlayer.getCapability(PlayerTeleportDataCapability.PLAYER_DATA);
        oldDataCap.ifPresent(oldData -> newDataCap.ifPresent(newData -> newData.copyFrom(oldData)));
        if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(newPlayer.getUUID().toString())) {
            NarcissusFarewell.getPlayerCapabilityStatus().put(newPlayer.getUUID().toString(), false);
        }
        // 如果是死亡，则记录死亡记录
        if (event.isWasDeath()) {
            TeleportRecord record = new TeleportRecord();
            record.setTeleportTime(new Date());
            record.setTeleportType(ETeleportType.DEATH);
            record.setBefore(new Coordinate().setX(original.getX()).setY(original.getY()).setZ(original.getZ()).setDimension(original.level.dimension.getType()));
            record.setAfter(new Coordinate().setX(newPlayer.getX()).setY(newPlayer.getY()).setZ(newPlayer.getZ()).setDimension(newPlayer.level.dimension.getType()));
            PlayerTeleportDataCapability.getData(newPlayer).addTeleportRecords(record);
        }
    }

    /**
     * 玩家进入维度
     */
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            // 初始化能力同步状态
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getUUID().toString())) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getUUID().toString(), false);
            }
            // 给予传送卡
            if (ServerConfig.TELEPORT_CARD.get()) {
                IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
                Date current = new Date();
                if (DateUtils.toDateInt(data.getLastCardTime()) < DateUtils.toDateInt(current)) {
                    data.setLastCardTime(current);
                    data.plusTeleportCard(ServerConfig.TELEPORT_CARD_DAILY.get());
                }
            }
        }
    }
}
