package xin.vanilla.narcissus.mixin;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;

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

    @Inject(method = "getDeathMessage", at = @At("RETURN"), cancellable = true)
    private void narcissus$cancelNarcissusDeathMessage(CallbackInfoReturnable<net.minecraft.network.chat.Component> cir) {
        if (!(mob instanceof ServerPlayer player)) return;
        if (entries.isEmpty()) return;
        CombatEntry combatEntry = entries.stream().filter(entry -> entry.source().getMsgId().equals(NarcissusFarewell.MODID)).findFirst().orElse(null);
        if (combatEntry == null) return;


        Component message = NarcissusComponent.get().transAuto("died_of_narcissus_" + (new Random().nextInt(4) + 1), player.getDisplayName().getString());
        Entity entity = combatEntry.source().getEntity();

        cir.setReturnValue(NarcissusComponent.get().literal("[%s] %s")
                .appendArg(entity != null ? entity.getDisplayName().getString() : NarcissusComponent.get().literal("Server"))
                .appendArg(message).toChat());
    }
}
