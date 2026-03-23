package xin.vanilla.narcissus.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true, fluent = true)
public class SafeWorldCoordinate extends WorldCoordinate implements Serializable, Cloneable {
    private boolean safe = false;
    private EnumSafeMode safeMode = EnumSafeMode.NONE;

    public SafeWorldCoordinate(PlayerEntity player) {
        super(player);
    }

    public SafeWorldCoordinate(double x, double y, double z, double yRot, double xRot, RegistryKey<World> dimension) {
        super(x, y, z, yRot, xRot, dimension);
    }

    public SafeWorldCoordinate(double x, double y, double z, RegistryKey<World> dimension) {
        super(x, y, z, dimension);
    }

    public SafeWorldCoordinate(double x, double y, double z, String dimension) {
        super(x, y, z, DimensionUtils.parse(dimension));
    }

    public static SafeWorldCoordinate random(ServerPlayerEntity player) {
        return random(player, CommonConfig.get().general().teleportRandomDistanceLimit());
    }

    public static SafeWorldCoordinate random(ServerPlayerEntity player, int range) {
        return random(player, range, player.level.dimension());
    }

    public static SafeWorldCoordinate random(ServerPlayerEntity player, int range, RegistryKey<World> dimension) {
        ServerWorld world = DimensionUtils.getLevel(dimension);
        range = Math.min(Math.max(range, 1), CommonConfig.get().general().teleportRandomDistanceLimit());
        double x = player.getX() + (Math.random() * 2 - 1) * range;
        double y = randomWithWeight(NarcissusUtils.getWorldMinY(world), NarcissusUtils.getWorldMaxY(world), (int) player.getY(), 0.75);
        double z = player.getZ() + (Math.random() * 2 - 1) * range;
        return new SafeWorldCoordinate(x, y, z, player.yRot, player.xRot, dimension);
    }

    @Override
    public SafeWorldCoordinate clone() {
        SafeWorldCoordinate cloned = (SafeWorldCoordinate) super.clone();
        cloned.safeMode = this.safeMode;
        return cloned;
    }

    public static SafeWorldCoordinate fromTag(CompoundNBT tag) {
        WorldCoordinate w = WorldCoordinate.fromTag(tag);
        return copyFromWorldCoordinate(w);
    }

    private static SafeWorldCoordinate copyFromWorldCoordinate(WorldCoordinate w) {
        SafeWorldCoordinate s = new SafeWorldCoordinate(w.x(), w.y(), w.z(), w.yaw(), w.pitch(), w.dimension());
        s.stepSize(w.stepSize());
        s.direction(w.direction());
        s.safe(false);
        s.safeMode(EnumSafeMode.NONE);
        return s;
    }

    public String getDimensionResourceId() {
        return dimensionId();
    }

    public String toXyzIntString(String delimiter) {
        return xInt() + delimiter + yInt() + delimiter + zInt();
    }
}
