package xin.vanilla.narcissus.client.ui;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class NarcissusUiRouteTest {
    private String source(String path) throws Exception {
        return Files.readString(Path.of("src/main/" + path));
    }
    @Test public void allPagesUseSharedHostAndKeepNativeFallbacks() throws Exception {
        String waypoint = source("java/xin/vanilla/narcissus/screen/WaypointScreen.java");
        String access = source("java/xin/vanilla/narcissus/screen/AccessListScreen.java");
        String player = source("java/xin/vanilla/narcissus/screen/PlayerConfigScreen.java");
        assertTrue(waypoint.contains("implements UiNativePage"));
        assertTrue(access.contains("implements UiNativePage"));
        assertTrue(player.contains("playerUiSession()"));
    }
    @Test public void legacyBackendAndDependencyAreRemoved() throws Exception {
        assertFalse(source("java/xin/vanilla/narcissus/integration/ScreenHelper.java").contains("new ApricityUI"));
        assertFalse(Files.exists(Path.of("src/main/java/xin/vanilla/narcissus/integration/ui/ApricityUI.java")));
        assertFalse(Files.readString(Path.of("build.gradle")).contains("curse.maven:apricityui"));
        assertFalse(Files.readString(Path.of("gradle.properties")).contains("apricityui_id"));
    }
    @Test public void threeFinitePagesUseLocalStylesWithoutScript() throws Exception {
        int total = 0;
        for (String name : new String[]{"waypoints", "access", "player"}) {
            Path path = Path.of("src/main/resources/assets/apricityui/apricity/narcissus_farewell/" + name + ".html");
            assertTrue(name, Files.exists(path));
            String html = Files.readString(path);
            assertTrue(html.contains("narcissus.css"));
            assertFalse(html.contains("<script"));
            assertTrue(Files.size(path) <= 12 * 1024);
            total += Files.size(path);
        }
        Path css = Path.of("src/main/resources/assets/apricityui/apricity/narcissus_farewell/narcissus.css");
        assertTrue(Files.size(css) <= 4 * 1024);
        assertTrue(total < 12 * 1024);
    }
}
