package xin.vanilla.narcissus.config;

import lombok.Data;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.enums.ECostType;

@Data
@Accessors(chain = true)
public class TeleportCost {
    private ECostType type;
    private int num;
    private double rate;
    private String conf;
}
