package xin.vanilla.narcissus.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代价类型
 */
public enum EnumCostType {
    NONE,
    EXP_POINT,
    EXP_LEVEL,
    HEALTH,
    HUNGER,
    ITEM,
    COMMAND,
    ;


    public static List<String> names() {
        return Arrays.stream(EnumCostType.values()).map(Enum::name).collect(Collectors.toList());
    }
}
