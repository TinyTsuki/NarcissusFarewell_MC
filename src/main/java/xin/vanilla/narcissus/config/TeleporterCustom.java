package xin.vanilla.narcissus.config;


import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import xin.vanilla.narcissus.NarcissusFarewell;

import javax.annotation.ParametersAreNonnullByDefault;

public class TeleporterCustom extends Teleporter {
    private final Coordinate coordinate;

    public TeleporterCustom(WorldServer world, Coordinate coordinate) {
        super(world);
        this.coordinate = coordinate;
    }

    /**
     * 放置实体
     */
    @Override
    @ParametersAreNonnullByDefault
    public void placeEntity(World world, Entity entity, float rotationYaw) {
        // 计算目标区块坐标
        int chunkX = coordinate.getXInt() >> 4;
        int chunkZ = coordinate.getZInt() >> 4;

        // 强制加载目标区块
        ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(NarcissusFarewell.instance, super.world, ForgeChunkManager.Type.NORMAL);
        if (ticket != null) {
            ForgeChunkManager.forceChunk(ticket, new ChunkPos(chunkX, chunkZ));
        }

        // 直接传送到指定坐标
        entity.setPositionAndUpdate(this.coordinate.getX(), this.coordinate.getY(), this.coordinate.getZ());
        entity.rotationYaw = (float) this.coordinate.getYaw() == 0 ? entity.rotationYaw : (float) this.coordinate.getYaw();
        entity.rotationPitch = (float) this.coordinate.getPitch() == 0 ? entity.rotationPitch : (float) this.coordinate.getPitch();

        // 释放区块加载
        if (ticket != null && entity instanceof EntityPlayer) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }

    /**
     * 是否寻找传送门
     */
    @Override
    @ParametersAreNonnullByDefault
    public boolean placeInExistingPortal(Entity entity, float rotationYaw) {
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
