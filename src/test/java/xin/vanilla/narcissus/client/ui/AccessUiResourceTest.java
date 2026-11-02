package xin.vanilla.narcissus.client.ui;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessUiResourceTest {
    private String resource(String name) throws Exception {
        return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/assets/apricityui/apricity/narcissus_farewell/" + name)), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    public void approvedAccessHasIndependentSearchListIdentityAndRequestResults() throws Exception {
        String html = resource("access.html");
        for (String id : new String[]{"brand", "search", "content", "player_name", "player_uuid", "tpa_result", "tph_result", "scope", "empty", "footer_status", "confirmation"})
            assertTrue("Missing access field " + id, html.contains("id=\"" + id + "\""));
        assertTrue(html.contains("access.css"));
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("permission_level"));
        assertFalse(html.contains("save_all"));
    }

    @Test
    public void detailAndActionsStaySeparateInCompactLayout() throws Exception {
        String html = resource("access.html");
        assertTrue(html.contains("id=\"detail_content\""));
        assertTrue(html.indexOf("id=\"detail_content\"") < html.indexOf("id=\"edit\""));
        assertTrue(resource("access.css").contains("overflow-y:auto"));
    }

    @Test
    public void accessSessionUsesOptionalTypedPortWithoutChangingServerPackets() throws Exception {
        String session = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/narcissus/client/ui/AccessListUiSession.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(session.contains("PresentationPort"));
        assertTrue(session.contains("selectionLocked"));
        assertTrue(session.contains("keyCode == 256"));
        String screen = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/narcissus/screen/AccessListScreen.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(screen.contains("AccessListUiSession.PresentationPort"));
        assertTrue(screen.contains("new AccessListEditToServer(1, \"\", currentUuid)"));
        assertTrue(screen.contains("3, EnumWhiteListMode.NONE.name(), currentUuid"));
    }

    @Test
    public void compactAddControlRemainsAnIconButton() throws Exception {
        assertTrue(resource("access.css").contains(".access .navigation #add{min-width:22px;width:22px}"));
    }

    @Test
    public void noExtraModeBadgeUsesItsLocalLabel() throws Exception {
        String screen = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/narcissus/screen/AccessListScreen.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(screen.contains("enum_white_list_mode_none"));
        assertTrue(screen.contains("AccessProfileIcons.location"));
    }
}
