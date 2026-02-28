package xin.vanilla.narcissus.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Accessor("DATA_HEALTH_ID")
    DataParameter<Float> narcissus$dataHealthId();

    @Invoker("die")
    void narcissus$invokeDie(DamageSource damageSource);
}
