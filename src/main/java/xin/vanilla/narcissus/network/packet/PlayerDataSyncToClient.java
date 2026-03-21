package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.packet.SplitPacket;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
public class PlayerDataSyncToClient extends SplitPacket
        implements SplitPacket.MergeableSplitPacket<PlayerDataSyncToClient>,
        SplitPacket.SplittableSplitPacket<PlayerDataSyncToClient> {

    private final UUID playerUUID;
    private final Date lastCardTime;
    private final Date lastTpTime;
    private final int teleportCard;
    private final List<TeleportRecord> teleportRecords;
    private final Map<KeyValue<String, String>, SafeWorldCoordinate> homeCoordinate;
    private final Map<String, String> defaultHome;

    public PlayerDataSyncToClient(UUID playerUUID, PlayerTeleportData data) {
        super();
        this.playerUUID = playerUUID;
        this.lastCardTime = data.getLastCardTime();
        this.lastTpTime = data.getLastTpTime();
        this.teleportCard = data.getTeleportCard();
        this.teleportRecords = data.getTeleportRecords();
        this.homeCoordinate = data.getHomeCoordinate();
        this.defaultHome = data.getDefaultHome();
    }

    public PlayerDataSyncToClient(FriendlyByteBuf buffer) {
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
            this.homeCoordinate.put(new KeyValue<>(buffer.readUtf(), buffer.readUtf()), SafeWorldCoordinate.fromTag(Objects.requireNonNull(buffer.readNbt())));
        }

        this.defaultHome = new HashMap<>();
        int defaultSize = buffer.readInt();
        for (int i = 0; i < defaultSize; i++) {
            this.defaultHome.put(buffer.readUtf(), buffer.readUtf());
        }
    }

    public PlayerDataSyncToClient(List<PlayerDataSyncToClient> packets) {
        super();
        this.playerUUID = packets.get(0).playerUUID;
        this.lastCardTime = packets.get(0).lastCardTime;
        this.lastTpTime = packets.get(0).lastTpTime;
        this.teleportCard = packets.get(0).teleportCard;
        this.teleportRecords = packets.stream()
                .map(PlayerDataSyncToClient::getTeleportRecords)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(TeleportRecord::getTeleportTime))
                .collect(Collectors.toList());
        this.homeCoordinate = packets.stream()
                .map(PlayerDataSyncToClient::getHomeCoordinate)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
        this.defaultHome = packets.get(0).defaultHome;
    }

    private PlayerDataSyncToClient(UUID playerUUID, Date lastCardTime, Date lastTpTime, int teleportCard) {
        super();
        this.playerUUID = playerUUID;
        this.lastCardTime = lastCardTime;
        this.lastTpTime = lastTpTime;
        this.teleportCard = teleportCard;
        this.teleportRecords = new ArrayList<>();
        this.homeCoordinate = new HashMap<>();
        this.defaultHome = new HashMap<>();
    }

    public void toBytes(FriendlyByteBuf buffer) {
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
        for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : this.homeCoordinate.entrySet()) {
            buffer.writeUtf(entry.getKey().key());
            buffer.writeUtf(entry.getKey().value());
            buffer.writeNbt(entry.getValue().toTag());
        }
        buffer.writeInt(this.defaultHome.size());
        for (Map.Entry<String, String> entry : this.defaultHome.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }
    }

    public static void handle(PlayerDataSyncToClient packet, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSide.handle(packet));
        }
        ctx.get().setPacketHandled(true);
    }

    @Override
    public int getChunkSize() {
        return 100;
    }

    @Override
    public PlayerDataSyncToClient mergePackets(List<PlayerDataSyncToClient> packets) {
        return new PlayerDataSyncToClient(packets);
    }

    @Override
    public List<PlayerDataSyncToClient> splitPacket() {
        List<PlayerDataSyncToClient> result = new ArrayList<>();
        KeyValue<String, String>[] keyArray = this.homeCoordinate.keySet().toArray(new KeyValue[0]);
        int teleportIndex = 0;
        int homeIndex = 0;

        int totalChunks = (int) Math.ceil((double) (teleportRecords.size() + homeCoordinate.size()) / getChunkSize());

        for (int i = 0; i < totalChunks; i++) {
            PlayerDataSyncToClient packet = new PlayerDataSyncToClient(this.playerUUID, this.lastCardTime, this.lastTpTime, this.teleportCard);
            for (int j = 0; j < getChunkSize() && teleportIndex < teleportRecords.size(); j++) {
                packet.teleportRecords.add(this.teleportRecords.get(teleportIndex));
                teleportIndex++;
            }
            for (int j = 0; j < getChunkSize() && homeIndex < keyArray.length; j++) {
                packet.homeCoordinate.put(keyArray[homeIndex], this.homeCoordinate.get(keyArray[homeIndex]));
                homeIndex++;
            }

            if (i == 0) {
                packet.defaultHome.putAll(this.defaultHome);
            }
            packet.setSort(i);
            result.add(packet);
        }

        int totalPackets = result.size();
        for (PlayerDataSyncToClient packet : result) {
            packet.setId(this.getId());
            packet.setTotal(totalPackets);
        }
        if (result.isEmpty()) {
            PlayerDataSyncToClient packet = new PlayerDataSyncToClient(this.playerUUID, this.lastCardTime, this.lastTpTime, this.teleportCard);
            packet.setSort(0);
            packet.setId(this.getId());
            packet.setTotal(1);
            result.add(packet);
        }
        return result;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientSide {
        public static final Logger LOGGER = LogManager.getLogger();

        public static void handle(PlayerDataSyncToClient packet) {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                try {
                    PlayerTeleportData clientData = PlayerTeleportData.getData(player);
                    clientData.copyFrom(getData(packet));
                    LOGGER.debug("Client: Player data received successfully.");
                } catch (Exception ignored) {
                    LOGGER.debug("Client: Player data received failed.");
                }
            }
        }

        public static PlayerTeleportData getData(PlayerDataSyncToClient packet) {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) {
                return null;
            }
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            if (data == null) {
                return null;
            }

            data.setLastCardTime(packet.lastCardTime);
            data.setLastTpTime(packet.lastTpTime);
            data.setTeleportCard(packet.teleportCard);
            data.setTeleportRecords(packet.teleportRecords);
            data.setHomeCoordinate(packet.homeCoordinate);
            return data;
        }
    }
}
