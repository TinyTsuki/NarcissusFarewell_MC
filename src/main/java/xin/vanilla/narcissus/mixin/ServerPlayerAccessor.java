package xin.vanilla.narcissus.mixin;

import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ServerPlayerEntity.class, remap = false)
public interface ServerPlayerAccessor {
    @Accessor(value = "language")
    String narcissus$language();

    @Accessor(value = "language")
    void narcissus$language(String language);
}
