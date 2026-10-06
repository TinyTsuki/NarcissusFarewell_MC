package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Development-only Carpet fake players used by the dedicated-server smoke.
 */
final class NarcissusCarpetFakePlayers {
    private static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList(
            "NarcissusA", "NarcissusB"));

    List<String> spawnCommands() {
        return NAMES.stream().map(name -> "player " + name + " spawn").collect(Collectors.toList());
    }

    List<String> cleanupCommands() {
        return NAMES.stream().map(name -> "player " + name + " kill").collect(Collectors.toList());
    }

    boolean isFixturePlayerName(String name) {
        return NAMES.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(name));
    }

    void spawn(MinecraftServer server) {
        requirePlayerCommand(server);
        executeAll(server, spawnCommands());
    }

    void cleanup(MinecraftServer server) {
        executeAll(server, cleanupCommands());
    }

    boolean allPresent(MinecraftServer server) {
        return resolve(server).size() == NAMES.size();
    }

    boolean allRemoved(MinecraftServer server) {
        return resolve(server).isEmpty();
    }

    List<ServerPlayer> resolve(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> isFixturePlayerName(player.getGameProfile().getName()))
                .collect(Collectors.toList());
    }

    private static void requirePlayerCommand(MinecraftServer server) {
        if (server.getCommands().getDispatcher().getRoot().getChild("player") == null) {
            throw new IllegalStateException("Carpet /player command was not registered for network smoke");
        }
    }

    private static void executeAll(MinecraftServer server, List<String> commands) {
        for (String command : commands) {
            int result = server.getCommands().performCommand(
                    server.createCommandSourceStack().withPermission(4), command);
            if (result <= 0) {
                throw new IllegalStateException("Carpet fake-player command failed: /" + command);
            }
        }
    }
}
