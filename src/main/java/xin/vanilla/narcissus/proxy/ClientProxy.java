package xin.vanilla.narcissus.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.PlayerDataReceivedNotice;
import xin.vanilla.narcissus.network.PlayerDataSyncPacket;

public class ClientProxy {
    public static final Logger LOGGER = LogManager.getLogger();

    public static void handleSynPlayerData(PlayerDataSyncPacket packet) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            try {
                IPlayerTeleportData clientData = PlayerTeleportData.get(player);
                clientData.copyFrom(packet.getData());
                ModNetworkHandler.INSTANCE.sendToServer(new PlayerDataReceivedNotice());
                LOGGER.debug("Client: Player data received successfully.");
            } catch (Exception ignored) {
                LOGGER.debug("Client: Player data received failed.");
            }
        }
    }
}
