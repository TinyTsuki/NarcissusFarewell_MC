package xin.vanilla.narcissus.client.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.Assert.*;

public class WaypointUiResourceTest {
    private final Map<String, byte[]> files = UiBoundaryChecks.ownResources("narcissus_farewell");
    public WaypointUiResourceTest() throws Exception { }
    private String resource(String name) {
        byte[] bytes = files.get(name);
        assertNotNull("Missing resource " + name, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    @Test public void approvedPageHasSeparateListSearchDetailAndCostFields() {
        String html = resource("waypoints.html");
        for (String id : new String[]{"point_name", "dimension", "dimension_id", "coordinate_x", "coordinate_y",
                "coordinate_z", "point_distance", "point_date", "cost_note", "empty", "footer_status", "search_hint"})
            assertTrue("Missing typed field " + id, html.contains("id=\"" + id + "\""));
        assertTrue(html.contains("waypoint-list"));
        assertTrue(html.contains("waypoint-inspector"));
        assertTrue(html.contains("waypoints.css"));
    }
    @Test public void searchAndTeleportRemainIndependentOfQuotePresentation() {
        String html = resource("waypoints.html");
        assertTrue(html.contains("id=\"search\""));
        assertTrue(html.contains("id=\"teleport\""));
        assertFalse(html.contains("payment_confirm"));
        assertFalse(html.contains("<script"));
    }
    @Test public void engineStylesUseDefaultFontAndCompactUnframedDetail() {
        String css = resource("waypoints.css");
        assertTrue(css.contains("font-family: default"));
        assertTrue(css.contains(".waypoint-inspector"));
        assertTrue(css.contains("#cost_note"));
        assertFalse(css.contains("linear-gradient"));
        assertFalse(css.contains("@import"));
        assertTrue(css.getBytes(StandardCharsets.UTF_8).length <= 4096);
    }
    @Test public void inspectorKeepsItsColumnWidthWhenHoverInvalidatesLayout() {
        String css = resource("waypoints.css");
        String inspector = css.substring(css.indexOf(".waypoint-inspector{"));
        inspector = inspector.substring(0, inspector.indexOf('}'));
        assertTrue(inspector.contains("width:47%"));
        assertTrue(inspector.contains("flex:0 0 47%"));
        assertFalse(css.contains("overflow-wrap"));
    }
    @Test public void pointDetailsScrollIndependentlyOfTheCostAndActions() {
        assertTrue(resource("waypoints.html").contains("id=\"point_content\""));
        String css = resource("waypoints.css");
        String point = css.substring(css.indexOf(".point{"));
        point = point.substring(0, point.indexOf('}'));
        assertTrue(point.contains("overflow-y:auto"));
    }
}
