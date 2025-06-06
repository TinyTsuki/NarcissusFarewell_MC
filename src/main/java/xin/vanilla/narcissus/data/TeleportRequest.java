package xin.vanilla.narcissus.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.DateUtils;

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
    private ETeleportType teleportType;
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
        this.expireTime = requestTime.getTime() + ServerConfig.TELEPORT_REQUEST_EXPIRE_TIME.get() * 1000;
        return this;
    }

    public String getRequestId() {
        return DateUtils.toDateTimeInt(this.requestTime) + "_" + this.id;
    }
}
