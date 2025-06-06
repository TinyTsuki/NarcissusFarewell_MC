package xin.vanilla.narcissus.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.network.packet.PlayerDataReceivedNotice;
import xin.vanilla.narcissus.network.packet.PlayerDataSyncPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;

public class ClientProxy {
    public static final Logger LOGGER = LogManager.getLogger();

    public static void handleSynPlayerData(PlayerDataSyncPacket packet) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            try {
                IPlayerTeleportData clientData = PlayerTeleportDataCapability.getData(player);
                PlayerTeleportDataCapability.PLAYER_DATA.readNBT(clientData, null, packet.getData().serializeNBT());
                NarcissusUtils.sendPacketToServer(new PlayerDataReceivedNotice());
                LOGGER.debug("Client: Player data received successfully.");
            } catch (Exception ignored) {
                LOGGER.debug("Client: Player data received failed.");
            }
        }
    }
}
