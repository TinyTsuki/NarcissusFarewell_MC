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
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.util.*;

import java.util.*;
import java.util.stream.Collectors;

public class FarewellCommand {

    public static final List<KeyValue<String, ECommandType>> HELP_MESSAGE = Arrays.stream(ECommandType.values())
            .map(type -> {
                String command = NarcissusUtils.getCommand(type);
                if (StringUtils.isNotNullOrEmpty(command)) {
                    return new KeyValue<>(command, type);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .filter(keyValue -> !keyValue.getValue().isIgnore())
            .sorted(Comparator.comparing(keyValue -> keyValue.getValue().getSort()))
            .collect(Collectors.toList());

    /*
        OP权限等级：
            1：绕过服务器原版的出生点保护系统，可以破坏出生点地形。
            2：使用原版单机一切作弊指令（除了/publish，因为其只能在单机使用，/debug也不能使用）。
            3：可以使用大多数多人游戏指令，例如/op，/ban（/debug属于3级OP使用的指令）。
            4：使用所有命令，可以使用/stop关闭服务器。

        ‌颜色代码‌：
            §0：黑色 §1：深蓝 §2：深绿 §3：天蓝
            §4：红色 §5：深紫 §6：金黄 §7：浅灰
            §8：深灰 §9：淡紫 §a：浅绿 §b：淡蓝
            §c：淡红 §d：淡紫 §e：淡黄 §f：白色
    */

    /**
     * 注册命令到命令调度器
     *
     * @param dispatcher 命令调度器，用于管理服务器中的所有命令
     */
    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        Command<CommandSource> helpCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
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
                    KeyValue<String, ECommandType> keyValue = HELP_MESSAGE.get((page - 1) * ServerConfig.HELP_INFO_NUM_PER_PAGE.get() + i);
                    Component commandTips;
                    if (keyValue.getValue().name().toLowerCase().contains("concise")) {
                        commandTips = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.COMMAND, "concise", NarcissusUtils.getCommand(keyValue.getValue().replaceConcise()));
                    } else {
                        commandTips = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.COMMAND, keyValue.getValue().name().toLowerCase());
                    }
                    commandTips.setColor(TextFormatting.GRAY.getColor());
                    helpInfo.append("/").append(keyValue.getKey())
                            .append(new Component(" -> ").setColor(TextFormatting.YELLOW.getColor()))
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
                        prevButton.setColor(EMCColor.AQUA.getColor())
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        String.format("/%s %s %d", NarcissusUtils.getCommandPrefix(), "help", page - 1)))
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "previous_page").toTextComponent()));
                    } else {
                        prevButton.setColor(EMCColor.DARK_AQUA.getColor());
                    }
                    helpInfo.append(prevButton);

                    helpInfo.append(Component.literal(String.format(" %s/%s "
                                    , StringUtils.padOptimizedLeft(page, String.valueOf(pages).length(), " ")
                                    , pages))
                            .setColor(EMCColor.WHITE.getColor()));

                    Component nextButton = Component.literal(" >>>");
                    if (page < pages) {
                        nextButton.setColor(EMCColor.AQUA.getColor())
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        String.format("/%s %s %d", NarcissusUtils.getCommandPrefix(), "help", page + 1)))
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "next_page").toTextComponent()));
                    } else {
                        nextButton.setColor(EMCColor.DARK_AQUA.getColor());
                    }
                    helpInfo.append(nextButton);
                }
            } else {
                ECommandType type = ECommandType.valueOf(command);
                helpInfo = Component.empty();
                helpInfo.append("/").append(NarcissusUtils.getCommand(type))
                        .append("\n")
                        .append(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.COMMAND, command.toLowerCase() + "_detail").setColor(TextFormatting.GRAY.getColor()));
            }
            NarcissusUtils.sendMessage(player, helpInfo);
            return 1;
        };

        SuggestionProvider<CommandSource> helpSuggestions = (context, builder) -> {
            String input = getStringEmpty(context, "command");
            boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
            int totalPages = (int) Math.ceil((double) HELP_MESSAGE.size() / ServerConfig.HELP_INFO_NUM_PER_PAGE.get());
            for (int i = 0; i < totalPages && isInputEmpty; i++) {
                builder.suggest(i + 1);
            }
            for (ECommandType type : Arrays.stream(ECommandType.values())
                    .filter(type -> type != ECommandType.HELP)
                    .filter(type -> !type.isIgnore())
                    .filter(type -> !type.name().toLowerCase().contains("concise"))
                    .filter(type -> isInputEmpty || type.name().toLowerCase().contains(input.toLowerCase()))
                    .sorted(Comparator.comparing(ECommandType::getSort))
                    .collect(Collectors.toList())) {
                builder.suggest(type.name());
            }
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSource> dimensionSuggestions = (context, builder) -> {
            for (ServerWorld level : context.getSource().getServer().getAllLevels()) {
                builder.suggest(level.dimension().location().toString());
            }
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSource> safeSuggestions = (context, builder) -> {
            builder.suggest("safe");
            builder.suggest("unsafe");
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSource> rangeSuggestions = (context, builder) -> {
            for (int i = 1; i <= 5; i++) {
                int index = (int) Math.pow(10, i);
                if (index <= ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get()) {
                    builder.suggest(index);
                }
            }
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSource> structureSuggestions = (context, builder) -> {
            String input = getStringEmpty(context, "rules");
            boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
            ForgeRegistries.STRUCTURE_FEATURES.getKeys().stream()
                    .filter(resourceLocation -> isInputEmpty || resourceLocation.toString().contains(input))
                    .forEach(location -> builder.suggest(location.toString()));
            ForgeRegistries.BIOMES.getValues().stream()
                    .filter(resourceLocation -> isInputEmpty || resourceLocation.toString().contains(input))
                    .forEach(biome -> builder.suggest(biome.toString()));
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSource> homeSuggestions = (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
                builder.suggest(StringUtils.formatString(key.getValue()));
            }
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSource> stageSuggestions = (context, builder) -> {
            for (KeyValue<String, String> key : WorldStageData.get().getStageCoordinate().keySet()) {
                builder.suggest(StringUtils.formatString(key.getValue()));
            }
            return builder.buildFuture();
        };

        Command<CommandSource> languageCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            IPlayerTeleportData signInData = PlayerTeleportDataCapability.getData(player);
            String language = StringArgumentType.getString(context, "language");
            if (I18nUtils.getI18nFiles().contains(language)) {
                signInData.setLanguage(language);
                NarcissusUtils.sendMessage(player, Component.translatable(player, EI18nType.MESSAGE, "player_default_language", language));
            } else if ("server".equalsIgnoreCase(language) || "client".equalsIgnoreCase(language)) {
                signInData.setLanguage(language);
                NarcissusUtils.sendMessage(player, Component.translatable(player, EI18nType.MESSAGE, "player_default_language", language));
            } else {
                NarcissusUtils.sendMessage(player, Component.translatable(player, EI18nType.MESSAGE, "language_not_exist").setColor(0xFFFF0000));
            }
            return 1;
        };

        Command<CommandSource> dimCommand = (context) -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.DIMENSION)) return 0;
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String dimString = player.getLevel().dimension().location().toString();
            Component dim = Component.literal(dimString);
            dim.setColor(EMCColor.GREEN.getColor())
                    .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dimString))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "chat_copy_click").toTextComponent()));
            Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "dimension_info", dim);
            NarcissusUtils.sendMessage(player, msg);
            return 1;
        };

        Command<CommandSource> uuidCommand = (context) -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.UUID)) return 0;
            CommandSource source = context.getSource();
            ServerPlayerEntity target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                    target = source.getPlayerOrException();
                } else {
                    throw CommandSource.ERROR_NOT_PLAYER.create();
                }
            }
            String language = ServerConfig.DEFAULT_LANGUAGE.get();
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                language = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
            }
            Component uuid = Component.literal(target.getStringUUID());
            uuid.setColor(EMCColor.GREEN.getColor())
                    .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, target.getStringUUID()))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(language, EI18nType.MESSAGE, "chat_copy_click").toTextComponent()));
            Component component = Component.translatable(language, EI18nType.MESSAGE, "player_uuid", target.getDisplayName().getString(), uuid);
            source.sendSuccess(component.toChatComponent(language), false);
            return 1;
        };

        Command<CommandSource> cardCommand = (context) -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.CARD)) return 0;
            CommandSource source = context.getSource();
            String type = getStringDefault(context, "type", "get");
            ServerPlayerEntity target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                    target = source.getPlayerOrException();
                } else {
                    throw CommandSource.ERROR_NOT_PLAYER.create();
                }
            }
            int num = 0;
            try {
                num = IntegerArgumentType.getInteger(context, "num");
            } catch (IllegalArgumentException ignored) {
            }

            String language = ServerConfig.DEFAULT_LANGUAGE.get();
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                language = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
            }
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(target);
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
            Component component = Component.translatable(language, EI18nType.MESSAGE, "player_card"
                    , target.getDisplayName().getString()
                    , data.getTeleportCard());
            source.sendSuccess(component.toChatComponent(language), false);
            return 1;
        };

        Command<CommandSource> shareCommand = (context) -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.SHARE)) return 0;
            CommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrException();

            String name = getStringDefault(context, "name", "Shared");

            List<ServerPlayerEntity> targetList;
            try {
                targetList = new ArrayList<>(EntityArgument.getPlayers(context, "player"));
            } catch (IllegalArgumentException ignored) {
                targetList = new ArrayList<>(context.getSource().getServer().getPlayerList().getPlayers());
            }

            Component nameComponent;
            Component tpButton = Component.translatable(EI18nType.MESSAGE, "tp_button");
            // Component addButton = Component.translatable(EI18nType.MESSAGE, "add_button");
            Component copyButton = Component.translatable(EI18nType.MESSAGE, "copy_button");

            // 若为home
            if (name.contains("->")) {
                IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
                KeyValue<String, Coordinate> keyValue = data.getHomeCoordinate().entrySet().stream()
                        .map(entry -> new KeyValue<>(entry.getKey().getValue() + "->" + entry.getKey().getKey(), entry.getValue()))
                        .filter(kv -> name.equals(kv.getKey()))
                        .findFirst()
                        .orElse(new KeyValue<>(name, null));
                String[] split = keyValue.getKey().split("->");
                Coordinate coordinate = keyValue.getValue();
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found_with_name_in_dimension")
                            , split[1], split[0]);
                    return 0;
                }
                nameComponent = Component.literal(split[0]);

                String tpCommand = String.format("/%s %s %s %s unsafe %s"
                        , NarcissusUtils.getCommand(ECommandType.TP_COORDINATE)
                        , coordinate.toXString()
                        , coordinate.toYString()
                        , coordinate.toZString()
                        , coordinate.getDimensionResourceId()
                );
                tpButton.setColor(EMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
                copyButton.setColor(EMCColor.GREEN.getColor())
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
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_not_found_with_name_in_dimension")
                            , split[1], split[0]);
                    return 0;
                }
                nameComponent = Component.literal(split[0]);

                String tpCommand = String.format("/%s %s %s %s unsafe %s"
                        , NarcissusUtils.getCommand(ECommandType.TP_COORDINATE)
                        , coordinate.toXString()
                        , coordinate.toYString()
                        , coordinate.toZString()
                        , coordinate.getDimensionResourceId()
                );
                tpButton.setColor(EMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
                copyButton.setColor(EMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
            }
            // 玩家当前坐标
            else {
                nameComponent = Component.literal(name);
                Coordinate coordinate = new Coordinate(player);

                String tpCommand = String.format("/%s %s %s %s unsafe %s"
                        , NarcissusUtils.getCommand(ECommandType.TP_COORDINATE)
                        , coordinate.toXString()
                        , coordinate.toYString()
                        , coordinate.toZString()
                        , coordinate.getDimensionResourceId()
                );
                tpButton.setColor(EMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
                copyButton.setColor(EMCColor.GREEN.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
            }

            String lang = ServerConfig.DEFAULT_LANGUAGE.get();
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                lang = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
            }
            Component component = Component.translatable(lang, EI18nType.MESSAGE, "shared_coordinates"
                    , player.getDisplayName().getString()
                    , nameComponent
                    , tpButton
                    // , addButton
                    , copyButton);
            for (ServerPlayerEntity target : targetList) {
                if (!target.getUUID().equals(player.getUUID())) {
                    NarcissusUtils.sendMessage(target, component);
                }
            }
            source.sendSuccess(component.toChatComponent(lang), false);
            return 1;
        };

        Command<CommandSource> feedCommand = (context) -> {
            notifyHelp(context);
            CommandSource source = context.getSource();
            // 传送功能前置校验
            if (checkTeleportPre(source, ECommandType.FEED)) return 0;
            if (source.getEntity() == null) {
                List<ServerPlayerEntity> targetList;
                try {
                    targetList = new ArrayList<>(EntityArgument.getPlayers(context, "player"));
                } catch (IllegalArgumentException ignored) {
                    throw CommandSource.ERROR_NOT_PLAYER.create();
                }
                for (ServerPlayerEntity target : targetList) {
                    if (NarcissusUtils.killPlayer(target)) {
                        NarcissusUtils.broadcastMessage(source.getServer(), Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "died_of_narcissus_" + (new Random().nextInt(4) + 1), target.getDisplayName().getString()));
                    }
                }
            }
            // 如果命令来自玩家
            else if (source.getEntity() instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = source.getPlayerOrException();
                // 判断是否开启功能
                if (!ServerConfig.SWITCH_FEED.get()) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
                    return 0;
                }
                List<ServerPlayerEntity> targetList = new ArrayList<>();
                try {
                    targetList.addAll(EntityArgument.getPlayers(context, "player"));
                } catch (IllegalArgumentException ignored) {
                    targetList.add(player);
                }
                for (ServerPlayerEntity target : targetList) {
                    if (NarcissusUtils.killPlayer(target)) {
                        NarcissusUtils.broadcastMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "died_of_narcissus_" + (new Random().nextInt(4) + 1), target.getDisplayName().getString()));
                    }
                }
            }
            return 1;
        };

        Command<CommandSource> tpCoordinateCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_COORDINATE)) return 0;
            Coordinate coordinate;
            try {
                Vector3d pos = Vec3Argument.getCoordinates(context, "coordinate").getPosition(context.getSource());
                RegistryKey<World> targetLevel;
                try {
                    targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
                } catch (IllegalArgumentException ignored) {
                    targetLevel = player.getLevel().dimension();
                }
                coordinate = new Coordinate(pos.x(), pos.y(), pos.z(), player.yRot, player.xRot, targetLevel);
            } catch (IllegalArgumentException ignored) {
                ServerPlayerEntity target = EntityArgument.getPlayer(context, "player");
                coordinate = new Coordinate(target.getX(), target.getY(), target.getZ(), target.yRot, target.xRot, target.getLevel().dimension());
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_COORDINATE, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_COORDINATE);
            return 1;
        };

        Command<CommandSource> tpStructureCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_STRUCTURE)) return 0;
            ResourceLocation structId = ResourceLocationArgument.getId(context, "struct");
            Structure<?> structure = NarcissusUtils.getStructure(structId);
            Biome biome = NarcissusUtils.getBiome(structId);
            if (structure == null && biome == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "structure_biome_not_found"), structId);
                return 0;
            }
            int range;
            RegistryKey<World> targetLevel;
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, ETeleportType.TP_STRUCTURE, range);
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            RegistryKey<World> finalTargetLevel = targetLevel;
            int finalRange = range;
            player.connection.send(new SChatPacket(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "tp_structure_searching").toTextComponent(), ChatType.GAME_INFO, player.getUUID()));
            new Thread(() -> {
                Coordinate coordinate;
                if (biome != null) {
                    coordinate = NarcissusUtils.findNearestBiome(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(finalTargetLevel)), new Coordinate(player).setDimension(finalTargetLevel), biome, finalRange, 8);
                } else {
                    coordinate = NarcissusUtils.findNearestStruct(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(finalTargetLevel)), new Coordinate(player).setDimension(finalTargetLevel), structure, finalRange);
                }
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "structure_biome_not_found_in_range"), structId);
                    return;
                }
                coordinate.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
                // 验证传送代价
                if (checkTeleportPost(player, coordinate, ETeleportType.TP_STRUCTURE, true)) return;
                player.server.submit(() -> NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_STRUCTURE));
            }).start();
            return 1;
        };

        Command<CommandSource> tpAskCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_ASK)) return 0;
            ServerPlayerEntity target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                target = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                        .filter(request -> {
                            PlayerEntity entity = request.getTarget();
                            return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, ETeleportType.TP_ASK)
                                    || entity != null && entity.level.dimension() == player.getLevel().dimension();
                        })
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                                .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                        .getTarget();
            }
            if (target == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "player_not_found"));
                return 0;
            }

            // 验证并添加传送请求
            TeleportRequest request = new TeleportRequest()
                    .setRequester(player)
                    .setTarget(target)
                    .setTeleportType(ETeleportType.TP_ASK)
                    .setRequestTime(new Date());
            request.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            if (checkTeleportPost(request)) return 0;
            NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request);

            // 通知目标玩家
            {
                // 创建 "Yes" 按钮
                Component yesButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "yes_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_ASK_YES.get(), request.getRequestId())));
                // 创建 "No" 按钮
                Component noButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "no_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_ASK_NO.get(), request.getRequestId())));
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "tp_ask_request_received"
                        , player.getDisplayName().getString(), yesButton, noButton);
                NarcissusUtils.sendMessage(target, msg);
            }
            // 通知请求者
            {
                // 创建 "Cancel" 按钮
                Component cancelButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "cancel_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_ASK_CANCEL.get(), request.getRequestId())));
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "tp_ask_request_sent"
                        , target.getDisplayName().getString(), cancelButton);
                NarcissusUtils.sendMessage(player, msg);
            }
            return 1;
        };

        Command<CommandSource> tpAskYesCommand = context -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_ASK_YES)) return 0;
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_ASK, true);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            if (checkTeleportPost(request, true)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_invalid"));
                return 0;
            }
            NarcissusUtils.teleportTo(request);
            return 1;
        };

        Command<CommandSource> tpAskNoCommand = context -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_ASK_NO)) return 0;
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_ASK, true);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_rejected"), request.getTarget().getDisplayName().getString());
            return 1;
        };

        Command<CommandSource> tpAskCancelCommand = context -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_ASK_CANCEL)) return 0;
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_ASK, false);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_cancelled"), request.getRequester().getDisplayName().getString());
            if (!request.getRequester().getUUID().equals(request.getTarget().getUUID())) {
                NarcissusUtils.sendTranslatableMessage(request.getTarget(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_cancelled"), request.getRequester().getDisplayName().getString());
            }
            return 1;
        };

        Command<CommandSource> tpHereCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_HERE)) return 0;
            ServerPlayerEntity target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                target = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                        .filter(request -> {
                            PlayerEntity entity = request.getTarget();
                            return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, ETeleportType.TP_HERE)
                                    || entity != null && entity.level.dimension() == player.getLevel().dimension();
                        })
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                                .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                        .getTarget();
            }
            if (target == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "player_not_found"));
                return 0;
            }

            // 验证并添加传送请求
            TeleportRequest request = new TeleportRequest()
                    .setRequester(player)
                    .setTarget(target)
                    .setTeleportType(ETeleportType.TP_HERE)
                    .setRequestTime(new Date());
            request.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            if (checkTeleportPost(request)) return 0;
            NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request);

            // 通知目标玩家
            {
                // 创建 "Yes" 按钮
                Component yesButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "yes_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_HERE_YES.get(), request.getRequestId())));
                // 创建 "No" 按钮
                Component noButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "no_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_HERE_NO.get(), request.getRequestId())));
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "tp_here_request_received"
                        , player.getDisplayName().getString(), Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.WORD, request.isSafe() ? "tp_here_safe" : "tp_here_unsafe"), yesButton, noButton);
                NarcissusUtils.sendMessage(target, msg);
            }
            // 通知请求者
            {
                // 创建 "Cancel" 按钮
                Component cancelButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "cancel_button", NarcissusUtils.getPlayerLanguage(target))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_HERE_CANCEL.get(), request.getRequestId())));
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "tp_here_request_sent"
                        , target.getDisplayName().getString(), cancelButton);
                NarcissusUtils.sendMessage(player, msg);
            }
            return 1;
        };

        Command<CommandSource> tpHereYesCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_HERE_YES)) return 0;
            String id = getRequestId(context, ETeleportType.TP_HERE, true);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            if (checkTeleportPost(request, true)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_invalid"));
                return 0;
            }
            NarcissusUtils.teleportTo(request);
            return 1;
        };

        Command<CommandSource> tpHereNoCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_HERE_YES)) return 0;
            String id = getRequestId(context, ETeleportType.TP_HERE, true);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_rejected"), request.getTarget().getDisplayName().getString());
            return 1;
        };

        Command<CommandSource> tpHereCancelCommand = context -> {
            notifyHelp(context);
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_HERE_CANCEL)) return 0;
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_HERE, false);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_cancelled"), request.getRequester().getDisplayName().getString());
            if (!request.getRequester().getUUID().equals(request.getTarget().getUUID())) {
                NarcissusUtils.sendTranslatableMessage(request.getTarget(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_cancelled"), request.getRequester().getDisplayName().getString());
            }
            return 1;
        };

        Command<CommandSource> tpRandomCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_RANDOM)) return 0;
            RegistryKey<World> targetLevel;
            int range;
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, ETeleportType.TP_RANDOM, range);
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            Coordinate coordinate = Coordinate.random(player, range, targetLevel).setSafe(true);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_RANDOM, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_RANDOM);
            return 1;
        };

        Command<CommandSource> tpSpawnCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_SPAWN)) return 0;
            ServerPlayerEntity target;
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
                respawnPosition = target.getServer().getLevel(World.OVERWORLD).getSharedSpawnPos();
                coordinate.setDimension(World.OVERWORLD);
            }
            coordinate.fromBlockPos(respawnPosition);
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_SPAWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_SPAWN);
            return 1;
        };

        Command<CommandSource> tpWorldSpawnCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_WORLD_SPAWN)) return 0;
            Coordinate coordinate = new Coordinate(player);
            BlockPos respawnPosition = player.getLevel().getSharedSpawnPos();
            coordinate.setDimension(player.getLevel().dimension());
            if (respawnPosition == null) {
                respawnPosition = player.getServer().getLevel(World.OVERWORLD).getSharedSpawnPos();
                coordinate.setDimension(World.OVERWORLD);
            }
            coordinate.fromBlockPos(respawnPosition);
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_WORLD_SPAWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_WORLD_SPAWN);
            return 1;
        };

        Command<CommandSource> tpTopCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_TOP)) return 0;
            Coordinate coordinate = NarcissusUtils.findTopCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_top_not_found"));
                return 0;
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"))).setSafeMode(ESafeMode.Y_DOWN);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_TOP, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_TOP);
            return 1;
        };

        Command<CommandSource> tpBottomCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_BOTTOM)) return 0;
            Coordinate coordinate = NarcissusUtils.findBottomCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_bottom_not_found"));
                return 0;
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"))).setSafeMode(ESafeMode.Y_UP);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_BOTTOM, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_BOTTOM);
            return 1;
        };

        Command<CommandSource> tpUpCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_UP)) return 0;
            Coordinate coordinate = NarcissusUtils.findUpCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_up_not_found"));
                return 0;
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"))).setSafeMode(ESafeMode.Y_UP);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_UP, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_UP);
            return 1;
        };

        Command<CommandSource> tpDownCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_DOWN)) return 0;
            Coordinate coordinate = NarcissusUtils.findDownCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_down_not_found"));
                return 0;
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringDefault(context, "safe", "safe"))).setSafeMode(ESafeMode.Y_DOWN);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_DOWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_DOWN);
            return 1;
        };

        Command<CommandSource> tpViewCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_VIEW)) return 0;
            boolean safe = true;
            int range;
            safe = "safe".equalsIgnoreCase(getStringEmpty(context, "safe"));
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, ETeleportType.TP_VIEW, range);
            player.connection.send(new SChatPacket(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "tp_view_searching").toTextComponent(), ChatType.GAME_INFO, player.getUUID()));
            boolean finalSafe = safe;
            int finalRange = range;
            new Thread(() -> {
                Coordinate coordinate = NarcissusUtils.findViewEndCandidate(player, finalSafe, finalRange);
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, finalSafe ? "tp_view_safe_not_found" : "tp_view_not_found"));
                    return;
                }
                coordinate.setSafeMode(ESafeMode.Y_OFFSET_3);
                // 验证传送代价
                if (checkTeleportPost(player, coordinate, ETeleportType.TP_VIEW, true)) return;
                player.server.submit(() -> NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_VIEW));
            }).start();
            return 1;
        };

        Command<CommandSource> tpHomeCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_HOME)) return 0;
            RegistryKey<World> targetLevel = null;
            try {
                RegistryKey<World> targetDimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(StringArgumentType.getString(context, "dimension")));
                ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
                if (level != null) {
                    targetLevel = targetDimension;
                }
            } catch (IllegalArgumentException ignored) {
            }
            String name = getStringDefault(context, "name", null);
            Coordinate coordinate = NarcissusUtils.getPlayerHome(player, targetLevel, name);
            if (coordinate == null) {
                if (targetLevel == null && name == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found"));
                } else if (targetLevel != null && name == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found_in_dimension"), targetLevel.location().toString());
                } else if (targetLevel == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found_with_name"), name);
                } else {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found_with_name_in_dimension"), targetLevel.location().toString(), name);
                }
                return 0;
            }
            try {
                coordinate.setSafe(BoolArgumentType.getBool(context, "safe"));
            } catch (IllegalArgumentException ignored) {
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_HOME, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_HOME);
            return 1;
        };

        Command<CommandSource> setHomeCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.SET_HOME)) return 0;
            // 判断设置数量是否超过限制
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            if (data.getHomeCoordinate().size() >= ServerConfig.TELEPORT_HOME_LIMIT.get()) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_limit"), ServerConfig.TELEPORT_HOME_LIMIT.get());
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
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_already_exists"), key.getKey(), key.getValue());
                return 0;
            }
            data.addHomeCoordinate(key, coordinate);
            if (defaultHome) {
                if (data.getDefaultHome().containsKey(player.getLevel().dimension().location().toString())) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_default_remove"), data.getDefaultHome(player.getLevel().dimension().location().toString()).getValue());
                }
                data.addDefaultHome(player.getLevel().dimension().location().toString(), name);
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_set_default"), name, coordinate.toXyzString());
            } else {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_set"), name, coordinate.toXyzString());
            }
            return 1;
        };

        Command<CommandSource> delHomeCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.DEL_HOME)) return 0;
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            String name = StringArgumentType.getString(context, "name");
            String dimension;
            try {
                RegistryKey<World> targetLevel = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(StringArgumentType.getString(context, "dimension")));
                dimension = targetLevel.location().toString();
            } catch (IllegalArgumentException ignored) {
                dimension = NarcissusUtils.getHomeDimensionByName(player, name);
            }
            Coordinate remove = data.getHomeCoordinate().remove(new KeyValue<>(dimension, name));
            if (remove == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found_with_name_in_dimension"), dimension, name);
                return 0;
            }
            if (data.getDefaultHome().containsKey(dimension)) {
                if (data.getDefaultHome().get(dimension).equals(name)) {
                    data.getDefaultHome().remove(dimension);
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_default_remove"), name);
                }
            }
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_del"), dimension, name);
            return 1;
        };

        Command<CommandSource> getHomeCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.GET_HOME)) return 0;
            Component component;
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            String language = NarcissusUtils.getPlayerLanguage(player);
            if (data.getHomeCoordinate().isEmpty()) {
                component = Component.translatable(language, EI18nType.MESSAGE, "home_is_empty");
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
                    Component dimension = Component.literal(entry.getKey()).setColor(EMCColor.DARK_GREEN.getColor());
                    dimension.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()));
                    dimension.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(entry.getKey()).toTextComponent()));
                    dimension.append(Component.literal(": ").setColor(EMCColor.GRAY.getColor()));
                    for (KeyValue<String, Coordinate> coordinates : entry.getValue()) {
                        Component defHome;
                        if (data.getDefaultHome().getOrDefault(entry.getKey(), "").equalsIgnoreCase(coordinates.getKey())) {
                            defHome = Component.translatable(language, EI18nType.WORD, "default").setColor(EMCColor.GRAY.getColor());
                        } else {
                            defHome = Component.empty();
                        }
                        Component name = Component.translatable(language, EI18nType.MESSAGE, "home_info"
                                , coordinates.getKey()
                                , coordinates.getValue().toXString()
                                , coordinates.getValue().toYString()
                                , coordinates.getValue().toZString()
                                , defHome);
                        name.toChatComponent();
                        Component name_hover = Component.translatable(language, EI18nType.MESSAGE, "home_info_hover"
                                , coordinates.getKey()
                                , coordinates.getValue().toXString()
                                , coordinates.getValue().toYString()
                                , coordinates.getValue().toZString()
                                , defHome);
                        name.setColor(EMCColor.GREEN.getColor());
                        name.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name_hover.toString(true)));
                        name.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, name_hover.toChatComponent()));
                        dimension.append(name);
                        dimension.append(Component.literal(", ").setColor(EMCColor.GRAY.getColor()));
                    }
                    info.append(dimension).append("\n");
                }
                component = Component.translatable(language, EI18nType.MESSAGE, "home_is", info);
            }
            NarcissusUtils.sendMessage(player, component);
            return 1;
        };

        Command<CommandSource> tpStageCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_STAGE)) return 0;
            RegistryKey<World> targetLevel = null;
            String name = getStringDefault(context, "name", null);
            try {
                RegistryKey<World> targetDimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(StringArgumentType.getString(context, "dimension")));
                ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
                if (level != null) {
                    targetLevel = targetDimension;
                }
            } catch (IllegalArgumentException ignored) {
            }
            String dimension = targetLevel != null ? targetLevel.location().toString() : null;
            if (StringUtils.isNullOrEmptyEx(name)) {
                KeyValue<String, String> stageKey = NarcissusUtils.findNearestStageKey(player);
                if (stageKey == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_nearest_not_found"));
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
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_not_found"), name);
                    return 0;
                }
            } else {
                coordinate = stageData.getCoordinate(dimension, name);
                if (coordinate == null) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_not_found_with_name_in_dimension"), dimension, name);
                    return 0;
                }
            }
            coordinate.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_STAGE, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_STAGE);
            return 1;
        };

        Command<CommandSource> setStageCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.SET_STAGE)) return 0;
            WorldStageData stageData = WorldStageData.get();
            String name = StringArgumentType.getString(context, "name");
            RegistryKey<World> targetLevel;
            try {
                RegistryKey<World> targetDimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(StringArgumentType.getString(context, "dimension")));
                ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
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
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_already_exists"), key.getKey(), key.getValue());
                return 0;
            }
            Coordinate coordinate = new Coordinate(player).setDimension(targetLevel);
            try {
                coordinate.fromVector3d(Vec3Argument.getCoordinates(context, "coordinate").getPosition(context.getSource()));
            } catch (IllegalArgumentException ignored) {
            }
            stageData.addCoordinate(key, coordinate);
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_set"), name, coordinate.toXyzString());
            return 1;
        };

        Command<CommandSource> delStageCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.DEL_STAGE)) return 0;
            String name = StringArgumentType.getString(context, "name");
            String dimension;
            try {
                RegistryKey<World> targetLevel = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(StringArgumentType.getString(context, "dimension")));
                dimension = targetLevel.location().toString();
            } catch (IllegalArgumentException ignored) {
                dimension = NarcissusUtils.getStageDimensionByName(name);
            }
            WorldStageData stageData = WorldStageData.get();
            Coordinate remove = stageData.getStageCoordinate().remove(new KeyValue<>(dimension, name));
            stageData.setDirty();
            if (remove == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_not_found_with_name_in_dimension"), dimension, name);
                return 0;
            }
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_del"), dimension, name);
            return 1;
        };

        Command<CommandSource> getStageCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.GET_STAGE)) return 0;
            Component component;
            WorldStageData data = WorldStageData.get();
            String language = NarcissusUtils.getPlayerLanguage(player);
            if (data.getStageCoordinate().isEmpty()) {
                component = Component.translatable(language, EI18nType.MESSAGE, "stage_is_empty");
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
                    Component dimension = Component.literal(entry.getKey()).setColor(EMCColor.DARK_GREEN.getColor());
                    dimension.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()));
                    dimension.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(entry.getKey()).toTextComponent()));
                    dimension.append(Component.literal(": ").setColor(EMCColor.GRAY.getColor()));
                    for (KeyValue<String, Coordinate> coordinates : entry.getValue()) {
                        Component name = Component.translatable(language, EI18nType.MESSAGE, "stage_info"
                                , coordinates.getKey()
                                , coordinates.getValue().toXString()
                                , coordinates.getValue().toYString()
                                , coordinates.getValue().toZString());
                        Component name_hover = Component.translatable(language, EI18nType.MESSAGE, "stage_info_hover"
                                , coordinates.getKey()
                                , coordinates.getValue().toXString()
                                , coordinates.getValue().toYString()
                                , coordinates.getValue().toZString());
                        name.setColor(EMCColor.GREEN.getColor());
                        name.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name_hover.toString(true)));
                        name.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, name_hover.toChatComponent()));
                        dimension.append(name);
                        dimension.append(Component.literal(", ").setColor(EMCColor.GRAY.getColor()));
                    }
                    info.append(dimension).append("\n");
                }
                component = Component.translatable(language, EI18nType.MESSAGE, "stage_is", info);
            }
            NarcissusUtils.sendMessage(player, component);
            return 1;
        };

        Command<CommandSource> tpBackCommand = context -> {
            notifyHelp(context);
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.TP_BACK)) return 0;
            ETeleportType type = ETeleportType.nullableValueOf(getStringEmpty(context, "type"));
            RegistryKey<World> targetLevel = null;
            try {
                RegistryKey<World> targetDimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(StringArgumentType.getString(context, "dimension")));
                ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
                if (level != null) {
                    targetLevel = targetDimension;
                }
            } catch (IllegalArgumentException ignored) {
                // targetLevel = player.getLevel().dimension();
            }
            TeleportRecord record = NarcissusUtils.getBackTeleportRecord(player, type, targetLevel);
            if (record == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "back_not_found"));
                return 0;
            }
            Coordinate coordinate = record.getBefore().clone();
            coordinate.setSafe("safe".equalsIgnoreCase(getStringEmpty(context, "safe")));
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_BACK, true)) return 0;
            NarcissusUtils.removeBackTeleportRecord(player, record);
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_BACK);
            return 1;
        };

        Command<CommandSource> virtualOpCommand = context -> {
            notifyHelp(context);
            CommandSource source = context.getSource();
            // 传送功能前置校验
            if (checkTeleportPre(context.getSource(), ECommandType.VIRTUAL_OP)) return 0;
            // 如果命令来自玩家
            if (source.getEntity() == null || source.getEntity() instanceof ServerPlayerEntity) {
                EOperationType type = EOperationType.fromString(StringArgumentType.getString(context, "operation"));
                ECommandType[] rules;
                try {
                    rules = Arrays.stream(StringArgumentType.getString(context, "rules").split(","))
                            .filter(StringUtils::isNotNullOrEmpty)
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .map(ECommandType::valueOf).toArray(ECommandType[]::new);
                } catch (IllegalArgumentException ignored) {
                    rules = new ECommandType[]{};
                }
                List<ServerPlayerEntity> targetList = new ArrayList<>();
                try {
                    targetList.addAll(EntityArgument.getPlayers(context, "player"));
                } catch (IllegalArgumentException ignored) {
                }
                String language = ServerConfig.DEFAULT_LANGUAGE.get();
                if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                    language = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
                }
                for (ServerPlayerEntity target : targetList) {
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
                    NarcissusUtils.sendTranslatableMessage(target, I18nUtils.getKey(EI18nType.MESSAGE, "player_virtual_op"), target.getDisplayName().getString(), permissions);
                    if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                        ServerPlayerEntity player = source.getPlayerOrException();
                        if (!target.getStringUUID().equalsIgnoreCase(player.getStringUUID())) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "player_virtual_op"), target.getDisplayName().getString(), permissions);
                        }
                    } else {
                        source.sendSuccess(Component.translatable(language, EI18nType.MESSAGE, "player_virtual_op", target.getDisplayName().getString(), permissions).toChatComponent(), true);
                    }
                    // 更新权限信息
                    source.getServer().getPlayerList().sendPlayerPermissionLevel(target);
                }
            }
            return 1;
        };

        LiteralArgumentBuilder<CommandSource> language = Commands.literal(ServerConfig.COMMAND_LANGUAGE.get())
                // 设置语言 /narcissus language
                .then(Commands.argument("language", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("client");
                            builder.suggest("server");
                            I18nUtils.getI18nFiles().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(languageCommand)
                );
        LiteralArgumentBuilder<CommandSource> dim = Commands.literal(ServerConfig.COMMAND_DIMENSION.get())
                .executes(dimCommand);
        LiteralArgumentBuilder<CommandSource> uuid = Commands.literal(ServerConfig.COMMAND_UUID.get())
                .executes(uuidCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(uuidCommand)
                );
        LiteralArgumentBuilder<CommandSource> card = Commands.literal(ServerConfig.COMMAND_CARD.get())
                .executes(cardCommand)
                .then(Commands.argument("type", StringArgumentType.word())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.SET_CARD))
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
                );
        LiteralArgumentBuilder<CommandSource> share = Commands.literal(ServerConfig.COMMAND_SHARE.get())
                .executes(shareCommand)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            String name = getStringEmpty(context, "name");
                            CommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayerOrException();
                            // 获取玩家私人传送点
                            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
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
                );
        LiteralArgumentBuilder<CommandSource> feed = Commands.literal(ServerConfig.COMMAND_FEED.get())
                .executes(feedCommand)
                .then(Commands.argument("player", EntityArgument.players())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.FEED_OTHER))
                        .executes(feedCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpx = Commands.literal(ServerConfig.COMMAND_TP_COORDINATE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_COORDINATE))
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
                );
        LiteralArgumentBuilder<CommandSource> tpst = Commands.literal(ServerConfig.COMMAND_TP_STRUCTURE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_STRUCTURE))
                .then(Commands.argument("struct", ResourceLocationArgument.id())
                        .suggests(structureSuggestions)
                        .executes(tpStructureCommand)
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .suggests(rangeSuggestions)
                                .executes(tpStructureCommand)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(tpStructureCommand)
                                )
                        )
                );
        LiteralArgumentBuilder<CommandSource> tpa = Commands.literal(ServerConfig.COMMAND_TP_ASK.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_ASK))
                .executes(tpAskCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpAskCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSource> tpaYes = Commands.literal(ServerConfig.COMMAND_TP_ASK_YES.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_ASK_YES))
                .executes(tpAskYesCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_ASK, true))
                        .executes(tpAskYesCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskYesCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpAskYesCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpaNo = Commands.literal(ServerConfig.COMMAND_TP_ASK_NO.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_ASK_NO))
                .executes(tpAskNoCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_ASK, true))
                        .executes(tpAskNoCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskNoCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpAskNoCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpaCancel = Commands.literal(ServerConfig.COMMAND_TP_ASK_CANCEL.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_ASK_CANCEL))
                .executes(tpAskCancelCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_ASK, false))
                        .executes(tpAskCancelCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskCancelCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpAskCancelCommand)
                );
        LiteralArgumentBuilder<CommandSource> tph = Commands.literal(ServerConfig.COMMAND_TP_HERE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_HERE))
                .executes(tpHereCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpHereCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSource> tphYes = Commands.literal(ServerConfig.COMMAND_TP_HERE_YES.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_HERE_YES))
                .executes(tpHereYesCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_HERE, true))
                        .executes(tpHereYesCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereYesCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpHereYesCommand)
                );
        LiteralArgumentBuilder<CommandSource> tphNo = Commands.literal(ServerConfig.COMMAND_TP_HERE_NO.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_HERE_NO))
                .executes(tpHereNoCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_HERE, true))
                        .executes(tpHereNoCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereNoCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpHereNoCommand)
                );
        LiteralArgumentBuilder<CommandSource> tphCancel = Commands.literal(ServerConfig.COMMAND_TP_HERE_CANCEL.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_HERE_CANCEL))
                .executes(tpHereCancelCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_HERE, false))
                        .executes(tpHereCancelCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereCancelCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpHereCancelCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpRandom = Commands.literal(ServerConfig.COMMAND_TP_RANDOM.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_RANDOM))
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
                );
        LiteralArgumentBuilder<CommandSource> tpSpawn = Commands.literal(ServerConfig.COMMAND_TP_SPAWN.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_SPAWN))
                .executes(tpSpawnCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpSpawnCommand)
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_SPAWN_OTHER))
                                .executes(tpSpawnCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSource> tpWorldSpawn = Commands.literal(ServerConfig.COMMAND_TP_WORLD_SPAWN.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_WORLD_SPAWN))
                .executes(tpWorldSpawnCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpWorldSpawnCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpTop = Commands.literal(ServerConfig.COMMAND_TP_TOP.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_TOP))
                .executes(tpTopCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpTopCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpBottom = Commands.literal(ServerConfig.COMMAND_TP_BOTTOM.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_BOTTOM))
                .executes(tpBottomCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpBottomCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpUp = Commands.literal(ServerConfig.COMMAND_TP_UP.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_UP))
                .executes(tpUpCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpUpCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpDown = Commands.literal(ServerConfig.COMMAND_TP_DOWN.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_DOWN))
                .executes(tpDownCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpDownCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpView = Commands.literal(ServerConfig.COMMAND_TP_VIEW.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_VIEW))
                .executes(tpViewCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpViewCommand)
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .suggests(rangeSuggestions)
                                .executes(tpViewCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSource> tpHome = Commands.literal(ServerConfig.COMMAND_TP_HOME.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_HOME))
                .executes(tpHomeCommand)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(homeSuggestions)
                        .executes(tpHomeCommand)
                        .then(Commands.argument("safe", BoolArgumentType.bool())
                                .suggests((context, builder) -> {
                                    String name = getStringDefault(context, "name", null);
                                    if ("true".equals(name) || "false".equals(name)) {
                                        ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
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
                                            ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
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
                );
        LiteralArgumentBuilder<CommandSource> setHome = Commands.literal(ServerConfig.COMMAND_SET_HOME.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_HOME))
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
                );
        LiteralArgumentBuilder<CommandSource> delHome = Commands.literal(ServerConfig.COMMAND_DEL_HOME.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_HOME))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(homeSuggestions)
                        .executes(delHomeCommand)
                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                    IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
                                    String name = StringArgumentType.getString(context, "name");
                                    for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
                                        if (keyValue.getValue().equals(name))
                                            builder.suggest(keyValue.getKey());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(delHomeCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSource> getHome = Commands.literal(ServerConfig.COMMAND_GET_HOME.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.GET_HOME))
                .executes(getHomeCommand);
        LiteralArgumentBuilder<CommandSource> tpStage = Commands.literal(ServerConfig.COMMAND_TP_STAGE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_STAGE))
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
                );
        LiteralArgumentBuilder<CommandSource> setStage = Commands.literal(ServerConfig.COMMAND_SET_STAGE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.SET_STAGE))
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
                );
        LiteralArgumentBuilder<CommandSource> delStage = Commands.literal(ServerConfig.COMMAND_DEL_STAGE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.DEL_STAGE))
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
                );
        LiteralArgumentBuilder<CommandSource> getStage = Commands.literal(ServerConfig.COMMAND_GET_STAGE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.GET_STAGE))
                .executes(getStageCommand);
        LiteralArgumentBuilder<CommandSource> tpBack = Commands.literal(ServerConfig.COMMAND_TP_BACK.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.TP_BACK))
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
                                    for (ETeleportType value : ETeleportType.values()) {
                                        if (StringUtils.isNullOrEmptyEx(type) || value.name().toLowerCase().contains(type.toLowerCase())) {
                                            builder.suggest(value.name());
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(tpBackCommand)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
                                            ETeleportType type = ETeleportType.nullableValueOf(getStringEmpty(context, "type"));
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
                );
        LiteralArgumentBuilder<CommandSource> virtualOp = Commands.literal(ServerConfig.COMMAND_VIRTUAL_OP.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.VIRTUAL_OP))
                .then(Commands.argument("operation", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest(EOperationType.ADD.name().toLowerCase());
                            builder.suggest(EOperationType.SET.name().toLowerCase());
                            builder.suggest(EOperationType.DEL.name().toLowerCase());
                            builder.suggest(EOperationType.CLEAR.name().toLowerCase());
                            builder.suggest(EOperationType.GET.name().toLowerCase());
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(virtualOpCommand)
                                .then(Commands.argument("rules", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            String operation = StringArgumentType.getString(context, "operation");
                                            if (operation.equalsIgnoreCase(EOperationType.GET.name().toLowerCase())
                                                    || operation.equalsIgnoreCase(EOperationType.CLEAR.name().toLowerCase())
                                                    || operation.equalsIgnoreCase(EOperationType.LIST.name().toLowerCase())) {
                                                return builder.buildFuture();
                                            }
                                            String input = getStringEmpty(context, "rules").replace(" ", ",");
                                            String[] split = input.split(",");
                                            String current = input.endsWith(",") ? "" : split[split.length - 1];
                                            for (ECommandType value : Arrays.stream(ECommandType.values())
                                                    .filter(ECommandType::isOp)
                                                    .filter(type -> Arrays.stream(split).noneMatch(in -> in.equalsIgnoreCase(type.name())))
                                                    .filter(type -> StringUtils.isNullOrEmptyEx(current) || type.name().toLowerCase().contains(current.toLowerCase()))
                                                    .sorted(Comparator.comparing(ECommandType::getSort))
                                                    .collect(Collectors.toList())) {
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
                );

        // 注册简短的指令
        {
            // 设置语言 /language
            if (ServerConfig.CONCISE_LANGUAGE.get()) {
                dispatcher.register(language);
            }

            // 获取玩家UUID /uuid
            if (ServerConfig.CONCISE_UUID.get()) {
                dispatcher.register(uuid);
            }

            // 获取当前世界的维度ID /dim
            if (ServerConfig.CONCISE_DIMENSION.get()) {
                dispatcher.register(dim);
            }

            // 获取玩家传送卡数量 /card
            if (ServerConfig.CONCISE_CARD.get() && ServerConfig.TELEPORT_CARD.get()) {
                dispatcher.register(card);
            }

            // 分享坐标 /share
            if (ServerConfig.CONCISE_SHARE.get() && ServerConfig.SWITCH_SHARE.get()) {
                dispatcher.register(share);
            }

            // 自杀或毒杀 /feed
            if (ServerConfig.CONCISE_FEED.get() && ServerConfig.SWITCH_FEED.get()) {
                dispatcher.register(feed);
            }

            // 传送至指定位置 /tpx
            if (ServerConfig.CONCISE_TP_COORDINATE.get() && ServerConfig.SWITCH_TP_COORDINATE.get()) {
                dispatcher.register(tpx);
            }

            // 传送至指定结构或生物群系 /tpst
            if (ServerConfig.CONCISE_TP_STRUCTURE.get() && ServerConfig.SWITCH_TP_STRUCTURE.get()) {
                dispatcher.register(tpst);
            }

            // 传送请求 /tpa
            if (ServerConfig.CONCISE_TP_ASK.get() && ServerConfig.SWITCH_TP_ASK.get()) {
                dispatcher.register(tpa);
            }

            // 传送请求同意 /tpay
            if (ServerConfig.CONCISE_TP_ASK_YES.get() && ServerConfig.SWITCH_TP_ASK.get()) {
                dispatcher.register(tpaYes);
            }

            // 传送请求拒绝 /tpan
            if (ServerConfig.CONCISE_TP_ASK_NO.get() && ServerConfig.SWITCH_TP_ASK.get()) {
                dispatcher.register(tpaNo);
            }

            // 传送请求取消 /tpac
            if (ServerConfig.CONCISE_TP_ASK_CANCEL.get() && ServerConfig.SWITCH_TP_ASK.get()) {
                dispatcher.register(tpaCancel);
            }

            // 被传送请求 /tph
            if (ServerConfig.CONCISE_TP_HERE.get() && ServerConfig.SWITCH_TP_HERE.get()) {
                dispatcher.register(tph);
            }

            // 被传送请求同意 /tphy
            if (ServerConfig.CONCISE_TP_HERE_YES.get() && ServerConfig.SWITCH_TP_HERE.get()) {
                dispatcher.register(tphYes);
            }

            // 被传送请求拒绝 /tphn
            if (ServerConfig.CONCISE_TP_HERE_NO.get() && ServerConfig.SWITCH_TP_HERE.get()) {
                dispatcher.register(tphNo);
            }

            // 被传送请求取消 /tphc
            if (ServerConfig.CONCISE_TP_HERE_CANCEL.get() && ServerConfig.SWITCH_TP_HERE.get()) {
                dispatcher.register(tphCancel);
            }

            // 随机传送，允许指定范围 /tpr
            if (ServerConfig.CONCISE_TP_RANDOM.get() && ServerConfig.SWITCH_TP_RANDOM.get()) {
                dispatcher.register(tpRandom);
            }

            // 传送至出生点 /tps
            if (ServerConfig.CONCISE_TP_SPAWN.get() && ServerConfig.SWITCH_TP_SPAWN.get()) {
                dispatcher.register(tpSpawn);
            }

            // 传送至世界出生点 /tpws
            if (ServerConfig.CONCISE_TP_WORLD_SPAWN.get() && ServerConfig.SWITCH_TP_WORLD_SPAWN.get()) {
                dispatcher.register(tpWorldSpawn);
            }

            // 传送至头顶最上方方块 /tpt
            if (ServerConfig.CONCISE_TP_TOP.get() && ServerConfig.SWITCH_TP_TOP.get()) {
                dispatcher.register(tpTop);
            }

            // 传送至脚下最下方方块 /tpb
            if (ServerConfig.CONCISE_TP_BOTTOM.get() && ServerConfig.SWITCH_TP_BOTTOM.get()) {
                dispatcher.register(tpBottom);
            }

            // 传送至头顶最近方块 /tpu
            if (ServerConfig.CONCISE_TP_UP.get() && ServerConfig.SWITCH_TP_UP.get()) {
                dispatcher.register(tpUp);
            }

            // 传送至脚下最近方块 /tpd
            if (ServerConfig.CONCISE_TP_DOWN.get() && ServerConfig.SWITCH_TP_DOWN.get()) {
                dispatcher.register(tpDown);
            }

            // 传送至视线尽头 /tpv
            if (ServerConfig.CONCISE_TP_VIEW.get() && ServerConfig.SWITCH_TP_VIEW.get()) {
                dispatcher.register(tpView);
            }

            // 传送至传送点 /home
            if (ServerConfig.CONCISE_TP_HOME.get() && ServerConfig.SWITCH_TP_HOME.get()) {
                dispatcher.register(tpHome);
            }

            // 添加传送点 /sethome
            if (ServerConfig.CONCISE_SET_HOME.get() && ServerConfig.SWITCH_TP_HOME.get()) {
                dispatcher.register(setHome);
            }

            // 删除传送点 /delhome
            if (ServerConfig.CONCISE_DEL_HOME.get() && ServerConfig.SWITCH_TP_HOME.get()) {
                dispatcher.register(delHome);
            }

            // 获取传送点 /gethome
            if (ServerConfig.CONCISE_GET_HOME.get() && ServerConfig.SWITCH_TP_HOME.get()) {
                dispatcher.register(getHome);
            }

            // 传送至驿站 /stage
            if (ServerConfig.CONCISE_TP_STAGE.get() && ServerConfig.SWITCH_TP_STAGE.get()) {
                dispatcher.register(tpStage);
            }

            // 添加驿站 /setstage
            if (ServerConfig.CONCISE_SET_STAGE.get() && ServerConfig.SWITCH_TP_STAGE.get()) {
                dispatcher.register(setStage);
            }

            // 删除驿站 /delstage
            if (ServerConfig.CONCISE_DEL_STAGE.get() && ServerConfig.SWITCH_TP_STAGE.get()) {
                dispatcher.register(delStage);
            }

            // 获取驿站 /getstage
            if (ServerConfig.CONCISE_GET_STAGE.get() && ServerConfig.SWITCH_TP_STAGE.get()) {
                dispatcher.register(getStage);
            }

            // 返回上次离开地方 /back
            if (ServerConfig.CONCISE_TP_BACK.get() && ServerConfig.SWITCH_TP_BACK.get()) {
                dispatcher.register(tpBack);
            }

            // 设置虚拟权限
            if (ServerConfig.CONCISE_VIRTUAL_OP.get()) {
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
                            .then(Commands.literal("get")
                                    .then(Commands.literal("teleportCard")
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                                Component msg = Component.translatable(I18nUtils.getKey(EI18nType.MESSAGE, "server_config_status")
                                                        , I18nUtils.enabled(NarcissusUtils.getPlayerLanguage(player), ServerConfig.TELEPORT_CARD.get())
                                                        , Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "teleport_card"));
                                                NarcissusUtils.sendMessage(player, msg);
                                                return 1;
                                            })
                                    )
                            )
                            // 修改服务器配置
                            .then(Commands.literal("set")
                                    .requires(source -> NarcissusUtils.hasCommandPermission(source, ECommandType.VIRTUAL_OP))
                                    .then(Commands.literal("teleportCard")
                                            .then(Commands.argument("bool", BoolArgumentType.bool())
                                                    .executes(context -> {
                                                        boolean bool = BoolArgumentType.getBool(context, "bool");
                                                        ServerConfig.TELEPORT_CARD.set(bool);
                                                        ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                                        Component msg = Component.translatable(I18nUtils.getKey(EI18nType.MESSAGE, "server_config_status")
                                                                , I18nUtils.enabled(NarcissusUtils.getPlayerLanguage(player), ServerConfig.TELEPORT_CARD.get())
                                                                , Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "teleport_card"));
                                                        NarcissusUtils.broadcastMessage(player, msg);
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(Commands.literal("mode")
                                            .then(Commands.argument("mode", IntegerArgumentType.integer(0))
                                                    .suggests((context, builder) -> {
                                                        builder.suggest(0);
                                                        builder.suggest(1);
                                                        builder.suggest(2);
                                                        builder.suggest(3);
                                                        return builder.buildFuture();
                                                    })
                                                    .executes(context -> {
                                                        int mode = IntegerArgumentType.getInteger(context, "mode");
                                                        CommandSource source = context.getSource();
                                                        String lang = ServerConfig.DEFAULT_LANGUAGE.get();
                                                        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                                                            lang = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
                                                        }
                                                        switch (mode) {
                                                            case 0:
                                                                ServerConfig.resetConfig();
                                                                break;
                                                            case 1:
                                                                ServerConfig.resetConfigWithMode1();
                                                                break;
                                                            case 2:
                                                                ServerConfig.resetConfigWithMode2();
                                                                break;
                                                            case 3:
                                                                ServerConfig.resetConfigWithMode3();
                                                                break;
                                                            default: {
                                                                throw new IllegalArgumentException("Mode " + mode + " does not exist");
                                                            }
                                                        }
                                                        Component component = Component.translatable(lang, EI18nType.MESSAGE, "server_config_mode", mode);
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
                                                    .suggests((context, builder) -> {
                                                        I18nUtils.getI18nFiles().forEach(builder::suggest);
                                                        return builder.buildFuture();
                                                    })
                                                    .executes(context -> {
                                                        String code = StringArgumentType.getString(context, "language");
                                                        ServerConfig.DEFAULT_LANGUAGE.set(code);
                                                        ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                                        NarcissusUtils.broadcastMessage(player, Component.translatable(player, EI18nType.MESSAGE, "server_default_language", ServerConfig.DEFAULT_LANGUAGE.get()));
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
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
    private static String getRequestId(CommandContext<CommandSource> context, ETeleportType teleportType, final boolean isTarget) {
        String result = null;
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            try {
                ServerPlayerEntity requester = EntityArgument.getPlayer(context, "player");
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
                try {
                    result = StringArgumentType.getString(context, "requestId");
                    if (!NarcissusFarewell.getTeleportRequest().containsKey(result)) {
                        result = null;
                    }
                } catch (IllegalArgumentException ignored1) {
                    try {
                        int askIndex = IntegerArgumentType.getInteger(context, "requestIndex");
                        List<Map.Entry<String, TeleportRequest>> entryList = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                                .filter(entry -> isTarget ? entry.getValue().getTarget().getUUID().equals(player.getUUID()) : entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                                .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                                // 使用负数实现倒序排列
                                .sorted(Comparator.comparing(entry -> -entry.getValue().getRequestTime().getTime()))
                                .collect(Collectors.toList());
                        if (askIndex > 0 && askIndex <= entryList.size()) {
                            result = entryList.get(askIndex - 1).getKey();
                        }
                    } catch (IllegalArgumentException ignored2) {
                        // 使用负数实现倒序排列
                        Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
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
    public static SuggestionProvider<CommandSource> buildReqIndexSuggestions(ETeleportType teleportType, final boolean isTarget) {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
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
    private static boolean checkTeleportPre(CommandSource source, ECommandType teleportType) {
        // 判断是否开启传送功能
        if (!NarcissusUtils.isCommandEnabled(teleportType)) {
            NarcissusUtils.sendTranslatableMessage(source, false, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
            return true;
        }
        // 判断是否有冷却时间
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
            ETeleportType type = teleportType.toTeleportType();
            if (type != null) {
                int teleportCoolDown = NarcissusUtils.getTeleportCoolDown(player, type);
                if (teleportCoolDown > 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_cooldown"), teleportCoolDown);
                    return true;
                }
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
    public static boolean checkTeleportPost(ServerPlayerEntity player, Coordinate target, ETeleportType type) {
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
    public static boolean checkTeleportPost(ServerPlayerEntity player, Coordinate target, ETeleportType type, boolean submit) {
        boolean result;
        // 判断跨维度传送
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(player, target.getDimension(), type);
        // 判断是否有传送代价
        result = result && NarcissusUtils.validTeleportCost(player, target, type, submit);
        return !result;
    }

    public static String getStringEmpty(CommandContext<?> context, String name) {
        return getStringDefault(context, name, "");
    }

    public static String getStringDefault(CommandContext<?> context, String name, String defaultValue) {
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
    public static void notifyHelp(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            if (!data.isNotified()) {
                Component button = Component.literal("/" + NarcissusUtils.getCommandPrefix())
                        .setColor(EMCColor.AQUA.getColor())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + NarcissusUtils.getCommandPrefix()))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/" + NarcissusUtils.getCommandPrefix())
                                .toTextComponent())
                        );
                NarcissusUtils.sendMessage(player, Component.translatable(EI18nType.MESSAGE, "notify_help", button));
                data.setNotified(true);
            }
        }
    }

}
