package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class WaypointTeleportToServer implements NetworkPacket {

    private static final int MAX_NAME_LEN = 64;
    private static final int MAX_DIMENSION_LEN = 256;

    private final int typeOrdinal;
    private final String name;
    private final String dimension;

    public WaypointTeleportToServer(EnumTeleportType type, String name, String dimension) {
        this.typeOrdinal = type.ordinal();
        this.name = name != null ? name : "";
        this.dimension = dimension != null ? dimension : "";
    }

    public WaypointTeleportToServer(PacketBuffer buf) {
        this.typeOrdinal = buf.readVarInt();
        this.name = buf.readUtf(MAX_NAME_LEN);
        this.dimension = buf.readUtf(MAX_DIMENSION_LEN);
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(typeOrdinal);
        buf.writeUtf(name, MAX_NAME_LEN);
        buf.writeUtf(dimension, MAX_DIMENSION_LEN);
    }

    public static void handle(WaypointTeleportToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                net.minecraft.entity.player.ServerPlayerEntity sender = ctx.get().getSender();
                if (sender == null) return;
                EnumTeleportType type;
                try {
                    type = EnumTeleportType.values()[packet.typeOrdinal()];
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
                String prefix = NarcissusUtils.getCommandPrefix();
                String cmd;
                switch (type) {
                    case TP_HOME:
                        cmd = prefix + " " + CommonConfig.get().commandNames().commandTpHome();
                        if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                        if (!packet.dimension().isEmpty()) cmd += " true " + packet.dimension();
                        break;
                    case TP_STAGE:
                        cmd = prefix + " " + CommonConfig.get().commandNames().commandTpStage();
                        if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                        if (!packet.dimension().isEmpty()) cmd += " safe " + packet.dimension();
                        break;
                    case TP_BACK:
                        cmd = prefix + " " + CommonConfig.get().commandNames().commandTpBack();
                        if (!packet.name().isEmpty()) cmd += " safe " + packet.name();
                        break;
                    default:
                        return;
                }
                CommandUtils.executeCommand(sender, cmd);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
