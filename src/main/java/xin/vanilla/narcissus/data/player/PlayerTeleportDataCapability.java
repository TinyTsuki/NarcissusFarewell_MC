package xin.vanilla.narcissus.data.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.PlayerDataSyncPacket;

/**
 * 玩家传送数据能力
 */
public class PlayerTeleportDataCapability {
    // 定义 Capability 实例
    public static Capability<IPlayerTeleportData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() {
    });

    /**
     * 获取玩家传送数据
     *
     * @param player 玩家实体
     * @return 玩家的传送数据
     */
    public static IPlayerTeleportData getData(Player player) {
        return player.getCapability(PLAYER_DATA).orElseThrow(() -> new IllegalArgumentException("Player data capability is missing."));
    }

    public static LazyOptional<IPlayerTeleportData> getDataOptional(ServerPlayer player) {
        return player.getCapability(PLAYER_DATA);
    }

    /**
     * 设置玩家传送数据
     *
     * @param player 玩家实体
     * @param data   玩家传送数据
     */
    public static void setData(Player player, IPlayerTeleportData data) {
        player.getCapability(PLAYER_DATA).ifPresent(capability -> capability.copyFrom(data));
    }

    /**
     * 同步玩家传送数据到客户端
     */
    public static void syncPlayerData(ServerPlayer player) {
        // 创建自定义包并发送到客户端
        PlayerDataSyncPacket packet = new PlayerDataSyncPacket(player.getUUID(), PlayerTeleportDataCapability.getData(player));
        for (PlayerDataSyncPacket syncPacket : packet.split()) {
            ModNetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), syncPacket);
        }
    }
}
