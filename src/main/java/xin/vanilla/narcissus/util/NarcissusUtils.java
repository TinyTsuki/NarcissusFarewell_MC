package xin.vanilla.narcissus.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CClientSettingsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.capability.TeleportRecord;
import xin.vanilla.narcissus.capability.player.IPlayerTeleportData;
import xin.vanilla.narcissus.capability.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.capability.world.WorldStageData;
import xin.vanilla.narcissus.config.*;
import xin.vanilla.narcissus.enums.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NarcissusUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static String getCommandPrefix() {
        String commandPrefix = ServerConfig.COMMAND_PREFIX.get();
        if (StringUtils.isNullOrEmptyEx(commandPrefix) || !commandPrefix.matches("^(\\w ?)+$")) {
            ServerConfig.COMMAND_PREFIX.set(NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        }
        return ServerConfig.COMMAND_PREFIX.get().trim();
    }

    // region 安全坐标

    /**
     * 安全的方块
     */
    private static final List<BlockState> SAFE_BLOCKS = ServerConfig.SAFE_BLOCKS.get().stream()
            .map(block -> {
                try {
                    return new BlockStateParser(new StringReader(block), false).parse(true).getState();
                } catch (CommandSyntaxException e) {
                    LOGGER.error("Invalid unsafe block: {}", block, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    /**
     * 不安全的方块
     */
    private static final List<BlockState> UNSAFE_BLOCKS = ServerConfig.UNSAFE_BLOCKS.get().stream()
            .map(block -> {
                try {
                    return new BlockStateParser(new StringReader(block), false).parse(true).getState();
                } catch (CommandSyntaxException e) {
                    LOGGER.error("Invalid unsafe block: {}", block, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    private static final List<BlockState> SUFFOCATING_BLOCKS = ServerConfig.SUFFOCATING_BLOCKS.get().stream()
            .map(block -> {
                try {
                    return new BlockStateParser(new StringReader(block), false).parse(true).getState();
                } catch (CommandSyntaxException e) {
                    LOGGER.error("Invalid unsafe block: {}", block, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

    public static Coordinate findTopCandidate(ServerWorld world, Coordinate start) {
        if (start.getY() >= world.getMaxBuildHeight()) return null;
        for (int y : IntStream.range((int) start.getY() + 1, world.getMaxBuildHeight()).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue).reversed())
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findBottomCandidate(ServerWorld world, Coordinate start) {
        if (start.getY() <= 0) return null;
        for (int y : IntStream.range(0, (int) start.getY() - 1).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findUpCandidate(ServerWorld world, Coordinate start) {
        if (start.getY() >= world.getMaxBuildHeight()) return null;
        for (int y : IntStream.range((int) start.getY() + 1, world.getMaxBuildHeight()).boxed()
                .sorted(Comparator.comparingInt(a -> a - (int) start.getY()))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findDownCandidate(ServerWorld world, Coordinate start) {
        if (start.getY() <= 0) return null;
        for (int y : IntStream.range(0, (int) start.getY() - 1).boxed()
                .sorted(Comparator.comparingInt(a -> (int) start.getY() - a))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findViewEndCandidate(ServerPlayerEntity player, boolean safe, int range) {
        double stepScale = 0.75;
        Coordinate start = new Coordinate(player);
        Coordinate result = null;

        // 获取玩家的起始位置
        Vector3d startPosition = player.getEyePosition(1.0F);

        // 获取玩家的视线方向
        Vector3d direction = player.getViewVector(1.0F).normalize();
        // 步长
        Vector3d stepVector = direction.scale(stepScale);

        // 初始化变量
        Vector3d currentPosition = startPosition;
        World world = player.getLevel();

        // 从近到远寻找碰撞点
        for (int stepCount = 0; stepCount <= range; stepCount++) {
            // 更新当前检测位置
            currentPosition = startPosition.add(stepVector.scale(stepCount));
            BlockPos currentBlockPos = new BlockPos(currentPosition.x, currentPosition.y, currentPosition.z);

            // 获取当前方块状态
            BlockState blockState = world.getBlockState(currentBlockPos);

            // 检测方块是否不可穿过
            if (blockState.getMaterial().blocksMotion()) {
                result = start.clone().fromVector3d(startPosition.add(stepVector.scale(stepCount - 1)));
                break;
            }
        }

        // 如果未找到碰撞点，则使用射线的终点
        if (result == null) {
            result = start.clone().fromVector3d(currentPosition);
        }

        // 如果 safe 为 true，从碰撞点反向查找安全位置
        if (safe) {
            Vector3d collisionVector = result.toVector3d(); // 碰撞点的三维向量
            for (int stepCount = (int) Math.ceil(collisionVector.distanceTo(startPosition) / stepScale); stepCount >= 0; stepCount--) {
                currentPosition = startPosition.add(stepVector.scale(stepCount));
                BlockPos currentBlockPos = new BlockPos(currentPosition.x, currentPosition.y, currentPosition.z);
                for (int yOffset = -3; yOffset < 3; yOffset++) {
                    Coordinate candidate = start.clone().fromBlockPos(currentBlockPos).addY(yOffset);
                    // 判断当前候选坐标是否安全
                    if (isSafeCoordinate(world, candidate)) {
                        result = candidate.addX(0.5).addY(0.15).addZ(0.5); // 找到安全位置
                        stepCount = 0; // 跳出循环
                        break;
                    }
                }
            }
        }
        // 如果起点与结果相同则返回null
        if (start.equalsOfRange(result, 1)) {
            result = null;
        }
        return result;
    }

    public static Coordinate findSafeCoordinate(Coordinate coordinate, boolean belowAllowAir) {
        World world = getWorld(coordinate.getDimension());

        int chunkX = (int) coordinate.getX() >> 4;
        int chunkZ = (int) coordinate.getZ() >> 4;

        // 搜索安全位置，限制在目标区块内
        return searchForSafeCoordinateInChunk(world, coordinate, chunkX, chunkZ, belowAllowAir);
    }

    private static Coordinate searchForSafeCoordinateInChunk(World world, Coordinate coordinate, int chunkX, int chunkZ, boolean belowAllowAir) {
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;

        // FIXME 1.18及之后版本range应该为(-64, world.getHeight())
        List<Integer> yList;
        List<Integer> xList;
        List<Integer> zList;
        if (coordinate.getSafeMode() == ESafeMode.Y_DOWN) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) coordinate.getY(), 0).boxed()
                    .sorted(Comparator.comparingInt(a -> (int) coordinate.getY() - a))
                    .collect(Collectors.toList());
        } else if (coordinate.getSafeMode() == ESafeMode.Y_UP) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) coordinate.getY(), world.getHeight()).boxed()
                    .sorted(Comparator.comparingInt(a -> a - (int) coordinate.getY()))
                    .collect(Collectors.toList());
        } else if (coordinate.getSafeMode() == ESafeMode.Y_OFFSET_3) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) (coordinate.getY() - 3), (int) (coordinate.getY() + 3)).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getY())))
                    .collect(Collectors.toList());
        } else {
            xList = IntStream.range(chunkMinX, chunkMaxX).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getX())))
                    .collect(Collectors.toList());
            zList = IntStream.range(chunkMinZ, chunkMaxZ).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getZ())))
                    .collect(Collectors.toList());
            yList = IntStream.range(0, world.getHeight()).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getY())))
                    .collect(Collectors.toList());
        }
        for (int y : yList) {
            if (coordinate.getSafeMode() == ESafeMode.NONE && y <= 0 || (y <= 0 || y > world.getHeight())) continue;
            for (int x : xList) {
                for (int z : zList) {
                    Coordinate candidate = new Coordinate().setX(x + 0.5).setY(y + 0.15).setZ(z + 0.5)
                            .setYaw(coordinate.getYaw()).setPitch(coordinate.getPitch())
                            .setDimension(coordinate.getDimension())
                            .setSafe(coordinate.isSafe()).setSafeMode(coordinate.getSafeMode());
                    if (belowAllowAir) {
                        if (isAirCoordinate(world, candidate)) {
                            return candidate;
                        }
                    } else {
                        if (isSafeCoordinate(world, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return coordinate;
    }

    private static boolean isAirCoordinate(World world, Coordinate coordinate) {
        BlockState block = world.getBlockState(coordinate.toBlockPos());
        BlockState blockAbove = world.getBlockState(coordinate.toBlockPos().above());
        BlockState blockBelow = world.getBlockState(coordinate.toBlockPos().below());
        return (!block.getMaterial().blocksMotion() && !UNSAFE_BLOCKS.contains(block) && !UNSAFE_BLOCKS.contains(block.getBlock().defaultBlockState()))
                && (!blockAbove.getMaterial().blocksMotion() && !UNSAFE_BLOCKS.contains(blockAbove) && !UNSAFE_BLOCKS.contains(blockAbove.getBlock().defaultBlockState()) && !SUFFOCATING_BLOCKS.contains(blockAbove) && !SUFFOCATING_BLOCKS.contains(blockAbove.getBlock().defaultBlockState()))
                && (blockBelow.is(Blocks.AIR) || blockBelow.is(Blocks.CAVE_AIR));
    }

    private static boolean isSafeCoordinate(World world, Coordinate coordinate) {
        BlockState block = world.getBlockState(coordinate.toBlockPos());
        BlockState blockAbove = world.getBlockState(coordinate.toBlockPos().above());
        BlockState blockBelow = world.getBlockState(coordinate.toBlockPos().below());
        return isSafeBlock(block, blockAbove, blockBelow);
    }

    private static boolean isSafeBlock(BlockState block, BlockState blockAbove, BlockState blockBelow) {
        return (!block.getMaterial().blocksMotion() && !UNSAFE_BLOCKS.contains(block) && !UNSAFE_BLOCKS.contains(block.getBlock().defaultBlockState()))
                && (!blockAbove.getMaterial().blocksMotion() && !UNSAFE_BLOCKS.contains(blockAbove) && !UNSAFE_BLOCKS.contains(blockAbove.getBlock().defaultBlockState()) && !SUFFOCATING_BLOCKS.contains(blockAbove) && !SUFFOCATING_BLOCKS.contains(blockAbove.getBlock().defaultBlockState()))
                && (blockBelow.getMaterial().isSolid() && !UNSAFE_BLOCKS.contains(blockBelow) && !UNSAFE_BLOCKS.contains(blockBelow.getBlock().defaultBlockState()));
    }

    // endregion 安全坐标

    /**
     * 获取指定维度的世界实例
     */
    public static ServerWorld getWorld(RegistryKey<World> dimension) {
        return NarcissusFarewell.getServerInstance().getLevel(dimension);
    }

    public static Biome getBiome(String id) {
        return getBiome(new ResourceLocation(id));
    }

    public static Biome getBiome(ResourceLocation id) {
        return NarcissusFarewell.getServerInstance().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
    }

    /**
     * 获取指定范围内某个生物群系位置
     *
     * @param world       世界
     * @param start       开始位置
     * @param biome       目标生物群系
     * @param radius      搜索半径
     * @param minDistance 最小距离
     */
    public static Coordinate findNearestBiome(ServerWorld world, Coordinate start, Biome biome, int radius, int minDistance) {
        // for (int x : IntStream.range((int) (start.getX() - radius), (int) (start.getX() + radius)).boxed()
        //         .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) start.getX())))
        //         .collect(Collectors.toList())) {
        //     for (int z : IntStream.range((int) (start.getZ() - radius), (int) (start.getZ() + radius)).boxed()
        //             .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) start.getZ())))
        //             .collect(Collectors.toList())) {
        //         Coordinate clone = start.clone();
        //         BlockPos pos = clone.setX(x).setZ(z).toBlockPos();
        //         Biome b = world.getBiome(pos);
        //         if (b == biome) {
        //             return clone;
        //         }
        //     }
        // }
        // // 未找到目标生物群系
        // return null;
        BlockPos pos = world.findNearestBiome(biome, start.toBlockPos(), radius, minDistance);
        if (pos != null) {
            return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
        }
        return null;
    }

    public static Structure<?> getStructure(String id) {
        return getStructure(new ResourceLocation(id));
    }

    public static Structure<?> getStructure(ResourceLocation id) {
        return ForgeRegistries.STRUCTURE_FEATURES.getValue(id);
    }

    /**
     * 获取指定范围内某个生物群系位置
     *
     * @param world  世界
     * @param start  开始位置
     * @param struct 目标结构
     * @param radius 搜索半径
     */
    public static Coordinate findNearestStruct(ServerWorld world, Coordinate start, Structure<?> struct, int radius) {
        BlockPos pos = world.findNearestMapFeature(struct, start.toBlockPos(), radius, true);
        if (pos != null) {
            return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
        }
        return null;
    }

    public static KeyValue<String, String> getPlayerHomeKey(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        Map<String, String> defaultHome = data.getDefaultHome();
        if (defaultHome.isEmpty() && dimension == null && StringUtils.isNullOrEmpty(name) && data.getHomeCoordinate().size() != 1) {
            return null;
        }
        KeyValue<String, String> keyValue = null;
        if (dimension == null && StringUtils.isNotNullOrEmpty(name)) {
            if (defaultHome.isEmpty() || !defaultHome.containsValue(name)) {
                keyValue = data.getHomeCoordinate().keySet().stream()
                        .filter(key -> key.getValue().equals(name))
                        .filter(key -> key.getKey().equals(player.level.dimension().location().toString()))
                        .findFirst().orElse(null);
            } else if (defaultHome.containsValue(name)) {
                List<Map.Entry<String, String>> entryList = defaultHome.entrySet().stream().filter(entry -> entry.getValue().equals(name)).collect(Collectors.toList());
                if (entryList.size() == 1) {
                    keyValue = new KeyValue<>(entryList.get(0).getKey(), entryList.get(0).getValue());
                }
            }
        } else if (dimension != null && StringUtils.isNullOrEmpty(name)) {
            if (defaultHome.containsKey(dimension.location().toString())) {
                keyValue = new KeyValue<>(dimension.location().toString(), defaultHome.get(dimension.location().toString()));
            }
        } else if (dimension != null && StringUtils.isNotNullOrEmpty(name)) {
            keyValue = data.getHomeCoordinate().keySet().stream()
                    .filter(key -> key.getValue().equals(name))
                    .filter(key -> key.getKey().equals(dimension.location().toString()))
                    .findFirst().orElse(null);
        } else if (!defaultHome.isEmpty() && dimension == null && StringUtils.isNullOrEmpty(name)) {
            if (defaultHome.size() == 1) {
                keyValue = new KeyValue<>(defaultHome.keySet().iterator().next(), defaultHome.values().iterator().next());
            } else {
                String value = defaultHome.getOrDefault(player.level.dimension().location().toString(), null);
                if (value != null) {
                    keyValue = new KeyValue<>(player.level.dimension().location().toString(), value);
                }
            }
        } else if (defaultHome.isEmpty() && dimension == null && StringUtils.isNullOrEmpty(name) && data.getHomeCoordinate().size() == 1) {
            keyValue = data.getHomeCoordinate().keySet().iterator().next();
        }
        return keyValue;
    }

    /**
     * 获取指定玩家的家坐标
     *
     * @param player    玩家
     * @param dimension 维度
     * @param name      名称
     */
    public static Coordinate getPlayerHome(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
        return PlayerTeleportDataCapability.getData(player).getHomeCoordinate().getOrDefault(getPlayerHomeKey(player, dimension, name), null);
    }

    /**
     * 获取距离玩家最近的驿站
     *
     * @param player 玩家
     * @return 驿站key
     */
    public static KeyValue<String, String> findNearestStageKey(ServerPlayerEntity player) {
        WorldStageData stageData = WorldStageData.get();
        Map.Entry<KeyValue<String, String>, Coordinate> stageEntry = stageData.getStageCoordinate().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(player.level.dimension().location().toString()))
                .min(Comparator.comparingInt(entry -> {
                    Coordinate value = entry.getValue();
                    double dx = value.getX() - player.getX();
                    double dy = value.getY() - player.getY();
                    double dz = value.getZ() - player.getZ();
                    // 返回欧几里得距离的平方（避免开方操作，提高性能）
                    return (int) (dx * dx + dy * dy + dz * dz);
                })).orElse(null);
        return stageEntry != null ? stageEntry.getKey() : null;
    }

    /**
     * 获取并移除玩家离开的坐标
     *
     * @param player    玩家
     * @param type      传送类型
     * @param dimension 维度
     * @return 查询到的离开坐标（如果未找到则返回 null）
     */
    public static TeleportRecord getBackTeleportRecord(ServerPlayerEntity player, @Nullable ETeleportType type, @Nullable RegistryKey<World> dimension) {
        TeleportRecord result = null;
        // 获取玩家的传送数据
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        List<TeleportRecord> records = data.getTeleportRecords();
        Optional<TeleportRecord> optionalRecord = records.stream()
                .filter(record -> type == null || record.getTeleportType() == type)
                .filter(record -> type == ETeleportType.TP_BACK || record.getTeleportType() != ETeleportType.TP_BACK)
                .filter(record -> dimension == null || record.getBefore().getDimension().equals(dimension))
                .max(Comparator.comparing(TeleportRecord::getTeleportTime));
        if (optionalRecord.isPresent()) {
            result = optionalRecord.get();
        }
        return result;
    }

    public static void removeBackTeleportRecord(ServerPlayerEntity player, TeleportRecord record) {
        PlayerTeleportDataCapability.getData(player).getTeleportRecords().remove(record);
    }

    /**
     * 检查传送范围
     */
    public static int checkRange(ServerPlayerEntity player, ETeleportType type, int range) {
        int maxRange;
        switch (type) {
            case TP_VIEW:
                maxRange = ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT.get();
                break;
            default:
                maxRange = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
                break;
        }
        if (range > maxRange) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "range_too_large"), maxRange);
        } else if (range <= 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "range_too_small"), 1);
        }
        return Math.min(Math.max(range, 1), maxRange);
    }

    /**
     * 执行传送请求
     */
    public static void teleportTo(@NonNull TeleportRequest request) {
        teleportTo(request.getRequester(), request.getTarget(), request.getTeleportType(), request.isSafe());
    }

    /**
     * 传送玩家到指定玩家
     *
     * @param from 传送者
     * @param to   目标玩家
     */
    public static void teleportTo(@NonNull ServerPlayerEntity from, @NonNull ServerPlayerEntity to, ETeleportType type, boolean safe) {
        if (ETeleportType.TP_HERE == type) {
            teleportTo(to, new Coordinate(from).setSafe(safe), type);
        } else {
            teleportTo(from, new Coordinate(to).setSafe(safe), type);
        }
    }

    /**
     * 传送玩家到指定坐标
     *
     * @param player 玩家
     * @param after  坐标
     */
    public static void teleportTo(@NonNull ServerPlayerEntity player, @NonNull Coordinate after, ETeleportType type) {
        Coordinate before = new Coordinate(player);
        World world = player.level;
        MinecraftServer server = player.getServer();
        if (world != null && server != null) {
            ServerWorld level = server.getLevel(after.getDimension());
            if (level != null) {
                if (after.isSafe()) {
                    after = findSafeCoordinate(after, false);
                    // 判断是否需要在脚下放置方块
                    if (ServerConfig.SETBLOCK_WHEN_SAFE_NOT_FOUND.get() && !isSafeCoordinate(level, after)) {
                        BlockState blockState = null;
                        List<ItemStack> playerItemList = getPlayerItemList(player);
                        if (CollectionUtils.isNotNullOrEmpty(SAFE_BLOCKS)) {
                            if (ServerConfig.GETBLOCK_FROM_INVENTORY.get()) {
                                blockState = SAFE_BLOCKS.stream()
                                        .filter(block -> playerItemList.stream().map(ItemStack::getItem).anyMatch(item -> new ItemStack(block.getBlock()).getItem().equals(item)))
                                        .findFirst().orElse(null);
                            } else {
                                blockState = SAFE_BLOCKS.get(0);
                            }
                        }
                        if (blockState != null) {
                            Coordinate airCoordinate = findSafeCoordinate(after, true);
                            if (!airCoordinate.toXyzString().equals(after.toXyzString())) {
                                after = airCoordinate;
                                Item blockItem = new ItemStack(blockState.getBlock()).getItem();
                                Item remove = playerItemList.stream().map(ItemStack::getItem).filter(blockItem::equals).findFirst().orElse(null);
                                if (remove != null) {
                                    ItemStack itemStack = new ItemStack(remove);
                                    itemStack.setCount(1);
                                    if (removeItemFromPlayerInventory(player, itemStack)) {
                                        level.setBlockAndUpdate(airCoordinate.toBlockPos().below(), blockState.getBlock().defaultBlockState());
                                    }
                                }
                            }
                        }
                    }
                }
                after.setY(Math.floor(after.getY()) + 0.1);
                player.teleportTo(level, after.getX(), after.getY(), after.getZ()
                        , after.getYaw() == 0 ? player.yRot : (float) after.getYaw()
                        , after.getPitch() == 0 ? player.xRot : (float) after.getPitch());
                TeleportRecord record = new TeleportRecord();
                record.setTeleportTime(new Date());
                record.setTeleportType(type);
                record.setBefore(before);
                record.setAfter(after);
                PlayerTeleportDataCapability.getData(player).addTeleportRecords(record);
            }
        }
    }

    /**
     * 获取随机玩家
     */
    public static ServerPlayerEntity getRandomPlayer() {
        try {
            List<ServerPlayerEntity> players = NarcissusFarewell.getServerInstance().getPlayerList().getPlayers();
            return players.get(new Random().nextInt(players.size()));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取随机玩家UUID
     */
    public static UUID getRandomPlayerUUID() {
        PlayerEntity randomPlayer = getRandomPlayer();
        return randomPlayer != null ? randomPlayer.getUUID() : null;
    }

    /**
     * 通过UUID获取对应的玩家
     *
     * @param uuid 玩家UUID
     */
    public static ServerPlayerEntity getPlayer(UUID uuid) {
        try {
            return Minecraft.getInstance().level.getServer().getPlayerList().getPlayer(uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 移除玩家背包中的指定物品
     *
     * @param player       玩家
     * @param itemToRemove 要移除的物品
     * @return 是否全部移除成功
     */
    public static boolean removeItemFromPlayerInventory(ServerPlayerEntity player, ItemStack itemToRemove) {
        IInventory inventory = player.inventory;

        // 剩余要移除的数量
        int remainingAmount = itemToRemove.getCount();
        // 记录成功移除的物品数量，以便失败时进行回滚
        int successfullyRemoved = 0;

        // 遍历玩家背包的所有插槽
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            // 获取背包中的物品
            ItemStack stack = inventory.getItem(i);
            ItemStack copy = itemToRemove.copy();
            copy.setCount(stack.getCount());

            // 如果插槽中的物品是目标物品
            if (stack.equals(copy, false)) {
                // 获取当前物品堆叠的数量
                int stackSize = stack.getCount();

                // 如果堆叠数量大于或等于剩余需要移除的数量
                if (stackSize >= remainingAmount) {
                    // 移除指定数量的物品
                    stack.shrink(remainingAmount);
                    // 记录成功移除的数量
                    successfullyRemoved += remainingAmount;
                    // 移除完毕
                    remainingAmount = 0;
                    break;
                } else {
                    // 移除该堆所有物品
                    stack.setCount(0);
                    // 记录成功移除的数量
                    successfullyRemoved += stackSize;
                    // 减少剩余需要移除的数量
                    remainingAmount -= stackSize;
                }
            }
        }

        // 如果没有成功移除所有物品，撤销已移除的部分
        if (remainingAmount > 0) {
            // 创建副本并还回成功移除的物品
            ItemStack copy = itemToRemove.copy();
            copy.setCount(successfullyRemoved);
            // 将已移除的物品添加回背包
            player.inventory.add(copy);
        }

        // 是否成功移除所有物品
        return remainingAmount == 0;
    }

    public static List<ItemStack> getPlayerItemList(ServerPlayerEntity player) {
        List<ItemStack> result = new ArrayList<>();
        if (player != null) {
            result.addAll(player.inventory.items);
            result.addAll(player.inventory.armor);
            result.addAll(player.inventory.offhand);
            result = result.stream().filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() != Items.AIR).collect(Collectors.toList());
        }
        return result;
    }

    /**
     * 广播消息
     *
     * @param player  发送者
     * @param message 消息
     */
    public static void broadcastMessage(ServerPlayerEntity player, Component message) {
        player.server.getPlayerList().broadcastMessage(new TranslationTextComponent("chat.type.announcement", player.getDisplayName(), message.toTextComponent(player.getLanguage())), ChatType.SYSTEM, Util.NIL_UUID);
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ServerPlayerEntity player, Component message) {
        player.sendMessage(message.toTextComponent(player.getLanguage()), player.getUUID());
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(Component.literal(message).toTextComponent(), player.getUUID());
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(Component.translatable(key, args).setLanguageCode(player.getLanguage()).toTextComponent(), player.getUUID());
    }

    /**
     * 判断传送类型是否开启
     *
     * @param type 传送类型
     */
    public static boolean isTeleportEnabled(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.SWITCH_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.SWITCH_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.SWITCH_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.SWITCH_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.SWITCH_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.SWITCH_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.SWITCH_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.SWITCH_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.SWITCH_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.SWITCH_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.SWITCH_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.SWITCH_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.SWITCH_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.SWITCH_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.SWITCH_TP_BACK.get();
            default:
                return false;
        }
    }

    public static String getCommand(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.COMMAND_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.COMMAND_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.COMMAND_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.COMMAND_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.COMMAND_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.COMMAND_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.COMMAND_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.COMMAND_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.COMMAND_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.COMMAND_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.COMMAND_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.COMMAND_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.COMMAND_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.COMMAND_TP_BACK.get();
            default:
                return "";
        }
    }

    public static String getCommand(ECommandType type) {
        String prefix = NarcissusUtils.getCommandPrefix();
        switch (type) {
            case HELP:
                return prefix + " help";
            case DIMENSION:
                return prefix + " " + ServerConfig.COMMAND_DIMENSION.get();
            case TP_COORDINATE:
                return prefix + " " + ServerConfig.COMMAND_TP_COORDINATE.get();
            case TP_COORDINATE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_COORDINATE.get() : "";
            case TP_STRUCTURE:
                return prefix + " " + ServerConfig.COMMAND_TP_STRUCTURE.get();
            case TP_STRUCTURE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_STRUCTURE.get() : "";
            case TP_ASK:
                return prefix + " " + ServerConfig.COMMAND_TP_ASK.get();
            case TP_ASK_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK.get() : "";
            case TP_ASK_YES:
                return prefix + " " + ServerConfig.COMMAND_TP_ASK_YES.get();
            case TP_ASK_YES_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_YES.get() : "";
            case TP_ASK_NO:
                return prefix + " " + ServerConfig.COMMAND_TP_ASK_NO.get();
            case TP_ASK_NO_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_NO.get() : "";
            case TP_HERE:
                return prefix + " " + ServerConfig.COMMAND_TP_HERE.get();
            case TP_HERE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE.get() : "";
            case TP_HERE_YES:
                return prefix + " " + ServerConfig.COMMAND_TP_HERE_YES.get();
            case TP_HERE_YES_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_YES.get() : "";
            case TP_HERE_NO:
                return prefix + " " + ServerConfig.COMMAND_TP_HERE_NO.get();
            case TP_HERE_NO_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_NO.get() : "";
            case TP_RANDOM:
                return prefix + " " + ServerConfig.COMMAND_TP_RANDOM.get();
            case TP_RANDOM_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_RANDOM.get() : "";
            case TP_SPAWN:
                return prefix + " " + ServerConfig.COMMAND_TP_SPAWN.get();
            case TP_SPAWN_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_SPAWN.get() : "";
            case TP_WORLD_SPAWN:
                return prefix + " " + ServerConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_WORLD_SPAWN_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_WORLD_SPAWN.get() : "";
            case TP_TOP:
                return prefix + " " + ServerConfig.COMMAND_TP_TOP.get();
            case TP_TOP_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_TOP.get() : "";
            case TP_BOTTOM:
                return prefix + " " + ServerConfig.COMMAND_TP_BOTTOM.get();
            case TP_BOTTOM_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_BOTTOM.get() : "";
            case TP_UP:
                return prefix + " " + ServerConfig.COMMAND_TP_UP.get();
            case TP_UP_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_UP.get() : "";
            case TP_DOWN:
                return prefix + " " + ServerConfig.COMMAND_TP_DOWN.get();
            case TP_DOWN_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_DOWN.get() : "";
            case TP_VIEW:
                return prefix + " " + ServerConfig.COMMAND_TP_VIEW.get();
            case TP_VIEW_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_VIEW.get() : "";
            case TP_HOME:
                return prefix + " " + ServerConfig.COMMAND_TP_HOME.get();
            case TP_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HOME.get() : "";
            case SET_HOME:
                return prefix + " " + ServerConfig.COMMAND_SET_HOME.get();
            case SET_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_SET_HOME.get() : "";
            case DEL_HOME:
                return prefix + " " + ServerConfig.COMMAND_DEL_HOME.get();
            case DEL_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_DEL_HOME.get() : "";
            case TP_STAGE:
                return prefix + " " + ServerConfig.COMMAND_TP_STAGE.get();
            case TP_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_STAGE.get() : "";
            case SET_STAGE:
                return prefix + " " + ServerConfig.COMMAND_SET_STAGE.get();
            case SET_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_SET_STAGE.get() : "";
            case DEL_STAGE:
                return prefix + " " + ServerConfig.COMMAND_DEL_STAGE.get();
            case DEL_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_DEL_STAGE.get() : "";
            case TP_BACK:
                return prefix + " " + ServerConfig.COMMAND_TP_BACK.get();
            case TP_BACK_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_BACK.get() : "";
            default:
                return "";
        }
    }

    public static boolean isConciseEnabled(ECommandType type) {
        switch (type) {
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ServerConfig.CONCISE_TP_COORDINATE.get();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ServerConfig.CONCISE_TP_STRUCTURE.get();
            case TP_ASK:
            case TP_ASK_CONCISE:
                return ServerConfig.CONCISE_TP_ASK.get();
            case TP_ASK_YES:
            case TP_ASK_YES_CONCISE:
                return ServerConfig.CONCISE_TP_ASK_YES.get();
            case TP_ASK_NO:
            case TP_ASK_NO_CONCISE:
                return ServerConfig.CONCISE_TP_ASK_NO.get();
            case TP_HERE:
            case TP_HERE_CONCISE:
                return ServerConfig.CONCISE_TP_HERE.get();
            case TP_HERE_YES:
            case TP_HERE_YES_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_YES.get();
            case TP_HERE_NO:
            case TP_HERE_NO_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_NO.get();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ServerConfig.CONCISE_TP_RANDOM.get();
            case TP_SPAWN:
            case TP_SPAWN_CONCISE:
                return ServerConfig.CONCISE_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ServerConfig.CONCISE_TP_WORLD_SPAWN.get();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ServerConfig.CONCISE_TP_TOP.get();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ServerConfig.CONCISE_TP_BOTTOM.get();
            case TP_UP:
            case TP_UP_CONCISE:
                return ServerConfig.CONCISE_TP_UP.get();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ServerConfig.CONCISE_TP_DOWN.get();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ServerConfig.CONCISE_TP_VIEW.get();
            case TP_HOME:
            case TP_HOME_CONCISE:
                return ServerConfig.CONCISE_TP_HOME.get();
            case SET_HOME:
            case SET_HOME_CONCISE:
                return ServerConfig.CONCISE_SET_HOME.get();
            case DEL_HOME:
            case DEL_HOME_CONCISE:
                return ServerConfig.CONCISE_DEL_HOME.get();
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return ServerConfig.CONCISE_TP_STAGE.get();
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return ServerConfig.CONCISE_SET_STAGE.get();
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return ServerConfig.CONCISE_DEL_STAGE.get();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ServerConfig.CONCISE_TP_BACK.get();
            default:
                return false;
        }
    }

    public static CClientSettingsPacket getCClientSettingsPacket(ServerPlayerEntity player) {
        CClientSettingsPacket result = new CClientSettingsPacket();
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeUtf(player.getLanguage());
            buffer.writeByte(0);
            buffer.writeEnum(player.getChatVisibility());
            buffer.writeBoolean(false);
            buffer.writeByte(0);
            buffer.writeEnum(player.getMainArm());
            result.read(buffer);
        } catch (IOException ignored) {
        }
        return result;
    }

    /**
     * 获取当前mod支持的mc版本
     *
     * @return 主版本*1000000+次版本*1000+修订版本， 如 1.16.5 -> 1 * 1000000 + 16 * 1000 + 5 = 10016005
     */
    public static int getMcVersion() {
        int version = 0;
        ModContainer container = ModList.get().getModContainerById(NarcissusFarewell.MODID).orElse(null);
        if (container != null) {
            IModInfo.ModVersion minecraftVersion = container.getModInfo().getDependencies().stream()
                    .filter(dependency -> dependency.getModId().equalsIgnoreCase("minecraft"))
                    .findFirst()
                    .orElse(null);
            if (minecraftVersion != null) {
                ArtifactVersion lowerBound = minecraftVersion.getVersionRange().getRestrictions().get(0).getLowerBound();
                int majorVersion = lowerBound.getMajorVersion();
                int minorVersion = lowerBound.getMinorVersion();
                int incrementalVersion = lowerBound.getIncrementalVersion();
                version = majorVersion * 1000000 + minorVersion * 1000 + incrementalVersion;
            }
        }
        return version;
    }

    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(ServerPlayerEntity player, RegistryKey<World> to, ETeleportType type) {
        boolean result = true;
        if (player.level.dimension() != to) {
            if (ServerConfig.TELEPORT_ACROSS_DIMENSION.get()) {
                if (!NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, type)) {
                    result = false;
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "across_dimension_not_enable_for"), getCommand(type));
                }
            } else {
                result = false;
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "across_dimension_not_enable"));
            }
        }
        return result;
    }

    /**
     * 判断传送类型跨维度传送是否开启
     */
    public static boolean isTeleportTypeAcrossDimensionEnabled(ServerPlayerEntity player, ETeleportType type) {
        int permission;
        switch (type) {
            case TP_COORDINATE:
                permission = ServerConfig.PERMISSION_TP_COORDINATE_ACROSS_DIMENSION.get();
                break;
            case TP_STRUCTURE:
                permission = ServerConfig.PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION.get();
                break;
            case TP_ASK:
                permission = ServerConfig.PERMISSION_TP_ASK_ACROSS_DIMENSION.get();
                break;
            case TP_HERE:
                permission = ServerConfig.PERMISSION_TP_HERE_ACROSS_DIMENSION.get();
                break;
            case TP_RANDOM:
                permission = ServerConfig.PERMISSION_TP_RANDOM_ACROSS_DIMENSION.get();
                break;
            case TP_SPAWN:
                permission = ServerConfig.PERMISSION_TP_SPAWN_ACROSS_DIMENSION.get();
                break;
            case TP_WORLD_SPAWN:
                permission = ServerConfig.PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION.get();
                break;
            case TP_HOME:
                permission = ServerConfig.PERMISSION_TP_HOME_ACROSS_DIMENSION.get();
                break;
            case TP_STAGE:
                permission = ServerConfig.PERMISSION_TP_STAGE_ACROSS_DIMENSION.get();
                break;
            case TP_BACK:
                permission = ServerConfig.PERMISSION_TP_BACK_ACROSS_DIMENSION.get();
                break;
            default:
                permission = 0;
                break;
        }
        return permission > -1 && player.hasPermissions(permission);
    }

    // endregion 跨维度传送

    // region 传送冷却

    /**
     * 获取传送/传送请求冷却时间
     *
     * @param player 玩家
     * @param type   传送类型
     */
    public static int getTeleportCoolDown(ServerPlayerEntity player, ETeleportType type) {
        // 如果传送卡类型为抵消冷却时间，则不计算冷却时间
        if (ServerConfig.TELEPORT_CARD_TYPE.get() == ECardType.REFUND_COOLDOWN || ServerConfig.TELEPORT_CARD_TYPE.get() == ECardType.REFUND_ALL_COST_AND_COOLDOWN) {
            if (PlayerTeleportDataCapability.getData(player).getTeleportCard() > 0) {
                return 0;
            }
        }
        Instant current = Instant.now();
        int commandCoolDown = getCommandCoolDown(type);
        Instant lastTpTime = PlayerTeleportDataCapability.getData(player).getTeleportRecords(type).stream()
                .map(TeleportRecord::getTeleportTime)
                .max(Comparator.comparing(Date::toInstant))
                .orElse(new Date(0)).toInstant();
        switch (ServerConfig.TELEPORT_REQUEST_COOLDOWN_TYPE.get()) {
            case COMMON:
                return calculateCooldown(player.getUUID(), current, lastTpTime, ServerConfig.TELEPORT_REQUEST_COOLDOWN.get(), null);
            case INDIVIDUAL:
                return calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type);
            case MIXED:
                int globalCommandCoolDown = ServerConfig.TELEPORT_REQUEST_COOLDOWN.get();
                int individualCooldown = calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type);
                int globalCooldown = calculateCooldown(player.getUUID(), current, lastTpTime, globalCommandCoolDown, null);
                return Math.max(individualCooldown, globalCooldown);
            default:
                return 0;
        }
    }

    /**
     * 获取传送命令冷却时间
     *
     * @param type 传送类型
     */
    public static int getCommandCoolDown(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.COOLDOWN_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.COOLDOWN_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.COOLDOWN_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.COOLDOWN_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.COOLDOWN_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.COOLDOWN_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.COOLDOWN_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.COOLDOWN_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.COOLDOWN_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.COOLDOWN_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.COOLDOWN_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.COOLDOWN_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.COOLDOWN_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.COOLDOWN_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.COOLDOWN_TP_BACK.get();
            default:
                return 0;
        }
    }

    private static int calculateCooldown(UUID uuid, Instant current, Instant lastTpTime, int cooldown, ETeleportType type) {
        Optional<TeleportRequest> latestRequest = NarcissusFarewell.getTeleportRequest().values().stream()
                .filter(request -> request.getRequester().getUUID().equals(uuid))
                .filter(request -> type == null || request.getTeleportType() == type)
                .max(Comparator.comparing(TeleportRequest::getRequestTime));

        Instant lastRequestTime = latestRequest.map(r -> r.getRequestTime().toInstant()).orElse(current.minusSeconds(cooldown));
        return Math.max(0, Math.max(cooldown - (int) Duration.between(lastRequestTime, current).getSeconds(), cooldown - (int) Duration.between(lastTpTime, current).getSeconds()));
    }

    // endregion 传送冷却

    // region 传送代价

    /**
     * 验证传送代价
     *
     * @param player 请求传送的玩家
     * @param target 目标坐标
     * @param type   传送类型
     * @param submit 是否收取代价
     * @return 是否验证通过
     */
    public static boolean validTeleportCost(ServerPlayerEntity player, Coordinate target, ETeleportType type, boolean submit) {
        return validateCost(player, target.getDimension(), calculateDistance(new Coordinate(player), target), type, submit);
    }

    /**
     * 验证并收取传送代价
     *
     * @param request 传送请求
     * @param submit  是否收取代价
     * @return 是否验证通过
     */
    public static boolean validTeleportCost(TeleportRequest request, boolean submit) {
        Coordinate requesterCoordinate = new Coordinate(request.getRequester());
        Coordinate targetCoordinate = new Coordinate(request.getTarget());
        return validateCost(request.getRequester(), request.getTarget().getLevel().dimension(), calculateDistance(requesterCoordinate, targetCoordinate), request.getTeleportType(), submit);
    }

    /**
     * 通用的传送代价验证逻辑
     *
     * @param player       请求传送的玩家
     * @param targetDim    目标维度
     * @param distance     计算的距离
     * @param teleportType 传送类型
     * @param submit       是否收取代价
     * @return 是否验证通过
     */
    private static boolean validateCost(ServerPlayerEntity player, RegistryKey<World> targetDim, double distance, ETeleportType teleportType, boolean submit) {
        TeleportCost cost = NarcissusUtils.getCommandCost(teleportType);
        if (cost.getType() == ECostType.NONE) return true;

        double adjustedDistance;
        if (player.getLevel().dimension() == targetDim) {
            adjustedDistance = Math.min(ServerConfig.TELEPORT_COST_DISTANCE_LIMIT.get(), distance);
        } else {
            adjustedDistance = ServerConfig.TELEPORT_COST_DISTANCE_ACROSS_DIMENSION.get();
        }

        double need = cost.getNum() * adjustedDistance * cost.getRate();
        int costNeed = getTeleportCostNeedPost(player, need);
        int cardNeed = getTeleportCardNeedPost(player, need);
        int cardNeedTotal = getTeleportCardNeedPre(player, need);
        boolean result = false;

        switch (cost.getType()) {
            case EXP_POINT:
                result = player.totalExperience >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "exp_point"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.giveExperiencePoints(-costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case EXP_LEVEL:
                result = player.experienceLevel >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "exp_level"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.giveExperienceLevels(-costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case HEALTH:
                result = player.getHealth() > costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "health"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.hurt(DamageSource.MAGIC, costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case HUNGER:
                result = player.getFoodData().getFoodLevel() >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "hunger"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case ITEM:
                try {
                    ItemStack itemStack = ItemStack.of(JsonToNBT.parseTag(cost.getConf()));
                    result = getItemCount(player.inventory.items, itemStack) >= costNeed && cardNeed == 0;
                    if (!result && cardNeed == 0) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), itemStack.getDisplayName(), (int) Math.ceil(need));
                    } else if (result && submit) {
                        itemStack.setCount(costNeed);
                        result = removeItemFromPlayerInventory(player, itemStack);
                        // 代价不足
                        if (result) {
                            PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                        } else {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), itemStack.getDisplayName(), (int) Math.ceil(need));
                        }
                    }
                } catch (Exception ignored) {
                }
                break;
            case COMMAND:
                try {
                    result = cardNeed == 0;
                    if (result && submit) {
                        String command = cost.getConf().replaceAll("\\[num]", String.valueOf(costNeed));
                        int commandResult = player.getServer().getCommands().performCommand(player.createCommandSourceStack(), command);
                        if (commandResult > 0) {
                            PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                        }
                        result = commandResult > 0;
                    }
                } catch (Exception ignored) {
                }
                break;
        }
        if (!result && cardNeed > 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "teleport_card"), (int) Math.ceil(need));
        }
        return result;
    }

    /**
     * 使用传送卡后还须支付多少代价
     */
    public static int getTeleportCostNeedPost(ServerPlayerEntity player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return ceil;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case NONE:
                return data.getTeleportCard() > 0 ? ceil : -1;
            case LIKE_COST:
                return data.getTeleportCard() >= ceil ? ceil : -1;
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
                return Math.max(0, ceil - data.getTeleportCard());
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
                return data.getTeleportCard() > 0 ? 0 : ceil;
            case REFUND_COOLDOWN:
            default:
                return ceil;
        }
    }

    /**
     * 须支付多少传送卡
     */
    public static int getTeleportCardNeedPre(ServerPlayerEntity player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return 0;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case LIKE_COST:
                return ceil;
            case NONE:
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
            case REFUND_COOLDOWN:
            default:
                return 1;
        }
    }

    /**
     * 使用传送卡后还须支付多少传送卡
     */
    public static int getTeleportCardNeedPost(ServerPlayerEntity player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return 0;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case NONE:
                return data.getTeleportCard() > 0 ? 0 : 1;
            case LIKE_COST:
                return Math.max(0, ceil - data.getTeleportCard());
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
            case REFUND_COOLDOWN:
            default:
                return 0;
        }
    }

    public static TeleportCost getCommandCost(ETeleportType type) {
        TeleportCost cost = new TeleportCost();
        switch (type) {
            case TP_COORDINATE:
                cost.setType(ServerConfig.COST_TP_COORDINATE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_COORDINATE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_COORDINATE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_COORDINATE_CONF.get());
                break;
            case TP_STRUCTURE:
                cost.setType(ServerConfig.COST_TP_STRUCTURE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_STRUCTURE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_STRUCTURE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_STRUCTURE_CONF.get());
                break;
            case TP_ASK:
                cost.setType(ServerConfig.COST_TP_ASK_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_ASK_NUM.get());
                cost.setRate(ServerConfig.COST_TP_ASK_RATE.get());
                cost.setConf(ServerConfig.COST_TP_ASK_CONF.get());
                break;
            case TP_HERE:
                cost.setType(ServerConfig.COST_TP_HERE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_HERE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_HERE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_HERE_CONF.get());
                break;
            case TP_RANDOM:
                cost.setType(ServerConfig.COST_TP_RANDOM_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_RANDOM_NUM.get());
                cost.setRate(ServerConfig.COST_TP_RANDOM_RATE.get());
                cost.setConf(ServerConfig.COST_TP_RANDOM_CONF.get());
                break;
            case TP_SPAWN:
                cost.setType(ServerConfig.COST_TP_SPAWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_SPAWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_SPAWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_SPAWN_CONF.get());
                break;
            case TP_WORLD_SPAWN:
                cost.setType(ServerConfig.COST_TP_WORLD_SPAWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_WORLD_SPAWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_WORLD_SPAWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_WORLD_SPAWN_CONF.get());
                break;
            case TP_TOP:
                cost.setType(ServerConfig.COST_TP_TOP_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_TOP_NUM.get());
                cost.setRate(ServerConfig.COST_TP_TOP_RATE.get());
                cost.setConf(ServerConfig.COST_TP_TOP_CONF.get());
                break;
            case TP_BOTTOM:
                cost.setType(ServerConfig.COST_TP_BOTTOM_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_BOTTOM_NUM.get());
                cost.setRate(ServerConfig.COST_TP_BOTTOM_RATE.get());
                cost.setConf(ServerConfig.COST_TP_BOTTOM_CONF.get());
                break;
            case TP_UP:
                cost.setType(ServerConfig.COST_TP_UP_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_UP_NUM.get());
                cost.setRate(ServerConfig.COST_TP_UP_RATE.get());
                cost.setConf(ServerConfig.COST_TP_UP_CONF.get());
                break;
            case TP_DOWN:
                cost.setType(ServerConfig.COST_TP_DOWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_DOWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_DOWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_DOWN_CONF.get());
                break;
            case TP_VIEW:
                cost.setType(ServerConfig.COST_TP_VIEW_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_VIEW_NUM.get());
                cost.setRate(ServerConfig.COST_TP_VIEW_RATE.get());
                cost.setConf(ServerConfig.COST_TP_VIEW_CONF.get());
                break;
            case TP_HOME:
                cost.setType(ServerConfig.COST_TP_HOME_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_HOME_NUM.get());
                cost.setRate(ServerConfig.COST_TP_HOME_RATE.get());
                cost.setConf(ServerConfig.COST_TP_HOME_CONF.get());
                break;
            case TP_STAGE:
                cost.setType(ServerConfig.COST_TP_STAGE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_STAGE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_STAGE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_STAGE_CONF.get());
                break;
            case TP_BACK:
                cost.setType(ServerConfig.COST_TP_BACK_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_BACK_NUM.get());
                cost.setRate(ServerConfig.COST_TP_BACK_RATE.get());
                cost.setConf(ServerConfig.COST_TP_BACK_CONF.get());
                break;
            default:
                break;
        }
        return cost;
    }

    public static int getItemCount(List<ItemStack> items, ItemStack itemStack) {
        ItemStack copy = itemStack.copy();
        return items.stream().filter(item -> {
            copy.setCount(item.getCount());
            return item.equals(copy, false);
        }).mapToInt(ItemStack::getCount).sum();
    }

    public static double calculateDistance(Coordinate coordinate1, Coordinate coordinate2) {
        double deltaX = coordinate1.getX() - coordinate2.getX();
        double deltaY = coordinate1.getY() - coordinate2.getY();
        double deltaZ = coordinate1.getZ() - coordinate2.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    // endregion 传送代价
}
