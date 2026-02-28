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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.registries.ForgeRegistries;
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
        StructureFeature<?> structure = NarcissusUtils.getStructure(structId);
        Biome biome = NarcissusUtils.getBiome(structId);
        if (structure == null && biome == null) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "structure_biome_not_found"), structId);
            return 0;
        }
        int range = CommandUtils.getIntDefault(context, "range", ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get());
        range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_STRUCTURE, range);
        ResourceKey<Level> targetLevel = CommandUtils.getDimensionKeyDefault(context, "dimension", player.getLevel().dimension());
        boolean safe = "safe".equalsIgnoreCase(CommandUtils.getStringDefault(context, "safe", "safe"));
        int finalRange = range;
        NarcissusUtils.sendActionBarMessage(player, Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "tp_structure_searching"));
        new Thread(() -> {
            Coordinate coordinate;
            if (biome != null) {
                coordinate = NarcissusUtils.findNearestBiome(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(targetLevel)), new Coordinate(player).dimension(targetLevel), biome, finalRange, 8);
            } else {
                coordinate = NarcissusUtils.findNearestStruct(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(targetLevel)), new Coordinate(player).dimension(targetLevel), structure, finalRange);
            }
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "structure_biome_not_found_in_range"), structId);
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
        String input = CommandUtils.getStringEmpty(context, "rules");
        boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
        ForgeRegistries.STRUCTURE_FEATURES.getKeys().stream()
                .filter(resourceLocation -> isInputEmpty || resourceLocation.toString().contains(input))
                .forEach(location -> builder.suggest(location.toString()));
        ForgeRegistries.BIOMES.getValues().stream()
                .filter(resourceLocation -> isInputEmpty || resourceLocation.toString().contains(input))
                .forEach(biome -> builder.suggest(biome.toString()));
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
