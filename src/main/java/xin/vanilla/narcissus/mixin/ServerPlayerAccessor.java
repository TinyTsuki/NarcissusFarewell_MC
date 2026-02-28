package xin.vanilla.narcissus.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ServerPlayer.class)
public interface ServerPlayerAccessor {
    @Accessor(value = "language")
    String narcissus$language();

    @Accessor(value = "language")
    void narcissus$language(String language);
}
