package xin.vanilla.narcissus.client.ui;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class AccessProfileIcons {
    private static final Identifier LEGACY_DEFAULT = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/steve.png");

    private AccessProfileIcons() {
    }

    public static Identifier location(UUID uuid, Identifier online) {
        return online == null || LEGACY_DEFAULT.equals(online) ? DefaultPlayerSkin.get(uuid).body().texturePath() : online;
    }
}
