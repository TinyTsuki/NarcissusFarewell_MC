package xin.vanilla.narcissus.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.CombatEntry;
import net.minecraft.util.CombatTracker;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.NarcissusUtils;

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
    private void narcissus$cancelNarcissusDeathMessage(CallbackInfoReturnable<ITextComponent> cir) {
        if (!(mob instanceof ServerPlayerEntity)) return;
        if (entries.isEmpty()) return;
        if (entries.stream().noneMatch(entry -> entry.getSource().getMsgId().equals(NarcissusFarewell.MODID))) return;
        PlayerEntity player = (PlayerEntity) mob;

        Component message = Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "died_of_narcissus_" + (new Random().nextInt(4) + 1), player.getDisplayName().getString());
        Entity entity = getKiller();

        cir.setReturnValue(Component.literal("[%s] %s")
                .appendArg(entity != null ? entity.getDisplayName().getString() : Component.literal("Server"))
                .appendArg(message).toChatComponent());
    }
}
