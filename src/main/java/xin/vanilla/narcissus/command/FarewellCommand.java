package xin.vanilla.narcissus.command;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CustomConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.*;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.util.*;

import java.util.*;
import java.util.stream.Collectors;

public class FarewellCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    public static List<KeyValue<String, EnumCommandType>> HELP_MESSAGE;

    private static void refreshHelpMessage() {
        HELP_MESSAGE = Arrays.stream(EnumCommandType.values())
                .map(type -> {
                    String command = NarcissusUtils.getCommand(type);
                    if (StringUtils.isNotNullOrEmpty(command)) {
                        return new KeyValue<>(command, type);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(command -> !command.getValue().isIgnore())
                .sorted(Comparator.comparing(command -> command.getValue().getSort()))
                .collect(Collectors.toList());
    }

    /**
     * 注册命令到命令调度器
     *
     * @param dispatcher 命令调度器，用于管理服务器中的所有命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 刷新帮助信息
        refreshHelpMessage();

        Command<CommandSourceStack> helpCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String command;
            int page;
            try {
                command = StringArgumentType.getString(context, "command");
                page = StringUtils.toInt(command);
            } catch (IllegalArgumentException ignored) {
                command = "";
                page = 1;
            }
            Component helpInfo;
            if (page > 0) {
                int pages = (int) Math.ceil((double) HELP_MESSAGE.size() / ServerConfig.HELP_INFO_NUM_PER_PAGE.get());
                helpInfo = Component.literal(StringUtils.format(ServerConfig.HELP_HEADER.get() + "\n", page, pages));
                for (int i = 0; (page - 1) * ServerConfig.HELP_INFO_NUM_PER_PAGE.get() + i < HELP_MESSAGE.size() && i < ServerConfig.HELP_INFO_NUM_PER_PAGE.get(); i++) {
                    KeyValue<String, EnumCommandType> keyValue = HELP_MESSAGE.get((page - 1) * ServerConfig.HELP_INFO_NUM_PER_PAGE.get() + i);
                    Component commandTips;
                    if (keyValue.getValue().name().toLowerCase().contains("concise")) {
                        commandTips = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.COMMAND, "concise", NarcissusUtils.getCommand(keyValue.getValue().replaceConcise()));
                    } else {
                        commandTips = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.COMMAND, keyValue.getValue().name().toLowerCase());
                    }
                    commandTips.setColor(EnumMCColor.GRAY.getColor());
                    String com = "/" + keyValue.getKey();
                    helpInfo.append(Component.literal(com)
                                    .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "click_to_suggest").toTextComponent()))
                            )
                            .append(new Component(" -> ").setColor(EnumMCColor.YELLOW.getColor()))
                            .append(commandTips);
                    if (i != HELP_MESSAGE.size() - 1) {
                        helpInfo.append("\n");
                    }
                }
                // 添加翻页按钮
                if (pages > 1) {
                    helpInfo.append("\n");
                    Component prevButton = Component.literal("<<< ");
                    if (page > 1) {
                        prevButton.setColor(EnumMCColor.AQUA.getColor())
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        String.format("/%s %s %d", NarcissusUtils.getCommandPrefix(), "help", page - 1)))
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "previous_page").toTextComponent()));
                    } else {
                        prevButton.setColor(EnumMCColor.DARK_AQUA.getColor());
                    }
                    helpInfo.append(prevButton);

                    helpInfo.append(Component.literal(String.format(" %s/%s "
                                    , StringUtils.padOptimizedLeft(page, String.valueOf(pages).length(), " ")
                                    , pages))
                            .setColor(EnumMCColor.WHITE.getColor()));

                    Component nextButton = Component.literal(" >>>");
                    if (page < pages) {
                        nextButton.setColor(EnumMCColor.AQUA.getColor())
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        String.format("/%s %s %d", NarcissusUtils.getCommandPrefix(), "help", page + 1)))
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "next_page").toTextComponent()));
                    } else {
                        nextButton.setColor(EnumMCColor.DARK_AQUA.getColor());
                    }
                    helpInfo.append(nextButton);
                }
            } else {
                EnumCommandType type = EnumCommandType.valueOf(command);
                helpInfo = Component.empty();
                String com = "/" + NarcissusUtils.getCommand(type);
                helpInfo.append(Component.literal(com)
                                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                        , Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "click_to_suggest").toTextComponent()))
                        )
                        .append("\n")
                        .append(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.COMMAND, command.toLowerCase() + "_detail").setColor(EnumMCColor.GRAY.getColor()));
            }
            NarcissusUtils.sendMessage(player, helpInfo);
            return 1;
        };
        SuggestionProvider<CommandSourceStack> helpSuggestions = (context, builder) -> {
            String input = getStringEmpty(context, "command");
            boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
            int totalPages = (int) Math.ceil((double) HELP_MESSAGE.size() / ServerConfig.HELP_INFO_NUM_PER_PAGE.get());
            for (int i = 0; i < totalPages && isInputEmpty; i++) {
                builder.suggest(i + 1);
            }
            for (EnumCommandType type : Arrays.stream(EnumCommandType.values())
                    .filter(type -> type != EnumCommandType.HELP)
                    .filter(type -> !type.isIgnore())
                    .filter(type -> !type.name().toLowerCase().contains("concise"))
                    .filter(type -> isInputEmpty || type.name().toLowerCase().contains(input.toLowerCase()))
                    .sorted(Comparator.comparing(EnumCommandType::getSort))
                    .toList()) {
                builder.suggest(type.name());
            }
            return builder.buildFuture();
        };


        SuggestionProvider<CommandSourceStack> dimensionSuggestions = (context, builder) -> {
            for (ServerLevel level : context.getSource().getServer().getAllLevels()) {
                builder.suggest(level.dimension().location().toString());
            }
            return builder.buildFuture();
        };
        SuggestionProvider<CommandSourceStack> safeSuggestions = (context, builder) -> {
            builder.suggest("safe");
            builder.suggest("unsafe");
            return builder.buildFuture();
        };
        SuggestionProvider<CommandSourceStack> rangeSuggestions = (context, builder) -> {
            for (int i = 1; i <= 5; i++) {
                int index = (int) Math.pow(10, i);
                if (index <= ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get()) {
                    builder.suggest(index);
                }
            }
            return builder.buildFuture();
        };
        SuggestionProvider<CommandSourceStack> structureSuggestions = (context, builder) -> {
            String input = getStringEmpty(context, "rules");
            boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
            // 具体结构(Recourse)
            NarcissusFarewell.getServerInstance().registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
                    .filter(location -> isInputEmpty || location.toString().contains(input))
                    .forEach(location -> builder.suggest(location.toString()));
            // 结构类型(Tag)
            NarcissusFarewell.getServerInstance().registryAccess().registryOrThrow(Registries.STRUCTURE).getTagNames()
                    .filter(tag -> isInputEmpty || tag.location().toString().contains(input))
                    .forEach(tag -> builder.suggest(tag.location().toString()));
            ForgeRegistries.BIOMES.getKeys().stream()
                    .filter(biome -> isInputEmpty || biome.toString().contains(input))
                    .forEach(biome -> builder.suggest(biome.toString()));
            return builder.buildFuture();
        };
        SuggestionProvider<CommandSourceStack> homeSuggestions = (context, builder) -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
                builder.suggest(StringUtils.formatString(key.getValue()));
            }
            return builder.buildFuture();
        };
        SuggestionProvider<CommandSourceStack> stageSuggestions = (context, builder) -> {
            for (KeyValue<String, String> key : WorldStageData.get().getStageCoordinate().keySet()) {
                builder.suggest(StringUtils.formatString(key.getValue()));
            }
            return builder.buildFuture();
        };


        Command<CommandSourceStack> languageCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String language = StringArgumentType.getString(context, "language");
            if (I18nUtils.getI18nFiles().contains(language)) {
                CustomConfig.setPlayerLanguage(NarcissusUtils.getPlayerUUIDString(player), language);
                NarcissusUtils.sendMessage(player, Component.translatable(player, EnumI18nType.MESSAGE, "player_default_language", language));
            } else if ("server".equalsIgnoreCase(language) || "client".equalsIgnoreCase(language)) {
                CustomConfig.setPlayerLanguage(NarcissusUtils.getPlayerUUIDString(player), language);
                NarcissusUtils.sendMessage(player, Component.translatable(player, EnumI18nType.MESSAGE, "player_default_language", language));
            } else {
                NarcissusUtils.sendMessage(player, Component.translatable(player, EnumI18nType.MESSAGE, "language_not_exist").setColor(0xFFFF0000));
            }
            return 1;
        };
        Command<CommandSourceStack> dimCommand = (context) -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.DIMENSION)) return 0;
            ServerPlayer player = context.getSource().getPlayerOrException();
            String dimString = player.getLevel().dimension().location().toString();
            Component dim = Component.literal(dimString);
            dim.setColor(EnumMCColor.GREEN.getColor())
                    .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dimString))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "chat_copy_click").toTextComponent()));
            Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "dimension_info", dim);
            NarcissusUtils.sendMessage(player, msg);
            return 1;
        };
        Command<CommandSourceStack> uuidCommand = (context) -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.UUID)) return 0;
            CommandSourceStack source = context.getSource();
            ServerPlayer target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                    target = source.getPlayerOrException();
                } else {
                    throw CommandSourceStack.ERROR_NOT_PLAYER.create();
                }
            }
            String language = ServerConfig.DEFAULT_LANGUAGE.get();
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                language = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
            }
            Component uuid = Component.literal(target.getStringUUID());
            uuid.setColor(EnumMCColor.GREEN.getColor())
                    .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, target.getStringUUID()))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(language, EnumI18nType.MESSAGE, "chat_copy_click").toTextComponent()));
            Component component = Component.translatable(language, EnumI18nType.MESSAGE, "player_uuid", target.getDisplayName().getString(), uuid);
            source.sendSuccess(component.toChatComponent(language), false);
            return 1;
        };
        Command<CommandSourceStack> cardCommand = (context) -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.CARD)) return 0;
            CommandSourceStack source = context.getSource();
            String type = getStringDefault(context, "type", "get");
            ServerPlayer target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                    target = source.getPlayerOrException();
                } else {
                    throw CommandSourceStack.ERROR_NOT_PLAYER.create();
                }
            }
            int num = 0;
            try {
                num = IntegerArgumentType.getInteger(context, "num");
            } catch (IllegalArgumentException ignored) {
            }

            String language = ServerConfig.DEFAULT_LANGUAGE.get();
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                language = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
            }
            PlayerTeleportData data = PlayerTeleportData.getData(target);
            switch (type) {
                case "set":
                    data.setTeleportCard(num);
                    break;
                case "add":
                    data.plusTeleportCard(num);
                    break;
                case "get":
                    break;
                default:
                    throw new IllegalArgumentException("Type " + type + " is not supported");
            }
            Component component = Component.translatable(language, EnumI18nType.MESSAGE, "player_card"
                    , target.getDisplayName().getString()
                    , data.getTeleportCard());
            source.sendSuccess(component.toChatComponent(language), false);
            return 1;
        };
        Command<CommandSourceStack> shareCommand = (context) -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.SHARE)) return 0;
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();

            String name = getStringDefault(context, "name", "Shared");

            List<ServerPlayer> targetList;
            try {
                targetList = new ArrayList<>(EntityArgument.getPlayers(context, "player"));
            } catch (IllegalArgumentException ignored) {
                targetList = new ArrayList<>(context.getSource().getServer().getPlayerList().getPlayers());
            }

            Component nameComponent;
            Component tpButton = Component.translatable(EnumI18nType.MESSAGE, "tp_button");
            // Component addButton = Component.translatable(EI18nType.MESSAGE, "add_button");
            Component copyButton = Component.translatable(EnumI18nType.MESSAGE, "copy_button");

            // 若为home
            if (name.contains("->")) {
                PlayerTeleportData data = PlayerTeleportData.getData(player);
                KeyValue<String, Coordinate> keyValue = data.getHomeCoordinate().entrySet().stream()
                        .map(entry -> new KeyValue<>(entry.getKey().getValue() + "->" + entry.getKey().getKey(), entry.getValue()))
                        .filter(kv -> name.equals(kv.getKey()))
                        .findFirst()
                        .orElse(new KeyValue<>(name, null));
                String[] split = keyValue.getKey().split("->");
                Coordinate coordinate = keyValue.getValue();
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_not_found_with_name_in_dimension")
                            , split[1], split[0]);
                    return 0;
                }
                nameComponent = Component.literal(split[0]);

                String tpCommand = String.format("/%s %s %s %s unsafe %s"
                        , NarcissusUtils.getCommand(EnumCommandType.TP_COORDINATE)
                        , coordinate.toXString()
                        , coordinate.toYString()
                        , coordinate.toZString()
                        , coordinate.getDimensionResourceId()
                );
                tpButton.setColor(EnumMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
                copyButton.setColor(EnumMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));

            }
            // 若为stage
            else if (name.contains(">>")) {
                KeyValue<String, Coordinate> keyValue = WorldStageData.get().getStageCoordinate().entrySet().stream()
                        .map(entry -> new KeyValue<>(entry.getKey().getValue() + ">>" + entry.getKey().getKey(), entry.getValue()))
                        .filter(kv -> name.equals(kv.getKey()))
                        .findFirst()
                        .orElse(new KeyValue<>(name, null));
                String[] split = keyValue.getKey().split(">>");
                Coordinate coordinate = keyValue.getValue();
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "stage_not_found_with_name_in_dimension")
                            , split[1], split[0]);
                    return 0;
                }
                nameComponent = Component.literal(split[0]);

                String tpCommand = String.format("/%s %s %s %s unsafe %s"
                        , NarcissusUtils.getCommand(EnumCommandType.TP_COORDINATE)
                        , coordinate.toXString()
                        , coordinate.toYString()
                        , coordinate.toZString()
                        , coordinate.getDimensionResourceId()
                );
                tpButton.setColor(EnumMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
                copyButton.setColor(EnumMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
            }
            // 玩家当前坐标
            else {
                nameComponent = Component.literal(name);
                Coordinate coordinate = new Coordinate(player);

                String tpCommand = String.format("/%s %s %s %s unsafe %s"
                        , NarcissusUtils.getCommand(EnumCommandType.TP_COORDINATE)
                        , coordinate.toXString()
                        , coordinate.toYString()
                        , coordinate.toZString()
                        , coordinate.getDimensionResourceId()
                );
                tpButton.setColor(EnumMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
                copyButton.setColor(EnumMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
            }

            String lang = ServerConfig.DEFAULT_LANGUAGE.get();
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                lang = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
            }
            Component component = Component.translatable(lang, EnumI18nType.MESSAGE, "shared_coordinates"
                    , player.getDisplayName().getString()
                    , nameComponent
                    , tpButton
                    // , addButton
                    , copyButton);
            for (ServerPlayer target : targetList) {
                if (!target.getUUID().equals(player.getUUID())) {
                    NarcissusUtils.sendMessage(target, component);
                }
            }
            source.sendSuccess(component.toChatComponent(lang), false);
            return 1;
        };
        Command<CommandSourceStack> feedCommand = (context) -> {
            notifyHelp(context);
            CommandSourceStack source = context.getSource();
            // 传送功能前置校验
            if (checkTeleportPre(source, EnumCommandType.FEED)) return 0;
            if (source.getEntity() == null) {
                List<ServerPlayer> targetList;
                try {
                    targetList = new ArrayList<>(EntityArgument.getPlayers(context, "player"));
                } catch (IllegalArgumentException ignored) {
                    throw CommandSourceStack.ERROR_NOT_PLAYER.create();
                }
                for (ServerPlayer target : targetList) {
                    if (NarcissusUtils.killPlayer(target)) {
                        NarcissusUtils.broadcastMessage(source.getServer(), Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "died_of_narcissus_" + (new Random().nextInt(4) + 1), target.getDisplayName().getString()));
                    }
                }
            }
            // 如果命令来自玩家
            else if (source.getEntity() instanceof ServerPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                // 判断是否开启功能
                if (!CommonConfig.SWITCH_FEED.get()) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "command_disabled"));
                    return 0;
                }
                List<ServerPlayer> targetList = new ArrayList<>();
                try {
                    targetList.addAll(EntityArgument.getPlayers(context, "player"));
                } catch (IllegalArgumentException ignored) {
                    targetList.add(player);
                }
                for (ServerPlayer target : targetList) {
                    if (NarcissusUtils.killPlayer(target)) {
                        NarcissusUtils.broadcastMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "died_of_narcissus_" + (new Random().nextInt(4) + 1), target.getDisplayName().getString()));
                    }
                }
            }
            return 1;
        };
        Command<CommandSourceStack> tpCoordinateCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_COORDINATE)) return 0;
            Coordinate coordinate;
            try {
                Vec3 pos = Vec3Argument.getCoordinates(context, "coordinate").getPosition(context.getSource());
                ResourceKey<Level> targetLevel;
                try {
                    targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
                } catch (IllegalArgumentException ignored) {
                    targetLevel = player.getLevel().dimension();
                }
                coordinate = new Coordinate(pos.x(), pos.y(), pos.z(), player.getYRot(), player.getXRot(), targetLevel);
            } catch (IllegalArgumentException ignored) {
                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                coordinate = new Coordinate(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot(), target.getLevel().dimension());
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_COORDINATE, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_COORDINATE);
            return 1;
        };
        Command<CommandSourceStack> tpStructureCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_STRUCTURE)) return 0;
            ResourceLocation structId = ResourceLocationArgument.getId(context, "struct");
            ResourceKey<Structure> structure = NarcissusUtils.getStructure(structId);
            TagKey<Structure> structureTag = NarcissusUtils.getStructureTag(structId);
            ResourceKey<Biome> biome = NarcissusUtils.getBiome(structId);
            if (structure == null && structureTag == null && biome == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "structure_biome_not_found"), structId);
                return 0;
            }
            int range;
            ResourceKey<Level> targetLevel;
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_STRUCTURE, range);
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            ResourceKey<Level> finalTargetLevel = targetLevel;
            boolean safe = "safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"));
            int finalRange = range;
            NarcissusUtils.sendActionBarMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "tp_structure_searching"));
            new Thread(() -> {
                Coordinate coordinate;
                if (biome != null) {
                    coordinate = NarcissusUtils.findNearestBiome(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(finalTargetLevel)), new Coordinate(player).setDimension(finalTargetLevel), biome, finalRange, 8);
                } else if (structure != null) {
                    coordinate = NarcissusUtils.findNearestStruct(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(finalTargetLevel)), new Coordinate(player).setDimension(finalTargetLevel), structure, finalRange);
                } else {
                    coordinate = NarcissusUtils.findNearestStruct(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(finalTargetLevel)), new Coordinate(player).setDimension(finalTargetLevel), structureTag, finalRange);
                }
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "structure_biome_not_found_in_range"), structId);
                    return;
                }
                coordinate.setSafe(safe);
                // 验证传送代价
                if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_STRUCTURE, true)) return;
                player.server.submit(() -> NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_STRUCTURE));
            }).start();
            return 1;
        };
        Command<CommandSourceStack> tpAskCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_ASK)) return 0;
            ServerPlayer target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                target = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                        .filter(request -> {
                            ServerPlayer entity = request.getTarget();
                            return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, EnumTeleportType.TP_ASK)
                                    || entity != null && entity.level.dimension() == player.getLevel().dimension();
                        })
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                                .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                        .getTarget();
            }
            if (target == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "player_not_found"));
                return 0;
            }

            // 验证并添加传送请求
            TeleportRequest request = new TeleportRequest()
                    .setRequester(player)
                    .setTarget(target)
                    .setTeleportType(EnumTeleportType.TP_ASK)
                    .setRequestTime(new Date());
            request.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            if (checkTeleportPost(request)) return 0;

            PlayerAccess targetAccess = PlayerTeleportData.getData(target).getAccess();
            String playerUUIDString = NarcissusUtils.getPlayerUUIDString(player);
            boolean ignore = targetAccess.getBlackList().contains(playerUUIDString)
                    || (!targetAccess.getWhiteList().isEmpty() && !targetAccess.getWhiteList().contains(playerUUIDString));
            boolean autoAccept = !ignore && targetAccess.getAutoTpaList().contains(playerUUIDString);

            NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request.setIgnore(ignore));

            // 通知目标玩家
            if (!ignore) {
                // 创建 "Yes" 按钮
                Component yesButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "yes_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_ASK_YES.get(), request.getRequestId())));
                // 创建 "No" 按钮
                Component noButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "no_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_ASK_NO.get(), request.getRequestId())));
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "tp_ask_request_received"
                        , player.getDisplayName().getString(), yesButton, noButton);
                NarcissusUtils.sendMessage(target, msg);
            }
            // 通知请求者
            {
                // 创建 "Cancel" 按钮
                Component cancelButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "cancel_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_ASK_CANCEL.get(), request.getRequestId())));
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "tp_ask_request_sent"
                        , target.getDisplayName().getString(), cancelButton);
                NarcissusUtils.sendMessage(player, msg);
            }
            if (autoAccept) {
                ServerPlayer finalTarget = target;
                new Thread(() -> NarcissusUtils.executeCommand(finalTarget, NarcissusUtils.getCommand(EnumCommandType.TP_ASK_YES) + " " + request.getRequestId())).start();
            }
            return 1;
        };
        Command<CommandSourceStack> tpAskYesCommand = context -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_ASK_YES)) return 0;
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, EnumTeleportType.TP_ASK, true);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            if (checkTeleportPost(request, true)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_invalid"));
                return 0;
            }
            NarcissusUtils.teleportTo(request);
            return 1;
        };
        Command<CommandSourceStack> tpAskNoCommand = context -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_ASK_NO)) return 0;
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, EnumTeleportType.TP_ASK, true);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_rejected"), request.getTarget().getDisplayName().getString());
            return 1;
        };
        Command<CommandSourceStack> tpAskCancelCommand = context -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_ASK_CANCEL)) return 0;
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, EnumTeleportType.TP_ASK, false);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_cancelled"), request.getRequester().getDisplayName().getString());
            if (!request.isIgnore() && !request.getRequester().getUUID().equals(request.getTarget().getUUID())) {
                NarcissusUtils.sendTranslatableMessage(request.getTarget(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_cancelled"), request.getRequester().getDisplayName().getString());
            }
            return 1;
        };
        Command<CommandSourceStack> tpHereCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_HERE)) return 0;
            ServerPlayer target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                target = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                        .filter(request -> {
                            Player entity = request.getTarget();
                            return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, EnumTeleportType.TP_HERE)
                                    || entity != null && entity.level.dimension() == player.getLevel().dimension();
                        })
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                                .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                        .getTarget();
            }
            if (target == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "player_not_found"));
                return 0;
            }

            // 验证并添加传送请求
            TeleportRequest request = new TeleportRequest()
                    .setRequester(player)
                    .setTarget(target)
                    .setTeleportType(EnumTeleportType.TP_HERE)
                    .setRequestTime(new Date());
            request.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            if (checkTeleportPost(request)) return 0;

            PlayerAccess targetAccess = PlayerTeleportData.getData(target).getAccess();
            String playerUUIDString = NarcissusUtils.getPlayerUUIDString(player);
            boolean ignore = targetAccess.getBlackList().contains(playerUUIDString)
                    || (!targetAccess.getWhiteList().isEmpty() && !targetAccess.getWhiteList().contains(playerUUIDString));
            boolean autoAccept = !ignore && targetAccess.getAutoTphList().contains(playerUUIDString);

            NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request.setIgnore(ignore));

            // 通知目标玩家
            if (!ignore) {
                // 创建 "Yes" 按钮
                Component yesButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "yes_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_HERE_YES.get(), request.getRequestId())));
                // 创建 "No" 按钮
                Component noButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "no_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_HERE_NO.get(), request.getRequestId())));
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "tp_here_request_received"
                        , player.getDisplayName().getString(), Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.WORD, request.isSafe() ? "tp_here_safe" : "tp_here_unsafe"), yesButton, noButton);
                NarcissusUtils.sendMessage(target, msg);
            }
            // 通知请求者
            {
                // 创建 "Cancel" 按钮
                Component cancelButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.MESSAGE, "cancel_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_HERE_CANCEL.get(), request.getRequestId())));
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "tp_here_request_sent"
                        , target.getDisplayName().getString(), cancelButton);
                NarcissusUtils.sendMessage(player, msg);
            }
            if (autoAccept) {
                ServerPlayer finalTarget = target;
                new Thread(() -> NarcissusUtils.executeCommand(finalTarget, NarcissusUtils.getCommand(EnumCommandType.TP_HERE_YES) + " " + request.getRequestId())).start();
            }
            return 1;
        };
        Command<CommandSourceStack> tpHereYesCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_HERE_YES)) return 0;
            String id = getRequestId(context, EnumTeleportType.TP_HERE, true);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            if (checkTeleportPost(request, true)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_invalid"));
                return 0;
            }
            NarcissusUtils.teleportTo(request);
            return 1;
        };
        Command<CommandSourceStack> tpHereNoCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_HERE_YES)) return 0;
            String id = getRequestId(context, EnumTeleportType.TP_HERE, true);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_rejected"), request.getTarget().getDisplayName().getString());
            return 1;
        };
        Command<CommandSourceStack> tpHereCancelCommand = context -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_HERE_CANCEL)) return 0;
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, EnumTeleportType.TP_HERE, false);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_cancelled"), request.getRequester().getDisplayName().getString());
            if (!request.isIgnore() && !request.getRequester().getUUID().equals(request.getTarget().getUUID())) {
                NarcissusUtils.sendTranslatableMessage(request.getTarget(), I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_here_cancelled"), request.getRequester().getDisplayName().getString());
            }
            return 1;
        };
        Command<CommandSourceStack> tpRandomCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_RANDOM)) return 0;
            ResourceKey<Level> targetLevel;
            int range;
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_RANDOM, range);
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            Coordinate coordinate = Coordinate.random(player, range, targetLevel).setSafe(true);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_RANDOM, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_RANDOM);
            return 1;
        };
        Command<CommandSourceStack> tpSpawnCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_SPAWN)) return 0;
            ServerPlayer target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                target = player;
            }
            Coordinate coordinate = new Coordinate(target);
            BlockPos respawnPosition = target.getRespawnPosition();
            coordinate.setDimension(target.getRespawnDimension());
            if (respawnPosition == null) {
                respawnPosition = target.getLevel().getSharedSpawnPos();
                coordinate.setDimension(target.getLevel().dimension());
            }
            if (respawnPosition == null) {
                respawnPosition = target.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos();
                coordinate.setDimension(Level.OVERWORLD);
            }
            coordinate.fromBlockPos(respawnPosition);
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_SPAWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_SPAWN);
            return 1;
        };
        Command<CommandSourceStack> tpWorldSpawnCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_WORLD_SPAWN)) return 0;
            Coordinate coordinate = new Coordinate(player);
            BlockPos respawnPosition = player.getLevel().getSharedSpawnPos();
            coordinate.setDimension(player.getLevel().dimension());
            if (respawnPosition == null) {
                respawnPosition = player.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos();
                coordinate.setDimension(Level.OVERWORLD);
            }
            coordinate.fromBlockPos(respawnPosition);
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_WORLD_SPAWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_WORLD_SPAWN);
            return 1;
        };
        Command<CommandSourceStack> tpTopCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_TOP)) return 0;
            Coordinate coordinate = NarcissusUtils.findTopCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_top_not_found"));
                return 0;
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"))).setSafeMode(EnumSafeMode.Y_T_TO_C);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_TOP, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_TOP);
            return 1;
        };
        Command<CommandSourceStack> tpBottomCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_BOTTOM)) return 0;
            Coordinate coordinate = NarcissusUtils.findBottomCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_bottom_not_found"));
                return 0;
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"))).setSafeMode(EnumSafeMode.Y_B_TO_C);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_BOTTOM, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_BOTTOM);
            return 1;
        };
        Command<CommandSourceStack> tpUpCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_UP)) return 0;
            Coordinate coordinate = NarcissusUtils.findUpCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_up_not_found"));
                return 0;
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"))).setSafeMode(EnumSafeMode.Y_C_TO_T);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_UP, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_UP);
            return 1;
        };
        Command<CommandSourceStack> tpDownCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_DOWN)) return 0;
            Coordinate coordinate = NarcissusUtils.findDownCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_down_not_found"));
                return 0;
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"))).setSafeMode(EnumSafeMode.Y_C_TO_B);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_DOWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_DOWN);
            return 1;
        };
        Command<CommandSourceStack> tpViewCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_VIEW)) return 0;
            boolean safe;
            int range;
            safe = "safe".equalsIgnoreCase(getStringEmpty(context, "safe"));
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_VIEW, range);
            NarcissusUtils.sendActionBarMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "tp_view_searching"));
            boolean finalSafe = safe;
            int finalRange = range;
            new Thread(() -> {
                Coordinate coordinate = NarcissusUtils.findViewEndCandidate(player, finalSafe, finalRange);
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, finalSafe ? "tp_view_safe_not_found" : "tp_view_not_found"));
                    return;
                }
                coordinate.setSafeMode(EnumSafeMode.Y_C_OFFSET_3);
                // 验证传送代价
                if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_VIEW, true)) return;
                player.server.submit(() -> NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_VIEW));
            }).start();
            return 1;
        };
        Command<CommandSourceStack> tpHomeCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_HOME)) return 0;
            ResourceKey<Level> targetLevel = null;
            try {
                ResourceKey<Level> targetDimension = ResourceKey.create(Registries.DIMENSION, NarcissusFarewell.parseResource(StringArgumentType.getString(context, "dimension")));
                ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
                if (level != null) {
                    targetLevel = targetDimension;
                }
            } catch (IllegalArgumentException ignored) {
            }
            String name = getStringDefault(context, "name", null);
            Coordinate coordinate = NarcissusUtils.getPlayerHome(player, targetLevel, name);
            if (coordinate == null) {
                if (targetLevel == null && name == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_not_found"));
                } else if (targetLevel != null && name == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_not_found_in_dimension"), targetLevel.location().toString());
                } else if (targetLevel == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_not_found_with_name"), name);
                } else {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_not_found_with_name_in_dimension"), targetLevel.location().toString(), name);
                }
                return 0;
            }
            try {
                coordinate.setSafe(BoolArgumentType.getBool(context, "safe"));
            } catch (IllegalArgumentException ignored) {
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_HOME, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_HOME);
            return 1;
        };
        Command<CommandSourceStack> setHomeCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.SET_HOME)) return 0;
            // 判断设置数量是否超过限制
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            if (data.getHomeCoordinate().size() >= ServerConfig.TELEPORT_HOME_LIMIT.get()) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_limit"), ServerConfig.TELEPORT_HOME_LIMIT.get());
                return 0;
            }
            String name = getStringDefault(context, "name", "home");
            boolean defaultHome = false;
            try {
                defaultHome = BoolArgumentType.getBool(context, "default");
            } catch (IllegalArgumentException ignored) {
            }
            Coordinate coordinate = new Coordinate(player);
            KeyValue<String, String> key = new KeyValue<>(player.getLevel().dimension().location().toString(), name);
            if (data.getHomeCoordinate().containsKey(key)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_already_exists"), key.getKey(), key.getValue());
                return 0;
            }
            data.addHomeCoordinate(key, coordinate);
            if (defaultHome) {
                if (data.getDefaultHome().containsKey(player.getLevel().dimension().location().toString())) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_default_remove"), data.getDefaultHome(player.getLevel().dimension().location().toString()).getValue());
                }
                data.addDefaultHome(player.getLevel().dimension().location().toString(), name);
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_set_default"), name, coordinate.toXyzString());
            } else {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_set"), name, coordinate.toXyzString());
            }
            return 1;
        };
        Command<CommandSourceStack> delHomeCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.DEL_HOME)) return 0;
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            String name = StringArgumentType.getString(context, "name");
            String dimension;
            try {
                ResourceKey<Level> targetLevel = ResourceKey.create(Registries.DIMENSION, NarcissusFarewell.parseResource(StringArgumentType.getString(context, "dimension")));
                dimension = targetLevel.location().toString();
            } catch (IllegalArgumentException ignored) {
                dimension = NarcissusUtils.getHomeDimensionByName(player, name);
            }
            Coordinate remove = data.getHomeCoordinate().remove(new KeyValue<>(dimension, name));
            data.setDirty();
            if (remove == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_not_found_with_name_in_dimension"), dimension, name);
                return 0;
            }
            if (data.getDefaultHome().containsKey(dimension)) {
                if (data.getDefaultHome().get(dimension).equals(name)) {
                    data.getDefaultHome().remove(dimension);
                    data.setDirty();
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_default_remove"), name);
                }
            }
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "home_del"), dimension, name);
            return 1;
        };
        Command<CommandSourceStack> getHomeCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.GET_HOME)) return 0;
            Component component;
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            String language = NarcissusUtils.getPlayerLanguage(player);
            if (data.getHomeCoordinate().isEmpty()) {
                component = Component.translatable(language, EnumI18nType.MESSAGE, "home_is_empty");
            } else {
                Component info = Component.empty();
                // dimension:name coordinate 转为 dimension [name:coordinate]
                Map<String, List<KeyValue<String, Coordinate>>> map = data.getHomeCoordinate().entrySet().stream()
                        .collect(Collectors.groupingBy(
                                entry -> entry.getKey().getKey(),
                                Collectors.mapping(
                                        entry -> new KeyValue<>(entry.getKey().getValue(), entry.getValue()),
                                        Collectors.toList()
                                )
                        ));
                for (Map.Entry<String, List<KeyValue<String, Coordinate>>> entry : map.entrySet()) {
                    Component dimension = Component.literal(entry.getKey()).setColor(EnumMCColor.DARK_GREEN.getColor());
                    dimension.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()));
                    dimension.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(entry.getKey()).toTextComponent()));
                    dimension.append(Component.literal(": ").setColor(EnumMCColor.GRAY.getColor()));
                    for (KeyValue<String, Coordinate> coordinates : entry.getValue()) {
                        Component defHome;
                        if (data.getDefaultHome().getOrDefault(entry.getKey(), "").equalsIgnoreCase(coordinates.getKey())) {
                            defHome = Component.translatable(language, EnumI18nType.WORD, "default").setColor(EnumMCColor.GRAY.getColor());
                        } else {
                            defHome = Component.empty();
                        }
                        Component name = Component.translatable(language, EnumI18nType.MESSAGE, "home_info"
                                , coordinates.getKey()
                                , coordinates.getValue().toXString()
                                , coordinates.getValue().toYString()
                                , coordinates.getValue().toZString()
                                , defHome);
                        name.toChatComponent();
                        Component name_hover = Component.translatable(language, EnumI18nType.MESSAGE, "home_info_hover"
                                , coordinates.getKey()
                                , coordinates.getValue().toXString()
                                , coordinates.getValue().toYString()
                                , coordinates.getValue().toZString()
                                , defHome);
                        name.setColor(EnumMCColor.GREEN.getColor());
                        name.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name_hover.toString(true)));
                        name.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, name_hover.toChatComponent()));
                        dimension.append(name);
                        dimension.append(Component.literal(", ").setColor(EnumMCColor.GRAY.getColor()));
                    }
                    info.append(dimension).append("\n");
                }
                component = Component.translatable(language, EnumI18nType.MESSAGE, "home_is", info);
            }
            NarcissusUtils.sendMessage(player, component);
            return 1;
        };
        Command<CommandSourceStack> tpStageCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_STAGE)) return 0;
            ResourceKey<Level> targetLevel = null;
            String name = getStringDefault(context, "name", null);
            try {
                ResourceKey<Level> targetDimension = ResourceKey.create(Registries.DIMENSION, NarcissusFarewell.parseResource(StringArgumentType.getString(context, "dimension")));
                ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
                if (level != null) {
                    targetLevel = targetDimension;
                }
            } catch (IllegalArgumentException ignored) {
            }
            String dimension = targetLevel != null ? targetLevel.location().toString() : null;
            if (StringUtils.isNullOrEmptyEx(name)) {
                KeyValue<String, String> stageKey = NarcissusUtils.findNearestStageKey(player);
                if (stageKey == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "stage_nearest_not_found"));
                    return 0;
                }
                name = stageKey.getValue();
                dimension = stageKey.getKey();
            }
            WorldStageData stageData = WorldStageData.get();
            Coordinate coordinate = null;
            if (dimension == null) {
                int coordinateSize = stageData.getCoordinateSize(name);
                if (coordinateSize == 1) {
                    coordinate = stageData.getCoordinate(name);
                } else if (coordinateSize > 1) {
                    coordinate = stageData.getCoordinate(player.getLevel().dimension().location().toString(), name);
                }
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "stage_not_found"), name);
                    return 0;
                }
            } else {
                coordinate = stageData.getCoordinate(dimension, name);
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "stage_not_found_with_name_in_dimension"), dimension, name);
                    return 0;
                }
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_STAGE, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_STAGE);
            return 1;
        };
        Command<CommandSourceStack> setStageCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.SET_STAGE)) return 0;
            WorldStageData stageData = WorldStageData.get();
            String name = StringArgumentType.getString(context, "name");
            ResourceKey<Level> targetLevel;
            try {
                ResourceKey<Level> targetDimension = ResourceKey.create(Registries.DIMENSION, NarcissusFarewell.parseResource(StringArgumentType.getString(context, "dimension")));
                ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
                if (level != null) {
                    targetLevel = targetDimension;
                } else {
                    targetLevel = player.getLevel().dimension();
                }
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            String dimension = targetLevel.location().toString();
            KeyValue<String, String> key = new KeyValue<>(dimension, name);
            if (stageData.getStageCoordinate().containsKey(key)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "stage_already_exists"), key.getKey(), key.getValue());
                return 0;
            }
            Coordinate coordinate = new Coordinate(player).setDimension(targetLevel);
            try {
                coordinate.fromVec3(Vec3Argument.getCoordinates(context, "coordinate").getPosition(context.getSource()));
            } catch (IllegalArgumentException ignored) {
            }
            stageData.addCoordinate(key, coordinate);
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "stage_set"), name, coordinate.toXyzString());
            return 1;
        };
        Command<CommandSourceStack> delStageCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.DEL_STAGE)) return 0;
            String name = StringArgumentType.getString(context, "name");
            String dimension;
            try {
                ResourceKey<Level> targetLevel = ResourceKey.create(Registries.DIMENSION, NarcissusFarewell.parseResource(StringArgumentType.getString(context, "dimension")));
                dimension = targetLevel.location().toString();
            } catch (IllegalArgumentException ignored) {
                dimension = NarcissusUtils.getStageDimensionByName(name);
            }
            WorldStageData stageData = WorldStageData.get();
            Coordinate remove = stageData.getStageCoordinate().remove(new KeyValue<>(dimension, name));
            stageData.setDirty();
            if (remove == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "stage_not_found_with_name_in_dimension"), dimension, name);
                return 0;
            }
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "stage_del"), dimension, name);
            return 1;
        };
        Command<CommandSourceStack> getStageCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.GET_STAGE)) return 0;
            Component component;
            WorldStageData data = WorldStageData.get();
            String language = NarcissusUtils.getPlayerLanguage(player);
            if (data.getStageCoordinate().isEmpty()) {
                component = Component.translatable(language, EnumI18nType.MESSAGE, "stage_is_empty");
            } else {
                Component info = Component.empty();
                // dimension:name coordinate 转为 dimension [name:coordinate]
                Map<String, List<KeyValue<String, Coordinate>>> map = data.getStageCoordinate().entrySet().stream()
                        .collect(Collectors.groupingBy(
                                entry -> entry.getKey().getKey(),
                                Collectors.mapping(
                                        entry -> new KeyValue<>(entry.getKey().getValue(), entry.getValue()),
                                        Collectors.toList()
                                )
                        ));
                for (Map.Entry<String, List<KeyValue<String, Coordinate>>> entry : map.entrySet()) {
                    Component dimension = Component.literal(entry.getKey()).setColor(EnumMCColor.DARK_GREEN.getColor());
                    dimension.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()));
                    dimension.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(entry.getKey()).toTextComponent()));
                    dimension.append(Component.literal(": ").setColor(EnumMCColor.GRAY.getColor()));
                    for (KeyValue<String, Coordinate> coordinates : entry.getValue()) {
                        Component name = Component.translatable(language, EnumI18nType.MESSAGE, "stage_info"
                                , coordinates.getKey()
                                , coordinates.getValue().toXString()
                                , coordinates.getValue().toYString()
                                , coordinates.getValue().toZString());
                        Component name_hover = Component.translatable(language, EnumI18nType.MESSAGE, "stage_info_hover"
                                , coordinates.getKey()
                                , coordinates.getValue().toXString()
                                , coordinates.getValue().toYString()
                                , coordinates.getValue().toZString());
                        name.setColor(EnumMCColor.GREEN.getColor());
                        name.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name_hover.toString(true)));
                        name.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, name_hover.toChatComponent()));
                        dimension.append(name);
                        dimension.append(Component.literal(", ").setColor(EnumMCColor.GRAY.getColor()));
                    }
                    info.append(dimension).append("\n");
                }
                component = Component.translatable(language, EnumI18nType.MESSAGE, "stage_is", info);
            }
            NarcissusUtils.sendMessage(player, component);
            return 1;
        };
        Command<CommandSourceStack> tpBackCommand = context -> {
            notifyHelp(context);
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.TP_BACK)) return 0;
            EnumTeleportType type = EnumTeleportType.nullableValueOf(getStringEmpty(context, "type"));
            ResourceKey<Level> targetLevel = null;
            try {
                ResourceKey<Level> targetDimension = ResourceKey.create(Registries.DIMENSION, NarcissusFarewell.parseResource(StringArgumentType.getString(context, "dimension")));
                ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
                if (level != null) {
                    targetLevel = targetDimension;
                }
            } catch (IllegalArgumentException ignored) {
                // targetLevel = player.getLevel().dimension();
            }
            TeleportRecord record = NarcissusUtils.getBackTeleportRecord(player, type, targetLevel);
            if (record == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "back_not_found"));
                return 0;
            }
            Coordinate coordinate = record.getBefore().clone();
            coordinate.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, EnumTeleportType.TP_BACK, true)) return 0;
            NarcissusUtils.removeBackTeleportRecord(player, record);
            NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_BACK);
            return 1;
        };
        Command<CommandSourceStack> virtualOpCommand = context -> {
            notifyHelp(context);
            CommandSourceStack source = context.getSource();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), EnumCommandType.VIRTUAL_OP)) return 0;
            // 如果命令来自玩家
            if (source.getEntity() == null || source.getEntity() instanceof ServerPlayer) {
                EnumOperationType type = EnumOperationType.fromString(StringArgumentType.getString(context, "operation"));
                EnumCommandType[] rules;
                try {
                    rules = Arrays.stream(StringArgumentType.getString(context, "rules").split(","))
                            .filter(StringUtils::isNotNullOrEmpty)
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .map(EnumCommandType::valueOf).toArray(EnumCommandType[]::new);
                } catch (IllegalArgumentException ignored) {
                    rules = new EnumCommandType[]{};
                }
                List<ServerPlayer> targetList = new ArrayList<>();
                try {
                    targetList.addAll(EntityArgument.getPlayers(context, "player"));
                } catch (IllegalArgumentException ignored) {
                }
                String language = ServerConfig.DEFAULT_LANGUAGE.get();
                if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                    language = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
                }
                for (ServerPlayer target : targetList) {
                    switch (type) {
                        case ADD:
                            VirtualPermissionManager.addVirtualPermission(target, rules);
                            break;
                        case SET:
                            VirtualPermissionManager.setVirtualPermission(target, rules);
                            break;
                        case DEL:
                        case REMOVE:
                            VirtualPermissionManager.delVirtualPermission(target, rules);
                            break;
                        case CLEAR:
                            VirtualPermissionManager.clearVirtualPermission(target);
                            break;
                    }
                    String permissions = VirtualPermissionManager.buildPermissionsString(VirtualPermissionManager.getVirtualPermission(target));
                    NarcissusUtils.sendTranslatableMessage(target, I18nUtils.getKey(EnumI18nType.MESSAGE, "player_virtual_op"), target.getDisplayName().getString(), permissions);
                    if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                        ServerPlayer player = source.getPlayerOrException();
                        if (!target.getStringUUID().equalsIgnoreCase(player.getStringUUID())) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "player_virtual_op"), target.getDisplayName().getString(), permissions);
                        }
                    } else {
                        source.sendSuccess(Component.translatable(language, EnumI18nType.MESSAGE, "player_virtual_op", target.getDisplayName().getString(), permissions).toChatComponent(), true);
                    }
                    // 更新权限信息
                    source.getServer().getPlayerList().sendPlayerPermissionLevel(target);
                }
            }
            return 1;
        };

        String[] whiteListModes = {"none", "both", "auto_accept_tpa", "auto_accept_tph",};
        Command<CommandSourceStack> addWhiteListCommand = context -> {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
            if (CollectionUtils.isNullOrEmpty(players)) {
                return 0;
            }
            String mode = getStringDefault(context, "mode", "none");
            if (!Arrays.asList(whiteListModes).contains(mode)) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
            }
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            PlayerAccess access = data.getAccess();
            String[] uuids = players.stream().map(NarcissusUtils::getPlayerUUIDString).toArray(String[]::new);
            Component msg;
            if (access.addWhiteList(uuids)) {
                msg = Component.translatable(EnumI18nType.MESSAGE, "list_add_success"
                        , getBlacklistOrWhitelistHelp(player, false)
                        , players.stream().map(NarcissusUtils::getPlayerName)
                                .collect(Collectors.joining(","))
                );
                switch (mode) {
                    case "both": {
                        access.addTpaList(uuids);
                        access.addTphList(uuids);
                    }
                    break;
                    case "auto_accept_tpa": {
                        access.addTpaList(uuids);
                    }
                    break;
                    case "auto_accept_tph": {
                        access.addTphList(uuids);
                    }
                    break;
                    default:
                }
                data.setDirty();
            } else {
                msg = Component.translatable(EnumI18nType.MESSAGE, "list_add_fail", getBlacklistOrWhitelistHelp(player, false), getBlacklistOrWhitelistHelp(player, true));
            }
            NarcissusUtils.sendMessage(player, msg);
            return 1;
        };
        Command<CommandSourceStack> removeWhiteListCommand = context -> {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
            if (CollectionUtils.isNullOrEmpty(players)) {
                return 0;
            }
            String mode = getStringDefault(context, "mode", "none");
            if (!Arrays.asList(whiteListModes).contains(mode)) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
            }
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            PlayerAccess access = data.getAccess();
            String[] uuids = players.stream().map(NarcissusUtils::getPlayerUUIDString).toArray(String[]::new);
            switch (mode) {
                case "both": {
                    access.removeAutoTpa(uuids);
                    access.removeAutoTph(uuids);
                }
                break;
                case "auto_accept_tpa": {
                    access.removeAutoTpa(uuids);
                }
                break;
                case "auto_accept_tph": {
                    access.removeAutoTph(uuids);
                }
                break;
                default: {
                    access.removeWhiteList(uuids);
                }
            }
            data.setDirty();
            Component msg = Component.translatable(EnumI18nType.MESSAGE, "remove_success")
                    .append(getWhiteListMessage(player, access));
            NarcissusUtils.sendMessage(player, msg);
            return 1;
        };


        LiteralArgumentBuilder<CommandSourceStack> language = // region language
                Commands.literal(CommonConfig.COMMAND_LANGUAGE.get())
                        .then(Commands.argument("language", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("client");
                                    builder.suggest("server");
                                    I18nUtils.getI18nFiles().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(languageCommand)
                        ); // endregion language
        LiteralArgumentBuilder<CommandSourceStack> dim =  // region dim
                Commands.literal(CommonConfig.COMMAND_DIMENSION.get())
                        .executes(dimCommand);// endregion dim
        LiteralArgumentBuilder<CommandSourceStack> uuid = // region uuid
                Commands.literal(CommonConfig.COMMAND_UUID.get())
                        .executes(uuidCommand)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(uuidCommand)
                        );// endregion uuid
        LiteralArgumentBuilder<CommandSourceStack> card = // region card
                Commands.literal(CommonConfig.COMMAND_CARD.get())
                        .executes(cardCommand)
                        .then(Commands.argument("type", StringArgumentType.word())
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.SET_CARD))
                                .suggests((context, builder) -> {
                                    builder.suggest("get");
                                    builder.suggest("add");
                                    builder.suggest("set");
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(cardCommand)
                                        .then(Commands.argument("num", IntegerArgumentType.integer())
                                                .suggests((context, builder) -> {
                                                    builder.suggest(-5);
                                                    builder.suggest(-1);
                                                    builder.suggest(1);
                                                    builder.suggest(5);
                                                    builder.suggest(10);
                                                    builder.suggest(20);
                                                    return builder.buildFuture();
                                                })
                                                .executes(cardCommand)
                                        )
                                )
                        );// endregion card
        LiteralArgumentBuilder<CommandSourceStack> share = // region share
                Commands.literal(CommonConfig.COMMAND_SHARE.get())
                        .executes(shareCommand)
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    String name = getStringEmpty(context, "name");
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    // 获取玩家私人传送点
                                    PlayerTeleportData data = PlayerTeleportData.getData(player);
                                    for (KeyValue<String, String> home : data.getHomeCoordinate().keySet()) {
                                        // -> 用于标识home，方便shareCommand中识别
                                        String homeString = home.getValue() + "->" + home.getKey();
                                        if (StringUtils.isNullOrEmptyEx(name) || homeString.toLowerCase().contains(name)) {
                                            builder.suggest(StringUtils.formatString(homeString));
                                        }
                                    }
                                    // 获取公共传送点
                                    for (KeyValue<String, String> stage : WorldStageData.get().getStageCoordinate().keySet()) {
                                        // >> 用于标识stage，方便shareCommand中识别
                                        String stageString = stage.getValue() + ">>" + stage.getKey();
                                        if (StringUtils.isNullOrEmptyEx(name) || stageString.toLowerCase().contains(name)) {
                                            builder.suggest(StringUtils.formatString(stageString));
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(shareCommand)
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(shareCommand)
                                )
                        );// endregion share
        LiteralArgumentBuilder<CommandSourceStack> feed = // region feed
                Commands.literal(CommonConfig.COMMAND_FEED.get())
                        .executes(feedCommand)
                        .then(Commands.argument("player", EntityArgument.players())
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.FEED_OTHER))
                                .executes(feedCommand)
                        );// endregion feed
        LiteralArgumentBuilder<CommandSourceStack> tpx = // region tpx
                Commands.literal(CommonConfig.COMMAND_TP_COORDINATE.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_COORDINATE))
                        .then(Commands.argument("coordinate", Vec3Argument.vec3())
                                .executes(tpCoordinateCommand)
                                .then(Commands.argument("safe", StringArgumentType.word())
                                        .suggests(safeSuggestions)
                                        .executes(tpCoordinateCommand)
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(tpCoordinateCommand)
                                        )
                                )
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpCoordinateCommand)
                                .then(Commands.argument("safe", StringArgumentType.word())
                                        .suggests(safeSuggestions)
                                        .executes(tpCoordinateCommand)
                                )
                        );// endregion tpx
        LiteralArgumentBuilder<CommandSourceStack> tpst = // region tpst
                Commands.literal(CommonConfig.COMMAND_TP_STRUCTURE.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_STRUCTURE))
                        .then(Commands.argument("struct", ResourceLocationArgument.id())
                                .suggests(structureSuggestions)
                                .executes(tpStructureCommand)
                                .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                        .suggests(rangeSuggestions)
                                        .executes(tpStructureCommand)
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(tpStructureCommand)
                                                .then(Commands.argument("safe", StringArgumentType.word())
                                                        .suggests(safeSuggestions)
                                                        .executes(tpStructureCommand)
                                                )
                                        )
                                )
                        );// endregion tpst
        LiteralArgumentBuilder<CommandSourceStack> tpa = // region tpa
                Commands.literal(CommonConfig.COMMAND_TP_ASK.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_ASK))
                        .executes(tpAskCommand)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpAskCommand)
                                .then(Commands.argument("safe", StringArgumentType.word())
                                        .suggests(safeSuggestions)
                                        .executes(tpAskCommand)
                                )
                        );// endregion tpa
        LiteralArgumentBuilder<CommandSourceStack> tpaYes = // region tpaYes
                Commands.literal(CommonConfig.COMMAND_TP_ASK_YES.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_ASK_YES))
                        .executes(tpAskYesCommand)
                        .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                                .suggests(buildReqIndexSuggestions(EnumTeleportType.TP_ASK, true))
                                .executes(tpAskYesCommand)
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpAskYesCommand)
                        )
                        .then(Commands.argument("requestId", StringArgumentType.word())
                                .executes(tpAskYesCommand)
                        );// endregion tpaYes
        LiteralArgumentBuilder<CommandSourceStack> tpaNo = // region tpaNo
                Commands.literal(CommonConfig.COMMAND_TP_ASK_NO.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_ASK_NO))
                        .executes(tpAskNoCommand)
                        .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                                .suggests(buildReqIndexSuggestions(EnumTeleportType.TP_ASK, true))
                                .executes(tpAskNoCommand)
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpAskNoCommand)
                        )
                        .then(Commands.argument("requestId", StringArgumentType.word())
                                .executes(tpAskNoCommand)
                        );// endregion tpaNo
        LiteralArgumentBuilder<CommandSourceStack> tpaCancel = // region tpaCancel
                Commands.literal(CommonConfig.COMMAND_TP_ASK_CANCEL.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_ASK_CANCEL))
                        .executes(tpAskCancelCommand)
                        .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                                .suggests(buildReqIndexSuggestions(EnumTeleportType.TP_ASK, false))
                                .executes(tpAskCancelCommand)
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpAskCancelCommand)
                        )
                        .then(Commands.argument("requestId", StringArgumentType.word())
                                .executes(tpAskCancelCommand)
                        );// endregion tpaCancel
        LiteralArgumentBuilder<CommandSourceStack> tph = // region tph
                Commands.literal(CommonConfig.COMMAND_TP_HERE.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HERE))
                        .executes(tpHereCommand)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpHereCommand)
                                .then(Commands.argument("safe", StringArgumentType.word())
                                        .suggests(safeSuggestions)
                                        .executes(tpHereCommand)
                                )
                        );// endregion tph
        LiteralArgumentBuilder<CommandSourceStack> tphYes = // region tphYes
                Commands.literal(CommonConfig.COMMAND_TP_HERE_YES.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HERE_YES))
                        .executes(tpHereYesCommand)
                        .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                                .suggests(buildReqIndexSuggestions(EnumTeleportType.TP_HERE, true))
                                .executes(tpHereYesCommand)
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpHereYesCommand)
                        )
                        .then(Commands.argument("requestId", StringArgumentType.word())
                                .executes(tpHereYesCommand)
                        );// endregion tphYes
        LiteralArgumentBuilder<CommandSourceStack> tphNo = // region tphNo
                Commands.literal(CommonConfig.COMMAND_TP_HERE_NO.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HERE_NO))
                        .executes(tpHereNoCommand)
                        .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                                .suggests(buildReqIndexSuggestions(EnumTeleportType.TP_HERE, true))
                                .executes(tpHereNoCommand)
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpHereNoCommand)
                        )
                        .then(Commands.argument("requestId", StringArgumentType.word())
                                .executes(tpHereNoCommand)
                        );// endregion tphNo
        LiteralArgumentBuilder<CommandSourceStack> tphCancel = // region tphCancel
                Commands.literal(CommonConfig.COMMAND_TP_HERE_CANCEL.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HERE_CANCEL))
                        .executes(tpHereCancelCommand)
                        .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                                .suggests(buildReqIndexSuggestions(EnumTeleportType.TP_HERE, false))
                                .executes(tpHereCancelCommand)
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(tpHereCancelCommand)
                        )
                        .then(Commands.argument("requestId", StringArgumentType.word())
                                .executes(tpHereCancelCommand)
                        );// endregion tphCancel
        LiteralArgumentBuilder<CommandSourceStack> tpRandom = // region tpRandom
                Commands.literal(CommonConfig.COMMAND_TP_RANDOM.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_RANDOM))
                        .executes(tpRandomCommand)
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .suggests(rangeSuggestions)
                                .executes(tpRandomCommand)
                                .then(Commands.argument("safe", StringArgumentType.word())
                                        .suggests(safeSuggestions)
                                        .executes(tpRandomCommand)
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(tpRandomCommand)
                                        )
                                )
                        );// endregion tpRandom
        LiteralArgumentBuilder<CommandSourceStack> tpSpawn = // region tpSpawn
                Commands.literal(CommonConfig.COMMAND_TP_SPAWN.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_SPAWN))
                        .executes(tpSpawnCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpSpawnCommand)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_SPAWN_OTHER))
                                        .executes(tpSpawnCommand)
                                )
                        );// endregion tpSpawn
        LiteralArgumentBuilder<CommandSourceStack> tpWorldSpawn = // region tpWorldSpawn
                Commands.literal(CommonConfig.COMMAND_TP_WORLD_SPAWN.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_WORLD_SPAWN))
                        .executes(tpWorldSpawnCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpWorldSpawnCommand)
                        );// endregion tpWorldSpawn
        LiteralArgumentBuilder<CommandSourceStack> tpTop = // region tpTop
                Commands.literal(CommonConfig.COMMAND_TP_TOP.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_TOP))
                        .executes(tpTopCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpTopCommand)
                        );// endregion tpTop
        LiteralArgumentBuilder<CommandSourceStack> tpBottom = // region tpBottom
                Commands.literal(CommonConfig.COMMAND_TP_BOTTOM.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_BOTTOM))
                        .executes(tpBottomCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpBottomCommand)
                        );// endregion tpBottom
        LiteralArgumentBuilder<CommandSourceStack> tpUp = // region tpUp
                Commands.literal(CommonConfig.COMMAND_TP_UP.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_UP))
                        .executes(tpUpCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpUpCommand)
                        );// endregion tpUp
        LiteralArgumentBuilder<CommandSourceStack> tpDown = // region tpDown
                Commands.literal(CommonConfig.COMMAND_TP_DOWN.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_DOWN))
                        .executes(tpDownCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpDownCommand)
                        );// endregion tpDown
        LiteralArgumentBuilder<CommandSourceStack> tpView = // region tpView
                Commands.literal(CommonConfig.COMMAND_TP_VIEW.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_VIEW))
                        .executes(tpViewCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpViewCommand)
                                .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                        .suggests(rangeSuggestions)
                                        .executes(tpViewCommand)
                                )
                        );// endregion tpView
        LiteralArgumentBuilder<CommandSourceStack> tpHome = // region tpHome
                Commands.literal(CommonConfig.COMMAND_TP_HOME.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HOME))
                        .executes(tpHomeCommand)
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(homeSuggestions)
                                .executes(tpHomeCommand)
                                .then(Commands.argument("safe", BoolArgumentType.bool())
                                        .suggests((context, builder) -> {
                                            String name = getStringDefault(context, "name", null);
                                            if ("true".equals(name) || "false".equals(name)) {
                                                ServerPlayer player = context.getSource().getPlayerOrException();
                                                PlayerTeleportData data = PlayerTeleportData.getData(player);
                                                for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
                                                    builder.suggest(keyValue.getKey());
                                                }
                                                if (data.getHomeCoordinate().keySet().stream()
                                                        .anyMatch(kv -> kv.getValue().equals("true") || kv.getValue().equals("false"))) {
                                                    builder.suggest("true");
                                                    builder.suggest("false");
                                                }
                                            } else {
                                                builder.suggest("true");
                                                builder.suggest("false");
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(tpHomeCommand)
                                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    PlayerTeleportData data = PlayerTeleportData.getData(player);
                                                    String name = StringArgumentType.getString(context, "name");
                                                    for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
                                                        if (keyValue.getValue().equals(name))
                                                            builder.suggest(keyValue.getKey());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(tpHomeCommand)
                                        )
                                )
                        )
                        .then(Commands.argument("safe", BoolArgumentType.bool())
                                .executes(tpHomeCommand)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        // .suggests(dimensionSuggestions)
                                        .executes(tpHomeCommand)
                                )
                        );// endregion tpHome
        LiteralArgumentBuilder<CommandSourceStack> setHome = // region setHome
                Commands.literal(CommonConfig.COMMAND_SET_HOME.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HOME))
                        .executes(setHomeCommand)
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    builder.suggest("home");
                                    builder.suggest("name");
                                    return builder.buildFuture();
                                })
                                .executes(setHomeCommand)
                                .then(Commands.argument("default", BoolArgumentType.bool())
                                        .executes(setHomeCommand)
                                )
                        );// endregion setHome
        LiteralArgumentBuilder<CommandSourceStack> delHome = // region delHome
                Commands.literal(CommonConfig.COMMAND_DEL_HOME.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HOME))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(homeSuggestions)
                                .executes(delHomeCommand)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            PlayerTeleportData data = PlayerTeleportData.getData(player);
                                            String name = StringArgumentType.getString(context, "name");
                                            for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
                                                if (keyValue.getValue().equals(name))
                                                    builder.suggest(keyValue.getKey());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(delHomeCommand)
                                )
                        );// endregion delHome
        LiteralArgumentBuilder<CommandSourceStack> getHome = // region getHome
                Commands.literal(CommonConfig.COMMAND_GET_HOME.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.GET_HOME))
                        .executes(getHomeCommand);// endregion getHome
        LiteralArgumentBuilder<CommandSourceStack> tpStage = // region tpStage
                Commands.literal(CommonConfig.COMMAND_TP_STAGE.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_STAGE))
                        .executes(tpStageCommand)
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(stageSuggestions)
                                .executes(tpStageCommand)
                                .then(Commands.argument("safe", StringArgumentType.word())
                                        .suggests(safeSuggestions)
                                        .executes(tpStageCommand)
                                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> {
                                                    WorldStageData data = WorldStageData.get();
                                                    String name = StringArgumentType.getString(context, "name");
                                                    for (KeyValue<String, String> keyValue : data.getStageCoordinate().keySet()) {
                                                        if (keyValue.getValue().equals(name))
                                                            builder.suggest(keyValue.getKey());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(tpStageCommand)
                                        )
                                )
                        );// endregion tpStage
        LiteralArgumentBuilder<CommandSourceStack> setStage = // region setStage
                Commands.literal(CommonConfig.COMMAND_SET_STAGE.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.SET_STAGE))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    builder.suggest("stage");
                                    return builder.buildFuture();
                                })
                                .executes(setStageCommand)
                                .then(Commands.argument("coordinate", Vec3Argument.vec3())
                                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                                .executes(setStageCommand)
                                        )
                                )
                        );// endregion setStage
        LiteralArgumentBuilder<CommandSourceStack> delStage = // region delStage
                Commands.literal(CommonConfig.COMMAND_DEL_STAGE.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.DEL_STAGE))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(stageSuggestions)
                                .executes(delStageCommand)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            WorldStageData data = WorldStageData.get();
                                            String name = StringArgumentType.getString(context, "name");
                                            for (KeyValue<String, String> keyValue : data.getStageCoordinate().keySet()) {
                                                if (keyValue.getValue().equals(name))
                                                    builder.suggest(keyValue.getKey());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(delStageCommand)
                                )
                        );// endregion delStage
        LiteralArgumentBuilder<CommandSourceStack> getStage = // region getStage
                Commands.literal(CommonConfig.COMMAND_GET_STAGE.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.GET_STAGE))
                        .executes(getStageCommand);// endregion getStage
        LiteralArgumentBuilder<CommandSourceStack> tpBack = // region tpBack
                Commands.literal(CommonConfig.COMMAND_TP_BACK.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_BACK))
                        .executes(tpBackCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpBackCommand)
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String type = getStringEmpty(context, "type");
                                            if (StringUtils.isNullOrEmptyEx(type)) {
                                                builder.suggest("ALL");
                                            }
                                            for (EnumTeleportType value : EnumTeleportType.values()) {
                                                if (StringUtils.isNullOrEmptyEx(type) || value.name().toLowerCase().contains(type.toLowerCase())) {
                                                    builder.suggest(value.name());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(tpBackCommand)
                                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    PlayerTeleportData data = PlayerTeleportData.getData(player);
                                                    EnumTeleportType type = EnumTeleportType.nullableValueOf(getStringEmpty(context, "type"));
                                                    data.getTeleportRecords().stream()
                                                            .filter(record -> type == null || record.getTeleportType().equals(type))
                                                            .filter(Objects::nonNull)
                                                            .map(record -> record.getBefore().getDimension().location().toString())
                                                            .filter(StringUtils::isNotNullOrEmpty)
                                                            .forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(tpBackCommand)
                                        )
                                )
                        );// endregion tpBack
        LiteralArgumentBuilder<CommandSourceStack> virtualOp = // region virtualOp
                Commands.literal(CommonConfig.COMMAND_VIRTUAL_OP.get())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                        .then(Commands.argument("operation", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest(EnumOperationType.ADD.name().toLowerCase());
                                    builder.suggest(EnumOperationType.SET.name().toLowerCase());
                                    builder.suggest(EnumOperationType.DEL.name().toLowerCase());
                                    builder.suggest(EnumOperationType.CLEAR.name().toLowerCase());
                                    builder.suggest(EnumOperationType.GET.name().toLowerCase());
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(virtualOpCommand)
                                        .then(Commands.argument("rules", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> {
                                                    String operation = StringArgumentType.getString(context, "operation");
                                                    if (operation.equalsIgnoreCase(EnumOperationType.GET.name().toLowerCase())
                                                            || operation.equalsIgnoreCase(EnumOperationType.CLEAR.name().toLowerCase())
                                                            || operation.equalsIgnoreCase(EnumOperationType.LIST.name().toLowerCase())) {
                                                        return builder.buildFuture();
                                                    }
                                                    String input = getStringEmpty(context, "rules").replace(" ", ",");
                                                    String[] split = input.split(",");
                                                    String current = input.endsWith(",") ? "" : split[split.length - 1];
                                                    for (EnumCommandType value : Arrays.stream(EnumCommandType.values())
                                                            .filter(EnumCommandType::isOp)
                                                            .filter(type -> Arrays.stream(split).noneMatch(in -> in.equalsIgnoreCase(type.name())))
                                                            .filter(type -> StringUtils.isNullOrEmptyEx(current) || type.name().toLowerCase().contains(current.toLowerCase()))
                                                            .sorted(Comparator.comparing(EnumCommandType::getSort))
                                                            .toList()) {
                                                        String suggest = value.name();
                                                        if (input.endsWith(",")) {
                                                            suggest = input + suggest;
                                                        }
                                                        builder.suggest(suggest);
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(virtualOpCommand)
                                        )
                                )
                        );// endregion virtualOp
        LiteralArgumentBuilder<CommandSourceStack> black = // region black
                Commands.literal("black")
                        .then(Commands.literal("get")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    PlayerTeleportData data = PlayerTeleportData.getData(player);
                                    PlayerAccess access = data.getAccess();
                                    Component msg;
                                    if (CollectionUtils.isNullOrEmpty(access.getBlackList())) {
                                        msg = Component.translatable(EnumI18nType.MESSAGE, "list_is_empty", getBlacklistOrWhitelistHelp(player, true));
                                    } else {
                                        msg = Component.translatable(EnumI18nType.MESSAGE, "list_detail"
                                                , getBlacklistOrWhitelistHelp(player, true)
                                                , access.getBlackList().stream()
                                                        .map(NarcissusUtils::getPlayerNameByUUIDString)
                                                        .collect(Collectors.joining(","))
                                        );
                                    }
                                    NarcissusUtils.sendMessage(player, msg);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                            if (CollectionUtils.isNullOrEmpty(players)) {
                                                return 0;
                                            }
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            PlayerTeleportData data = PlayerTeleportData.getData(player);
                                            PlayerAccess access = data.getAccess();
                                            String[] uuids = players.stream().map(NarcissusUtils::getPlayerUUIDString).toArray(String[]::new);
                                            Component msg;
                                            if (access.addBlackList(uuids)) {
                                                data.setDirty();
                                                msg = Component.translatable(EnumI18nType.MESSAGE, "list_add_success"
                                                        , getBlacklistOrWhitelistHelp(player, true)
                                                        , players.stream().map(NarcissusUtils::getPlayerName)
                                                                .collect(Collectors.joining(","))
                                                );
                                            } else {
                                                msg = Component.translatable(EnumI18nType.MESSAGE, "list_add_fail", getBlacklistOrWhitelistHelp(player, true), getBlacklistOrWhitelistHelp(player, false));
                                            }
                                            NarcissusUtils.sendMessage(player, msg);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("del")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                            if (CollectionUtils.isNullOrEmpty(players)) {
                                                return 0;
                                            }
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            PlayerTeleportData data = PlayerTeleportData.getData(player);
                                            PlayerAccess access = data.getAccess();
                                            String[] uuids = players.stream().map(NarcissusUtils::getPlayerUUIDString).toArray(String[]::new);
                                            access.removeBlackList(uuids);
                                            data.setDirty();
                                            Component msg = Component.translatable(EnumI18nType.MESSAGE, "remove_success");
                                            if (CollectionUtils.isNullOrEmpty(access.getBlackList())) {
                                                msg.append(Component.translatable(EnumI18nType.MESSAGE, "list_is_empty", getBlacklistOrWhitelistHelp(player, true)));
                                            } else {
                                                msg.append(Component.translatable(EnumI18nType.MESSAGE, "list_detail"
                                                        , getBlacklistOrWhitelistHelp(player, true)
                                                        , access.getBlackList().stream()
                                                                .map(NarcissusUtils::getPlayerNameByUUIDString)
                                                                .collect(Collectors.joining(","))
                                                ));
                                            }
                                            NarcissusUtils.sendMessage(player, msg);
                                            return 1;
                                        })
                                )
                        );// endregion black
        LiteralArgumentBuilder<CommandSourceStack> white = // region white
                Commands.literal("white")
                        .then(Commands.literal("get")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    PlayerTeleportData data = PlayerTeleportData.getData(player);
                                    PlayerAccess access = data.getAccess();
                                    Component msg = getWhiteListMessage(player, access);
                                    NarcissusUtils.sendMessage(player, msg);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(addWhiteListCommand)
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (String mode : whiteListModes) builder.suggest(mode);
                                                    return builder.buildFuture();
                                                })
                                                .executes(addWhiteListCommand)
                                        )
                                )
                        )
                        .then(Commands.literal("del")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(removeWhiteListCommand)
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (String mode : whiteListModes) builder.suggest(mode);
                                                    return builder.buildFuture();
                                                })
                                                .executes(removeWhiteListCommand)
                                        )
                                )
                        );// endregion white

        // 注册简短的指令
        {
            // 移除原版tp指令
            if (CommonConfig.REMOVE_ORIGINAL_TP.get()) {
                for (String fieldName : FieldUtils.getPrivateFieldNames(CommandNode.class, Map.class)) {
                    try {
                        Map<String, ?> map = (Map<String, ?>) FieldUtils.getPrivateFieldValue(CommandNode.class, dispatcher.getRoot(), fieldName);
                        if (map != null) {
                            map.remove("tp");
                            map.remove("teleport");
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to remove original command: ", e);
                    }
                }
            }

            // 设置语言 /language
            if (CommonConfig.CONCISE_LANGUAGE.get()) {
                dispatcher.register(language);
            }

            // 获取玩家UUID /uuid
            if (CommonConfig.CONCISE_UUID.get()) {
                dispatcher.register(uuid);
            }

            // 获取当前世界的维度ID /dim
            if (CommonConfig.CONCISE_DIMENSION.get()) {
                dispatcher.register(dim);
            }

            // 获取玩家传送卡数量 /card
            if (CommonConfig.CONCISE_CARD.get() && CommonConfig.TELEPORT_CARD.get()) {
                dispatcher.register(card);
            }

            // 分享坐标 /share
            if (CommonConfig.CONCISE_SHARE.get() && CommonConfig.SWITCH_SHARE.get()) {
                dispatcher.register(share);
            }

            // 自杀或毒杀 /feed
            if (CommonConfig.CONCISE_FEED.get() && CommonConfig.SWITCH_FEED.get()) {
                dispatcher.register(feed);
            }

            // 传送至指定位置 /tpx
            if (CommonConfig.CONCISE_TP_COORDINATE.get() && CommonConfig.SWITCH_TP_COORDINATE.get()) {
                dispatcher.register(tpx);
            }

            // 传送至指定结构或生物群系 /tpst
            if (CommonConfig.CONCISE_TP_STRUCTURE.get() && CommonConfig.SWITCH_TP_STRUCTURE.get()) {
                dispatcher.register(tpst);
            }

            // 传送请求 /tpa
            if (CommonConfig.CONCISE_TP_ASK.get() && CommonConfig.SWITCH_TP_ASK.get()) {
                dispatcher.register(tpa);
            }

            // 传送请求同意 /tpay
            if (CommonConfig.CONCISE_TP_ASK_YES.get() && CommonConfig.SWITCH_TP_ASK.get()) {
                dispatcher.register(tpaYes);
            }

            // 传送请求拒绝 /tpan
            if (CommonConfig.CONCISE_TP_ASK_NO.get() && CommonConfig.SWITCH_TP_ASK.get()) {
                dispatcher.register(tpaNo);
            }

            // 传送请求取消 /tpac
            if (CommonConfig.CONCISE_TP_ASK_CANCEL.get() && CommonConfig.SWITCH_TP_ASK.get()) {
                dispatcher.register(tpaCancel);
            }

            // 被传送请求 /tph
            if (CommonConfig.CONCISE_TP_HERE.get() && CommonConfig.SWITCH_TP_HERE.get()) {
                dispatcher.register(tph);
            }

            // 被传送请求同意 /tphy
            if (CommonConfig.CONCISE_TP_HERE_YES.get() && CommonConfig.SWITCH_TP_HERE.get()) {
                dispatcher.register(tphYes);
            }

            // 被传送请求拒绝 /tphn
            if (CommonConfig.CONCISE_TP_HERE_NO.get() && CommonConfig.SWITCH_TP_HERE.get()) {
                dispatcher.register(tphNo);
            }

            // 被传送请求取消 /tphc
            if (CommonConfig.CONCISE_TP_HERE_CANCEL.get() && CommonConfig.SWITCH_TP_HERE.get()) {
                dispatcher.register(tphCancel);
            }

            // 随机传送，允许指定范围 /tpr
            if (CommonConfig.CONCISE_TP_RANDOM.get() && CommonConfig.SWITCH_TP_RANDOM.get()) {
                dispatcher.register(tpRandom);
            }

            // 传送至出生点 /tps
            if (CommonConfig.CONCISE_TP_SPAWN.get() && CommonConfig.SWITCH_TP_SPAWN.get()) {
                dispatcher.register(tpSpawn);
            }

            // 传送至世界出生点 /tpws
            if (CommonConfig.CONCISE_TP_WORLD_SPAWN.get() && CommonConfig.SWITCH_TP_WORLD_SPAWN.get()) {
                dispatcher.register(tpWorldSpawn);
            }

            // 传送至头顶最上方方块 /tpt
            if (CommonConfig.CONCISE_TP_TOP.get() && CommonConfig.SWITCH_TP_TOP.get()) {
                dispatcher.register(tpTop);
            }

            // 传送至脚下最下方方块 /tpb
            if (CommonConfig.CONCISE_TP_BOTTOM.get() && CommonConfig.SWITCH_TP_BOTTOM.get()) {
                dispatcher.register(tpBottom);
            }

            // 传送至头顶最近方块 /tpu
            if (CommonConfig.CONCISE_TP_UP.get() && CommonConfig.SWITCH_TP_UP.get()) {
                dispatcher.register(tpUp);
            }

            // 传送至脚下最近方块 /tpd
            if (CommonConfig.CONCISE_TP_DOWN.get() && CommonConfig.SWITCH_TP_DOWN.get()) {
                dispatcher.register(tpDown);
            }

            // 传送至视线尽头 /tpv
            if (CommonConfig.CONCISE_TP_VIEW.get() && CommonConfig.SWITCH_TP_VIEW.get()) {
                dispatcher.register(tpView);
            }

            // 传送至传送点 /home
            if (CommonConfig.CONCISE_TP_HOME.get() && CommonConfig.SWITCH_TP_HOME.get()) {
                dispatcher.register(tpHome);
            }

            // 添加传送点 /sethome
            if (CommonConfig.CONCISE_SET_HOME.get() && CommonConfig.SWITCH_TP_HOME.get()) {
                dispatcher.register(setHome);
            }

            // 删除传送点 /delhome
            if (CommonConfig.CONCISE_DEL_HOME.get() && CommonConfig.SWITCH_TP_HOME.get()) {
                dispatcher.register(delHome);
            }

            // 获取传送点 /gethome
            if (CommonConfig.CONCISE_GET_HOME.get() && CommonConfig.SWITCH_TP_HOME.get()) {
                dispatcher.register(getHome);
            }

            // 传送至驿站 /stage
            if (CommonConfig.CONCISE_TP_STAGE.get() && CommonConfig.SWITCH_TP_STAGE.get()) {
                dispatcher.register(tpStage);
            }

            // 添加驿站 /setstage
            if (CommonConfig.CONCISE_SET_STAGE.get() && CommonConfig.SWITCH_TP_STAGE.get()) {
                dispatcher.register(setStage);
            }

            // 删除驿站 /delstage
            if (CommonConfig.CONCISE_DEL_STAGE.get() && CommonConfig.SWITCH_TP_STAGE.get()) {
                dispatcher.register(delStage);
            }

            // 获取驿站 /getstage
            if (CommonConfig.CONCISE_GET_STAGE.get() && CommonConfig.SWITCH_TP_STAGE.get()) {
                dispatcher.register(getStage);
            }

            // 返回上次离开地方 /back
            if (CommonConfig.CONCISE_TP_BACK.get() && CommonConfig.SWITCH_TP_BACK.get()) {
                dispatcher.register(tpBack);
            }

            // 设置虚拟权限
            if (CommonConfig.CONCISE_VIRTUAL_OP.get()) {
                dispatcher.register(virtualOp);
            }

        }

        // 注册有前缀的指令
        {
            dispatcher.register(Commands.literal(NarcissusUtils.getCommandPrefix())
                    .executes(helpCommand)
                    .then(Commands.literal("help")
                            .executes(helpCommand)
                            .then(Commands.argument("command", StringArgumentType.word())
                                    .suggests(helpSuggestions)
                                    .executes(helpCommand)
                            )
                    )
                    // 设置语言 /narcissus language
                    .then(language)
                    // 获取当前世界的维度ID /narcissus dim
                    .then(dim)
                    // 获取玩家UUID /narcissus uuid
                    .then(uuid)
                    // 获取玩家传送卡数量 /narcissus card
                    .then(card)
                    // 分享坐标 /narcissus share
                    .then(share)
                    // 自杀或毒杀 /narcissus feed
                    .then(feed)
                    // 传送至指定位置 /narcissus tpx
                    .then(tpx)
                    // 传送至指定结构或生物群系 /narcissus tpst
                    .then(tpst)
                    // 传送请求 /narcissus tpa
                    .then(tpa)
                    // 传送请求同意 /narcissus tpay
                    .then(tpaYes)
                    // 传送请求拒绝 /narcissus tpan
                    .then(tpaNo)
                    // 传送请求取消 /narcissus tpac
                    .then(tpaCancel)
                    // 被传送请求 /narcissus tph
                    .then(tph)
                    // 被传送请求同意 /narcissus tphy
                    .then(tphYes)
                    // 被传送请求拒绝 /narcissus tphn
                    .then(tphNo)
                    // 被传送请求取消 /narcissus tphc
                    .then(tphCancel)
                    // 随机传送，允许指定范围 /narcissus tpr
                    .then(tpRandom)
                    // 传送至出生点 /narcissus tps
                    .then(tpSpawn)
                    // 传送至世界出生点 /narcissus tpws
                    .then(tpWorldSpawn)
                    // 传送至头顶最上方方块 /narcissus tpt
                    .then(tpTop)
                    // 传送至脚下最下方方块 /narcissus tpb
                    .then(tpBottom)
                    // 传送至头顶最近方块 /narcissus tpu
                    .then(tpUp)
                    // 传送至脚下最近方块 /narcissus tpd
                    .then(tpDown)
                    // 传送至视线尽头 /narcissus tpv
                    .then(tpView)
                    // 传送至传送点 /narcissus home
                    .then(tpHome)
                    // 添加传送点 /narcissus sethome
                    .then(setHome)
                    // 删除传送点 /narcissus delhome
                    .then(delHome)
                    // 获取传送点 /narcissus gethome
                    .then(getHome)
                    // 传送至驿站 /narcissus stage
                    .then(tpStage)
                    // 添加驿站 /narcissus setstage
                    .then(setStage)
                    // 删除驿站 /narcissus delstage
                    .then(delStage)
                    // 获取驿站 /narcissus getstage
                    .then(getStage)
                    // 返回上次离开地方 /narcissus back
                    .then(tpBack)
                    // 设置虚拟权限 /narcissus opv
                    .then(virtualOp)
                    // 获取服务器配置 /narcissus config get
                    .then(Commands.literal("config")
                            .then(Commands.literal("teleportCard")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        Component msg = Component.translatable(EnumI18nType.MESSAGE, "server_config_status"
                                                , I18nUtils.enabled(CommonConfig.TELEPORT_CARD.get())
                                                , Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.WORD, "teleport_card"));
                                        NarcissusUtils.sendMessage(player, msg);
                                        return 1;
                                    })
                                    .then(Commands.argument("bool", BoolArgumentType.bool())
                                            .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                            .executes(context -> {
                                                boolean bool = BoolArgumentType.getBool(context, "bool");
                                                CommonConfig.TELEPORT_CARD.set(bool);
                                                ServerPlayer player = context.getSource().getPlayerOrException();
                                                Component msg = Component.translatable(EnumI18nType.MESSAGE, "server_config_status"
                                                        , I18nUtils.enabled(CommonConfig.TELEPORT_CARD.get())
                                                        , Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.WORD, "teleport_card"));
                                                NarcissusUtils.broadcastMessage(player, msg);
                                                return 1;
                                            })
                                    )
                            )
                            .then(Commands.literal("mode")
                                    .then(Commands.argument("mode", IntegerArgumentType.integer(0))
                                            .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                            .suggests((context, builder) -> {
                                                builder.suggest(0);
                                                builder.suggest(1);
                                                builder.suggest(2);
                                                builder.suggest(3);
                                                return builder.buildFuture();
                                            })
                                            .executes(context -> {
                                                int mode = IntegerArgumentType.getInteger(context, "mode");
                                                CommandSourceStack source = context.getSource();
                                                String lang = ServerConfig.DEFAULT_LANGUAGE.get();
                                                if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                                                    lang = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
                                                }
                                                switch (mode) {
                                                    case 0:
                                                        ServerConfig.resetConfig();
                                                        CommonConfig.resetConfig();
                                                        break;
                                                    case 1:
                                                        ServerConfig.resetConfigWithMode1();
                                                        CommonConfig.resetConfigWithMode1();
                                                        break;
                                                    case 2:
                                                        ServerConfig.resetConfigWithMode2();
                                                        CommonConfig.resetConfigWithMode2();
                                                        break;
                                                    case 3:
                                                        ServerConfig.resetConfigWithMode3();
                                                        CommonConfig.resetConfigWithMode3();
                                                        break;
                                                    default: {
                                                        throw new IllegalArgumentException("Mode " + mode + " does not exist");
                                                    }
                                                }
                                                Component component = Component.translatable(lang, EnumI18nType.MESSAGE, "server_config_mode", mode);
                                                source.sendSuccess(component.toChatComponent(lang), false);

                                                // 更新权限信息
                                                source.getServer().getPlayerList().getPlayers()
                                                        .forEach(player -> source.getServer()
                                                                .getPlayerList()
                                                                .sendPlayerPermissionLevel(player)
                                                        );
                                                return 1;
                                            })
                                    )
                            )
                            .then(Commands.literal("language")
                                    .then(Commands.argument("language", StringArgumentType.word())
                                            .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                            .suggests((context, builder) -> {
                                                I18nUtils.getI18nFiles().forEach(builder::suggest);
                                                return builder.buildFuture();
                                            })
                                            .executes(context -> {
                                                String code = StringArgumentType.getString(context, "language");
                                                ServerConfig.DEFAULT_LANGUAGE.set(code);
                                                ServerPlayer player = context.getSource().getPlayerOrException();
                                                NarcissusUtils.broadcastMessage(player, Component.translatable(player, EnumI18nType.MESSAGE, "server_default_language", ServerConfig.DEFAULT_LANGUAGE.get()));
                                                return 1;
                                            })
                                    )
                            )
                            .then(white)
                            .then(black)
                    )
            );
        }
    }

    /**
     * 获取传送请求ID
     *
     * @param context      指令上下文
     * @param teleportType 传送类型
     * @param isTarget     是否根据接收方查找
     */
    private static String getRequestId(CommandContext<CommandSourceStack> context, EnumTeleportType teleportType, final boolean isTarget) {
        String result = null;
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            try { // 指定玩家
                ServerPlayer requester = EntityArgument.getPlayer(context, "player");
                Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> isTarget ? entry.getValue().getTarget().getUUID().equals(player.getUUID()) : entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                        .filter(entry -> isTarget ? entry.getValue().getRequester().getUUID().equals(requester.getUUID()) : entry.getValue().getTarget().getUUID().equals(requester.getUUID()))
                        .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                        .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                        .orElse(null);
                if (entry1 != null) {
                    result = entry1.getKey();
                }
            } catch (IllegalArgumentException ignored) {
                try { // 指定ID
                    result = StringArgumentType.getString(context, "requestId");
                    if (!NarcissusFarewell.getTeleportRequest().containsKey(result)) {
                        result = null;
                    }
                } catch (IllegalArgumentException ignored1) {
                    try { // 指定序号
                        int askIndex = IntegerArgumentType.getInteger(context, "requestIndex");
                        List<Map.Entry<String, TeleportRequest>> entryList = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                                .filter(entry -> !isTarget || !entry.getValue().isIgnore())
                                .filter(entry -> isTarget ? entry.getValue().getTarget().getUUID().equals(player.getUUID()) : entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                                .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                                // 使用负数实现倒序排列
                                .sorted(Comparator.comparing(entry -> -entry.getValue().getRequestTime().getTime()))
                                .toList();
                        if (askIndex > 0 && askIndex <= entryList.size()) {
                            result = entryList.get(askIndex - 1).getKey();
                        }
                    } catch (IllegalArgumentException ignored2) {
                        // 使用负数实现倒序排列
                        Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                                .filter(entry -> !isTarget || !entry.getValue().isIgnore())
                                .filter(entry -> isTarget ? entry.getValue().getTarget().getUUID().equals(player.getUUID()) : entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                                .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                                .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                                .orElse(null);
                        if (entry1 != null) {
                            result = entry1.getKey();
                        }
                    }
                }
            }
        } catch (CommandSyntaxException ignored) {
        }
        return result;
    }

    /**
     * 构建传送请求ID补全
     *
     * @param teleportType 传送类型
     * @param isTarget     是否根据接收方查找
     */
    private static SuggestionProvider<CommandSourceStack> buildReqIndexSuggestions(EnumTeleportType teleportType, final boolean isTarget) {
        return (context, builder) -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> isTarget ? entry.getValue().getRequester().getUUID().equals(player.getUUID()) : entry.getValue().getTarget().getUUID().equals(player.getUUID()))
                    .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                    .forEach(entry -> builder.suggest(entry.getKey()));
            for (int i = 0; i < NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> isTarget ? entry.getValue().getRequester().getUUID().equals(player.getUUID()) : entry.getValue().getTarget().getUUID().equals(player.getUUID()))
                    .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                    .count(); i++) {
                builder.suggest(i + 1);
            }
            return builder.buildFuture();
        };
    }

    /**
     * 传送解析前置校验
     *
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPre(CommandSourceStack source, EnumCommandType teleportType) {
        // 判断是否开启传送功能
        if (!NarcissusUtils.isCommandEnabled(teleportType)) {
            NarcissusUtils.sendTranslatableMessage(source, false, I18nUtils.getKey(EnumI18nType.MESSAGE, "command_disabled"));
            return true;
        }
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer player) {
            // 判断是否有冷却时间
            EnumTeleportType type = teleportType.toTeleportType();
            if (type != null) {
                int teleportCoolDown = NarcissusUtils.getTeleportCoolDown(player, type);
                if (teleportCoolDown > 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "command_cooldown"), teleportCoolDown);
                    return true;
                }
            }
            // 判断是否被敌对生物锁定
            if (ServerConfig.TP_WITH_ENEMY.get() && NarcissusUtils.isTargetedByHostile(player)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "locked_by_mob"));
                return true;
            }
        }
        return false;
    }

    /**
     * 传送解析后置校验
     *
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPost(TeleportRequest request) {
        return checkTeleportPost(request, false);
    }

    /**
     * 传送解析后置校验
     *
     * @param submit 是否收取代价
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPost(TeleportRequest request, boolean submit) {
        boolean result;
        // 判断跨维度传送
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(request.getRequester(), request.getTarget().getLevel().dimension(), request.getTeleportType());
        // 判断是否有传送代价
        result = result && NarcissusUtils.validTeleportCost(request, submit);
        // 判断是否被敌对生物锁定
        if (ServerConfig.TP_WITH_ENEMY.get() && NarcissusUtils.isTargetedByHostile(request.getRequester())) {
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.MESSAGE, "locked_by_mob"));
            result = false;
        }
        return !result;
    }

    /**
     * 传送解析后置校验
     *
     * @param player 请求传送的玩家
     * @param target 目标坐标
     * @param type   传送类型
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPost(ServerPlayer player, Coordinate target, EnumTeleportType type) {
        return checkTeleportPost(player, target, type, false);
    }

    /**
     * 传送解析后置校验
     *
     * @param player 请求传送的玩家
     * @param target 目标坐标
     * @param type   传送类型
     * @param submit 是否收取代价
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPost(ServerPlayer player, Coordinate target, EnumTeleportType type, boolean submit) {
        boolean result;
        // 判断跨维度传送
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(player, target.getDimension(), type);
        // 判断是否有传送代价
        result = result && NarcissusUtils.validTeleportCost(player, target, type, submit);
        // 判断是否被敌对生物锁定
        if (ServerConfig.TP_WITH_ENEMY.get() && NarcissusUtils.isTargetedByHostile(player)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "locked_by_mob"));
            result = false;
        }
        return !result;
    }

    private static String getStringEmpty(CommandContext<?> context, String name) {
        return getStringDefault(context, name, "");
    }

    private static String getStringDefault(CommandContext<?> context, String name, String defaultValue) {
        String result;
        try {
            result = StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * 若为第一次使用指令则进行提示
     */
    private static void notifyHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player) {
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            if (!data.isNotified()) {
                Component button = Component.literal("/" + NarcissusUtils.getCommandPrefix())
                        .setColor(EnumMCColor.AQUA.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + NarcissusUtils.getCommandPrefix()))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/" + NarcissusUtils.getCommandPrefix())
                                .toTextComponent())
                        );
                NarcissusUtils.sendMessage(player, Component.translatable(EnumI18nType.MESSAGE, "notify_help", button));
                data.setNotified(true);
            }
        }
    }

    private static Component getWhiteListMessage(ServerPlayer player, PlayerAccess access) {
        Component msg;
        if (CollectionUtils.isNullOrEmpty(access.getWhiteList())) {
            msg = Component.translatable(EnumI18nType.MESSAGE, "list_is_empty", getBlacklistOrWhitelistHelp(player, false));
        } else {
            String language = NarcissusUtils.getPlayerLanguage(player);
            Component playerList = Component.empty();
            access.getWhiteList().stream()
                    .map(s -> {
                        Component flag = Component.literal(NarcissusUtils.getPlayerNameByUUIDString(s));
                        if (access.getAutoTpaList().contains(s)) {
                            flag.append(Component.literal("A")
                                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                            , Component.translatable(EnumI18nType.MESSAGE, "auto_accept_tpa")
                                            .toChatComponent(language))
                                    )
                            );
                        }
                        if (access.getAutoTphList().contains(s)) {
                            flag.append(Component.literal("H")
                                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                            , Component.translatable(EnumI18nType.MESSAGE, "auto_accept_tph")
                                            .toChatComponent(language))
                                    )
                            );
                        }
                        if (CollectionUtils.isNotNullOrEmpty(flag.getChildren())) {
                            flag.appendIndex(0, "[").append("]");
                        }
                        return flag;
                    }).forEach(playerList::append);
            msg = Component.translatable(EnumI18nType.MESSAGE, "list_detail"
                    , getBlacklistOrWhitelistHelp(player, false)
                    , playerList
            );
        }
        return msg;
    }

    public static Component getBlacklistOrWhitelistHelp(Player player, boolean black) {
        return Component.translatable(EnumI18nType.MESSAGE, black ? "blacklist" : "whitelist")
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                        , Component.translatable(EnumI18nType.MESSAGE, black ? "blacklist_help" : "whitelist_help")
                        .toChatComponent(NarcissusUtils.getPlayerLanguage(player)))
                );
    }

}
