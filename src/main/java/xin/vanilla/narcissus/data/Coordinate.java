package xin.vanilla.narcissus.data;

import com.mojang.math.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.StringUtils;

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
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private boolean safe = false;
    private EnumSafeMode safeMode = EnumSafeMode.NONE;

    public Coordinate(Player player) {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.yaw = player.getYRot();
        this.pitch = player.getXRot();
        this.dimension = player.level.dimension();
    }

    public Coordinate(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coordinate(double x, double y, double z, ResourceKey<Level> dimension) {
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

    public Coordinate(double x, double y, double z, double yaw, double pitch, ResourceKey<Level> dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
    }

    public int getXInt() {
        return (int) x;
    }

    public int getYInt() {
        return (int) y;
    }

    public int getZInt() {
        return (int) z;
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

    public static Coordinate random(ServerPlayer player) {
        return random(player, ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get());
    }

    public static Coordinate random(ServerPlayer player, int range) {
        return random(player, range, player.level.dimension());
    }

    public static Coordinate random(ServerPlayer player, int range, ResourceKey<Level> dimension) {
        ServerLevel world = NarcissusUtils.getWorld(dimension);
        range = Math.min(Math.max(range, 1), ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get());
        double x = player.getX() + (Math.random() * 2 - 1) * range;
        double y = getRandomWithWeight(NarcissusUtils.getWorldMinY(world), NarcissusUtils.getWorldMaxY(world), (int) player.getY(), 0.75);
        double z = player.getZ() + (Math.random() * 2 - 1) * range;
        return new Coordinate(x, y, z, player.getYRot(), player.getXRot(), dimension);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public Vector3d toVector3d() {
        return new Vector3d(x, y, z);
    }

    public Vec3 toVec3() {
        return new Vec3(x, y, z);
    }

    public Coordinate fromBlockPos(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        return this;
    }

    public Coordinate fromVector3d(Vector3d pos) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        return this;
    }

    public Coordinate fromVec3(Vec3 pos) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        return this;
    }

    public Coordinate addX(double offset) {
        this.x += offset;
        return this;
    }

    public Coordinate addY(double offset) {
        this.y += offset;
        return this;
    }

    public Coordinate addZ(double offset) {
        this.z += offset;
        return this;
    }

    /**
     * 序列化到 NBT
     */
    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putDouble("yaw", yaw);
        tag.putDouble("pitch", pitch);
        tag.putString("dimension", dimension.location().toString());
        return tag;
    }

    public boolean equalsOfRange(Coordinate coordinate, int range) {
        return Math.abs((int) coordinate.x - (int) x) <= range
                && Math.abs((int) coordinate.y - (int) y) <= range
                && Math.abs((int) coordinate.z - (int) z) <= range
                && coordinate.dimension.equals(dimension);
    }

    /**
     * 反序列化
     */
    public static Coordinate readFromNBT(CompoundTag tag) {
        Coordinate coordinate = new Coordinate();
        coordinate.x = tag.getDouble("x");
        coordinate.y = tag.getDouble("y");
        coordinate.z = tag.getDouble("z");
        coordinate.yaw = tag.getDouble("yaw");
        coordinate.pitch = tag.getDouble("pitch");
        coordinate.dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, NarcissusFarewell.parseResource(tag.getString("dimension")));
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
            cloned.yaw = this.yaw;
            cloned.pitch = this.pitch;
            cloned.safe = this.safe;
            cloned.safeMode = this.safeMode;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public Coordinate above() {
        return this.clone().addY(1);
    }

    public Coordinate below() {
        return this.clone().addY(-1);
    }

    public double distanceFrom(Coordinate coordinate) {
        return Math.sqrt(Math.pow(coordinate.x - x, 2) + Math.pow(coordinate.y - y, 2) + Math.pow(coordinate.z - z, 2));
    }

    public double distanceFrom(double x, double y, double z) {
        return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2) + Math.pow(z - this.z, 2));
    }

    public double distanceFrom2D(Coordinate coordinate) {
        return Math.sqrt(Math.pow(coordinate.x - x, 2) + Math.pow(coordinate.z - z, 2));
    }

    public String toXString() {
        return StringUtils.toFixedEx(x, 1);
    }

    public String toYString() {
        return StringUtils.toFixedEx(y, 1);
    }

    public String toZString() {
        return StringUtils.toFixedEx(z, 1);
    }

    public String toXyzIntString() {
        return getXInt() + ", " + getYInt() + ", " + getZInt();
    }

    public String toXyzString() {
        return StringUtils.toFixedEx(x, 1) + ", " + StringUtils.toFixedEx(y, 1) + ", " + StringUtils.toFixedEx(z, 1);
    }

    public String getDimensionResourceId() {
        return dimension.location().toString();
    }
}
