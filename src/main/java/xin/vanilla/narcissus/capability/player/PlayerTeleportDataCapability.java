package xin.vanilla.narcissus.capability.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.PlayerDataSyncPacket;

/**
 * 玩家传送数据能力
 */
public class PlayerTeleportDataCapability {
    // 定义 Capability 实例
    @CapabilityInject(IPlayerTeleportData.class)
    public static Capability<IPlayerTeleportData> PLAYER_DATA;

    // 注册方法，用于在模组初始化期间注册 Capability
    public static void register() {
        // 注册 Capability 时，绑定接口、存储以及默认实现类
        CapabilityManager.INSTANCE.register(IPlayerTeleportData.class, new PlayerTeleportDataStorage(), PlayerTeleportData::new);
    }

    /**
     * 获取玩家传送数据
     *
     * @param player 玩家实体
     * @return 玩家的传送数据
     */
    public static IPlayerTeleportData getData(PlayerEntity player) {
        return player.getCapability(PLAYER_DATA).orElseThrow(() -> new IllegalArgumentException("Player data capability is missing."));
    }

    public static LazyOptional<IPlayerTeleportData> getDataOptional(ServerPlayerEntity player) {
        return player.getCapability(PLAYER_DATA);
    }

    /**
     * 设置玩家传送数据
     *
     * @param player 玩家实体
     * @param data   玩家传送数据
     */
    public static void setData(PlayerEntity player, IPlayerTeleportData data) {
        player.getCapability(PLAYER_DATA).ifPresent(capability -> capability.copyFrom(data));
    }

    /**
     * 同步玩家传送数据到客户端
     */
    public static void syncPlayerData(ServerPlayerEntity player) {
        // 创建自定义包并发送到客户端
        PlayerDataSyncPacket packet = new PlayerDataSyncPacket(player.getUUID(), PlayerTeleportDataCapability.getData(player));
        for (PlayerDataSyncPacket syncPacket : packet.split()) {
            ModNetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), syncPacket);
        }
    }
}
