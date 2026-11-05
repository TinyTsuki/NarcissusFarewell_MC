package xin.vanilla.narcissus.internal.fabric.search;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.search.SearchChunkPool;

import java.util.Objects;

public final class FabricSearchChunkBackend implements SearchChunkPool.Backend {
    private static final TicketType SEARCH_TICKET = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private final MinecraftServer server;

    public FabricSearchChunkBackend(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public boolean isReady(Identifier dimension, int cx, int cz) {
        return world(dimension).getChunkSource().getChunkNow(cx, cz) != null;
    }

    @Override
    public void retain(Identifier dimension, int cx, int cz) {
        ChunkPos position = new ChunkPos(cx, cz);
        world(dimension).getChunkSource().addTicketWithRadius(SEARCH_TICKET, position, 0);
    }

    @Override
    public void release(Identifier dimension, int cx, int cz) {
        ChunkPos position = new ChunkPos(cx, cz);
        world(dimension).getChunkSource().removeTicketWithRadius(SEARCH_TICKET, position, 0);
    }

    private ServerLevel world(Identifier dimension) {
        if (!server.isSameThread()) throw new IllegalStateException("Search chunks require server owner thread");
        ServerLevel world = server.getLevel(DimensionUtils.parse(dimension.toString()));
        if (world == null || world.getServer() != server) {
            throw new IllegalStateException("Search dimension is unavailable: " + dimension);
        }
        return world;
    }
}
