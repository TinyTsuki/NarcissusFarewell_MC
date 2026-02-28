package xin.vanilla.narcissus.mixin;

import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TemptGoal.class, remap = false)
public interface TemptGoalAccessor {
    @Accessor(value = "player")
    Player narcissus$player();
}
