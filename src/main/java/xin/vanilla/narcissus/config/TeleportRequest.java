package xin.vanilla.narcissus.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.DateUtils;

import java.util.Date;
import java.util.Random;

@Accessors(chain = true)
public class TeleportRequest {
    private final int id = new Random().nextInt(Integer.MAX_VALUE);
    @Getter
    @Setter
    private ServerPlayerEntity requester;
    @Getter
    @Setter
    private ServerPlayerEntity target;
    @Getter
    @Setter
    private Date requestTime;
    @Getter
    @Setter
    private ETeleportType teleportType;
    @Getter
    @Setter
    private boolean safe;

    public String getRequestId() {
        return DateUtils.toDateTimeInt(this.requestTime) + "_" + this.id;
    }
}
