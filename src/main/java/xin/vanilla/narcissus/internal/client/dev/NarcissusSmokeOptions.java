package xin.vanilla.narcissus.internal.client.dev;

import lombok.Value;
import lombok.experimental.Accessors;

import java.util.function.Function;

/**
 * 开发烟测开关。独立于 Minecraft 类型，便于各版本保持相同契约。
 */
@Value
@Accessors(fluent = true)
class NarcissusSmokeOptions {
    private static final String PREFIX = "narcissus.uiSmoke";

    boolean enabled;
    boolean exitOnFinish;
    boolean teleportEnabled;
    String worldName;

    static NarcissusSmokeOptions from(Function<String, String> properties) {
        return new NarcissusSmokeOptions(
                booleanValue(properties.apply(PREFIX)),
                booleanValue(properties.apply(PREFIX + ".exitOnFinish")),
                booleanValue(properties.apply(PREFIX + ".teleport")),
                stringValue(properties.apply(PREFIX + ".world")));
    }

    private static boolean booleanValue(String value) {
        return Boolean.parseBoolean(stringValue(value));
    }

    private static String stringValue(String value) {
        return value == null ? "" : value.trim();
    }

}
