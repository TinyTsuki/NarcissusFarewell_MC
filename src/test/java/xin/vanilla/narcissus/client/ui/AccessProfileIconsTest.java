package xin.vanilla.narcissus.client.ui;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class AccessProfileIconsTest {
    private final UUID id = UUID.fromString("00000000-0000-4000-8000-000000000021");

    private ResourceLocation resolve(ResourceLocation skin) throws Exception {
        Class<?> type;
        try {
            type = Class.forName("xin.vanilla.narcissus.client.ui.AccessProfileIcons");
        } catch (ClassNotFoundException failure) {
            fail("Missing version-correct access profile fallback");
            return null;
        }
        return (ResourceLocation) type.getMethod("location", UUID.class, ResourceLocation.class).invoke(null, id, skin);
    }

    @Test
    public void absentOnlineSkinUsesTheActualMinecraftDefault() throws Exception {
        assertEquals(DefaultPlayerSkin.getDefaultSkin(id), resolve(null));
    }

    @Test
    public void legacyFallbackUsesTheVersionSpecificVanillaHead() throws Exception {
        ResourceLocation result = resolve(new ResourceLocation("minecraft", "textures/entity/steve.png"));
        assertEquals(DefaultPlayerSkin.getDefaultSkin(id), result);
        assertEquals("minecraft", result.getNamespace());
        assertTrue(java.util.Set.of("textures/entity/steve.png", "textures/entity/alex.png").contains(result.getPath()));
    }

    @Test
    public void actualOnlineSkinIsPreserved() throws Exception {
        ResourceLocation online = new ResourceLocation("minecraft", "skins/current-profile");
        assertSame(online, resolve(online));
    }
}
