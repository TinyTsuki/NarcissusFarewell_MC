package xin.vanilla.narcissus.enums;

public enum EOperationType {
    ADD,
    SET,
    REMOVE,
    DEL,
    LIST,
    GET,
    CLEAR;

    public static EOperationType fromString(String type) {
        try {
            return EOperationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operation type: " + type);
        }
    }
}
