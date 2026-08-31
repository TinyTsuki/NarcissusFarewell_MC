package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NarcissusCarpetFakePlayersTest {
    @Test
    public void buildsIsolatedSpawnAndCleanupCommands() {
        NarcissusCarpetFakePlayers players = new NarcissusCarpetFakePlayers();

        assertEquals(Arrays.asList(
                "player NarcissusA spawn",
                "player NarcissusB spawn"), players.spawnCommands());
        assertEquals(Arrays.asList(
                "player NarcissusA kill",
                "player NarcissusB kill"), players.cleanupCommands());
        assertTrue(players.isFixturePlayerName("NarcissusA"));
        assertTrue(players.isFixturePlayerName("narcissusa"));
        assertTrue(players.spawnCommands().stream().allMatch(command -> command.split(" ")[1].length() <= 16));
    }
}
