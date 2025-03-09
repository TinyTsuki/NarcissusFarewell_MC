package xin.vanilla.narcissus.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import xin.vanilla.narcissus.enums.ESafeMode;
import xin.vanilla.narcissus.util.DimensionUtils;
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
    private String dimension = DimensionUtils.getOverworldDimensionStringId();
    private boolean safe = false;
    private ESafeMode safeMode = ESafeMode.NONE;

    public Coordinate(EntityPlayer player) {
        this.x = player.posX;
        this.y = player.posY;
        this.z = player.posZ;
        this.yaw = player.cameraYaw;
        this.pitch = player.cameraPitch;
        this.dimension = DimensionUtils.getStringId(player.getEntityWorld().provider.dimensionId);
    }

    public Coordinate(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coordinate(double x, double y, double z, String dimension) {
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

    public Coordinate(double x, double y, double z, double yaw, double pitch, String dimension) {
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

    public static Coordinate random(EntityPlayerMP player) {
        return random(player, ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT);
    }

    public static Coordinate random(EntityPlayerMP player, int range) {
        return random(player, range, DimensionUtils.getStringId(player.getEntityWorld().provider.dimensionId));
    }

    public static Coordinate random(EntityPlayerMP player, int range, String dimension) {
        WorldServer world = NarcissusUtils.getWorld(DimensionUtils.getDimensionType(dimension));
        range = Math.min(Math.max(range, 1), ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT);
        double x = player.posX + (Math.random() * 2 - 1) * range;
        double y = getRandomWithWeight(NarcissusUtils.getWorldMinY(world), NarcissusUtils.getWorldMaxY(world), (int) player.posY, 0.75);
        double z = player.posZ + (Math.random() * 2 - 1) * range;
        return new Coordinate(x, y, z, player.cameraYaw, player.cameraPitch, dimension);
    }

    public Vec3 toVector3d() {
        return Vec3.createVectorHelper(x, y, z);
    }

    public Coordinate fromVector3(Vec3 pos) {
        this.x = pos.xCoord;
        this.y = pos.yCoord;
        this.z = pos.zCoord;
        return this;
    }

    public Coordinate addX(double x) {
        this.x += x;
        return this;
    }

    public Coordinate addY(double y) {
        this.y += y;
        return this;
    }

    public Coordinate addZ(double z) {
        this.z += z;
        return this;
    }

    /**
     * 序列化到 NBT
     */
    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("x", x);
        tag.setDouble("y", y);
        tag.setDouble("z", z);
        tag.setDouble("yaw", yaw);
        tag.setDouble("pitch", pitch);
        tag.setString("dimension", dimension);
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
    public static Coordinate readFromNBT(NBTTagCompound tag) {
        Coordinate coordinate = new Coordinate();
        coordinate.x = tag.getDouble("x");
        coordinate.y = tag.getDouble("y");
        coordinate.z = tag.getDouble("z");
        coordinate.yaw = tag.getDouble("yaw");
        coordinate.pitch = tag.getDouble("pitch");
        coordinate.dimension = tag.getString("dimension");
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

    public String toXString() {
        return StringUtils.toFixedEx(x, 1);
    }

    public String toYString() {
        return StringUtils.toFixedEx(y, 1);
    }

    public String toZString() {
        return StringUtils.toFixedEx(z, 1);
    }

    public String toXyzString() {
        return StringUtils.toFixedEx(x, 1) + ", " + StringUtils.toFixedEx(y, 1) + ", " + StringUtils.toFixedEx(z, 1);
    }

    public double distanceSq(Coordinate coordinate) {
        return Math.pow(coordinate.x - x, 2) + Math.pow(coordinate.y - y, 2) + Math.pow(coordinate.z - z, 2);
    }

    public double distanceSq(double x, double y, double z) {
        return Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2) + Math.pow(z - this.z, 2);
    }

}
