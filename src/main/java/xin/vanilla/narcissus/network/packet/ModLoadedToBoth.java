package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModLoadedToBoth {

    public ModLoadedToBoth() {
    }

    public ModLoadedToBoth(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(ModLoadedToBoth packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // 同步玩家传送数据到客户端
                PlayerTeleportData.syncPlayerData(player);
                // 同步驿站数据到客户端
                NarcissusUtils.sendPacketToPlayer(new StageDataSyncToClient(WorldStageData.get().getStageCoordinate()), player);
                // 同步传送代价配置到客户端
                Map<EnumTeleportType, TeleportCost> costMap = new HashMap<>();
                costMap.put(EnumTeleportType.TP_HOME, NarcissusUtils.getCommandCost(EnumTeleportType.TP_HOME));
                costMap.put(EnumTeleportType.TP_STAGE, NarcissusUtils.getCommandCost(EnumTeleportType.TP_STAGE));
                costMap.put(EnumTeleportType.TP_BACK, NarcissusUtils.getCommandCost(EnumTeleportType.TP_BACK));
                NarcissusUtils.sendPacketToPlayer(new CostConfigSyncToClient(costMap,
                        ServerConfig.TELEPORT_COST_DISTANCE_LIMIT.get(),
                        ServerConfig.TELEPORT_COST_DISTANCE_ACROSS_DIMENSION.get()), player);
                // 同步自定义配置到客户端
                NarcissusUtils.sendPacketToPlayer(new CustomConfigSyncToClient(), player);
                // 刷新权限信息
                NarcissusUtils.refreshPermission(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
