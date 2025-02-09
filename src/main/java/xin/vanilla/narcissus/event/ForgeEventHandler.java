package xin.vanilla.narcissus.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.BuildConfig;
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

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Forge 事件处理
 */
@Mod.EventBusSubscriber(modid = BuildConfig.MODID)
public class ForgeEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 客户端玩家登录
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            LOGGER.debug("Client: Player logged in.");
            // 同步客户端配置到服务器
            try {
                ModNetworkHandler.INSTANCE.sendToServer(new ClientModLoadedNotice());
            } catch (Exception e) {
                LOGGER.error("Failed to send ClientModLoadedNotice", e);
            }
        }
    }

    /**
     * 服务器端玩家 Tick 事件
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (!player.world.isRemote && event.phase == TickEvent.Phase.END) {
            if (!NarcissusFarewell.getPlayerCapabilityStatus().getOrDefault(player.getUniqueID().toString(), true)) {
                try {
                    PlayerTeleportDataCapability.syncPlayerData((EntityPlayerMP) player);
                } catch (Exception e) {
                    LOGGER.error("Failed to sync player data to client", e);
                }
            }
        }
    }

    /**
     * 服务器 Tick 事件（处理传送请求过期）
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter() % 20 == 0) {
                long currentTimeMillis = System.currentTimeMillis();
                NarcissusFarewell.getTeleportRequest().entrySet().removeIf(entry -> {
                    TeleportRequest request = entry.getValue();
                    if (request.getExpireTime() < currentTimeMillis) {
                        if (request.getTeleportType() == ETeleportType.TP_ASK) {
                            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_expired"), request.getTarget().getDisplayName().getUnformattedText());
                        } else if (request.getTeleportType() == ETeleportType.TP_HERE) {
                            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_expired"), request.getTarget().getDisplayName().getUnformattedText());
                        }
                        return true;
                    }
                    return false;
                });
            }
        }
    }

    /**
     * 绑定玩家能力（Capability）
     */
    @SubscribeEvent
    public static void onAttachCapabilityEvent(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(new ResourceLocation(BuildConfig.MODID, "player_teleport_data"), new PlayerTeleportDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        EntityPlayer player = (EntityPlayer) event.getEntity();
        IPlayerTeleportData data = player.getCapability(PlayerTeleportDataCapability.PLAYER_DATA, null);
        if (data != null) {
            NBTTagCompound modData = new NBTTagCompound();
            modData.setTag(BuildConfig.MODID, data.serializeNBT());
            try {
                File file = event.getPlayerFile(BuildConfig.MODID);
                CompressedStreamTools.write(modData, file);
            } catch (IOException e) {
                LOGGER.error("Failed to save player data to file", e);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        EntityPlayer player = (EntityPlayer) event.getEntity();
        IPlayerTeleportData data = player.getCapability(PlayerTeleportDataCapability.PLAYER_DATA, null);
        if (data != null) {
            try {
                File file = event.getPlayerFile(BuildConfig.MODID);
                if (file.exists() && file.isFile()) {
                    NBTTagCompound modData = CompressedStreamTools.read(file);
                    if (modData != null && modData.hasKey(BuildConfig.MODID)) {
                        NBTTagCompound playerData = modData.getCompoundTag(BuildConfig.MODID);
                        data.deserializeNBT(playerData);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load player data from file", e);
            }
        }
    }

    /**
     * 处理玩家克隆（死亡重生、跨维度）
     */
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        EntityPlayerMP original = (EntityPlayerMP) event.getOriginal();
        EntityPlayerMP newPlayer = (EntityPlayerMP) event.getEntityPlayer();
        NarcissusUtils.clonePlayerLanguage(original, newPlayer);

        IPlayerTeleportData oldData = PlayerTeleportDataCapability.getData(original);
        IPlayerTeleportData newData = PlayerTeleportDataCapability.getData(newPlayer);
        newData.copyFrom(oldData);

        if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(newPlayer.getUniqueID().toString())) {
            NarcissusFarewell.getPlayerCapabilityStatus().put(newPlayer.getUniqueID().toString(), false);
        }

        if (event.isWasDeath()) {
            TeleportRecord record = new TeleportRecord();
            record.setTeleportTime(new Date());
            record.setTeleportType(ETeleportType.DEATH);
            record.setBefore(new Coordinate(original.posX, original.posY, original.posZ, original.world.provider.getDimensionType()));
            record.setAfter(new Coordinate(newPlayer.posX, newPlayer.posY, newPlayer.posZ, newPlayer.world.provider.getDimensionType()));
            newData.addTeleportRecords(record);
        }
    }

    /**
     * 玩家进入世界
     */
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
            if (NarcissusFarewell.getPlayerCapabilityStatus().containsKey(player.getUniqueID().toString())) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getUniqueID().toString(), false);
            }
            if (ServerConfig.TELEPORT_CARD) {
                IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
                Date current = new Date();
                if (DateUtils.toDateInt(data.getLastCardTime()) < DateUtils.toDateInt(current)) {
                    data.setLastCardTime(current);
                    data.plusTeleportCard(ServerConfig.TELEPORT_CARD_DAILY);
                }
            }
        }
    }
}
