package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.CollectionUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumWhiteListMode;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.CommandUtils;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 客户端请求修改黑白名单（逻辑与 {@link xin.vanilla.narcissus.command.impl.BlacklistCommand} /
 * {@link xin.vanilla.narcissus.command.impl.WhitelistCommand} 一致）。
 */
@Getter
@Accessors(fluent = true)
public class AccessListEditToServer implements NetworkPacket {

    private static final int MAX_PAYLOAD_LEN = 64;
    private static final int MAX_MODE_LEN = 32;

    /**
     * 0 黑名单添加（payload：规范 UUID，或当前在线玩家名）<br>
     * 1 黑名单移除（payload：UUID 或在线玩家名）<br>
     * 2 白名单添加（payload：规范 UUID 或在线玩家名 + mode）<br>
     * 3 白名单移除（payload：UUID 或在线玩家名 + mode）
     */
    private final int op;
    /**
     * 白名单模式：{@link EnumWhiteListMode#name()}，黑名单操作忽略。
     */
    private final String mode;
    /**
     * 添加/删除目标：规范 UUID 字符串，或能匹配到在线玩家的显示名。
     */
    private final String payload;

    public AccessListEditToServer(int op, String mode, String payload) {
        this.op = op;
        this.mode = mode != null ? mode : "";
        this.payload = payload != null ? payload : "";
    }

    public AccessListEditToServer(FriendlyByteBuf buf) {
        this.op = buf.readByte();
        this.mode = buf.readUtf(MAX_MODE_LEN);
        this.payload = buf.readUtf(MAX_PAYLOAD_LEN);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(op);
        buf.writeUtf(mode, MAX_MODE_LEN);
        buf.writeUtf(payload, MAX_PAYLOAD_LEN);
    }

    public static void handle(AccessListEditToServer packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            int op = packet.op();
            String modeStr = packet.mode().trim();
            EnumWhiteListMode whiteMode = EnumWhiteListMode.NONE;
            if (op == 2 || op == 3) {
                if (modeStr.isEmpty()) {
                    modeStr = EnumWhiteListMode.NONE.name();
                }
                EnumWhiteListMode parsed = EnumWhiteListMode.valueOfEx(modeStr);
                if (parsed == null) {
                    MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("access_list_err_mode"), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
                    return;
                }
                whiteMode = parsed;
            }
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            PlayerAccess access = data.getAccess();
            switch (op) {
                case 0:
                    handleBlackAdd(player, data, access, packet.payload());
                    break;
                case 1:
                    handleBlackDel(player, data, access, packet.payload());
                    break;
                case 2:
                    handleWhiteAdd(player, data, access, packet.payload(), whiteMode);
                    break;
                case 3:
                    handleWhiteDel(player, data, access, packet.payload(), whiteMode);
                    break;
                default:
                    break;
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleBlackAdd(ServerPlayer player, PlayerTeleportData data, PlayerAccess access, String rawPayload) {
        String uuidStr = resolveTargetUuid(player, rawPayload);
        if (uuidStr == null) {
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("access_list_err_target"), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
            return;
        }
        String[] uuids = new String[]{uuidStr};
        if (access.addBlackList(uuids)) {
            data.setDirty();
            PlayerTeleportData.syncPlayerData(player);
            String display = PlayerUtils.getPlayerNameString(PlayerUtils.getPlayerByUUID(uuidStr));
            if (StringUtils.isNullOrEmptyEx(display)) {
                display = uuidStr;
            }
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("list_add_success"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , display), EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
        } else {
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("list_add_fail"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, false)), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
        }
    }

    private static void handleBlackDel(ServerPlayer player, PlayerTeleportData data, PlayerAccess access, String rawPayload) {
        String uuidStr = resolveTargetUuid(player, rawPayload);
        if (uuidStr == null) {
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("access_list_err_target"), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
            return;
        }
        access.removeBlackList(uuidStr);
        data.setDirty();
        PlayerTeleportData.syncPlayerData(player);
        Component msg = NarcissusComponent.get().transAuto("remove_success");
        if (CollectionUtils.isNullOrEmpty(access.getBlackList())) {
            msg.append(NarcissusComponent.get().transAuto("list_is_empty"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)));
        } else {
            msg.append(NarcissusComponent.get().transAuto("list_detail"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , access.getBlackList().stream()
                            .map(uuid -> PlayerUtils.getPlayerNameString(PlayerUtils.getPlayerByUUID(uuid)))
                            .collect(Collectors.joining(","))));
        }
        MessageUtils.sendDefaultNotification(player, msg, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
    }

    private static void handleWhiteAdd(ServerPlayer player, PlayerTeleportData data, PlayerAccess access, String rawPayload, EnumWhiteListMode mode) {
        String uuidStr = resolveTargetUuid(player, rawPayload);
        if (uuidStr == null) {
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("access_list_err_target"), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
            return;
        }
        String[] uuids = new String[]{uuidStr};
        if (access.addWhiteList(uuids)) {
            switch (mode) {
                case BOTH:
                    access.addTpaList(uuids);
                    access.addTphList(uuids);
                    break;
                case AUTO_ACCEPT_TPA:
                    access.addTpaList(uuids);
                    break;
                case AUTO_ACCEPT_TPH:
                    access.addTphList(uuids);
                    break;
                default:
                    break;
            }
            data.setDirty();
            PlayerTeleportData.syncPlayerData(player);
            String display = PlayerUtils.getPlayerNameString(PlayerUtils.getPlayerByUUID(uuidStr));
            if (StringUtils.isNullOrEmptyEx(display)) {
                display = uuidStr;
            }
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("list_add_success"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, false)
                    , display), EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
        } else {
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("list_add_fail"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, false)
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
        }
    }

    private static void handleWhiteDel(ServerPlayer player, PlayerTeleportData data, PlayerAccess access, String rawPayload, EnumWhiteListMode mode) {
        String uuidStr = resolveTargetUuid(player, rawPayload);
        if (uuidStr == null) {
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("access_list_err_target"), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
            return;
        }
        String[] uuids = new String[]{uuidStr};
        switch (mode) {
            case BOTH:
                access.removeAutoTpa(uuids);
                access.removeAutoTph(uuids);
                break;
            case AUTO_ACCEPT_TPA:
                access.removeAutoTpa(uuids);
                break;
            case AUTO_ACCEPT_TPH:
                access.removeAutoTph(uuids);
                break;
            default:
                access.removeWhiteList(uuids);
                break;
        }
        data.setDirty();
        PlayerTeleportData.syncPlayerData(player);
        MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("remove_success")
                .append(CommandUtils.getWhiteListMessage(player, access)), EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
    }

    private static boolean isValidUuidString(String s) {
        if (s == null || s.trim().isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(s.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 合法 UUID 则规范为字符串；否则按不区分大小写匹配当前在线玩家名。
     */
    @Nullable
    private static String resolveTargetUuid(ServerPlayer sender, String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (isValidUuidString(s)) {
            return UUID.fromString(s).toString();
        }
        ServerPlayer target = findOnlinePlayer(sender, s);
        return target != null ? PlayerUtils.getPlayerUUIDString(target) : null;
    }

    private static ServerPlayer findOnlinePlayer(ServerPlayer sender, String name) {
        for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
}
