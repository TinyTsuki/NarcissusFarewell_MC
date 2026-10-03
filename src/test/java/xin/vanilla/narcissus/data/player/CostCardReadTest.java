package xin.vanilla.narcissus.data.player;

import net.minecraft.entity.player.PlayerEntity;
import org.junit.Test;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

public class CostCardReadTest {
    @Test public void readingCardsForACostDoesNotFlushDirtyPlayerData() throws Exception {
        Constructor<PlayerTeleportData> constructor = PlayerTeleportData.class.getDeclaredConstructor(PlayerEntity.class);
        constructor.setAccessible(true);
        PlayerTeleportData data = constructor.newInstance((Object) null);
        data.setDirty(true);
        assertEquals(0, data.peekTeleportCard());
        assertTrue(data.isDirty());
    }

    @Test public void readingCountdownForACostDoesNotFlushDirtyPlayerData() throws Exception {
        Constructor<PlayerTeleportData> constructor = PlayerTeleportData.class.getDeclaredConstructor(PlayerEntity.class);
        constructor.setAccessible(true);
        PlayerTeleportData data = constructor.newInstance((Object) null);
        data.setDirty(true);
        assertEquals(0, data.peekTeleportCountdownSeconds(EnumTeleportType.TP_HOME));
        assertTrue(data.isDirty());
    }
}
