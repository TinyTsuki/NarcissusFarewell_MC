package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.util.ConfigEditPermission;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;

public final class CostReloadCommand {
    private CostReloadCommand() {
    }

    public static boolean permitted(CommandSourceStack source) {
        return source.getEntity() == null ? source.hasPermission(4)
                : source.getEntity() instanceof ServerPlayer && ConfigEditPermission.canAccessServerConfigEditor(source.getEntity());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("cost").requires(CostReloadCommand::permitted)
                .then(Commands.literal("reload").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (!permitted(source)) return 0;
                    NarcissusCostService service = NarcissusCostService.get();
                    if (service == null) {
                        report(source, false);
                        return 0;
                    }
                    try {
                        service.reload().whenComplete((success, error) -> source.getServer().execute(() -> {
                            if (source.getEntity() instanceof ServerPlayer
                                    && source.getServer().getPlayerList().getPlayer(source.getEntity().getUUID()) != source.getEntity())
                                return;
                            report(source, error == null && Boolean.TRUE.equals(success));
                        }));
                    } catch (RuntimeException error) {
                        report(source, false);
                        return 0;
                    }
                    return 1;
                }));
    }

    private static void report(CommandSourceStack source, boolean success) {
        MessageUtils.sendMessage(source, false, NarcissusComponent.get().transAuto(success ? "cost_reload_success" : "cost_reload_failed"));
    }
}
