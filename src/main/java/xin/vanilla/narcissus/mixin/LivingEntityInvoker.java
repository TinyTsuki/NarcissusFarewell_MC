package xin.vanilla.narcissus.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Accessor("DATA_HEALTH_ID")
    EntityDataAccessor<Float> narcissus$dataHealthId();

    @Invoker("die")
    void narcissus$invokeDie(DamageSource damageSource);
}
