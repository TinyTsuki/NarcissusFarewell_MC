package xin.vanilla.narcissus.screen;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class PlayerConfigStyleContractTest {

    @Test
    public void matchesAotakePlayerConfigShellAndSaveFlow() throws Exception {
        String screen = source("src/main/java/xin/vanilla/narcissus/screen/PlayerConfigScreen.java");
        String smoke = source("src/main/java/xin/vanilla/narcissus/internal/client/dev/NarcissusUiSmokeRunner.java");

        assertTrue(screen.contains("CollapsiblePanelWidget.createAutoHeight"));
        assertTrue(screen.contains("AbstractGuiUtils.drawRoundedRect"));
        assertTrue(screen.contains("config_editor_save"));
        assertTrue(screen.contains("config_editor_close"));
        assertTrue(screen.contains("saveToServer()"));
        assertTrue(screen.contains("onClose();"));
        assertTrue(smoke.contains("new ScreenStep(\"player-preferences\", true"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
