package xin.vanilla.narcissus.integration;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import xin.vanilla.narcissus.integration.ui.ApricityUI;
import xin.vanilla.narcissus.screen.WaypointScreen;


public final class ScreenHelper {
    private ScreenHelper() {
    }

    public static void openScreen() {
        if (ModList.get().isLoaded("apricityui")) {
            Minecraft.getInstance().setScreen(new ApricityUI());
        } else {
            Minecraft.getInstance().setScreen(new WaypointScreen());
        }
    }

}
