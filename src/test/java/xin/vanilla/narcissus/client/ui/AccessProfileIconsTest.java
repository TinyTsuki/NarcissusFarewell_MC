package xin.vanilla.narcissus.client.ui;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class AccessProfileIconsTest {
    private final UUID id = UUID.fromString("00000000-0000-4000-8000-000000000021");

    private Identifier resolve(Identifier skin) throws Exception {
        Class<?> type;
        try {
            type = Class.forName("xin.vanilla.narcissus.client.ui.AccessProfileIcons");
        } catch (ClassNotFoundException failure) {
            fail("Missing version-correct access profile fallback");
            return null;
        }
        return (Identifier) type.getMethod("location", UUID.class, Identifier.class).invoke(null, id, skin);
    }

    @Test
    public void absentOnlineSkinUsesTheActualMinecraftDefault() throws Exception {
        assertEquals(DefaultPlayerSkin.get(id).body().texturePath(), resolve(null));
    }

    @Test
    public void legacyFallbackCannotRenderAsABlackOfflineHead() throws Exception {
        Identifier result = resolve(Identifier.fromNamespaceAndPath("minecraft", "textures/entity/steve.png"));
        assertEquals(DefaultPlayerSkin.get(id).body().texturePath(), result);
        assertTrue(result.getPath().startsWith("textures/entity/player/"));
    }

    @Test
    public void actualOnlineSkinIsPreserved() throws Exception {
        Identifier online = Identifier.fromNamespaceAndPath("minecraft", "skins/current-profile");
        assertSame(online, resolve(online));
    }
}
