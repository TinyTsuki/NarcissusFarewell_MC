package xin.vanilla.narcissus.network;

import lombok.Getter;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.narcissus.capability.IPlayerTeleportData;
import xin.vanilla.narcissus.capability.PlayerTeleportData;
import xin.vanilla.narcissus.capability.TeleportRecord;
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

    public PlayerDataSyncPacket(UUID playerUUID, IPlayerTeleportData data) {
        super();
        this.playerUUID = playerUUID;
        this.lastCardTime = data.getLastCardTime();
        this.lastTpTime = data.getLastTpTime();
        this.teleportCard = data.getTeleportCard();
        this.teleportRecords = data.getTeleportRecords();
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
    }

    private PlayerDataSyncPacket(UUID playerUUID, Date lastCardTime, Date lastTpTime, int teleportCard) {
        super();
        this.playerUUID = playerUUID;
        this.lastCardTime = lastCardTime;
        this.lastTpTime = lastTpTime;
        this.teleportCard = teleportCard;
        this.teleportRecords = new ArrayList<>();
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
        for (int i = 0, index = 0; i < teleportRecords.size() / getChunkSize() + 1; i++) {
            PlayerDataSyncPacket packet = new PlayerDataSyncPacket(this.playerUUID, this.lastCardTime, this.lastTpTime, this.teleportCard);
            for (int j = 0; j < getChunkSize(); j++) {
                if (index >= teleportRecords.size()) break;
                packet.teleportRecords.add(this.teleportRecords.get(index));
                index++;
            }
            packet.setId(this.getId());
            packet.setSort(i);
            result.add(packet);
        }
        result.forEach(packet -> packet.setTotal(result.size()));
        return result;
    }

    public IPlayerTeleportData getData() {
        IPlayerTeleportData data = new PlayerTeleportData();
        data.setLastCardTime(this.lastCardTime);
        data.setLastTpTime(this.lastTpTime);
        data.setTeleportCard(this.teleportCard);
        data.setTeleportRecords(this.teleportRecords);
        return data;
    }
}
