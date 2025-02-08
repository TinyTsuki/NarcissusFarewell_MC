package xin.vanilla.narcissus.command;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.capability.TeleportRecord;
import xin.vanilla.narcissus.capability.player.IPlayerTeleportData;
import xin.vanilla.narcissus.capability.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.capability.world.WorldStageData;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.enums.EI18nType;
import xin.vanilla.narcissus.enums.ESafeMode;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.StringUtils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class FarewellCommand {

    public static int HELP_INFO_NUM_PER_PAGE = 5;

    public static final List<KeyValue<String, ECommandType>> HELP_MESSAGE = Arrays.stream(ECommandType.values())
            .map(type -> {
                String command = NarcissusUtils.getCommand(type);
                if (StringUtils.isNotNullOrEmpty(command)) {
                    return new KeyValue<>(command, type);
                }
                return null;
            })
            .filter(Objects::nonNull)
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
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

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
                int pages = (int) Math.ceil((double) HELP_MESSAGE.size() / HELP_INFO_NUM_PER_PAGE);
                helpInfo = Component.literal("-----==== Narcissus Farewell Help (" + page + "/" + pages + ") ====-----\n");
                for (int i = 0; (page - 1) * HELP_INFO_NUM_PER_PAGE + i < HELP_MESSAGE.size() && i < HELP_INFO_NUM_PER_PAGE; i++) {
                    KeyValue<String, ECommandType> keyValue = HELP_MESSAGE.get((page - 1) * HELP_INFO_NUM_PER_PAGE + i);
                    Component commandTips;
                    if (keyValue.getValue().name().toLowerCase().contains("concise")) {
                        commandTips = Component.translatable(player.getLanguage(), EI18nType.COMMAND, "concise", NarcissusUtils.getCommand(keyValue.getValue().replaceConcise()));
                    } else {
                        commandTips = Component.translatable(player.getLanguage(), EI18nType.COMMAND, keyValue.getValue().name().toLowerCase());
                    }
                    commandTips.setColor(Color.GRAY.getRGB());
                    helpInfo.append("/").append(keyValue.getKey())
                            .append(new Component(" -> ").setColor(Color.YELLOW.getRGB()))
                            .append(commandTips);
                    if (i != HELP_MESSAGE.size() - 1) {
                        helpInfo.append("\n");
                    }
                }
            } else {
                ECommandType type = ECommandType.valueOf(command);
                helpInfo = Component.literal("");
                helpInfo.append("/").append(NarcissusUtils.getCommand(type))
                        .append("\n")
                        .append(Component.translatable(player.getLanguage(), EI18nType.COMMAND, command.toLowerCase() + "_detail").setColor(Color.GRAY.getRGB()));
            }
            NarcissusUtils.sendMessage(player, helpInfo);
            return 1;
        };

        SuggestionProvider<CommandSourceStack> helpSuggestions = (context, builder) -> {
            StringRange stringRange = context.getNodes().stream()
                    .filter(o -> o.getNode().getName().equalsIgnoreCase("command"))
                    .map(ParsedCommandNode::getRange)
                    .findFirst().orElse(new StringRange(0, 0));
            String input = context.getInput().substring(stringRange.getStart(), stringRange.getEnd());
            boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
            int totalPages = (int) Math.ceil((double) HELP_MESSAGE.size() / HELP_INFO_NUM_PER_PAGE);
            for (int i = 0; i < totalPages && isInputEmpty; i++) {
                builder.suggest(i + 1);
            }
            for (ECommandType type : Arrays.stream(ECommandType.values())
                    .filter(type -> type != ECommandType.HELP)
                    .filter(type -> !type.name().toLowerCase().contains("concise"))
                    .filter(type -> isInputEmpty || type.name().contains(input))
                    .sorted(Comparator.comparing(ECommandType::getSort))
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
            StringRange stringRange = context.getNodes().stream()
                    .filter(o -> o.getNode().getName().equalsIgnoreCase("struct"))
                    .map(ParsedCommandNode::getRange)
                    .findFirst().orElse(new StringRange(0, 0));
            String input = context.getInput().substring(stringRange.getStart(), stringRange.getEnd());
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
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
                builder.suggest(key.getValue());
            }
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSourceStack> stageSuggestions = (context, builder) -> {
            for (KeyValue<String, String> key : WorldStageData.get().getStageCoordinate().keySet()) {
                builder.suggest(key.getValue());
            }
            return builder.buildFuture();
        };

        Command<CommandSourceStack> tpCoordinateCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_COORDINATE)) return 0;
            Coordinate coordinate;
            try {
                Vec3 pos = Vec3Argument.getVec3(context, "coordinate");
                ResourceKey<Level> targetLevel;
                try {
                    targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
                } catch (IllegalArgumentException ignored) {
                    targetLevel = player.level().dimension();
                }
                coordinate = new Coordinate(pos.x(), pos.y(), pos.z(), player.getYRot(), player.getXRot(), targetLevel);
            } catch (IllegalArgumentException ignored) {
                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                coordinate = new Coordinate(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot(), target.level().dimension());
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_COORDINATE, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_COORDINATE);
            return 1;
        };

        Command<CommandSourceStack> tpStructureCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_STRUCTURE)) return 0;
            ResourceLocation structId = ResourceLocationArgument.getId(context, "struct");
            ResourceKey<Structure> structure = NarcissusUtils.getStructure(structId);
            TagKey<Structure> structureTag = NarcissusUtils.getStructureTag(structId);
            ResourceKey<Biome> biome = NarcissusUtils.getBiome(structId);
            if (structure == null && structureTag == null && biome == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "structure_biome_not_found"), structId);
                return 0;
            }
            int range;
            ResourceKey<Level> targetLevel;
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, ETeleportType.TP_STRUCTURE, range);
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.level().dimension();
            }
            ResourceKey<Level> finalTargetLevel = targetLevel;
            int finalRange = range;
            player.displayClientMessage(Component.translatable(player.getLanguage(), EI18nType.MESSAGE, "tp_structure_searching").toTextComponent(), true);
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
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "structure_biome_not_found_in_range"), structId);
                    return;
                }
                try {
                    coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
                } catch (IllegalArgumentException ignored) {
                }
                // 验证传送代价
                if (checkTeleportPost(player, coordinate, ETeleportType.TP_STRUCTURE, true)) return;
                player.server.submit(() -> NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_STRUCTURE));
            }).start();
            return 1;
        };

        Command<CommandSourceStack> tpAskCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_ASK)) return 0;
            ServerPlayer target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                target = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                        .filter(request -> {
                            ServerPlayer entity = request.getTarget();
                            return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, ETeleportType.TP_ASK)
                                    || entity != null && entity.level().dimension() == player.level().dimension();
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
            try {
                request.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            if (checkTeleportPost(request)) return 0;
            NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request);

            // 通知目标玩家
            {
                // 创建 "Yes" 按钮
                Component yesButton = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "yes_button", target.getLanguage())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_ASK_YES.get(), request.getRequestId())));
                // 创建 "No" 按钮
                Component noButton = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "no_button", target.getLanguage())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_ASK_NO.get(), request.getRequestId())));
                Component msg = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "tp_ask_request_received"
                        , target.getDisplayName().getString(), yesButton, noButton);
                NarcissusUtils.sendMessage(target, msg);
            }
            // 通知请求者
            {
                NarcissusUtils.sendMessage(player, Component.translatable(player.getLanguage(), EI18nType.MESSAGE, "tp_ask_request_sent", target.getDisplayName().getString()));
            }
            return 1;
        };

        Command<CommandSourceStack> tpAskYesCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_ASK);
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

        Command<CommandSourceStack> tpAskNoCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_ASK);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_rejected"), request.getTarget().getDisplayName().getString());
            return 1;
        };

        Command<CommandSourceStack> tpHereCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_HERE)) return 0;
            ServerPlayer target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                target = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                        .filter(request -> {
                            Player entity = request.getTarget();
                            return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, ETeleportType.TP_HERE)
                                    || entity != null && entity.level().dimension() == player.level().dimension();
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
            try {
                request.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            if (checkTeleportPost(request)) return 0;
            NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request);

            // 通知目标玩家
            {
                // 创建 "Yes" 按钮
                Component yesButton = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "yes_button", target.getLanguage())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_HERE_YES.get(), request.getRequestId())));
                // 创建 "No" 按钮
                Component noButton = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "no_button", target.getLanguage())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_HERE_NO.get(), request.getRequestId())));
                Component msg = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "tp_here_request_received"
                        , target.getDisplayName().getString(), Component.translatable(target.getLanguage(), EI18nType.WORD, request.isSafe() ? "tp_here_safe" : "tp_here_unsafe"), yesButton, noButton);
                NarcissusUtils.sendMessage(target, msg);
            }
            // 通知请求者
            {
                NarcissusUtils.sendMessage(player, Component.translatable(player.getLanguage(), EI18nType.MESSAGE, "tp_here_request_sent", target.getDisplayName().getString()));
            }
            return 1;
        };

        Command<CommandSourceStack> tpHereYesCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_HERE);
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

        Command<CommandSourceStack> tpHereNoCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_HERE);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_rejected"), request.getTarget().getDisplayName().getString());
            return 1;
        };

        Command<CommandSourceStack> tpRandomCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_RANDOM)) return 0;
            ResourceKey<Level> targetLevel;
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
                targetLevel = player.level().dimension();
            }
            Coordinate coordinate = Coordinate.random(player, range, targetLevel).setSafe(true);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_RANDOM, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_RANDOM);
            return 1;
        };

        Command<CommandSourceStack> tpSpawnCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_SPAWN)) return 0;
            Coordinate coordinate = new Coordinate(player);
            BlockPos respawnPosition = player.getRespawnPosition();
            coordinate.setDimension(player.getRespawnDimension());
            if (respawnPosition == null) {
                respawnPosition = player.level().getSharedSpawnPos();
                coordinate.setDimension(player.level().dimension());
            }
            if (respawnPosition == null) {
                respawnPosition = player.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos();
                coordinate.setDimension(Level.OVERWORLD);
            }
            coordinate.fromBlockPos(respawnPosition);
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_SPAWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_SPAWN);
            return 1;
        };

        Command<CommandSourceStack> tpWorldSpawnCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_WORLD_SPAWN)) return 0;
            Coordinate coordinate = new Coordinate(player);
            BlockPos respawnPosition = player.level().getSharedSpawnPos();
            coordinate.setDimension(player.level().dimension());
            if (respawnPosition == null) {
                respawnPosition = player.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos();
                coordinate.setDimension(Level.OVERWORLD);
            }
            coordinate.fromBlockPos(respawnPosition);
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_WORLD_SPAWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_WORLD_SPAWN);
            return 1;
        };

        Command<CommandSourceStack> tpTopCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_TOP)) return 0;
            Coordinate coordinate = NarcissusUtils.findTopCandidate(player.serverLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_top_not_found"));
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"))).setSafeMode(ESafeMode.Y_DOWN);
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true).setSafeMode(ESafeMode.Y_DOWN);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_TOP, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_TOP);
            return 1;
        };

        Command<CommandSourceStack> tpBottomCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_BOTTOM)) return 0;
            Coordinate coordinate = NarcissusUtils.findBottomCandidate(player.serverLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_bottom_not_found"));
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"))).setSafeMode(ESafeMode.Y_UP);
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true).setSafeMode(ESafeMode.Y_UP);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_BOTTOM, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_BOTTOM);
            return 1;
        };

        Command<CommandSourceStack> tpUpCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_UP)) return 0;
            Coordinate coordinate = NarcissusUtils.findUpCandidate(player.serverLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_up_not_found"));
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"))).setSafeMode(ESafeMode.Y_UP);
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true).setSafeMode(ESafeMode.Y_UP);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_UP, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_UP);
            return 1;
        };

        Command<CommandSourceStack> tpDownCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_DOWN)) return 0;
            Coordinate coordinate = NarcissusUtils.findDownCandidate(player.serverLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_down_not_found"));
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"))).setSafeMode(ESafeMode.Y_DOWN);
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true).setSafeMode(ESafeMode.Y_DOWN);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_DOWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_DOWN);
            return 1;
        };

        Command<CommandSourceStack> tpViewCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_VIEW)) return 0;
            boolean safe = true;
            int range;
            try {
                safe = "safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, ETeleportType.TP_VIEW, range);
            player.displayClientMessage(Component.translatable(player.getLanguage(), EI18nType.MESSAGE, "tp_view_searching").toTextComponent(), true);
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

        Command<CommandSourceStack> tpHomeCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_HOME)) return 0;
            ResourceKey<Level> targetLevel = null;
            String name = null;
            try {
                ResourceKey<Level> targetDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(StringArgumentType.getString(context, "dimension")));
                ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
                if (level != null) {
                    targetLevel = targetDimension;
                }
            } catch (IllegalArgumentException ignored) {
            }
            try {
                name = StringArgumentType.getString(context, "name");
            } catch (IllegalArgumentException ignored) {
            }
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

        Command<CommandSourceStack> setHomeCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 判断是否开启传送功能
            if (!NarcissusUtils.isTeleportEnabled(ETeleportType.TP_HOME)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
                return 0;
            }
            // 判断设置数量是否超过限制
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            if (data.getHomeCoordinate().size() >= ServerConfig.TELEPORT_HOME_LIMIT.get()) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_limit"), ServerConfig.TELEPORT_HOME_LIMIT.get());
                return 0;
            }
            String name;
            try {
                name = StringArgumentType.getString(context, "name");
            } catch (IllegalArgumentException ignored) {
                name = "home";
            }
            boolean defaultHome = false;
            try {
                defaultHome = BoolArgumentType.getBool(context, "default");
            } catch (IllegalArgumentException ignored) {
            }
            Coordinate coordinate = new Coordinate(player);
            KeyValue<String, String> key = new KeyValue<>(player.level().dimension().location().toString(), name);
            if (data.getHomeCoordinate().containsKey(key)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_already_exists"), key.getKey(), key.getValue());
                return 0;
            }
            data.addHomeCoordinate(key, coordinate);
            if (defaultHome) {
                if (data.getDefaultHome().containsKey(player.level().dimension().location().toString())) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_default_remove"), data.getDefaultHome(player.level().dimension().location().toString()).getValue());
                }
                data.addDefaultHome(player.level().dimension().location().toString(), name);
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_set_default"), name, coordinate.toXyzString());
            } else {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_set"), name, coordinate.toXyzString());
            }
            return 1;
        };

        Command<CommandSourceStack> delHomeCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 判断是否开启传送功能
            if (!NarcissusUtils.isTeleportEnabled(ETeleportType.TP_HOME)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
                return 0;
            }
            IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
            String name = StringArgumentType.getString(context, "name");
            ResourceKey<Level> targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            String dimension = targetLevel.location().toString();
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

        Command<CommandSourceStack> tpStageCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_STAGE)) return 0;
            ResourceKey<Level> targetLevel = null;
            String name = null;
            try {
                name = StringArgumentType.getString(context, "name");
            } catch (IllegalArgumentException ignored) {
            }
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
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
                    coordinate = stageData.getCoordinate(player.level().dimension().location().toString(), name);
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
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_STAGE, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_STAGE);
            return 1;
        };

        Command<CommandSourceStack> setStageCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 判断是否开启传送功能
            if (!NarcissusUtils.isTeleportEnabled(ETeleportType.TP_STAGE)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
                return 0;
            }
            WorldStageData stageData = WorldStageData.get();
            String name = StringArgumentType.getString(context, "name");
            ResourceKey<Level> targetLevel;
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.level().dimension();
            }
            String dimension = targetLevel.location().toString();
            KeyValue<String, String> key = new KeyValue<>(dimension, name);
            if (stageData.getStageCoordinate().containsKey(key)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_already_exists"), key.getKey(), key.getValue());
                return 0;
            }
            Coordinate coordinate = new Coordinate(player).setDimension(targetLevel);
            try {
                coordinate.fromVec3(Vec3Argument.getVec3(context, "coordinate"));
            } catch (IllegalArgumentException ignored) {
            }
            stageData.addCoordinate(key, coordinate);
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_set"), name, coordinate.toXyzString());
            return 1;
        };

        Command<CommandSourceStack> delStageCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 判断是否开启传送功能
            if (!NarcissusUtils.isTeleportEnabled(ETeleportType.TP_STAGE)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
                return 0;
            }
            String name = StringArgumentType.getString(context, "name");
            ResourceKey<Level> targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            WorldStageData stageData = WorldStageData.get();
            String dimension = targetLevel.location().toString();
            Coordinate remove = stageData.getStageCoordinate().remove(new KeyValue<>(dimension, name));
            stageData.setDirty();
            if (remove == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_not_found_with_name_in_dimension"), dimension, name);
                return 0;
            }
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_del"), dimension, name);
            return 1;
        };

        Command<CommandSourceStack> tpBackCommand = context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_BACK)) return 0;
            ETeleportType type = null;
            try {
                type = ETeleportType.valueOf(StringArgumentType.getString(context, "type"));
            } catch (IllegalArgumentException ignored) {
            }
            ResourceKey<Level> targetLevel;
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                // targetLevel = player.getLevel().dimension();
                targetLevel = null;
            }
            TeleportRecord record = NarcissusUtils.getBackTeleportRecord(player, type, targetLevel);
            if (record == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "back_not_found"));
                return 0;
            }
            Coordinate coordinate = record.getBefore().clone();
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_BACK, true)) return 0;
            NarcissusUtils.removeBackTeleportRecord(player, record);
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_BACK);
            return 1;
        };

        LiteralArgumentBuilder<CommandSourceStack> tpx = Commands.literal(ServerConfig.COMMAND_TP_COORDINATE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_COORDINATE.get()))
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
        LiteralArgumentBuilder<CommandSourceStack> tpst = Commands.literal(ServerConfig.COMMAND_TP_STRUCTURE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_STRUCTURE.get()))
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
        LiteralArgumentBuilder<CommandSourceStack> tpa = Commands.literal(ServerConfig.COMMAND_TP_ASK.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_ASK.get()))
                .executes(tpAskCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpAskCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> tpaYes = Commands.literal(ServerConfig.COMMAND_TP_ASK_YES.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_ASK.get()))
                .executes(tpAskYesCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_ASK))
                        .executes(tpAskYesCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskYesCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpAskYesCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tpaNo = Commands.literal(ServerConfig.COMMAND_TP_ASK_NO.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_ASK.get()))
                .executes(tpAskNoCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_ASK))
                        .executes(tpAskNoCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskNoCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpAskNoCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tph = Commands.literal(ServerConfig.COMMAND_TP_HERE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HERE.get()))
                .executes(tpHereCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpHereCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> tphYes = Commands.literal(ServerConfig.COMMAND_TP_HERE_YES.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HERE.get()))
                .executes(tpHereYesCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_HERE))
                        .executes(tpHereYesCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereYesCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpHereYesCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tphNo = Commands.literal(ServerConfig.COMMAND_TP_HERE_NO.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HERE.get()))
                .executes(tpHereNoCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_HERE))
                        .executes(tpHereNoCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereNoCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(tpHereNoCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tpRandom = Commands.literal(ServerConfig.COMMAND_TP_RANDOM.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_RANDOM.get()))
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
        LiteralArgumentBuilder<CommandSourceStack> tpSpawn = Commands.literal(ServerConfig.COMMAND_TP_SPAWN.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_SPAWN.get()))
                .executes(tpSpawnCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpSpawnCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tpWorldSpawn = Commands.literal(ServerConfig.COMMAND_TP_WORLD_SPAWN.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_WORLD_SPAWN.get()))
                .executes(tpWorldSpawnCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpWorldSpawnCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tpTop = Commands.literal(ServerConfig.COMMAND_TP_TOP.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_TOP.get()))
                .executes(tpTopCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpTopCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tpBottom = Commands.literal(ServerConfig.COMMAND_TP_BOTTOM.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_BOTTOM.get()))
                .executes(tpBottomCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpBottomCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tpUp = Commands.literal(ServerConfig.COMMAND_TP_UP.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_UP.get()))
                .executes(tpUpCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpUpCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tpDown = Commands.literal(ServerConfig.COMMAND_TP_DOWN.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_DOWN.get()))
                .executes(tpDownCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpDownCommand)
                );
        LiteralArgumentBuilder<CommandSourceStack> tpView = Commands.literal(ServerConfig.COMMAND_TP_VIEW.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_VIEW.get()))
                .executes(tpViewCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpViewCommand)
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .suggests(rangeSuggestions)
                                .executes(tpViewCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> tpHome = Commands.literal(ServerConfig.COMMAND_TP_HOME.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HOME.get()))
                .executes(tpHomeCommand)
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(homeSuggestions)
                        .executes(tpHomeCommand)
                        .then(Commands.argument("safe", BoolArgumentType.bool())
                                .executes(tpHomeCommand)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests(dimensionSuggestions)
                                        .executes(tpHomeCommand)
                                )
                        )
                )
                .then(Commands.argument("safe", BoolArgumentType.bool())
                        .executes(tpHomeCommand)
                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                .suggests(dimensionSuggestions)
                                .executes(tpHomeCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> setHome = Commands.literal(ServerConfig.COMMAND_SET_HOME.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HOME.get()))
                .executes(setHomeCommand)
                .then(Commands.argument("name", StringArgumentType.word())
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
        LiteralArgumentBuilder<CommandSourceStack> delHome = Commands.literal(ServerConfig.COMMAND_DEL_HOME.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HOME.get()))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(homeSuggestions)
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .executes(delHomeCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> tpStage = Commands.literal(ServerConfig.COMMAND_TP_STAGE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_STAGE.get()))
                .executes(tpStageCommand)
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(stageSuggestions)
                        .executes(tpStageCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpStageCommand)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(tpStageCommand)
                                )
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> setStage = Commands.literal(ServerConfig.COMMAND_SET_STAGE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_SET_STAGE.get()))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("stage");
                            return builder.buildFuture();
                        })
                        .executes(setStageCommand)
                        .then(Commands.argument("coordinate", Vec3Argument.vec3())
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(setStageCommand)
                                )
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> delStage = Commands.literal(ServerConfig.COMMAND_DEL_STAGE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_DEL_STAGE.get()))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(stageSuggestions)
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .executes(delStageCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> tpBack = Commands.literal(ServerConfig.COMMAND_TP_BACK.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_BACK.get()))
                .executes(tpBackCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpBackCommand)
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (ETeleportType value : ETeleportType.values()) {
                                        builder.suggest(value.name());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(tpBackCommand)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(tpBackCommand)
                                )
                        )
                );

        // 注册简短的指令
        {
            // 传送至指定位置 /tpx
            if (ServerConfig.CONCISE_TP_COORDINATE.get()) {
                dispatcher.register(tpx);
            }

            // 传送至指定结构或生物群系 /tpst
            if (ServerConfig.CONCISE_TP_STRUCTURE.get()) {
                dispatcher.register(tpst);
            }

            // 传送请求 /tpa
            if (ServerConfig.CONCISE_TP_ASK.get()) {
                dispatcher.register(tpa);
            }

            // 传送请求同意 /tpay
            if (ServerConfig.CONCISE_TP_ASK_YES.get()) {
                dispatcher.register(tpaYes);
            }

            // 传送请求拒绝 /tpan
            if (ServerConfig.CONCISE_TP_ASK_NO.get()) {
                dispatcher.register(tpaNo);
            }

            // 被传送请求 /tph
            if (ServerConfig.CONCISE_TP_HERE.get()) {
                dispatcher.register(tph);
            }

            // 被传送请求同意 /tphy
            if (ServerConfig.CONCISE_TP_HERE_YES.get()) {
                dispatcher.register(tphYes);
            }

            // 被传送请求拒绝 /tphn
            if (ServerConfig.CONCISE_TP_HERE_NO.get()) {
                dispatcher.register(tphNo);
            }

            // 随机传送，允许指定范围 /tpr
            if (ServerConfig.CONCISE_TP_RANDOM.get()) {
                dispatcher.register(tpRandom);
            }

            // 传送至出生点 /tps
            if (ServerConfig.CONCISE_TP_SPAWN.get()) {
                dispatcher.register(tpSpawn);
            }

            // 传送至世界出生点 /tpws
            if (ServerConfig.CONCISE_TP_WORLD_SPAWN.get()) {
                dispatcher.register(tpWorldSpawn);
            }

            // 传送至头顶最上方方块 /tpt
            if (ServerConfig.CONCISE_TP_TOP.get()) {
                dispatcher.register(tpTop);
            }

            // 传送至脚下最下方方块 /tpb
            if (ServerConfig.CONCISE_TP_BOTTOM.get()) {
                dispatcher.register(tpBottom);
            }

            // 传送至头顶最近方块 /tpu
            if (ServerConfig.CONCISE_TP_UP.get()) {
                dispatcher.register(tpUp);
            }

            // 传送至脚下最近方块 /tpd
            if (ServerConfig.CONCISE_TP_DOWN.get()) {
                dispatcher.register(tpDown);
            }

            // 传送至视线尽头 /tpv
            if (ServerConfig.CONCISE_TP_VIEW.get()) {
                dispatcher.register(tpView);
            }

            // 传送至传送点 /home
            if (ServerConfig.CONCISE_TP_HOME.get()) {
                dispatcher.register(tpHome);
            }

            // 添加传送点 /sethome
            if (ServerConfig.CONCISE_SET_HOME.get()) {
                dispatcher.register(setHome);
            }

            // 删除传送点 /delhome
            if (ServerConfig.CONCISE_DEL_HOME.get()) {
                dispatcher.register(delHome);
            }

            // 传送至驿站 /stage
            if (ServerConfig.CONCISE_TP_STAGE.get()) {
                dispatcher.register(tpStage);
            }

            // 添加驿站 /setstage
            if (ServerConfig.CONCISE_SET_STAGE.get()) {
                dispatcher.register(setStage);
            }

            // 删除驿站 /delstage
            if (ServerConfig.CONCISE_DEL_STAGE.get()) {
                dispatcher.register(delStage);
            }

            // 返回上次离开地方 /back
            if (ServerConfig.CONCISE_TP_BACK.get()) {
                dispatcher.register(tpBack);
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
                    .then(Commands.literal(ServerConfig.COMMAND_DIMENSION.get())
                            .executes((context) -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                Component msg = Component.translatable(player.getLanguage(), EI18nType.MESSAGE, "dimension_info", player.level().dimension().location().toString());
                                NarcissusUtils.sendMessage(player, msg);
                                return 1;
                            })
                    )
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
                    // 被传送请求 /narcissus tph
                    .then(tph)
                    // 被传送请求同意 /narcissus tphy
                    .then(tphYes)
                    // 被传送请求拒绝 /narcissus tphn
                    .then(tphNo)
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
                    // 传送至驿站 /narcissus stage
                    .then(tpStage)
                    // 添加驿站 /narcissus setstage
                    .then(setStage)
                    // 删除驿站 /narcissus delstage
                    .then(delStage)
                    // 返回上次离开地方 /narcissus back
                    .then(tpBack)
                    // 获取服务器配置 /narcissus config get
                    .then(Commands.literal("config")
                            .then(Commands.literal("get")
                                    .then(Commands.literal("teleportCard")
                                            .executes(context -> {
                                                ServerPlayer player = context.getSource().getPlayerOrException();
                                                Component msg = Component.translatable(player.getLanguage()
                                                        , I18nUtils.getKey(EI18nType.MESSAGE, "server_config_status")
                                                        , I18nUtils.enabled(player.getLanguage(), ServerConfig.TELEPORT_CARD.get())
                                                        , Component.translatable(player.getLanguage(), EI18nType.WORD, "teleport_card"));
                                                NarcissusUtils.sendMessage(player, msg);
                                                return 1;
                                            })
                                    )
                            )
                            // 修改服务器配置
                            .then(Commands.literal("set")
                                    .requires(source -> source.hasPermission(3))
                                    .then(Commands.literal("teleportCard")
                                            .then(Commands.argument("bool", BoolArgumentType.bool())
                                                    .executes(context -> {
                                                        boolean bool = BoolArgumentType.getBool(context, "bool");
                                                        ServerConfig.TELEPORT_CARD.set(bool);
                                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                                        Component msg = Component.translatable(player.getLanguage()
                                                                , I18nUtils.getKey(EI18nType.MESSAGE, "server_config_status")
                                                                , I18nUtils.enabled(player.getLanguage(), ServerConfig.TELEPORT_CARD.get())
                                                                , Component.translatable(player.getLanguage(), EI18nType.WORD, "teleport_card"));
                                                        NarcissusUtils.broadcastMessage(player, msg);
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
            );
        }
    }

    private static String getRequestId(CommandContext<CommandSourceStack> context, ETeleportType teleportType) {
        String result = null;
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            try {
                ServerPlayer requester = EntityArgument.getPlayer(context, "player");
                Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> entry.getValue().getTarget().getUUID().equals(player.getUUID()))
                        .filter(entry -> entry.getValue().getRequester().getUUID().equals(requester.getUUID()))
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
                                .filter(entry -> entry.getValue().getTarget().getUUID().equals(player.getUUID()))
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
                                .filter(entry -> entry.getValue().getTarget().getUUID().equals(player.getUUID()))
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

    public static SuggestionProvider<CommandSourceStack> buildReqIndexSuggestions(ETeleportType teleportType) {
        return (context, builder) -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                    .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                    .forEach(entry -> builder.suggest(entry.getKey()));
            for (int i = 0; i < NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> entry.getValue().getRequester().getUUID().equals(player.getUUID()))
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
    private static boolean checkTeleportPre(ServerPlayer player, ETeleportType teleportType) {
        // 判断是否开启传送功能
        if (!NarcissusUtils.isTeleportEnabled(teleportType)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
            return true;
        }
        // 判断是否有冷却时间
        int teleportCoolDown = NarcissusUtils.getTeleportCoolDown(player, teleportType);
        if (teleportCoolDown > 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_cooldown"), teleportCoolDown);
            return true;
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
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(request.getRequester(), request.getTarget().level().dimension(), request.getTeleportType());
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
    public static boolean checkTeleportPost(ServerPlayer player, Coordinate target, ETeleportType type) {
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
    public static boolean checkTeleportPost(ServerPlayer player, Coordinate target, ETeleportType type, boolean submit) {
        boolean result;
        // 判断跨维度传送
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(player, target.getDimension(), type);
        // 判断是否有传送代价
        result = result && NarcissusUtils.validTeleportCost(player, target, type, submit);
        return !result;
    }

}
