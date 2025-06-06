package xin.vanilla.narcissus.data;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.DateUtils;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
public class TeleportRecord implements Serializable, Cloneable {
    /**
     * 传送时间
     */
    @NonNull
    private Date teleportTime;
    /**
     * 传送类型
     */
    @NonNull
    private ETeleportType teleportType;
    /**
     * 传送前的坐标
     */
    private Coordinate before;
    /**
     * 传送后的坐标
     */
    private Coordinate after;

    public TeleportRecord() {
        this.teleportTime = new Date();
        this.teleportType = ETeleportType.TP_ASK;
        this.before = new Coordinate();
        this.after = new Coordinate();
    }

    /**
     * 序列化
     */
    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("teleportTime", DateUtils.toDateTimeString(teleportTime));
        tag.putString("teleportType", teleportType.name());
        tag.put("before", before.writeToNBT());
        tag.put("after", after.writeToNBT());
        return tag;
    }

    /**
     * 反序列化
     */
    public static TeleportRecord readFromNBT(CompoundTag tag) {
        TeleportRecord record = new TeleportRecord();
        record.teleportTime = DateUtils.format(tag.getString("teleportTime").orElse(DateUtils.toDateTimeString(new Date(0))));
        record.teleportType = ETeleportType.valueOf(tag.getString("teleportType").orElse(ETeleportType.OTHER.name()));
        record.before = Coordinate.readFromNBT(tag.getCompound("before").orElse(new Coordinate().writeToNBT()));
        record.after = Coordinate.readFromNBT(tag.getCompound("after").orElse(new Coordinate().writeToNBT()));
        return record;
    }

    @Override
    public TeleportRecord clone() {
        try {
            TeleportRecord cloned = (TeleportRecord) super.clone();
            cloned.teleportTime = (Date) this.teleportTime.clone();
            cloned.teleportType = this.teleportType;
            cloned.before = this.before.clone();
            cloned.after = this.after.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
