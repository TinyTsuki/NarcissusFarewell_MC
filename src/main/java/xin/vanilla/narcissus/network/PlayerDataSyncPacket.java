package xin.vanilla.narcissus.network;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import xin.vanilla.narcissus.capability.TeleportRecord;
import xin.vanilla.narcissus.capability.player.IPlayerTeleportData;
import xin.vanilla.narcissus.capability.player.PlayerTeleportData;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.proxy.ClientProxy;
import xin.vanilla.narcissus.util.DateUtils;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class PlayerDataSyncPacket extends SplitPacket implements IMessage {
    private UUID playerUUID;
    private Date lastCardTime;
    private Date lastTpTime;
    private int teleportCard;
    private List<TeleportRecord> teleportRecords;
    private Map<KeyValue<String, String>, Coordinate> homeCoordinate;
    private Map<String, String> defaultHome;

    public PlayerDataSyncPacket() {
    }

    public PlayerDataSyncPacket(UUID playerUUID, IPlayerTeleportData data) {
        super();
        this.playerUUID = playerUUID;
        this.lastCardTime = data.getLastCardTime();
        this.lastTpTime = data.getLastTpTime();
        this.teleportCard = data.getTeleportCard();
        this.teleportRecords = data.getTeleportRecords();
        this.homeCoordinate = data.getHomeCoordinate();
        this.defaultHome = data.getDefaultHome();
    }

    public PlayerDataSyncPacket(ByteBuf buf) {
        super(buf);
        this.playerUUID = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        this.lastCardTime = DateUtils.format(ByteBufUtils.readUTF8String(buf));
        this.lastTpTime = DateUtils.format(ByteBufUtils.readUTF8String(buf));
        this.teleportCard = buf.readInt();

        this.teleportRecords = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.teleportRecords.add(TeleportRecord.readFromNBT(Objects.requireNonNull(ByteBufUtils.readTag(buf))));
        }

        this.homeCoordinate = new HashMap<>();
        int homeSize = buf.readInt();
        for (int i = 0; i < homeSize; i++) {
            this.homeCoordinate.put(
                    new KeyValue<>(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf)),
                    Coordinate.readFromNBT(Objects.requireNonNull(ByteBufUtils.readTag(buf)))
            );
        }

        this.defaultHome = new HashMap<>();
        int defaultSize = buf.readInt();
        for (int i = 0; i < defaultSize; i++) {
            this.defaultHome.put(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf));
        }
    }

    public PlayerDataSyncPacket(List<PlayerDataSyncPacket> packets) {
        super();
        this.playerUUID = packets.get(0).playerUUID;
        this.lastCardTime = packets.get(0).lastCardTime;
        this.lastTpTime = packets.get(0).lastTpTime;
        this.teleportCard = packets.get(0).teleportCard;
        this.teleportRecords = packets.stream()
                .map(PlayerDataSyncPacket::getTeleportRecords)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(TeleportRecord::getTeleportTime))
                .collect(Collectors.toList());
        this.homeCoordinate = packets.stream()
                .map(PlayerDataSyncPacket::getHomeCoordinate)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
        this.defaultHome = packets.get(0).defaultHome;
    }

    private PlayerDataSyncPacket(UUID playerUUID, Date lastCardTime, Date lastTpTime, int teleportCard) {
        super();
        this.playerUUID = playerUUID;
        this.lastCardTime = lastCardTime;
        this.lastTpTime = lastTpTime;
        this.teleportCard = teleportCard;
        this.teleportRecords = new ArrayList<>();
        this.homeCoordinate = new HashMap<>();
        this.defaultHome = new HashMap<>();
    }

    public void fromBytes(ByteBuf buf) {
        PlayerDataSyncPacket packet = new PlayerDataSyncPacket(buf);
        this.setId(packet.getId());
        this.setTotal(packet.getTotal());
        this.setSort(packet.getSort());
        this.playerUUID = packet.getPlayerUUID();
        this.lastCardTime = packet.getLastCardTime();
        this.lastTpTime = packet.getLastTpTime();
        this.teleportCard = packet.getTeleportCard();
        this.teleportRecords = packet.getTeleportRecords();
        this.homeCoordinate = packet.getHomeCoordinate();
        this.defaultHome = packet.getDefaultHome();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        ByteBufUtils.writeUTF8String(buf, playerUUID.toString());
        ByteBufUtils.writeUTF8String(buf, DateUtils.toDateTimeString(this.lastCardTime));
        ByteBufUtils.writeUTF8String(buf, DateUtils.toDateTimeString(this.lastTpTime));
        buf.writeInt(this.teleportCard);

        buf.writeInt(this.teleportRecords.size());
        for (TeleportRecord record : this.teleportRecords) {
            ByteBufUtils.writeTag(buf, record.writeToNBT());
        }

        buf.writeInt(this.homeCoordinate.size());
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.homeCoordinate.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey().getKey());
            ByteBufUtils.writeUTF8String(buf, entry.getKey().getValue());
            ByteBufUtils.writeTag(buf, entry.getValue().writeToNBT());
        }

        buf.writeInt(this.defaultHome.size());
        for (Map.Entry<String, String> entry : this.defaultHome.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey());
            ByteBufUtils.writeUTF8String(buf, entry.getValue());
        }
    }

    public static class Handler implements IMessageHandler<PlayerDataSyncPacket, IMessage> {
        @Override
        public IMessage onMessage(PlayerDataSyncPacket packet, MessageContext ctx) {
            if (ctx.side.isClient()) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    // 获取玩家并更新 Capability 数据
                    List<PlayerDataSyncPacket> packets = SplitPacket.handle(packet);
                    if (!packets.isEmpty()) {
                        ClientProxy.handleSynPlayerData(new PlayerDataSyncPacket(packets));
                    }
                });
            }
            return null;
        }
    }

    @Override
    public int getChunkSize() {
        return 100;
    }

    /**
     * 将数据包拆分为多个小包
     */
    public List<PlayerDataSyncPacket> split() {
        List<PlayerDataSyncPacket> result = new ArrayList<>();
        KeyValue<String, String>[] keyArray = this.homeCoordinate.keySet().toArray(new KeyValue[0]);
        int teleportIndex = 0;
        int homeIndex = 0;

        int totalChunks = (int) Math.ceil((double) (teleportRecords.size() + homeCoordinate.size()) / getChunkSize());

        for (int i = 0; i < totalChunks; i++) {
            PlayerDataSyncPacket packet = new PlayerDataSyncPacket(this.playerUUID, this.lastCardTime, this.lastTpTime, this.teleportCard);
            // teleportRecords
            for (int j = 0; j < getChunkSize() && teleportIndex < teleportRecords.size(); j++) {
                packet.teleportRecords.add(this.teleportRecords.get(teleportIndex));
                teleportIndex++;
            }
            // home
            for (int j = 0; j < getChunkSize() && homeIndex < keyArray.length; j++) {
                packet.homeCoordinate.put(keyArray[homeIndex], this.homeCoordinate.get(keyArray[homeIndex]));
                homeIndex++;
            }

            if (i == 0) packet.defaultHome.putAll(this.defaultHome);
            packet.setSort(i);
            result.add(packet);
        }

        int totalPackets = result.size();
        for (PlayerDataSyncPacket packet : result) {
            packet.setId(this.getId());
            packet.setTotal(totalPackets);
        }
        return result;
    }

    public IPlayerTeleportData getData() {
        IPlayerTeleportData data = new PlayerTeleportData();
        data.setLastCardTime(this.lastCardTime);
        data.setLastTpTime(this.lastTpTime);
        data.setTeleportCard(this.teleportCard);
        data.setTeleportRecords(this.teleportRecords);
        data.setHomeCoordinate(this.homeCoordinate);
        return data;
    }
}
