package xin.vanilla.narcissus.integration;

import net.minecraft.client.Minecraft;
import xin.vanilla.narcissus.screen.AccessListScreen;
import xin.vanilla.narcissus.screen.PlayerConfigScreen;
import xin.vanilla.narcissus.screen.WaypointScreen;

/**
 * 共用界面入口，由 Banira 选择外观后端。
 */
public final class ScreenHelper {
    private ScreenHelper() {
    }

    public static void openScreen() {
        Minecraft.getInstance().setScreen(new WaypointScreen());
    }

    public static void openAccessListScreen() {
        Minecraft.getInstance().setScreen(new AccessListScreen());
    }

    public static void openPlayerTeleportPrefsScreen() {
        Minecraft.getInstance().setScreen(new PlayerConfigScreen(
                new PlayerConfigScreen.Args().parentScreen(Minecraft.getInstance().screen)));
    }
}
