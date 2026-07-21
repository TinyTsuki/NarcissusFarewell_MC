package xin.vanilla.narcissus.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.Date;
import java.util.Random;

@Accessors(chain = true)
public class TeleportRequest {
    private final int id = new Random().nextInt(Integer.MAX_VALUE);
    @Getter
    @Setter
    private ServerPlayer requester;
    @Getter
    @Setter
    private ServerPlayer target;
    @Getter
    private Date requestTime;
    @Getter
    @Setter
    private EnumTeleportType teleportType;
    @Getter
    @Setter
    private boolean safe;
    @Getter
    private long expireTime;
    @Getter
    @Setter
    private boolean ignore;

    public TeleportRequest setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
        this.expireTime = requestTime.getTime() + CommonConfig.get().general().teleportRequestExpireTime() * 1000L;
        return this;
    }

    public String getRequestId() {
        return DateUtils.toDateTimeInt(this.requestTime) + "_" + this.id;
    }
}
