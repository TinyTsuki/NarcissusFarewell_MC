package xin.vanilla.narcissus.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumSafeMode;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true, fluent = true)
public class SafeWorldCoordinate extends WorldCoordinate implements Serializable, Cloneable {
    private boolean safe = false;
    private EnumSafeMode safeMode = EnumSafeMode.NONE;

    public SafeWorldCoordinate(Player player) {
        super(player);
    }

    public SafeWorldCoordinate(double x, double y, double z, double yRot, double xRot, ResourceKey<Level> dimension) {
        super(x, y, z, yRot, xRot, dimension);
    }

    public SafeWorldCoordinate(double x, double y, double z, ResourceKey<Level> dimension) {
        super(x, y, z, dimension);
    }

    public SafeWorldCoordinate(double x, double y, double z, String dimension) {
        super(x, y, z, DimensionUtils.parse(dimension));
    }

    public static SafeWorldCoordinate random(ServerPlayer player) {
        return random(player, CommonConfig.get().base().randomTeleport().teleportRandomDistanceLimit());
    }

    public static SafeWorldCoordinate random(ServerPlayer player, int range) {
        return random(player, range, player.level.dimension());
    }

    public static SafeWorldCoordinate random(ServerPlayer player, int range, ResourceKey<Level> dimension) {
        ServerLevel world = DimensionUtils.getLevel(dimension);
        range = Math.min(Math.max(range, 1), CommonConfig.get().base().randomTeleport().teleportRandomDistanceLimit());
        double x = player.getX() + (Math.random() * 2 - 1) * range;
        double y = randomWithWeight(DimensionUtils.getWorldMinY(world), DimensionUtils.getWorldMaxY(world), (int) player.getY(), 0.75);
        double z = player.getZ() + (Math.random() * 2 - 1) * range;
        return new SafeWorldCoordinate(x, y, z, player.getYRot(), player.getXRot(), dimension);
    }

    @Override
    public SafeWorldCoordinate clone() {
        SafeWorldCoordinate cloned = (SafeWorldCoordinate) super.clone();
        cloned.safeMode = this.safeMode;
        return cloned;
    }

    public static SafeWorldCoordinate fromTag(CompoundTag tag) {
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
