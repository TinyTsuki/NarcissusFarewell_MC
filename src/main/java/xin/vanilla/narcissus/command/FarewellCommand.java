package xin.vanilla.narcissus.command;


import lombok.NonNull;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
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
import xin.vanilla.narcissus.util.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class FarewellCommand extends CommandBase {

    // region 实现CommandBase

    @Override
    public int getRequiredPermissionLevel() {
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
        return 0;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return NarcissusUtils.hasPermissions((EntityPlayerMP) sender, this.getRequiredPermissionLevel());
    }

    @Override
    @NonNull
    public String getName() {
        return NarcissusUtils.getCommandPrefix();
    }

    @Override
    @NonNull
    public String getUsage(@NonNull ICommandSender sender) {
        return "/" + ServerConfig.COMMAND_PREFIX + " help";
    }

    @Override
    @NonNull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return getSuggestions(server, sender, args, targetPos);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(@NonNull MinecraftServer server, @NonNull ICommandSender sender, @ParametersAreNonnullByDefault String[] args) {
        verifyExecuteResult(sender, executeCommand(server, sender, args));
    }

    // endregion 实现CommandBase

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

    public static void verifyExecuteResult(ICommandSender sender, int result) {
        if (result < 0) {
            NarcissusUtils.sendTranslatableMessage((EntityPlayerMP) sender, I18nUtils.getKey(EI18nType.MESSAGE, "command_failed"));
        }
    }

    /**
     * 获取指令补全提示(快乐堆粪)
     *
     * @param server 服务器
     * @param sender 指令发送者
     * @param args   指令参数
     * @return 指令补全提示
     */
    public static List<String> getSuggestions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        EntityPlayer player = (EntityPlayer) sender;
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0) {
            Arrays.stream(ECommandType.values())
                    .filter(NarcissusUtils::isTeleportEnabled)
                    .filter(type -> NarcissusUtils.hasCommandPermission(player, type))
                    .sorted(Comparator.comparing(ECommandType::getSort))
                    .map(value -> NarcissusUtils.getCommand(value, false))
                    .distinct()
                    .forEach(suggestions::add);
        } else if (args.length == 1) {
            Arrays.stream(ECommandType.values())
                    .filter(NarcissusUtils::isTeleportEnabled)
                    .filter(type -> NarcissusUtils.hasCommandPermission(player, type))
                    .sorted(Comparator.comparing(ECommandType::getSort))
                    .map(value -> NarcissusUtils.getCommand(value, false))
                    .filter(command -> StringUtils.isNullOrEmpty(args[0]) || command.startsWith(args[0]))
                    .distinct()
                    .forEach(suggestions::add);
        }
        // 根据指令帮助中的参数列表进行补全提示
        else {
            String arg = args[args.length - 1];
            // 帮助信息
            if (args[0].equals("help")) {
                if (args.length == 2) {
                    String input = args[1];
                    boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
                    int totalPages = (int) Math.ceil((double) HELP_MESSAGE.size() / HELP_INFO_NUM_PER_PAGE);
                    for (int i = 0; i < totalPages && isInputEmpty; i++) {
                        suggestions.add(String.valueOf(i + 1));
                    }
                    Arrays.stream(ECommandType.values())
                            .filter(type -> type != ECommandType.HELP)
                            .filter(type -> !type.name().toLowerCase().contains("concise"))
                            .filter(type -> isInputEmpty || type.name().contains(input))
                            .sorted(Comparator.comparing(ECommandType::getSort))
                            .forEach(type -> suggestions.add(type.name()));
                }
            }
            // 毒杀玩家
            else if (args[0].equals(ServerConfig.COMMAND_FEED) && NarcissusUtils.isTeleportEnabled(ECommandType.FEED) && NarcissusUtils.hasCommandPermission(player, ECommandType.FEED)) {
                if (args.length == 2) {
                    suggestions.addAll(getPlayerNameSuggestions(server, args));
                    suggestions.add("@a");
                }
            }
            // 传送到指定坐标
            else if (args[0].equals(ServerConfig.COMMAND_TP_COORDINATE) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_COORDINATE) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_COORDINATE)) {
                if (args.length == 2) {
                    suggestions.addAll(getPlayerNameSuggestions(server, args));
                    suggestions.addAll(getCoordinateSuggestions(player.getPosition(), args, 1));
                } else if (args.length == 3) {
                    List<String> coordinateSuggestions = getCoordinateSuggestions(player.getPosition(), args, 1);
                    suggestions.addAll(coordinateSuggestions);
                    if (coordinateSuggestions.isEmpty()) {
                        if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                            suggestions.add("safe");
                        }
                        if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                            suggestions.add("unsafe");
                        }
                    }
                } else if (args.length == 4) {
                    suggestions.addAll(getCoordinateSuggestions(player.getPosition(), args, 1));
                } else if (args.length == 5) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                } else if (args.length == 6) {
                    Arrays.stream(DimensionType.values())
                            .map(dimensionType -> DimensionUtils.getStringIdFromInt(dimensionType.getId()))
                            .filter(Objects::nonNull)
                            .filter(string -> StringUtils.isNullOrEmpty(arg) || string.contains(arg))
                            .filter(StringUtils::isNotNullOrEmpty)
                            .distinct()
                            .forEach(suggestions::add);
                }
            }
            // 传送到指定结构
            else if (args[0].equals(ServerConfig.COMMAND_TP_STRUCTURE) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_STRUCTURE) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_STRUCTURE)) {
                if (args.length == 2) {
                    NarcissusUtils.getStructureList().stream()
                            .filter(string -> StringUtils.isNullOrEmpty(arg) || string.contains(arg))
                            .forEach(suggestions::add);
                    ForgeRegistries.BIOMES.getValuesCollection().stream()
                            .filter(biome -> StringUtils.isNullOrEmpty(arg) || biome.getRegistryName().toString().contains(arg))
                            .forEach(biome -> suggestions.add(biome.getRegistryName().toString()));
                } else if (args.length == 3) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                } else if (args.length == 4) {
                    Arrays.stream(DimensionType.values())
                            .map(dimensionType -> DimensionUtils.getStringIdFromInt(dimensionType.getId()))
                            .filter(Objects::nonNull)
                            .filter(string -> StringUtils.isNullOrEmpty(arg) || string.contains(arg))
                            .forEach(suggestions::add);
                }
            }
            // 请求传送到指定玩家
            else if (args[0].equals(ServerConfig.COMMAND_TP_ASK) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_ASK) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_ASK)) {
                if (args.length == 2) {
                    suggestions.addAll(getPlayerNameSuggestions(server, args));
                } else if (args.length == 3) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                }
            }
            // 同意传送请求
            else if (args[0].equals(ServerConfig.COMMAND_TP_ASK_YES) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_ASK) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_ASK_YES)) {
                if (args.length == 2) {
                    suggestions.addAll(getReqIndexSuggestions(player, ETeleportType.TP_ASK, arg));
                }
            }
            // 拒绝传送请求
            else if (args[0].equals(ServerConfig.COMMAND_TP_ASK_NO) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_ASK) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_ASK_NO)) {
                if (args.length == 2) {
                    suggestions.addAll(getReqIndexSuggestions(player, ETeleportType.TP_ASK, arg));
                }
            }
            // 请求指定玩家传送
            else if (args[0].equals(ServerConfig.COMMAND_TP_HERE) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_HERE) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_HERE)) {
                if (args.length == 2) {
                    suggestions.addAll(getPlayerNameSuggestions(server, args));
                } else if (args.length == 3) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                }
            }
            // 同意传送请求
            else if (args[0].equals(ServerConfig.COMMAND_TP_HERE_YES) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_HERE) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_HERE_YES)) {
                if (args.length == 2) {
                    suggestions.addAll(getReqIndexSuggestions(player, ETeleportType.TP_HERE, arg));
                }
            }
            // 拒绝传送请求
            else if (args[0].equals(ServerConfig.COMMAND_TP_HERE_NO) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_HERE) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_HERE_NO)) {
                if (args.length == 2) {
                    suggestions.addAll(getReqIndexSuggestions(player, ETeleportType.TP_HERE, arg));
                }
            }
            // 传送到随机位置
            else if (args[0].equals(ServerConfig.COMMAND_TP_RANDOM) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_RANDOM) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_RANDOM)) {
                if (args.length == 2) {
                    for (int i = 1; i <= 5; i++) {
                        int index = (int) Math.pow(10, i);
                        if (index <= ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT) {
                            suggestions.add(String.valueOf(index));
                        }
                    }
                } else if (args.length == 3) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                } else if (args.length == 4) {
                    Arrays.stream(DimensionType.values())
                            .map(dimensionType -> DimensionUtils.getStringIdFromInt(dimensionType.getId()))
                            .filter(Objects::nonNull)
                            .filter(string -> StringUtils.isNullOrEmpty(arg) || string.contains(arg))
                            .forEach(suggestions::add);
                }
            }
            // 传送到出生点
            else if (args[0].equals(ServerConfig.COMMAND_TP_SPAWN) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_SPAWN) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_SPAWN)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                }
                // else if (args.length == 3) {
                //     suggestions.addAll(getPlayerNameSuggestions(server, args));
                // }
            }
            // 传送到世界出生点
            else if (args[0].equals(ServerConfig.COMMAND_TP_WORLD_SPAWN) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_WORLD_SPAWN) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_WORLD_SPAWN)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                }
            }
            // 传送到顶部
            else if (args[0].equals(ServerConfig.COMMAND_TP_TOP) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_TOP) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_TOP)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                }
            }
            // 传送到底部
            else if (args[0].equals(ServerConfig.COMMAND_TP_BOTTOM) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_BOTTOM) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_BOTTOM)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                }
            }
            // 传送到上方
            else if (args[0].equals(ServerConfig.COMMAND_TP_UP) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_UP) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_UP)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                }
            }
            // 传送到下方
            else if (args[0].equals(ServerConfig.COMMAND_TP_DOWN) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_DOWN) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_DOWN)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                }
            }
            // 传送到视线尽头
            else if (args[0].equals(ServerConfig.COMMAND_TP_VIEW) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_VIEW) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_VIEW)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                } else if (args.length == 3) {
                    for (int i = 1; i <= 5; i++) {
                        int index = (int) Math.pow(10, i);
                        if (index <= ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT) {
                            suggestions.add(String.valueOf(index));
                        }
                    }
                }
            }
            // 传送到预设位置
            else if (args[0].equals(ServerConfig.COMMAND_TP_HOME) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_HOME) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_HOME)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                    PlayerTeleportDataCapability.getData(player).getHomeCoordinate().keySet().stream()
                            .filter(keyValue -> StringUtils.isNullOrEmpty(arg) || keyValue.getValue().startsWith(arg))
                            .map(KeyValue::getValue)
                            .forEach(suggestions::add);
                } else if (args.length == 3) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                    Arrays.stream(DimensionType.values())
                            .map(dimensionType -> DimensionUtils.getStringIdFromInt(dimensionType.getId()))
                            .filter(Objects::nonNull)
                            .filter(string -> StringUtils.isNullOrEmpty(arg) || string.contains(arg))
                            .forEach(suggestions::add);
                } else if (args.length == 4) {
                    PlayerTeleportDataCapability.getData(player).getHomeCoordinate().keySet().stream()
                            .filter(keyValue -> keyValue.getValue().equals(args[1]))
                            .filter(keyValue -> StringUtils.isNullOrEmpty(arg) || keyValue.getKey().contains(arg))
                            .map(KeyValue::getKey)
                            .forEach(suggestions::add);
                }
            }
            // 添加预设位置
            else if (args[0].equals(ServerConfig.COMMAND_SET_HOME) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_HOME) && NarcissusUtils.hasCommandPermission(player, ECommandType.SET_HOME)) {
                if (args.length == 2) {
                    String name = "home";
                    int index = 0;
                    while (true) {
                        int finalIndex = index;
                        if (PlayerTeleportDataCapability.getData(player).getHomeCoordinate().keySet().stream()
                                .noneMatch(keyValue -> keyValue.getValue().equals(name + (finalIndex == 0 ? "" : finalIndex)))) {
                            suggestions.add(name + index);
                            break;
                        }
                        index++;
                    }
                } else if (args.length == 3) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "default".startsWith(arg)) {
                        suggestions.add("default");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "notdefault".startsWith(arg)) {
                        suggestions.add("notdefault");
                    }
                }
            }
            // 删除预设位置
            else if (args[0].equals(ServerConfig.COMMAND_DEL_HOME) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_HOME) && NarcissusUtils.hasCommandPermission(player, ECommandType.DEL_HOME)) {
                if (args.length == 2) {
                    PlayerTeleportDataCapability.getData(player).getHomeCoordinate().keySet().stream()
                            .filter(keyValue -> StringUtils.isNullOrEmpty(arg) || keyValue.getValue().startsWith(arg))
                            .map(KeyValue::getValue)
                            .forEach(suggestions::add);
                } else if (args.length == 3) {
                    PlayerTeleportDataCapability.getData(player).getHomeCoordinate().keySet().stream()
                            .filter(keyValue -> keyValue.getValue().equals(args[1]))
                            .filter(keyValue -> StringUtils.isNullOrEmpty(arg) || keyValue.getKey().contains(arg))
                            .map(KeyValue::getKey)
                            .forEach(suggestions::add);
                }
            }
            // 传送到驿站
            else if (args[0].equals(ServerConfig.COMMAND_TP_STAGE) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_STAGE) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_STAGE)) {
                if (args.length == 2) {
                    WorldStageData.get().getStageCoordinate().keySet().stream()
                            .filter(keyValue -> StringUtils.isNullOrEmpty(arg) || keyValue.getValue().contains(arg))
                            .map(KeyValue::getValue)
                            .forEach(suggestions::add);
                } else if (args.length == 3) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                } else if (args.length == 4) {
                    WorldStageData.get().getStageCoordinate().keySet().stream()
                            .filter(keyValue -> keyValue.getValue().equals(args[1]))
                            .filter(keyValue -> StringUtils.isNullOrEmpty(arg) || keyValue.getKey().contains(arg))
                            .map(KeyValue::getKey)
                            .forEach(suggestions::add);
                }
            }
            // 添加驿站
            else if (args[0].equals(ServerConfig.COMMAND_SET_STAGE) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_STAGE) && NarcissusUtils.hasCommandPermission(player, ECommandType.SET_STAGE)) {
                if (args.length == 2) {
                    String name = "stage";
                    int index = 0;
                    while (true) {
                        int finalIndex = index;
                        if (WorldStageData.get().getStageCoordinate().keySet().stream()
                                .noneMatch(keyValue -> keyValue.getValue().equals(name + (finalIndex == 0 ? "" : finalIndex)))) {
                            suggestions.add(name + index);
                            break;
                        }
                        index++;
                    }
                } else if (args.length == 3) {
                    suggestions.addAll(getCoordinateSuggestions(player.getPosition(), args, 2));
                } else if (args.length == 4) {
                    suggestions.addAll(getCoordinateSuggestions(player.getPosition(), args, 2));
                } else if (args.length == 5) {
                    suggestions.addAll(getCoordinateSuggestions(player.getPosition(), args, 2));
                } else if (args.length == 6) {
                    Arrays.stream(DimensionType.values())
                            .map(dimensionType -> DimensionUtils.getStringIdFromInt(dimensionType.getId()))
                            .filter(Objects::nonNull)
                            .filter(string -> StringUtils.isNullOrEmpty(arg) || string.contains(arg))
                            .forEach(suggestions::add);
                }
            }
            // 删除驿站
            else if (args[0].equals(ServerConfig.COMMAND_DEL_STAGE) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_STAGE) && NarcissusUtils.hasCommandPermission(player, ECommandType.DEL_STAGE)) {
                if (args.length == 2) {
                    WorldStageData.get().getStageCoordinate().keySet().stream()
                            .filter(keyValue -> StringUtils.isNullOrEmpty(arg) || keyValue.getValue().contains(arg))
                            .map(KeyValue::getValue)
                            .forEach(suggestions::add);
                } else if (args.length == 3) {
                    WorldStageData.get().getStageCoordinate().keySet().stream()
                            .filter(keyValue -> keyValue.getValue().equals(args[1]))
                            .filter(keyValue -> StringUtils.isNullOrEmpty(arg) || keyValue.getKey().contains(arg))
                            .map(KeyValue::getKey)
                            .forEach(suggestions::add);
                }
            }
            // 传送到上一次离开位置
            else if (args[0].equals(ServerConfig.COMMAND_TP_BACK) && NarcissusUtils.isTeleportEnabled(ECommandType.TP_BACK) && NarcissusUtils.hasCommandPermission(player, ECommandType.TP_BACK)) {
                if (args.length == 2) {
                    if (StringUtils.isNullOrEmptyEx(arg) || "safe".startsWith(arg)) {
                        suggestions.add("safe");
                    }
                    if (StringUtils.isNullOrEmptyEx(arg) || "unsafe".contains(arg)) {
                        suggestions.add("unsafe");
                    }
                } else if (args.length == 3) {
                    Arrays.stream(ETeleportType.values()).map(Enum::name)
                            .filter(string -> StringUtils.isNullOrEmpty(arg) || string.contains(arg))
                            .forEach(suggestions::add);
                } else if (args.length == 4) {
                    PlayerTeleportDataCapability.getData(player).getTeleportRecords().stream()
                            .filter(record -> record.getTeleportType() == ETeleportType.valueOf(args[2]))
                            .map(record -> DimensionUtils.getStringIdFromInt(record.getBefore().getDimension().getId()))
                            .filter(string -> StringUtils.isNullOrEmpty(arg) || string.contains(arg))
                            .forEach(suggestions::add);
                }
            }
        }
        return suggestions;
    }

    /**
     * 解析并执行指令(快乐堆粪)
     *
     * @param server 服务器
     * @param sender 指令发送者
     * @param args   指令参数
     */
    public static int executeCommand(@NonNull MinecraftServer server, @NonNull ICommandSender sender, @ParametersAreNonnullByDefault String[] args) {
        EntityPlayerMP player = (EntityPlayerMP) sender;
        // 帮助信息
        if (args.length == 0 || ((args.length == 1 || args.length == 2) && args[0].equals("help"))) {
            String command;
            int page;
            if (args.length == 2) {
                command = args[1];
                page = StringUtils.toInt(command);
            } else {
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
                        commandTips = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.COMMAND, "concise", NarcissusUtils.getCommand(keyValue.getValue().replaceConcise()));
                    } else {
                        commandTips = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.COMMAND, keyValue.getValue().name().toLowerCase());
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
                        .append(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.COMMAND, command.toLowerCase() + "_detail").setColor(Color.GRAY.getRGB()));
            }
            NarcissusUtils.sendMessage(player, helpInfo);
            return 1;
        } else {
            String prefix = args[0];
            // 当前纬度ID
            if (prefix.equals(ServerConfig.COMMAND_DIMENSION)) {
                Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "dimension_info", DimensionUtils.getStringIdFromInt(player.world.provider.getDimensionType().getId()));
                NarcissusUtils.sendMessage(player, msg);
                return 1;
            }
            // 自杀或毒杀
            else if (prefix.equals(ServerConfig.COMMAND_FEED)) {
                // 判断是否开启功能
                if (!ServerConfig.SWITCH_FEED) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
                    return 0;
                }
                if (args.length <= 2) {
                    List<EntityPlayerMP> targetList = new ArrayList<>();
                    if (args.length == 1) {
                        targetList.add(player);
                    } else {
                        // 判断是否有毒杀权限
                        boolean hasPermission = ServerConfig.PERMISSION_FEED_OTHER > -1 && NarcissusUtils.hasPermissions(player, ServerConfig.PERMISSION_FEED_OTHER);
                        if (!hasPermission) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_no_permission"));
                            return 0;
                        }
                        targetList.addAll(NarcissusUtils.getPlayer(player, args[1]));
                    }
                    if (CollectionUtils.isNullOrEmpty(targetList)) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "player_not_found"));
                        return 0;
                    }
                    for (EntityPlayerMP target : targetList) {
                        if (NarcissusUtils.killPlayer(target)) {
                            NarcissusUtils.broadcastMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "died_of_narcissus_" + (new Random().nextInt(4) + 1), target.getDisplayNameString()));
                        }
                    }
                    return 1;
                }
            }
            // 传送到指定坐标
            else if (prefix.equals(ServerConfig.COMMAND_TP_COORDINATE)) {
                if (args.length <= 6) {
                    // 传送功能前置校验
                    if (checkTeleportPre(player, ECommandType.TP_COORDINATE)) return 0;
                    Coordinate coordinate = null;
                    if (args.length == 2 || args.length == 3) {
                        List<EntityPlayerMP> targetList = NarcissusUtils.getPlayer(player, args[1]);
                        if (CollectionUtils.isNotNullOrEmpty(targetList)) {
                            coordinate = new Coordinate(targetList.get(0)).setSafe(args[args.length - 1].equals("safe"));
                        } else {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "player_not_found"));
                        }
                    } else {
                        Double x = StringUtils.toCoordinate(args[1], player.posX);
                        Double y = StringUtils.toCoordinate(args[2], player.posY);
                        Double z = StringUtils.toCoordinate(args[3], player.posZ);
                        if (x != null && y != null && z != null) {
                            coordinate = new Coordinate(x, y, z, player.cameraYaw, player.cameraPitch, player.world.provider.getDimensionType());
                            if (args.length > 4) {
                                coordinate.setSafe(args[4].equals("safe"));
                                if (args.length == 6) {
                                    coordinate.setDimension(DimensionUtils.getDimensionType(args[5]));
                                }
                            }
                        }
                    }
                    if (coordinate == null) {
                        return -1;
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_COORDINATE, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_COORDINATE);
                    return 1;
                }
            }
            // 传送到指定结构
            else if (prefix.equals(ServerConfig.COMMAND_TP_STRUCTURE)) {
                if (checkTeleportPre(player, ECommandType.TP_STRUCTURE)) {
                    return 0;
                }
                if (args.length >= 2) {
                    String structId = args[1];
                    Biome biome = NarcissusUtils.getBiome(structId);
                    if (!NarcissusUtils.getStructureList().contains(structId) && biome == null) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "structure_biome_not_found"), structId);
                        return 0;
                    }
                    int range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT;
                    if (args.length >= 3) {
                        int anInt = StringUtils.toInt(args[2]);
                        range = anInt > 0 ? anInt : range;
                    }
                    range = NarcissusUtils.checkRange(player, ETeleportType.TP_STRUCTURE, range);
                    DimensionType targetLevel = null;
                    if (args.length == 4) {
                        targetLevel = DimensionUtils.getDimensionType(args[3]);
                    }
                    if (targetLevel == null) {
                        targetLevel = player.world.provider.getDimensionType();
                    }
                    WorldServer world = Objects.requireNonNull(NarcissusFarewell.getServerInstance().getWorld(targetLevel.getId()));
                    Coordinate start = new Coordinate(player).setDimension(targetLevel);
                    int finalRange = range;
                    player.connection.sendPacket(new SPacketChat(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "tp_structure_searching").toTextComponent(), ChatType.GAME_INFO));
                    new Thread(() -> {
                        Coordinate coordinate;
                        if (biome != null) {
                            coordinate = NarcissusUtils.findNearestBiome(world, start, biome, finalRange, 8);
                        } else {
                            coordinate = NarcissusUtils.findNearestStruct(world, start, structId, finalRange);
                        }
                        if (coordinate == null) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "structure_biome_not_found_in_range"), structId);
                            return;
                        }
                        coordinate.setSafe(true);
                        // 验证传送代价
                        if (checkTeleportPost(player, coordinate, ETeleportType.TP_STRUCTURE, true)) return;
                        server.addScheduledTask(() -> NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_STRUCTURE));
                    }).start();
                    return 1;
                }
            }
            // 请求传送到指定玩家
            else if (prefix.equals(ServerConfig.COMMAND_TP_ASK)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_ASK)) {
                    return 0;
                }
                if (args.length <= 3) {
                    EntityPlayerMP target = null;
                    if (args.length == 1) {
                        // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                        target = NarcissusFarewell.getTeleportRequest().values().stream()
                                .filter(request -> request.getRequester().getUniqueID().equals(player.getUniqueID()))
                                .filter(request -> {
                                    EntityPlayer entity = request.getTarget();
                                    return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, ETeleportType.TP_ASK)
                                            || entity != null && entity.world.provider.getDimensionType() == player.world.provider.getDimensionType();
                                })
                                .max(Comparator.comparing(TeleportRequest::getRequestTime))
                                .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                                        .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                                .getTarget();
                    } else {
                        List<EntityPlayerMP> targetList = NarcissusUtils.getPlayer(player, args[1]);
                        if (CollectionUtils.isNotNullOrEmpty(targetList)) {
                            target = targetList.get(0);
                        }
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
                        request.setSafe("safe".equalsIgnoreCase(args[args.length - 1]));
                    } catch (IllegalArgumentException ignored) {
                    }
                    if (checkTeleportPost(request)) return 0;
                    NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request);

                    // 通知目标玩家
                    {
                        // 创建 "Yes" 按钮
                        Component yesButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "yes_button", NarcissusUtils.getPlayerLanguage(target))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_ASK_YES, request.getRequestId())));
                        // 创建 "No" 按钮
                        Component noButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "no_button", NarcissusUtils.getPlayerLanguage(target))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_ASK_NO, request.getRequestId())));
                        Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "tp_ask_request_received"
                                , player.getDisplayNameString(), yesButton, noButton);
                        NarcissusUtils.sendMessage(target, msg);
                    }
                    // 通知请求者
                    {
                        NarcissusUtils.sendMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "tp_ask_request_sent", target.getDisplayNameString()));
                    }
                    return 1;
                }
            }
            // 同意传送请求
            else if (prefix.equals(ServerConfig.COMMAND_TP_ASK_YES)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_ASK_YES)) {
                    return 0;
                }
                if (args.length == 1 || args.length == 2) {
                    String id = getRequestId(player, args.length == 2 ? args[1] : "", ETeleportType.TP_ASK);
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
                }
            }
            // 拒绝传送请求
            else if (prefix.equals(ServerConfig.COMMAND_TP_ASK_NO)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_ASK_NO)) {
                    return 0;
                }
                if (args.length == 1 || args.length == 2) {
                    String id = getRequestId(player, args.length == 2 ? args[1] : "", ETeleportType.TP_ASK);
                    if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                        return 0;
                    }
                    TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
                    NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_rejected"), request.getTarget().getDisplayNameString());
                    return 1;
                }
            }
            // 请求指定玩家传送
            else if (prefix.equals(ServerConfig.COMMAND_TP_HERE)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_HERE)) {
                    return 0;
                }
                if (args.length <= 3) {
                    EntityPlayerMP target = null;
                    if (args.length == 1) {
                        // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                        target = NarcissusFarewell.getTeleportRequest().values().stream()
                                .filter(request -> request.getRequester().getUniqueID().equals(player.getUniqueID()))
                                .filter(request -> {
                                    EntityPlayer entity = request.getTarget();
                                    return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, ETeleportType.TP_HERE)
                                            || entity != null && entity.world.provider.getDimensionType() == player.world.provider.getDimensionType();
                                })
                                .max(Comparator.comparing(TeleportRequest::getRequestTime))
                                .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                                        .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                                .getTarget();
                    } else {
                        List<EntityPlayerMP> targetList = NarcissusUtils.getPlayer(player, args[1]);
                        if (CollectionUtils.isNotNullOrEmpty(targetList)) {
                            target = targetList.get(0);
                        }
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
                        request.setSafe("safe".equalsIgnoreCase(args[args.length - 1]));
                    } catch (IllegalArgumentException ignored) {
                    }
                    if (checkTeleportPost(request)) return 0;
                    NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request);

                    // 通知目标玩家
                    {
                        // 创建 "Yes" 按钮
                        Component yesButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "yes_button", NarcissusUtils.getPlayerLanguage(target))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_HERE_YES, request.getRequestId())));
                        // 创建 "No" 按钮
                        Component noButton = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "no_button", NarcissusUtils.getPlayerLanguage(target))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), ServerConfig.COMMAND_TP_HERE_NO, request.getRequestId())));
                        Component msg = Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.MESSAGE, "tp_here_request_received"
                                , player.getDisplayNameString(), Component.translatable(NarcissusUtils.getPlayerLanguage(target), EI18nType.WORD, request.isSafe() ? "tp_here_safe" : "tp_here_unsafe"), yesButton, noButton);
                        NarcissusUtils.sendMessage(target, msg);
                    }
                    // 通知请求者
                    {
                        NarcissusUtils.sendMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "tp_here_request_sent", target.getDisplayNameString()));
                    }
                    return 1;
                }
            }
            // 同意传送请求
            else if (prefix.equals(ServerConfig.COMMAND_TP_HERE_YES)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_HERE_YES)) {
                    return 0;
                }
                if (args.length == 1 || args.length == 2) {
                    String id = getRequestId(player, args.length == 2 ? args[1] : "", ETeleportType.TP_HERE);
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
                }
            }
            // 拒绝传送请求
            else if (prefix.equals(ServerConfig.COMMAND_TP_HERE_NO)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_HERE_NO)) {
                    return 0;
                }
                if (args.length == 1 || args.length == 2) {
                    String id = getRequestId(player, args.length == 2 ? args[1] : "", ETeleportType.TP_HERE);
                    if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_not_found"));
                        return 0;
                    }
                    TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
                    NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_rejected"), request.getTarget().getDisplayNameString());
                    return 1;
                }
            }
            // 传送到随机位置
            else if (prefix.equals(ServerConfig.COMMAND_TP_RANDOM)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_RANDOM)) {
                    return 0;
                }
                if (args.length <= 4) {
                    int range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT;
                    if (args.length >= 2) {
                        int anInt = StringUtils.toInt(args[1]);
                        range = anInt > 0 ? anInt : range;
                    }
                    range = NarcissusUtils.checkRange(player, ETeleportType.TP_RANDOM, range);
                    boolean safe = false;
                    if (args.length >= 3) {
                        safe = args[2].equals("safe");
                    }
                    DimensionType targetLevel = null;
                    if (args.length == 4) {
                        targetLevel = DimensionUtils.getDimensionType(args[3]);
                    }
                    if (targetLevel == null) {
                        targetLevel = player.world.provider.getDimensionType();
                    }
                    Coordinate coordinate = Coordinate.random(player, range, targetLevel).setSafe(safe);
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_RANDOM, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_RANDOM);
                    return 1;
                }
            }
            // 传送到出生点
            else if (prefix.equals(ServerConfig.COMMAND_TP_SPAWN)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_SPAWN)) {
                    return 0;
                }
                if (args.length <= 3) {
                    EntityPlayerMP target = player;
                    boolean hasPermission = ServerConfig.PERMISSION_TP_SPAWN_OTHER > -1 && NarcissusUtils.hasPermissions(player, ServerConfig.PERMISSION_TP_SPAWN_OTHER);
                    if (hasPermission) {
                        List<EntityPlayerMP> targetList = NarcissusUtils.getPlayer(player, args[1]);
                        if (CollectionUtils.isNotNullOrEmpty(targetList)) {
                            target = targetList.get(0);
                        }
                    } else if (args.length == 3) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_no_permission"));
                        return 0;
                    }
                    Coordinate coordinate = new Coordinate(target);
                    BlockPos respawnPosition = target.getBedLocation(player.world.provider.getDimensionType().getId());
                    // idea很烦诶，明明就会是null非警告我不会为null
                    if (respawnPosition == null && target.world.provider.getDimensionType().getId() != DimensionType.OVERWORLD.getId()) {
                        respawnPosition = target.getBedLocation(DimensionType.OVERWORLD.getId());
                        coordinate.setDimension(DimensionType.OVERWORLD);
                    }
                    if (respawnPosition == null) {
                        respawnPosition = target.world.getSpawnPoint();
                        coordinate.setDimension(target.world.provider.getDimensionType());
                    }
                    if (respawnPosition == null) {
                        respawnPosition = NarcissusFarewell.getServerInstance().getWorld(DimensionType.OVERWORLD.getId()).getSpawnPoint();
                        coordinate.setDimension(DimensionType.OVERWORLD);
                    }
                    coordinate.fromBlockPos(respawnPosition);
                    try {
                        coordinate.setSafe("safe".equalsIgnoreCase(args[args.length - 1]));
                    } catch (IllegalArgumentException ignored) {
                        coordinate.setSafe(true);
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_SPAWN, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_SPAWN);
                    return 1;
                }
            }
            // 传送到世界出生点
            else if (prefix.equals(ServerConfig.COMMAND_TP_WORLD_SPAWN)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_WORLD_SPAWN)) {
                    return 0;
                }
                if (args.length <= 2) {
                    Coordinate coordinate = new Coordinate(player);
                    BlockPos respawnPosition = player.world.getSpawnPoint();
                    if (respawnPosition == null) {
                        respawnPosition = NarcissusFarewell.getServerInstance().getWorld(DimensionType.OVERWORLD.getId()).getSpawnPoint();
                        coordinate.setDimension(DimensionType.OVERWORLD);
                    }
                    coordinate.fromBlockPos(respawnPosition);
                    try {
                        coordinate.setSafe("safe".equalsIgnoreCase(args[args.length - 1]));
                    } catch (IllegalArgumentException ignored) {
                        coordinate.setSafe(true);
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_WORLD_SPAWN, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_WORLD_SPAWN);
                    return 1;
                }
            }
            // 传送到顶部
            else if (prefix.equals(ServerConfig.COMMAND_TP_TOP)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_TOP)) {
                    return 0;
                }
                if (args.length <= 2) {
                    Coordinate coordinate = NarcissusUtils.findTopCandidate(player.getServerWorld(), new Coordinate(player));
                    if (coordinate == null) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_top_not_found"));
                        return 0;
                    }
                    try {
                        coordinate.setSafe("safe".equalsIgnoreCase(args[args.length - 1])).setSafeMode(ESafeMode.Y_DOWN);
                    } catch (IllegalArgumentException ignored) {
                        coordinate.setSafe(true).setSafeMode(ESafeMode.Y_DOWN);
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_TOP, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_TOP);
                    return 1;
                }
            }
            // 传送到底部
            else if (prefix.equals(ServerConfig.COMMAND_TP_BOTTOM)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_BOTTOM)) {
                    return 0;
                }
                if (args.length <= 2) {
                    Coordinate coordinate = NarcissusUtils.findBottomCandidate(player.getServerWorld(), new Coordinate(player));
                    if (coordinate == null) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_bottom_not_found"));
                        return 0;
                    }
                    try {
                        coordinate.setSafe("safe".equalsIgnoreCase(args[args.length - 1])).setSafeMode(ESafeMode.Y_UP);
                    } catch (IllegalArgumentException ignored) {
                        coordinate.setSafe(true).setSafeMode(ESafeMode.Y_UP);
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_BOTTOM, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_BOTTOM);
                    return 1;
                }
            }
            // 传送到上方
            else if (prefix.equals(ServerConfig.COMMAND_TP_UP)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_UP)) {
                    return 0;
                }
                if (args.length <= 2) {
                    Coordinate coordinate = NarcissusUtils.findUpCandidate(player.getServerWorld(), new Coordinate(player));
                    if (coordinate == null) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_up_not_found"));
                        return 0;
                    }
                    try {
                        coordinate.setSafe("safe".equalsIgnoreCase(args[args.length - 1])).setSafeMode(ESafeMode.Y_UP);
                    } catch (IllegalArgumentException ignored) {
                        coordinate.setSafe(true).setSafeMode(ESafeMode.Y_UP);
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_UP, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_UP);
                    return 1;
                }
            }
            // 传送到下方
            else if (prefix.equals(ServerConfig.COMMAND_TP_DOWN)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_DOWN)) {
                    return 0;
                }
                if (args.length <= 2) {
                    Coordinate coordinate = NarcissusUtils.findDownCandidate(player.getServerWorld(), new Coordinate(player));
                    if (coordinate == null) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_down_not_found"));
                        return 0;
                    }
                    try {
                        coordinate.setSafe("safe".equalsIgnoreCase(args[args.length - 1])).setSafeMode(ESafeMode.Y_DOWN);
                    } catch (IllegalArgumentException ignored) {
                        coordinate.setSafe(true).setSafeMode(ESafeMode.Y_DOWN);
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_DOWN, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_DOWN);
                    return 1;
                }
            }
            // 传送到视线尽头
            else if (prefix.equals(ServerConfig.COMMAND_TP_VIEW)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_VIEW)) {
                    return 0;
                }
                if (args.length <= 3) {
                    boolean safe;
                    if (args.length >= 2) {
                        safe = args[1].equals("safe");
                    } else {
                        safe = false;
                    }
                    int range = ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT;
                    if (args.length == 3) {
                        int anInt = StringUtils.toInt(args[2]);
                        range = anInt > 0 ? anInt : range;
                    }
                    range = NarcissusUtils.checkRange(player, ETeleportType.TP_VIEW, range);
                    int finalRange = range;
                    player.connection.sendPacket(new SPacketChat(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "tp_view_searching").toTextComponent(), ChatType.GAME_INFO));
                    new Thread(() -> {
                        Coordinate coordinate = NarcissusUtils.findViewEndCandidate(player, safe, finalRange);
                        if (coordinate == null) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, safe ? "tp_view_safe_not_found" : "tp_view_not_found"));
                            return;
                        }
                        coordinate.setSafeMode(ESafeMode.Y_OFFSET_3);
                        // 验证传送代价
                        if (checkTeleportPost(player, coordinate, ETeleportType.TP_VIEW, true)) return;
                        server.addScheduledTask(() -> NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_VIEW));
                    }).start();
                    return 1;
                }
            }
            // 传送到预设位置
            else if (prefix.equals(ServerConfig.COMMAND_TP_HOME)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_HOME)) {
                    return 0;
                }
                if (args.length <= 4) {
                    DimensionType targetLevel = null;
                    DimensionType targetDimension = DimensionUtils.getDimensionType(args[args.length - 1]);
                    if (targetDimension != null) {
                        targetLevel = targetDimension;
                    }
                    String name = null;
                    if (args.length >= 2 && NarcissusUtils.isPlayerHome(player, args[1])) {
                        name = args[1];
                    }
                    Coordinate coordinate = NarcissusUtils.getPlayerHome(player, targetLevel, name);
                    if (coordinate == null) {
                        if (targetLevel == null && name == null) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found"));
                        } else if (targetLevel != null && name == null) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found_in_dimension"), DimensionUtils.getStringIdFromInt(targetLevel.getId()));
                        } else if (targetLevel == null) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found_with_name"), name);
                        } else {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found_with_name_in_dimension"), DimensionUtils.getStringIdFromInt(targetLevel.getId()), name);
                        }
                        return 0;
                    }
                    try {
                        coordinate.setSafe(args.length >= 2 && args[args.length - 2].equals("safe"));
                    } catch (IllegalArgumentException ignored) {
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_HOME, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_HOME);
                    return 1;
                }
            }
            // 添加预设位置
            else if (prefix.equals(ServerConfig.COMMAND_SET_HOME)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.SET_HOME)) {
                    return 0;
                }
                if (args.length <= 3) {
                    // 判断设置数量是否超过限制
                    IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
                    if (data.getHomeCoordinate().size() >= ServerConfig.TELEPORT_HOME_LIMIT) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_limit"), ServerConfig.TELEPORT_HOME_LIMIT);
                        return 0;
                    }
                    String name = "home";
                    if (args.length >= 2) {
                        name = args[1];
                    }
                    boolean defaultHome = false;
                    if (args.length == 3) {
                        defaultHome = args[2].equals("default");
                    }
                    Coordinate coordinate = new Coordinate(player);
                    String dimension = DimensionUtils.getStringIdFromInt(player.world.provider.getDimensionType().getId());
                    KeyValue<String, String> key = new KeyValue<>(dimension, name);
                    if (data.getHomeCoordinate().containsKey(key)) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_already_exists"), key.getKey(), key.getValue());
                        return 0;
                    }
                    data.addHomeCoordinate(key, coordinate);
                    if (defaultHome) {
                        if (data.getDefaultHome().containsKey(dimension)) {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_default_remove"), data.getDefaultHome(dimension).getValue());
                        }
                        data.addDefaultHome(dimension, name);
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_set_default"), name, coordinate.toXyzString());
                    } else {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_set"), name, coordinate.toXyzString());
                    }
                    return 1;
                }
            }
            // 删除预设位置
            else if (prefix.equals(ServerConfig.COMMAND_DEL_HOME)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.DEL_HOME)) {
                    return 0;
                }
                if (args.length == 3) {
                    IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
                    String name = args[1];
                    String dimension = null;
                    if (DimensionUtils.getDimensionType(args[2]) != null) {
                        dimension = args[2];
                    }
                    if (StringUtils.isNullOrEmptyEx(dimension)) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "dimension_not_found"), args[2]);
                        return 0;
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
                }
            }
            // 传送到驿站
            else if (prefix.equals(ServerConfig.COMMAND_TP_STAGE)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_STAGE)) {
                    return 0;
                }
                if (args.length <= 4) {
                    String name = null;
                    if (args.length >= 2) {
                        name = args[1];
                    }
                    DimensionType targetLevel = null;
                    if (args.length == 4) {
                        targetLevel = DimensionUtils.getDimensionType(args[3]);
                    }
                    String dimension = targetLevel != null ? args[3] : null;
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
                            coordinate = stageData.getCoordinate(DimensionUtils.getStringIdFromInt(player.world.provider.getDimensionType().getId()), name);
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
                        coordinate.setSafe(args.length >= 3 && "safe".equalsIgnoreCase(args[2]));
                    } catch (IllegalArgumentException ignored) {
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_STAGE, true)) return 0;
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_STAGE);
                    return 1;
                }
            }
            // 添加驿站
            else if (prefix.equals(ServerConfig.COMMAND_SET_STAGE)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.SET_STAGE)) {
                    return 0;
                }
                if (args.length > 1 && args.length <= 6) {
                    WorldStageData stageData = WorldStageData.get();
                    String name = args[1];
                    DimensionType targetLevel = null;
                    if (args.length == 6) {
                        targetLevel = DimensionUtils.getDimensionType(args[5]);
                    }
                    if (targetLevel == null) {
                        targetLevel = player.world.provider.getDimensionType();
                    }
                    String dimension = DimensionUtils.getStringIdFromInt(targetLevel.getId());
                    KeyValue<String, String> key = new KeyValue<>(dimension, name);
                    if (stageData.getStageCoordinate().containsKey(key)) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_already_exists"), key.getKey(), key.getValue());
                        return 0;
                    }
                    Coordinate coordinate = new Coordinate(player).setDimension(targetLevel);
                    if (args.length >= 5) {
                        Double x = StringUtils.toCoordinate(args[1], player.posX);
                        Double y = StringUtils.toCoordinate(args[2], player.posY);
                        Double z = StringUtils.toCoordinate(args[3], player.posZ);
                        if (x == null || y == null || z == null) {
                            return -1;
                        }
                        coordinate.setX(x).setY(y).setZ(z);
                    }
                    stageData.addCoordinate(key, coordinate);
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_set"), name, coordinate.toXyzString());
                    return 1;
                }
            }
            // 删除驿站
            else if (prefix.equals(ServerConfig.COMMAND_DEL_STAGE)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.DEL_STAGE)) {
                    return 0;
                }
                if (args.length == 3) {
                    String name = args[1];
                    String dimension = null;
                    if (DimensionUtils.getDimensionType(args[2]) != null) {
                        dimension = args[2];
                    }
                    if (StringUtils.isNullOrEmptyEx(dimension)) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "dimension_not_found"), args[2]);
                        return 0;
                    }
                    WorldStageData stageData = WorldStageData.get();
                    Coordinate remove = stageData.getStageCoordinate().remove(new KeyValue<>(dimension, name));
                    stageData.markDirty();
                    if (remove == null) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_not_found_with_name_in_dimension"), dimension, name);
                        return 0;
                    }
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_del"), dimension, name);
                    return 1;
                }
            }
            // 传送到上一次离开位置
            else if (prefix.equals(ServerConfig.COMMAND_TP_BACK)) {
                // 传送功能前置校验
                if (checkTeleportPre(player, ECommandType.TP_BACK)) {
                    return 0;
                }
                if (args.length <= 4) {
                    ETeleportType type = null;
                    if (args.length >= 3) {
                        type = ETeleportType.valueOf(args[2]);
                    }
                    DimensionType targetLevel = null;
                    if (args.length == 4) {
                        targetLevel = DimensionUtils.getDimensionType(args[3]);
                    }
                    TeleportRecord record = NarcissusUtils.getBackTeleportRecord(player, type, targetLevel);
                    if (record == null) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "back_not_found"));
                        return 0;
                    }
                    Coordinate coordinate = record.getBefore().clone();
                    try {
                        coordinate.setSafe(args.length >= 2 && "safe".equalsIgnoreCase(args[1]));
                    } catch (IllegalArgumentException ignored) {
                    }
                    // 验证传送代价
                    if (checkTeleportPost(player, coordinate, ETeleportType.TP_BACK, true)) return 0;
                    NarcissusUtils.removeBackTeleportRecord(player, record);
                    NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_BACK);
                    return 1;
                }
            }
        }
        return -1;
    }

    private static List<String> getPlayerNameSuggestions(MinecraftServer server, String[] args) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isNullOrEmptyEx(args[args.length - 1]) || args[args.length - 1].equals("@")) {
            result.add("@p");
            result.add("@r");
            result.add("@s");
        }
        result.addAll(getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()));
        return result;
    }

    private static List<String> getCoordinateSuggestions(BlockPos playerPos, String[] args, int start) {
        List<String> result = new ArrayList<>();
        if (args.length - 1 == start) {
            result.add("~");
            result.add(String.valueOf(playerPos.getX()));
        } else if (args.length - 1 == start + 1) {
            String pre = args[args.length - 2];
            if (StringUtils.isNotNullOrEmpty(pre) && (pre.equals("~") || pre.equals(StringUtils.toInt(pre) + ""))) {
                result.add("~");
                result.add(String.valueOf(playerPos.getY()));
            }
        } else if (args.length - 1 == start + 2) {
            String pre = args[args.length - 2];
            String prePre = args[args.length - 3];
            if ((StringUtils.isNotNullOrEmpty(pre) && (pre.equals("~") || pre.equals(StringUtils.toInt(pre) + "")))
                    && (StringUtils.isNotNullOrEmpty(prePre) && (prePre.equals("~") || prePre.equals(StringUtils.toInt(prePre) + "")))) {
                result.add("~");
                result.add(String.valueOf(playerPos.getZ()));
            }
        }
        return result;
    }

    private static List<String> getReqIndexSuggestions(EntityPlayer player, ETeleportType teleportType, String arg) {
        Set<String> result = new HashSet<>();
        NarcissusFarewell.getTeleportRequest().entrySet().stream()
                .filter(entry -> entry.getValue().getRequester().getUniqueID().equals(player.getUniqueID()))
                .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                .filter(entry -> StringUtils.isNullOrEmpty(arg) || entry.getKey().contains(arg))
                .forEach(entry -> {
                    result.add(entry.getKey());
                    result.add(entry.getValue().getRequester().getDisplayNameString());
                });
        if (StringUtils.isNullOrEmpty(arg)) {
            for (int i = 0; i < NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> entry.getValue().getRequester().getUniqueID().equals(player.getUniqueID()))
                    .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                    .count(); i++) {
                result.add(String.valueOf(i + 1));
            }
        }
        return result.stream().sorted().collect(Collectors.toList());
    }

    /**
     * 传送解析前置校验
     *
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPre(EntityPlayerMP player, ECommandType commandType) {
        // 判断是否开启传送功能
        if (!NarcissusUtils.isTeleportEnabled(commandType)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
            return true;
        }
        // 判断是否有传送权限
        if (!NarcissusUtils.hasCommandPermission(player, commandType)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_no_permission"));
            return true;
        }
        // 判断是否有冷却时间
        ETeleportType teleportType = null;
        try {
            teleportType = ETeleportType.valueOf(commandType.name());
        } catch (IllegalArgumentException ignored) {
        }
        if (teleportType != null) {
            int teleportCoolDown = NarcissusUtils.getTeleportCoolDown(player, teleportType);
            if (teleportCoolDown > 0) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_cooldown"), teleportCoolDown);
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
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(request.getRequester(), request.getTarget().world.provider.getDimensionType(), request.getTeleportType());
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
    public static boolean checkTeleportPost(EntityPlayerMP player, Coordinate target, ETeleportType type) {
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
    public static boolean checkTeleportPost(EntityPlayerMP player, Coordinate target, ETeleportType type, boolean submit) {
        boolean result;
        // 判断跨维度传送
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(player, target.getDimension(), type);
        // 判断是否有传送代价
        result = result && NarcissusUtils.validTeleportCost(player, target, type, submit);
        return !result;
    }

    private static String getRequestId(EntityPlayerMP player, String arg, ETeleportType teleportType) {
        String result = null;
        try {
            List<EntityPlayerMP> playerMPList = NarcissusUtils.getPlayer(player, arg);
            if (CollectionUtils.isNotNullOrEmpty(playerMPList)) {
                EntityPlayerMP requester = playerMPList.get(0);
                Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> entry.getValue().getTarget().getUniqueID().equals(player.getUniqueID()))
                        .filter(entry -> entry.getValue().getRequester().getUniqueID().equals(requester.getUniqueID()))
                        .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                        .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                        .orElse(null);
                if (entry1 != null) {
                    result = entry1.getKey();
                }
            } else if (NarcissusFarewell.getTeleportRequest().containsKey(arg)) {
                result = arg;
            } else if (String.valueOf(StringUtils.toInt(arg)).equals(arg)) {
                int askIndex = StringUtils.toInt(arg);
                List<Map.Entry<String, TeleportRequest>> entryList = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> entry.getValue().getTarget().getUniqueID().equals(player.getUniqueID()))
                        .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                        // 使用负数实现倒序排列
                        .sorted(Comparator.comparing(entry -> -entry.getValue().getRequestTime().getTime()))
                        .collect(Collectors.toList());
                if (askIndex > 0 && askIndex <= entryList.size()) {
                    result = entryList.get(askIndex - 1).getKey();
                }
            } else {
                // 使用负数实现倒序排列
                Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> entry.getValue().getTarget().getUniqueID().equals(player.getUniqueID()))
                        .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                        .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                        .orElse(null);
                if (entry1 != null) {
                    result = entry1.getKey();
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
