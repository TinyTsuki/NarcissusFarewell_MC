package xin.vanilla.narcissus.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import xin.vanilla.narcissus.enums.ESafeMode;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Coordinate implements Serializable, Cloneable {
    private double x = 0;
    private double y = 0;
    private double z = 0;
    private double yaw = 0;
    private double pitch = 0;
    private RegistryKey<World> dimension = World.OVERWORLD;
    private boolean safe = false;
    private ESafeMode safeMode = ESafeMode.NONE;

    public Coordinate(PlayerEntity player) {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.yaw = player.yRot;
        this.pitch = player.xRot;
        this.dimension = player.level.dimension();
    }

    public Coordinate(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coordinate(double x, double y, double z, RegistryKey<World> dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    public Coordinate(double x, double y, double z, double yaw, double pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Coordinate(double x, double y, double z, double yaw, double pitch, RegistryKey<World> dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
    }

    /**
     * 根据距离和权重生成随机数
     *
     * @param a 最小值
     * @param b 最大值
     * @param c 中心值
     * @param k 权重系数
     * @return 随机数
     */
    public static int getRandomWithWeight(int a, int b, int c, double k) {
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0;
        // 计算每个值的权重
        for (int i = a; i <= b; i++) {
            double weight = 1.0 / (1 + k * Math.abs(i - c));
            weights.add(weight);
            totalWeight += weight;
        }
        // 生成随机数并选中对应的值
        double rand = new Random().nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        for (int i = 0; i < weights.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (rand <= cumulativeWeight) {
                return a + i;
            }
        }
        // 默认返回最小值（理论上不会执行到这里）
        return a;
    }

    public static Coordinate random(ServerPlayerEntity player) {
        return random(player, ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get());
    }

    public static Coordinate random(ServerPlayerEntity player, int range) {
        return random(player, range, player.level.dimension());
    }

    public static Coordinate random(ServerPlayerEntity player, int range, RegistryKey<World> dimension) {
        range = Math.min(Math.max(range, 1), ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get());
        double x = player.getX() + (Math.random() * 2 - 1) * range;
        double y = getRandomWithWeight(0, NarcissusUtils.getWorld(dimension).getMaxBuildHeight(), (int) player.getY(), 0.75);
        double z = player.getZ() + (Math.random() * 2 - 1) * range;
        return new Coordinate(x, y, z, player.yRot, player.xRot, dimension);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public Coordinate fromBlockPos(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        return this;
    }

    /**
     * 序列化到 NBT
     */
    public CompoundNBT writeToNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putString("dimension", dimension.location().toString());
        return tag;
    }

    /**
     * 反序列化
     */
    public static Coordinate readFromNBT(CompoundNBT tag) {
        Coordinate coordinate = new Coordinate();
        coordinate.x = tag.getDouble("x");
        coordinate.y = tag.getDouble("y");
        coordinate.z = tag.getDouble("z");
        coordinate.dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(tag.getString("dimension")));
        return coordinate;
    }

    @Override
    public Coordinate clone() {
        try {
            Coordinate cloned = (Coordinate) super.clone();
            cloned.dimension = this.dimension;
            cloned.x = this.x;
            cloned.y = this.y;
            cloned.z = this.z;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
