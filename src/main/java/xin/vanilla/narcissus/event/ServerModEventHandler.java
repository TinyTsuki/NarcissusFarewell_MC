package xin.vanilla.narcissus.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;

/**
 * 服务端 Mod事件处理器
 */
@EventBusSubscriber(modid = NarcissusFarewell.MODID, value = Dist.DEDICATED_SERVER, bus = EventBusSubscriber.Bus.MOD)
public class ServerModEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
}
