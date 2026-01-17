package xin.vanilla.narcissus.config;


import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

import javax.annotation.ParametersAreNonnullByDefault;

public class TeleporterCustom extends Teleporter {
    private final WorldServer world;

    public TeleporterCustom(WorldServer world) {
        super(world);
        this.world = world;
    }

    /**
     * 放置实体
     */
    @Override
    public void placeInPortal(Entity entity, double x, double y, double z, float rotationYaw) {
    }

    /**
     * 是否寻找传送门
     */
    @Override
    @ParametersAreNonnullByDefault
    public boolean placeInExistingPortal(Entity entity, double x, double y, double z, float rotationYaw) {
        return false; // 不寻找现有传送门
    }

    /**
     * 是否生成传送门
     */
    @Override
    @ParametersAreNonnullByDefault
    public boolean makePortal(Entity entity) {
        return false;
    }
}
