package xin.vanilla.narcissus.screen;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 保证两个自定义管理界面复用同一套轻量面板层级。 */
public class NarcissusCustomScreenStyleContractTest {

    @Test
    public void waypointAndAccessListShareLightweightChrome() throws Exception {
        String chrome = source("src/main/java/xin/vanilla/narcissus/screen/NarcissusScreenChrome.java");
        String waypoint = source("src/main/java/xin/vanilla/narcissus/screen/WaypointScreen.java");
        String accessList = source("src/main/java/xin/vanilla/narcissus/screen/AccessListScreen.java");

        assertTrue(chrome.contains("RoundedCornerMode.FINE"));
        assertTrue(chrome.contains("drawOuterSurface"));
        assertTrue(chrome.contains("drawTopBar"));
        assertTrue(chrome.contains("drawListSurface"));
        assertTrue(chrome.contains("drawListRow"));
        assertTrue(chrome.contains("drawEmptyState"));
        assertTrue(chrome.contains("drawFooterSurface"));

        assertTrue(waypoint.contains("NarcissusScreenChrome.drawOuterSurface"));
        assertTrue(waypoint.contains("NarcissusScreenChrome.drawListRow"));
        assertTrue(waypoint.contains("NarcissusScreenChrome.drawEmptyState"));
        assertTrue(accessList.contains("NarcissusScreenChrome.drawOuterSurface"));
        assertTrue(accessList.contains("NarcissusScreenChrome.drawListRow"));
        assertTrue(accessList.contains("NarcissusScreenChrome.drawEmptyState"));

        assertFalse(waypoint.contains("private void drawTopBarAndDividers"));
        assertFalse(accessList.contains("private void drawTopBarAndDividers"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
