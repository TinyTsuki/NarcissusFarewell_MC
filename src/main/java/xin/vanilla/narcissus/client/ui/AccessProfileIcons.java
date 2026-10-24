package xin.vanilla.narcissus.client.ui;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public final class AccessProfileIcons {
    private static final ResourceLocation LEGACY_DEFAULT = new ResourceLocation("minecraft", "textures/entity/steve.png");
    private AccessProfileIcons() { }
    public static ResourceLocation location(UUID uuid, ResourceLocation online) {
        return online == null || LEGACY_DEFAULT.equals(online) ? DefaultPlayerSkin.getDefaultSkin(uuid) : online;
    }
}
