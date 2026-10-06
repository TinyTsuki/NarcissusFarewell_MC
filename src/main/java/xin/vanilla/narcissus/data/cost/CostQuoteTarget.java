package xin.vanilla.narcissus.data.cost;

import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Value identity only; server records determine the destination and actors.
 */
@Value
@Accessors(fluent = true)
public class CostQuoteTarget {
    public static final UUID NO_OWNER = new UUID(0, 0);
    Kind kind;
    EnumTeleportType teleportType;
    UUID owner;
    String dimension;
    String name;
    double x, y, z;
    String historyTime;
    EnumTeleportType historyType;

    public enum Kind implements IEnumDescribable {
        HOME, STAGE, HISTORY, PLAYER, COORDINATE, UNKNOWN;

        @Override
        public Component enumDescription() {
            return NarcissusComponent.get().trans("word.narcissus_farewell.enum_cost_quote_target_" + name().toLowerCase(Locale.ROOT));
        }
    }

    public CostQuoteTarget(Kind kind, EnumTeleportType type, UUID owner, String dimension, String name,
                           double x, double y, double z, String historyTime, EnumTeleportType historyType) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.teleportType = Objects.requireNonNull(type, "type");
        this.owner = Objects.requireNonNull(owner, "owner");
        checkedBytes(dimension);
        checkedBytes(name);
        checkedBytes(historyTime);
        this.dimension = dimension;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.historyTime = historyTime;
        this.historyType = Objects.requireNonNull(historyType, "historyType");
        if (type.toCommandType() == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            invalid();
        boolean positioned = kind == Kind.HISTORY || kind == Kind.COORDINATE;
        if (!positioned && (x != 0 || y != 0 || z != 0)) invalid();
        if (kind != Kind.HISTORY && (!historyTime.isEmpty() || historyType != EnumTeleportType.OTHER)) invalid();
        boolean owned = kind == Kind.HOME || kind == Kind.HISTORY || kind == Kind.PLAYER;
        if (owned == NO_OWNER.equals(owner)) invalid();
        boolean dimensional = kind == Kind.HOME || kind == Kind.STAGE || positioned;
        if (dimensional ? !dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") : !dimension.isEmpty()) invalid();
        if (kind == Kind.HOME || kind == Kind.STAGE) {
            if (name.isEmpty() || type != (kind == Kind.HOME ? EnumTeleportType.TP_HOME : EnumTeleportType.TP_STAGE))
                invalid();
        } else if (!name.isEmpty()) invalid();
        if (kind == Kind.HISTORY && (!historyTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
                || type != EnumTeleportType.TP_BACK && type != EnumTeleportType.TP_GRAVE)) invalid();
        if (kind == Kind.COORDINATE && type != EnumTeleportType.TP_COORDINATE) invalid();
        if (kind == Kind.PLAYER && type != EnumTeleportType.TP_ASK && type != EnumTeleportType.TP_HERE) invalid();
    }

    public static CostQuoteTarget home(UUID owner, String dimension, String name) {
        return new CostQuoteTarget(Kind.HOME, EnumTeleportType.TP_HOME, owner, dimension, name, 0, 0, 0, "", EnumTeleportType.OTHER);
    }

    public static CostQuoteTarget stage(String dimension, String name) {
        return new CostQuoteTarget(Kind.STAGE, EnumTeleportType.TP_STAGE, NO_OWNER, dimension, name, 0, 0, 0, "", EnumTeleportType.OTHER);
    }

    public static CostQuoteTarget history(UUID owner, EnumTeleportType operation, long timeMillis, EnumTeleportType recordType,
                                          String dimension, double x, double y, double z) {
        return new CostQuoteTarget(Kind.HISTORY, operation, owner, dimension, "", x, y, z, DateUtils.toDateTimeString(new Date(timeMillis)), recordType);
    }

    public static CostQuoteTarget player(UUID target, EnumTeleportType type) {
        return new CostQuoteTarget(Kind.PLAYER, type, target, "", "", 0, 0, 0, "", EnumTeleportType.OTHER);
    }

    public static CostQuoteTarget coordinate(String dimension, double x, double y, double z) {
        return new CostQuoteTarget(Kind.COORDINATE, EnumTeleportType.TP_COORDINATE, NO_OWNER, dimension, "", x, y, z, "", EnumTeleportType.OTHER);
    }

    public static CostQuoteTarget unknown(EnumTeleportType type) {
        return new CostQuoteTarget(Kind.UNKNOWN, type, NO_OWNER, "", "", 0, 0, 0, "", EnumTeleportType.OTHER);
    }

    public static byte[] checkedBytes(String text) {
        if (text == null || text.length() > 256)
            throw new IllegalArgumentException("Quote text exceeds 256 characters");
        try {
            ByteBuffer bytes = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));
            if (bytes.remaining() > 1024) throw new IllegalArgumentException("Quote text exceeds 1024 UTF-8 bytes");
            byte[] result = new byte[bytes.remaining()];
            bytes.get(result);
            return result;
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Malformed quote text", e);
        }
    }

    private static void invalid() {
        throw new IllegalArgumentException("Invalid quote target identity");
    }
}
