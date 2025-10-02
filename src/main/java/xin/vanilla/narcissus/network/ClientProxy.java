package xin.vanilla.narcissus.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.network.packet.PlayerDataSyncPacket;

public class ClientProxy {
    public static final Logger LOGGER = LogManager.getLogger();

    public static void handleSynPlayerData(PlayerDataSyncPacket packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            try {
                PlayerTeleportData clientData = PlayerTeleportData.getData(player);
                clientData.copyFrom(packet.getData());
                LOGGER.debug("Client: Player data received successfully.");
            } catch (Exception ignored) {
                LOGGER.debug("Client: Player data received failed.");
            }
        }
    }

    public static PlayerTeleportData createClientData() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return PlayerTeleportData.getData(player);
        }
        return null;
    }
}
