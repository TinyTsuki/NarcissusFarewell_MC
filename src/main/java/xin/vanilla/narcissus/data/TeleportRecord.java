package xin.vanilla.narcissus.data;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundNBT;
import xin.vanilla.narcissus.enums.EnumTeleportType;
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
    private EnumTeleportType teleportType;
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
        this.teleportType = EnumTeleportType.TP_ASK;
        this.before = new Coordinate();
        this.after = new Coordinate();
    }

    /**
     * 序列化
     */
    public CompoundNBT writeToNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putString("teleportTime", DateUtils.toDateTimeString(teleportTime));
        tag.putString("teleportType", teleportType.name());
        tag.put("before", before.writeToNBT());
        tag.put("after", after.writeToNBT());
        return tag;
    }

    /**
     * 反序列化
     */
    public static TeleportRecord readFromNBT(CompoundNBT tag) {
        TeleportRecord record = new TeleportRecord();
        record.teleportTime = DateUtils.format(tag.getString("teleportTime"));
        record.teleportType = EnumTeleportType.valueOf(tag.getString("teleportType"));
        record.before = Coordinate.readFromNBT(tag.getCompound("before"));
        record.after = Coordinate.readFromNBT(tag.getCompound("after"));
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
