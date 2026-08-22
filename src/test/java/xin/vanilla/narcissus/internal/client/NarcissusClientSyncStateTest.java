package xin.vanilla.narcissus.internal.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NarcissusClientSyncStateTest {
    @Test
    public void playerDataSyncAlsoInvalidatesWaypointViews() {
        long playerBefore = NarcissusClientSyncState.playerDataGeneration();
        long waypointBefore = NarcissusClientSyncState.waypointDataGeneration();

        NarcissusClientSyncState.markPlayerDataReceived();

        assertEquals(playerBefore + 1L, NarcissusClientSyncState.playerDataGeneration());
        assertEquals(waypointBefore + 1L, NarcissusClientSyncState.waypointDataGeneration());
    }

    @Test
    public void stageDataSyncInvalidatesWaypointViewsWithoutPretendingPlayerDataChanged() {
        long playerBefore = NarcissusClientSyncState.playerDataGeneration();
        long waypointBefore = NarcissusClientSyncState.waypointDataGeneration();

        NarcissusClientSyncState.markStageDataReceived();

        assertEquals(playerBefore, NarcissusClientSyncState.playerDataGeneration());
        assertEquals(waypointBefore + 1L, NarcissusClientSyncState.waypointDataGeneration());
    }
}
