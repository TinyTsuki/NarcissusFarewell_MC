package xin.vanilla.narcissus.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;


@Mixin(CombatTracker.class)
public abstract class CombatTrackerMixin {

    @Shadow
    @Final
    private LivingEntity mob;

    @Shadow
    @Final
    private List<CombatEntry> entries;

    @Shadow
    @Nullable
    public abstract LivingEntity getKiller();

    @Inject(method = "getDeathMessage", at = @At("RETURN"), cancellable = true)
    private void narcissus$cancelNarcissusDeathMessage(CallbackInfoReturnable<Component> cir) {
        if (!(mob instanceof ServerPlayer)) return;
        if (entries.isEmpty()) return;
        if (entries.stream().noneMatch(entry -> entry.getSource().getMsgId().equals(NarcissusFarewell.MODID))) return;
        Player player = (Player) mob;

        xin.vanilla.banira.common.data.Component message = NarcissusComponent.get().transAuto(
                "died_of_narcissus_" + (new Random().nextInt(4) + 1), player.getDisplayName().getString());
        Entity entity = getKiller();

        cir.setReturnValue(NarcissusComponent.get().literal("[%s] %s")
                .appendArg(entity != null ? entity.getDisplayName().getString() : NarcissusComponent.get().literal("Server"))
                .appendArg(message).toChat());
    }
}
