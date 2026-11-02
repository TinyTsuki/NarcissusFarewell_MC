package xin.vanilla.narcissus.client.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class WaypointRowHitLayoutTest {
    private String rule(String selector) throws Exception {
        String css = new String(UiBoundaryChecks.ownResources("narcissus_farewell").get("waypoints.css"), StandardCharsets.UTF_8);
        var match = Pattern.compile(Pattern.quote(selector + "{") + "([^}]+)}").matcher(css);
        assertTrue(match.find());
        return match.group(1);
    }

    @Test
    public void selectionTargetCoversAllThirtySixPixels() throws Exception {
        String action = rule(".row .row-action");
        assertTrue(action.contains("position:absolute"));
        assertTrue(action.contains("height:36px"));
        assertTrue(action.contains("max-height:36px"));
        assertTrue(rule(".row").contains("position:relative"));
    }

    @Test
    public void metadataPassesPointerInputToTheSameSelectionAction() throws Exception {
        String label = rule(".row .label");
        assertTrue(label.contains("position:absolute"));
        assertTrue(label.contains("pointer-events:none"));
    }
}
