package xin.vanilla.narcissus.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.EntityPlayerMP;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.DateUtils;

import java.util.Date;
import java.util.Random;

@Accessors(chain = true)
public class TeleportRequest {
    private final int id = new Random().nextInt(Integer.MAX_VALUE);
    @Getter
    @Setter
    private EntityPlayerMP requester;
    @Getter
    @Setter
    private EntityPlayerMP target;
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

    public TeleportRequest setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
        this.expireTime = requestTime.getTime() + ServerConfig.TELEPORT_REQUEST_EXPIRE_TIME * 1000L;
        return this;
    }

    public String getRequestId() {
        return DateUtils.toDateTimeInt(this.requestTime) + "_" + this.id;
    }
}
