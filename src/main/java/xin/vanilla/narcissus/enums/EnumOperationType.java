package xin.vanilla.narcissus.enums;

public enum EnumOperationType {
    ADD,
    SET,
    REMOVE,
    DEL,
    LIST,
    GET,
    CLEAR,
    ;

    public static EnumOperationType valueOfEx(Object obj) {
        if (obj instanceof EnumOperationType) return (EnumOperationType) obj;
        if (obj instanceof String) {
            for (EnumOperationType value : values()) {
                if (value.name().equalsIgnoreCase((String) obj)) {
                    return value;
                }
            }
        }
        return null;
    }
}
