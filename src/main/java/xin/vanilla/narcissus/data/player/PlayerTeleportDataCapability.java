package xin.vanilla.narcissus.data.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.LazyOptional;
import xin.vanilla.narcissus.network.packet.PlayerDataSyncPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;

/**
 * 玩家能力
 */
public class PlayerTeleportDataCapability {
    @CapabilityInject(IPlayerTeleportData.class)
    public static Capability<IPlayerTeleportData> PLAYER_DATA;

    public static void register() {
        CapabilityManager.INSTANCE.register(IPlayerTeleportData.class, new PlayerTeleportDataStorage(), PlayerTeleportData::new);
    }

    /**
     * 获取玩家数据
     *
     * @param player 玩家实体
     * @return 玩家的数据
     */
    public static IPlayerTeleportData getData(PlayerEntity player) {
        return player.getCapability(PLAYER_DATA).orElseThrow(() -> new IllegalArgumentException("Player data capability is missing."));
    }

    public static LazyOptional<IPlayerTeleportData> getDataOptional(ServerPlayerEntity player) {
        return player.getCapability(PLAYER_DATA);
    }

    /**
     * 设置玩家数据
     *
     * @param player 玩家实体
     * @param data   玩家数据
     */
    public static void setData(PlayerEntity player, IPlayerTeleportData data) {
        player.getCapability(PLAYER_DATA).ifPresent(capability -> capability.copyFrom(data));
    }

    /**
     * 同步玩家数据到客户端
     */
    public static void syncPlayerData(ServerPlayerEntity player) {
        // 创建自定义包并发送到客户端
        PlayerDataSyncPacket packet = new PlayerDataSyncPacket(player.getUUID(), PlayerTeleportDataCapability.getData(player));
        for (PlayerDataSyncPacket syncPacket : packet.split()) {
            NarcissusUtils.sendPacketToPlayer(syncPacket, player);
        }
    }
}
