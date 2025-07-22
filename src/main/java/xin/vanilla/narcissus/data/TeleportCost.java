package xin.vanilla.narcissus.data;

import lombok.Data;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.enums.EnumCostType;

@Data
@Accessors(chain = true)
public class TeleportCost {
    private EnumCostType type;
    private int num;
    private double rate;
    private String conf;
    private int upper;
    private int lower;
    private String exp;

    public TeleportCost setType(String type) {
        this.type = EnumCostType.valueOf(type);
        return this;
    }
}
