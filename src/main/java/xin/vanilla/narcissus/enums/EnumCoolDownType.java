package xin.vanilla.narcissus.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum EnumCoolDownType {
    COMMON,
    INDIVIDUAL,
    MIXED,
    ;


    public static List<String> names() {
        return Arrays.stream(EnumCoolDownType.values()).map(Enum::name).collect(Collectors.toList());
    }
}
