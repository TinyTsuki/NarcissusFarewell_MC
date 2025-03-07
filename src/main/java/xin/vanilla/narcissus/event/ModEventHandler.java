package xin.vanilla.narcissus.event;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;

/**
 * Mod 事件处理器
 */
@Mod.EventBusSubscriber(modid = NarcissusFarewell.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        // 注册 PlayerDataCapability
        event.register(IPlayerTeleportData.class);
    }

}
