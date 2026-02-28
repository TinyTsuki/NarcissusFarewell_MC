package xin.vanilla.narcissus.network.packet;

import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import xin.vanilla.narcissus.config.CustomConfig;
import xin.vanilla.narcissus.util.JsonUtils;
import xin.vanilla.narcissus.util.VirtualPermissionManager;

import java.util.function.Supplier;

@Getter
public class CustomConfigSyncToClient {
    /**
     * 自定义配置
     */
    private final JsonObject customConfig;

    public CustomConfigSyncToClient() {
        this.customConfig = CustomConfig.getCustomConfig();
    }

    public CustomConfigSyncToClient(FriendlyByteBuf buf) {
        this.customConfig = JsonUtils.GSON.fromJson(buf.readUtf(), JsonObject.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.customConfig.toString());
    }

    public static void handle(CustomConfigSyncToClient packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CustomConfig.setClientConfig(packet.getCustomConfig());
            VirtualPermissionManager.reloadClient();
        });
        ctx.get().setPacketHandled(true);
    }
}
