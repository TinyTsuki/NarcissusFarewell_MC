package xin.vanilla.narcissus.client.ui;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessUiResourceTest {
    private String resource(String name) throws Exception {
        return Files.readString(Path.of("src/main/resources/assets/apricityui/apricity/narcissus_farewell/" + name));
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
        String session = Files.readString(Path.of("src/main/java/xin/vanilla/narcissus/client/ui/AccessListUiSession.java"));
        assertTrue(session.contains("PresentationPort"));
        assertTrue(session.contains("selectionLocked"));
        assertTrue(session.contains("keyCode == 256"));
        String screen = Files.readString(Path.of("src/main/java/xin/vanilla/narcissus/screen/AccessListScreen.java"));
        assertTrue(screen.contains("AccessListUiSession.PresentationPort"));
        assertTrue(screen.contains("new AccessListEditToServer(1, \"\", currentUuid)"));
        assertTrue(screen.contains("3, EnumWhiteListMode.NONE.name(), currentUuid"));
    }

    @Test
    public void compactAddControlRemainsAnIconButton() throws Exception {
        assertTrue(resource("access.css").contains(".access .navigation #add{min-width:22px;width:22px}"));
    }

    @Test
    public void absoluteRowTextReservesTheProfileIconColumn() throws Exception {
        String css = resource("access.css");
        assertTrue(css.contains(".access .row-action{padding-left:32px}"));
        assertTrue(css.contains(".access .row .label{left:32px}"));
    }

    @Test
    public void noExtraModeBadgeUsesItsLocalLabel() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/xin/vanilla/narcissus/screen/AccessListScreen.java"));
        assertTrue(screen.contains("enum_white_list_mode_none"));
        assertTrue(screen.contains("AccessProfileIcons.location"));
    }
}
