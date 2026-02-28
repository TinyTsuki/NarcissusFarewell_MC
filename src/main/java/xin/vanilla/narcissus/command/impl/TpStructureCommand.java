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
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.*;

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
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "structure_biome_not_found"), structId);
            return 0;
        }
        int range = CommandUtils.getIntDefault(context, "range", ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get());
        range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_STRUCTURE, range);
        ResourceKey<Level> targetLevel = CommandUtils.getDimensionKeyDefault(context, "dimension", player.getLevel().dimension());
        boolean safe = "safe".equalsIgnoreCase(CommandUtils.getStringDefault(context, "safe", "safe"));
        int finalRange = range;
        boolean isBiome = biome != null;
        String searchingKey = isBiome ? "tp_structure_searching_biome" : "tp_structure_searching_structure";
        NarcissusUtils.sendActionBarMessage(player, Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, searchingKey, structId));
        new Thread(() -> {
            ServerLevel world = Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(targetLevel));
            Coordinate coordinate;
            if (biome != null) {
                Biome biomeFromWorld = BiomeUtils.getBiome(world, structId);
                coordinate = biomeFromWorld != null ? BiomeUtils.findNearestBiome(world, new Coordinate(player).dimension(targetLevel), biomeFromWorld, finalRange, 8) : null;
            } else {
                coordinate = StructureUtils.findNearestStructure(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(targetLevel)), new Coordinate(player).dimension(targetLevel), structId, finalRange);
            }
            if (coordinate == null) {
                String notFoundKey = isBiome ? "biome_not_found_in_range" : "structure_not_found_in_range";
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, notFoundKey), structId);
                return;
            }
            coordinate.safe(safe);
            if (CommandUtils.checkTeleportPost(player, coordinate, EnumTeleportType.TP_STRUCTURE, true))
                return;
            player.server.submit(() -> NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_STRUCTURE));
        }).start();
        return 1;
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = CommandUtils.getStringEx(context, "struct", "");
        boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
        String language = ServerConfig.DEFAULT_LANGUAGE.get();
        try {
            language = NarcissusUtils.getPlayerLanguage(context.getSource().getPlayerOrException());
        } catch (CommandSyntaxException ignored) {
        }
        Component structureTooltip = Component.trans(language, EnumI18nType.FORMAT, "tp_structure_type_structure");
        Component biomeTooltip = Component.trans(language, EnumI18nType.FORMAT, "tp_structure_type_biome");
        for (String id : StructureUtils.getAllIds()) {
            if (isInputEmpty || id.contains(input)) builder.suggest(id, structureTooltip.toTextComponent());
        }
        for (String id : BiomeUtils.getAllIds()) {
            if (isInputEmpty || id.contains(input)) builder.suggest(id, biomeTooltip.toTextComponent());
        }
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_STRUCTURE.get())
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
