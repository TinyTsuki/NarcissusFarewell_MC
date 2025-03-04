package xin.vanilla.narcissus.event;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EI18nType;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.DateUtils;
import xin.vanilla.narcissus.util.DimensionUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Date;

/**
 * Forge 事件处理
 */
public class ForgeEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public ForgeEventHandler() {
        // 注册事件到事件总线
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 服务器端玩家 Tick 事件
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (!player.getEntityWorld().isRemote && event.phase == TickEvent.Phase.END) {
            // 仅给安装了mod的玩家发送数据包
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getUniqueID().toString())
                    && !NarcissusFarewell.getPlayerCapabilityStatus().get(player.getUniqueID().toString())) {
                // 如果玩家还活着则同步玩家传送数据到客户端
                if (player.isEntityAlive()) {
                    try {
                        PlayerTeleportData.syncPlayerData((EntityPlayerMP) player);
                    } catch (Exception e) {
                        LOGGER.error("Failed to sync player data to client", e);
                    }
                }
            }
        }
    }

    /**
     * 服务器 Tick 事件（处理传送请求过期）
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter() % 20 == 0) {
                long currentTimeMillis = System.currentTimeMillis();
                NarcissusFarewell.getTeleportRequest().entrySet().removeIf(entry -> {
                    TeleportRequest request = entry.getValue();
                    if (request.getExpireTime() < currentTimeMillis) {
                        if (request.getTeleportType() == ETeleportType.TP_ASK) {
                            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_expired"), request.getTarget().getDisplayName());
                        } else if (request.getTeleportType() == ETeleportType.TP_HERE) {
                            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_expired"), request.getTarget().getDisplayName());
                        }
                        return true;
                    }
                    return false;
                });
            }
        }
    }

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entity;
            // 动态注册扩展属性
            if (player.getExtendedProperties(PlayerTeleportData.ID) == null) {
                player.registerExtendedProperties(PlayerTeleportData.ID, new PlayerTeleportData());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        // 加载数据
        PlayerTeleportData.get(event.entityPlayer).deserializeNBT(event.entityPlayer.getEntityData());
    }

    @SubscribeEvent
    public void onPlayerSave(PlayerEvent.SaveToFile event) {
        // 保存数据
        PlayerTeleportData.get(event.entityPlayer).serializeNBT(event.entityPlayer.getEntityData());
    }

    /**
     * 处理玩家克隆（死亡重生、跨维度）
     */
    @SubscribeEvent
    public void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.entityPlayer != null && !event.entityPlayer.worldObj.isRemote) {
            EntityPlayerMP original = (EntityPlayerMP) event.original;
            EntityPlayerMP newPlayer = (EntityPlayerMP) event.entityPlayer;
            NarcissusUtils.clonePlayerLanguage(original, newPlayer);

            IPlayerTeleportData oldData = PlayerTeleportData.get(original);
            IPlayerTeleportData newData = PlayerTeleportData.get(newPlayer);
            newData.copyFrom(oldData);

            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(newPlayer.getUniqueID().toString())) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(newPlayer.getUniqueID().toString(), false);
            }

            if (event.wasDeath) {
                TeleportRecord record = new TeleportRecord();
                record.setTeleportTime(new Date());
                record.setTeleportType(ETeleportType.DEATH);
                record.setBefore(new Coordinate(original.posX, original.posY, original.posZ, DimensionUtils.getStringId(original.getEntityWorld().provider.dimensionId)));
                record.setAfter(new Coordinate(newPlayer.posX, newPlayer.posY, newPlayer.posZ, DimensionUtils.getStringId(newPlayer.getEntityWorld().provider.dimensionId)));
                newData.addTeleportRecords(record);
            }
        }
    }

    /**
     * 玩家进入世界
     */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.entity;
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getUniqueID().toString())) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getUniqueID().toString(), false);
            }
            if (ServerConfig.TELEPORT_CARD) {
                IPlayerTeleportData data = PlayerTeleportData.get(player);
                Date current = new Date();
                if (DateUtils.toDateInt(data.getLastCardTime()) < DateUtils.toDateInt(current)) {
                    data.setLastCardTime(current);
                    data.plusTeleportCard(ServerConfig.TELEPORT_CARD_DAILY);
                }
            }
        }
    }

    /**
     * 玩家登出事件
     */
    @SubscribeEvent
    public void onPlayerLoggedOut(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        // 玩家退出服务器时移除键(移除mod安装状态)
        if (event.player instanceof EntityPlayerMP) {
            NarcissusFarewell.getPlayerCapabilityStatus().remove(event.player.getUniqueID().toString());
        }
    }
}
