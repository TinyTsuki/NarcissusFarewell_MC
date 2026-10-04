package xin.vanilla.narcissus.internal.neoforge.search;

import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.search.SearchChunkPool;

import java.util.Comparator;
import java.util.Objects;

public final class NeoForgeSearchChunkBackend implements SearchChunkPool.Backend {
    private static final TicketType<ChunkPos> SEARCH_TICKET = TicketType.create(
            "narcissus_search", Comparator.comparingLong(ChunkPos::toLong));
    private final MinecraftServer server;

    public NeoForgeSearchChunkBackend(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public boolean isReady(ResourceLocation dimension, int cx, int cz) {
        return world(dimension).getChunkSource().getChunkNow(cx, cz) != null;
    }

    @Override
    public void retain(ResourceLocation dimension, int cx, int cz) {
        ChunkPos position = new ChunkPos(cx, cz);
        world(dimension).getChunkSource().addRegionTicket(SEARCH_TICKET, position, 0, position);
    }

    @Override
    public void release(ResourceLocation dimension, int cx, int cz) {
        ChunkPos position = new ChunkPos(cx, cz);
        world(dimension).getChunkSource().removeRegionTicket(SEARCH_TICKET, position, 0, position);
    }

    private ServerLevel world(ResourceLocation dimension) {
        if (!server.isSameThread()) throw new IllegalStateException("Search chunks require server owner thread");
        ServerLevel world = server.getLevel(DimensionUtils.parse(dimension.toString()));
        if (world == null || world.getServer() != server) {
            throw new IllegalStateException("Search dimension is unavailable: " + dimension);
        }
        return world;
    }
}
