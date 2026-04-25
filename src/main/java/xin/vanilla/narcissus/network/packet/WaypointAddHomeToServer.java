package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.function.Supplier;

@Getter
@Accessors(fluent = true)
public class WaypointAddHomeToServer implements NetworkPacket {

    @Override
    public Supplier<SimpleChannel> channel() {
        return () -> NetworkInit.INSTANCE;
    }

    private static final int MAX_NAME_LEN = 64;

    private final String name;

    public WaypointAddHomeToServer(String name) {
        this.name = name != null ? name : "";
    }

    public WaypointAddHomeToServer(PacketBuffer buf) {
        this.name = buf.readUtf(MAX_NAME_LEN);
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUtf(name, MAX_NAME_LEN);
    }

    public static void handle(WaypointAddHomeToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ctx.get().getDirection().getReceptionSide().isServer()) {
                return;
            }
            net.minecraft.entity.player.ServerPlayerEntity sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }
            String prefix = NarcissusUtils.getCommandPrefix();
            String cmd = prefix + " " + CommonConfig.get().commandNames().commandSetHome();
            if (!packet.name().isEmpty()) {
                cmd += " " + StringUtils.formatString(packet.name());
            }
            CommandUtils.executeCommand(sender, cmd);
        });
        ctx.get().setPacketHandled(true);
    }
}
