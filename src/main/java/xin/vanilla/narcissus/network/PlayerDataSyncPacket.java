package xin.vanilla.narcissus.network;

import lombok.Getter;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.util.CollectionUtils;
import xin.vanilla.narcissus.util.DateUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
public class PlayerDataSyncPacket extends SplitPacket {
    private final UUID playerUUID;
    private final Date lastCardTime;
    private final Date lastTpTime;
    private final int teleportCard;
    private final List<TeleportRecord> teleportRecords;
    private final Map<KeyValue<String, String>, Coordinate> homeCoordinate;
    private final Map<String, String> defaultHome;

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

    public PlayerDataSyncPacket(PacketBuffer buffer) {
        super(buffer);
        this.playerUUID = buffer.readUUID();
        this.lastCardTime = DateUtils.format(buffer.readUtf());
        this.lastTpTime = DateUtils.format(buffer.readUtf());
        this.teleportCard = buffer.readInt();

        this.teleportRecords = new ArrayList<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            this.teleportRecords.add(TeleportRecord.readFromNBT(Objects.requireNonNull(buffer.readNbt())));
        }

        this.homeCoordinate = new HashMap<>();
        int homeSize = buffer.readInt();
        for (int i = 0; i < homeSize; i++) {
            this.homeCoordinate.put(new KeyValue<>(buffer.readUtf(), buffer.readUtf()), Coordinate.readFromNBT(Objects.requireNonNull(buffer.readNbt())));
        }

        this.defaultHome = new HashMap<>();
        int defaultSize = buffer.readInt();
        for (int i = 0; i < defaultSize; i++) {
            this.defaultHome.put(buffer.readUtf(), buffer.readUtf());
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

    public void toBytes(PacketBuffer buffer) {
        super.toBytes(buffer);
        buffer.writeUUID(playerUUID);
        buffer.writeUtf(DateUtils.toDateTimeString(this.lastCardTime));
        buffer.writeUtf(DateUtils.toDateTimeString(this.lastTpTime));
        buffer.writeInt(this.teleportCard);
        buffer.writeInt(this.teleportRecords.size());
        for (TeleportRecord record : this.teleportRecords) {
            buffer.writeNbt(record.writeToNBT());
        }
        buffer.writeInt(this.homeCoordinate.size());
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.homeCoordinate.entrySet()) {
            buffer.writeUtf(entry.getKey().getKey());
            buffer.writeUtf(entry.getKey().getValue());
            buffer.writeNbt(entry.getValue().writeToNBT());
        }
        buffer.writeInt(this.defaultHome.size());
        for (Map.Entry<String, String> entry : this.defaultHome.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }
    }

    public static void handle(PlayerDataSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                // 获取玩家并更新 Capability 数据
                List<PlayerDataSyncPacket> packets = SplitPacket.handle(packet);
                if (CollectionUtils.isNotNullOrEmpty(packets)) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProxy.handleSynPlayerData(new PlayerDataSyncPacket(packets)));
                }
            }
        });
        ctx.get().setPacketHandled(true);
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
        if (result.isEmpty()) {
            PlayerDataSyncPacket packet = new PlayerDataSyncPacket(this.playerUUID, this.lastCardTime, this.lastTpTime, this.teleportCard);
            packet.setSort(0);
            packet.setId(this.getId());
            packet.setTotal(1);
            result.add(packet);
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
