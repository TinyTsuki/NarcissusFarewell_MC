package xin.vanilla.narcissus.data.player;

import net.minecraft.world.entity.player.Player;
import org.junit.Test;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CostCardReadTest {
    @Test
    public void readingCardsForACostDoesNotFlushDirtyPlayerData() throws Exception {
        Constructor<PlayerTeleportData> constructor = PlayerTeleportData.class.getDeclaredConstructor(Player.class);
        constructor.setAccessible(true);
        PlayerTeleportData data = constructor.newInstance((Object) null);
        data.setDirty(true);
        assertEquals(0, data.peekTeleportCard());
        assertTrue(data.isDirty());
    }

    @Test
    public void readingCountdownForACostDoesNotFlushDirtyPlayerData() throws Exception {
        Constructor<PlayerTeleportData> constructor = PlayerTeleportData.class.getDeclaredConstructor(Player.class);
        constructor.setAccessible(true);
        PlayerTeleportData data = constructor.newInstance((Object) null);
        data.setDirty(true);
        assertEquals(0, data.peekTeleportCountdownSeconds(EnumTeleportType.TP_HOME));
        assertTrue(data.isDirty());
    }
}
