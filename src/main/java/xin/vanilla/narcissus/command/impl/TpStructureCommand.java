package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.common.util.BiomeUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.common.util.StructureUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class TpStructureCommand {
    private TpStructureCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_STRUCTURE)) return 0;
        ResourceLocation structId = ResourceLocationArgument.getId(context, "struct");
        boolean hasStructure = StructureUtils.hasStructure(structId);
        Biome biome = BiomeUtils.getBiome(structId);
        if (!hasStructure && biome == null) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("structure_biome_not_found", structId));
            return 0;
        }
        int range = xin.vanilla.banira.common.util.CommandUtils.getIntDefault(context, "range", CommonConfig.get().general().teleportRandomDistanceLimit());
        range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_STRUCTURE, range);
        ResourceKey<Level> targetLevel = xin.vanilla.banira.common.util.CommandUtils.getDimensionKeyDefault(context, "dimension", player.getLevel().dimension());
        boolean safe = "safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "safe", "safe"));
        int finalRange = range;
        boolean isBiome = biome != null;
        String searchingKey = isBiome ? "tp_structure_searching_biome" : "tp_structure_searching_structure";
        MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto(searchingKey, structId));
        new Thread(() -> {
            ServerLevel world = Objects.requireNonNull(BaniraCodex.serverInstance().key().getLevel(targetLevel));
            SafeWorldCoordinate safeWorldCoordinate;
            if (biome != null) {
                Biome biomeFromWorld = BiomeUtils.getBiome(world, structId);
                if (biomeFromWorld != null) {
                    WorldCoordinate start = new WorldCoordinate(player).dimension(targetLevel);
                    WorldCoordinate found = BiomeUtils.findNearestBiome(world, start, biomeFromWorld, finalRange, 8);
                    safeWorldCoordinate = found != null
                            ? new SafeWorldCoordinate(found.x(), found.y(), found.z(), found.yaw(), found.pitch(), found.dimension()).safe(true)
                            : null;
                } else {
                    safeWorldCoordinate = null;
                }
            } else {
                ServerLevel structureWorld = Objects.requireNonNull(BaniraCodex.serverInstance().key().getLevel(targetLevel));
                WorldCoordinate structStart = new WorldCoordinate(player).dimension(targetLevel);
                WorldCoordinate foundStruct = StructureUtils.findNearestStructure(structureWorld, structStart, structId, finalRange);
                safeWorldCoordinate = foundStruct != null
                        ? new SafeWorldCoordinate(foundStruct.x(), foundStruct.y(), foundStruct.z(), foundStruct.yaw(), foundStruct.pitch(), foundStruct.dimension()).safe(true)
                        : null;
            }
            if (safeWorldCoordinate == null) {
                String notFoundKey = isBiome ? "biome_not_found_in_range" : "structure_not_found_in_range";
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto(notFoundKey, structId));
                return;
            }
            safeWorldCoordinate.safe(safe);
            if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_STRUCTURE, true))
                return;
            player.server.submit(() -> NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_STRUCTURE));
        }).start();
        return 1;
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = xin.vanilla.banira.common.util.CommandUtils.getStringEx(context, "struct", "");
        boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
        String language = CommonConfig.get().general().defaultLanguage();
        try {
            language = NarcissusLang.getPlayerLanguage(context.getSource().getPlayerOrException());
        } catch (CommandSyntaxException ignored) {
        }
        Component structureTooltip = NarcissusComponent.get().transAuto("tp_structure_type_structure");
        Component biomeTooltip = NarcissusComponent.get().transAuto("tp_structure_type_biome");
        for (String id : StructureUtils.getAllIds()) {
            if (isInputEmpty || id.contains(input)) builder.suggest(id, structureTooltip.toVanilla(language));
        }
        for (String id : BiomeUtils.getAllIds()) {
            if (isInputEmpty || id.contains(input)) builder.suggest(id, biomeTooltip.toVanilla(language));
        }
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpStructure())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_STRUCTURE))
                .then(Commands.argument("struct", ResourceLocationArgument.id())
                        .suggests(TpStructureCommand::suggestion)
                        .executes(TpStructureCommand::execute)
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .suggests(CommandUtils::rangeSuggestion)
                                .executes(TpStructureCommand::execute)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(TpStructureCommand::execute)
                                        .then(Commands.argument("safe", StringArgumentType.word())
                                                .suggests(CommandUtils::safeSuggestion)
                                                .executes(TpStructureCommand::execute)
                                        )
                                )
                        )
                );
    }
}
