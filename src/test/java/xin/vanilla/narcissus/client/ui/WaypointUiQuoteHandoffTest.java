package xin.vanilla.narcissus.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.data.cost.CostQuote;
import xin.vanilla.narcissus.data.cost.CostQuoteRequest;
import xin.vanilla.narcissus.data.cost.CostQuoteTarget;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.internal.client.ClientCostQuotes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.*;

public class WaypointUiQuoteHandoffTest {
    @Test
    public void legacyReturningSwitchesDoNotLeaveUnreachableEmptyStatements() throws Exception {
        for (String page : new String[]{"WaypointScreen", "AccessListScreen"}) {
            String source = new String(Files.readAllBytes(java.nio.file.Paths.get(
                    "src/main/java/xin/vanilla/narcissus/screen/" + page + ".java")), java.nio.charset.StandardCharsets.UTF_8);
            assertFalse(page, java.util.regex.Pattern.compile("default:\\s*return[^;]+;\\s*};").matcher(source).find());
        }
    }

    @Test
    public void legacyPositionSnapshotPreservesFractionalAndNegativeCoordinates() {
        WaypointUiParityTest.bootstrap();
        xin.vanilla.narcissus.data.SafeWorldCoordinate position = new xin.vanilla.narcissus.data.SafeWorldCoordinate();
        net.minecraft.util.math.vector.Vector3d moved = new net.minecraft.util.math.vector.Vector3d(-92.75, 77.125, 49.25);
        position.fromVector3d(moved).dimension(net.minecraft.world.World.NETHER);
        assertEquals(moved.x, position.x(), 0.0);
        assertEquals(moved.y, position.y(), 0.0);
        assertEquals(moved.z, position.z(), 0.0);
        assertSame(net.minecraft.world.World.NETHER, position.dimension());
    }

    @Test
    public void nativeHandoffCancelsTheUncommittedDragWithoutSubmittingAnOrder() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/narcissus/screen/WaypointScreen.java")), java.nio.charset.StandardCharsets.UTF_8);
        String capture = source.substring(source.indexOf("public void captureUiState"), source.indexOf("public void prepareUiState"));
        assertTrue(capture.contains("clearDragState()"));
        assertFalse(capture.contains("finishDrag()"));
        assertFalse(capture.contains("submitActiveOrder()"));
    }

    @Test
    public void confirmedAndPendingQuotesSurviveEveryPresentationDetachWithoutPayment() {
        ClientCostQuotes quotes = new ClientCostQuotes();
        quotes.capabilities(UUID.randomUUID(), 1);
        CostQuoteTarget target = CostQuoteTarget.home(UUID.randomUUID(), "minecraft:overworld", "home");
        CostQuoteRequest request = quotes.request(target, 0).get();
        CostQuote confirmed = CostQuote.ready(request, EnumCostType.NONE, CostPaymentPlan.ready(0, 0, 0, false, false));
        assertTrue(quotes.accept(confirmed, 1));
        WaypointUiParityTest.Port port = new WaypointUiParityTest.Port() {
            @Override
            public void close() {
                super.close();
                quotes.closeView();
            }
        };
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        session.bind(new WaypointUiParityTest.View());
        for (UiDetachReason reason : new UiDetachReason[]{UiDetachReason.RELOAD, UiDetachReason.RESIZE, UiDetachReason.SWITCH, UiDetachReason.FALLBACK}) {
            session.detach(reason);
            session.bind(new WaypointUiParityTest.View());
            assertSame(confirmed, quotes.display(target, 100).get());
        }
        CostQuoteRequest refresh = quotes.request(target, 600_000_000).get();
        session.detach(UiDetachReason.SWITCH);
        session.bind(new WaypointUiParityTest.View());
        assertSame(confirmed, quotes.display(target, 600_000_001).get());
        assertTrue(quotes.accept(CostQuote.ready(refresh, EnumCostType.NONE,
                CostPaymentPlan.ready(0, 0, 0, false, false)), 600_000_002));
        assertEquals(0, port.actions);
        session.close(UiCloseReason.DISCONNECT);
        assertFalse(quotes.display(target, 600_000_003).isPresent());
    }

    @Test
    public void nativeHandoffRetainsOriginalCostCoordinateAndPacketPaths() throws Exception {
        String nativePage = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/narcissus/screen/WaypointScreen.java")), java.nio.charset.StandardCharsets.UTF_8);
        String removed = nativePage.substring(nativePage.indexOf("public void removed()"), nativePage.indexOf("private String calculateCostDisplay"));
        assertFalse(removed.contains("closeView"));
        assertTrue(removed.contains("CommonUiPages.removed"));
        assertTrue(nativePage.contains("quotes.request(item.quoteTarget, now)"));
        assertTrue(nativePage.contains("quotes.display(item.quoteTarget, now)"));
        assertTrue(nativePage.contains("toXyzIntString(\",\")"));
        assertTrue(nativePage.contains("new WaypointTeleportToServer(type, name, dimension)"));
        assertTrue(nativePage.contains("WaypointDragModel.insertionIndex"));
        assertTrue(nativePage.contains("lastPlayerPos.fromVector3d(minecraft.player.position()).dimension(minecraft.player.level.dimension())"));
        for (String type : new String[]{"NarcissusListUiSession", "WaypointUiSession", "AccessListUiSession"}) {
            String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/narcissus/client/ui/" + type + ".java")), java.nio.charset.StandardCharsets.UTF_8);
            assertFalse(source.contains("PacketUtils"));
            assertFalse(source.contains("payCost"));
        }
    }
}
