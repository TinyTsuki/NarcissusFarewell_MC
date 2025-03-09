package xin.vanilla.narcissus.config;


import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

import javax.annotation.ParametersAreNonnullByDefault;

public class TeleporterCustom extends Teleporter {
    private final WorldServer world;
    private final Coordinate coordinate;

    public TeleporterCustom(WorldServer world, Coordinate coordinate) {
        super(world);
        this.world = world;
        this.coordinate = coordinate;
    }

    /**
     * 放置实体
     */
    @Override
    public void placeInPortal(Entity entity, double x, double y, double z, float rotationYaw) {
        // // 计算目标区块坐标
        // int chunkX = coordinate.getXInt() >> 4;
        // int chunkZ = coordinate.getZInt() >> 4;
        //
        // // 强制加载目标区块
        // ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(NarcissusFarewell.instance, this.world, ForgeChunkManager.Type.NORMAL);
        // if (ticket != null) {
        //     ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(chunkX, chunkZ));
        // }

        // 直接传送到指定坐标
        entity.setPosition(this.coordinate.getX(), this.coordinate.getY(), this.coordinate.getZ());
        entity.rotationYaw = (float) this.coordinate.getYaw() == 0 ? entity.rotationYaw : (float) this.coordinate.getYaw();
        entity.rotationPitch = (float) this.coordinate.getPitch() == 0 ? entity.rotationPitch : (float) this.coordinate.getPitch();

        // // 释放区块加载
        // if (ticket != null && entity instanceof EntityPlayer) {
        //     ForgeChunkManager.releaseTicket(ticket);
        // }
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
