package xin.vanilla.narcissus.mixin;

import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TemptGoal.class, remap = false)
public interface TemptGoalAccessor {
    @Accessor(value = "player")
    PlayerEntity narcissus$player();
}
