package xin.vanilla.narcissus.util;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.List;

public class ChunkLoadingCallback implements ForgeChunkManager.LoadingCallback {
    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (ForgeChunkManager.Ticket ticket : tickets) {
            for (ChunkCoordIntPair chunk : ticket.getChunkList()) {
                ForgeChunkManager.forceChunk(ticket, chunk);
            }
        }
    }
}
