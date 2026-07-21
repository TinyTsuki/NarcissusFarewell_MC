package xin.vanilla.narcissus.internal.network;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;

/**
 * 以 SNBT 隔离不同版本的原生 NBT 类型，避免其进入 Banira 公共 buffer 契约。
 */
public final class NarcissusNbtPacketCodec {
    private static final int MAX_SNBT_LENGTH = 1_048_576;

    private NarcissusNbtPacketCodec() {
    }

    public static void write(BaniraPacketBuffer buffer, CompoundTag tag) {
        buffer.writeUtf(serialize(tag), MAX_SNBT_LENGTH);
    }

    public static CompoundTag read(BaniraPacketBuffer buffer) {
        return deserialize(buffer.readUtf(MAX_SNBT_LENGTH));
    }

    public static String serialize(CompoundTag tag) {
        return tag == null ? "{}" : tag.toString();
    }

    public static CompoundTag deserialize(String value) {
        try {
            return TagParser.parseTag(value == null || value.isEmpty() ? "{}" : value);
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException("Invalid Narcissus packet SNBT", e);
        }
    }
}
