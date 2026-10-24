package xin.vanilla.narcissus.client.ui;

import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class WaypointUiPresentationTest {
    @BeforeClass public static void bootstrap() { WaypointUiParityTest.bootstrap(); }
    static class Port extends WaypointUiParityTest.Port implements WaypointUiSession.PresentationPort {
        String amount="9 XP", note="", name="A long point name";
        int legacyCostReads;
        @Override public String cost() { legacyCostReads++; return super.cost(); }
        public WaypointUiPresentation presentation() {
            return new WaypointUiPresentation(name,"Overworld","minecraft:overworld","-1","64","-2",
                    "125.3m","2026-10-24",amount,note,"","Private 2",confirming,tab==2);
        }
        @Override public void action(NarcissusListUiSession.Action action) {
            super.action(action);
            if (action == NarcissusListUiSession.Action.CANCEL) confirming=false;
        }
    }
    @Test public void structuredDetailCostAndTooltipsAreBoundFromTheLivePort() {
        Port p=new Port(); WaypointUiParityTest.View v=new WaypointUiParityTest.View();
        WaypointUiSession s=new WaypointUiSession(p,()->true);s.bind(v);
        assertEquals(p.name,v.text("point_name")); assertEquals(p.name,v.tooltip("point_name"));
        assertEquals("-1",v.text("coordinate_x")); assertEquals("minecraft:overworld",v.text("dimension_id"));
        assertEquals("9 XP",v.text("cost")); assertEquals("Private 2",v.text("footer_status"));
    }
    @Test public void quoteFailureIsDisplayOnlyAndNeverCreatesAnotherTeleportGate() {
        Port p=new Port(); p.amount="Unavailable";p.note="Not enough resources";
        WaypointUiParityTest.View v=new WaypointUiParityTest.View();WaypointUiSession s=new WaypointUiSession(p,()->true);s.bind(v);
        assertEquals(p.note,v.text("cost_note")); assertTrue(v.enabled("teleport"));
        v.activate("teleport");assertEquals(1,p.actions);
    }
    @Test public void deleteConfirmationLocksSelectionAndSearchUntilCancelled() {
        Port p=new Port();WaypointUiParityTest.View v=new WaypointUiParityTest.View();WaypointUiSession s=new WaypointUiSession(p,()->true);s.bind(v);
        v.activate("delete");assertTrue(v.enabled("confirmation"));
        var row=new WaypointUiParityTest.View();v.rows().bind(row,1);
        row.activate("open");v.inputDraft("search","blocked");v.activate("tab2");
        assertEquals("dimension:home",p.selected);assertEquals("",p.query);assertEquals(0,p.tab);
        v.activate("cancel");assertFalse(v.enabled("confirmation"));
    }
    @Test public void rebindKeepsTheTypedDetailWithoutSubmittingAndDisconnectClearsQuote() {
        Port p=new Port();AtomicBoolean live=new AtomicBoolean(true);WaypointUiSession s=new WaypointUiSession(p,live::get);
        WaypointUiParityTest.View first=new WaypointUiParityTest.View();s.bind(first);first.inputDraft("search","retained");
        s.detach(UiDetachReason.SWITCH);WaypointUiParityTest.View next=new WaypointUiParityTest.View();s.bind(next);
        assertEquals("retained",next.draft("search"));assertEquals(p.name,next.text("point_name"));assertEquals(0,p.actions);
        live.set(false);s.tick();assertEquals("",next.text("cost"));assertFalse(next.enabled("teleport"));
        first.activate("teleport");assertEquals(0,p.actions);
    }
    @Test public void structuredCostDoesNotRewriteTheLegacyLabelEveryTick() {
        Port p=new Port();WaypointUiSession s=new WaypointUiSession(p,()->true);s.bind(new WaypointUiParityTest.View());
        for(int i=0;i<20;i++)s.tick();
        assertEquals(0,p.legacyCostReads);
    }
    @Test public void footerShowsHistoryStateWithoutRepeatingUsageInstructions() {
        Port p=new Port();WaypointUiParityTest.View v=new WaypointUiParityTest.View();WaypointUiSession s=new WaypointUiSession(p,()->true);s.bind(v);
        assertEquals("",v.text("ordering_hint"));
        p.tab=2;p.revision++;s.tick();
        assertEquals(p.label("history_order"),v.text("ordering_hint"));
    }
    @Test public void escapeCancelsOnlyTheCurrentConfirmationAndDetachedKeysCannotReplay() {
        Port p=new Port();WaypointUiSession s=new WaypointUiSession(p,()->true);WaypointUiParityTest.View v=new WaypointUiParityTest.View();s.bind(v);
        v.activate("delete");assertTrue(s.key(256,0,0,true));assertFalse(p.confirming);assertEquals(2,p.actions);
        s.detach(UiDetachReason.SWITCH);p.confirming=true;assertFalse(s.key(256,0,0,true));assertEquals(2,p.actions);
    }
    @Test public void refusedDeletionReleasesRowLockEvenWithoutAnotherPortRevision() {
        Port p=new Port(){
            @Override public void action(NarcissusListUiSession.Action action) {
                if(action==NarcissusListUiSession.Action.CONFIRM){actions++;confirming=false;return;}
                super.action(action);
            }
        };
        WaypointUiSession s=new WaypointUiSession(p,()->true);WaypointUiParityTest.View v=new WaypointUiParityTest.View();s.bind(v);
        v.activate("delete");var locked=v.rows();var row=new WaypointUiParityTest.View();locked.bind(row,0);assertFalse(row.enabled("open"));
        long revision=p.revision;v.activate("confirm");assertEquals(revision,p.revision);
        assertNotSame("Confirmation state must invalidate realized row locks",locked,v.rows());
        var fresh=new WaypointUiParityTest.View();v.rows().bind(fresh,1);assertTrue(fresh.enabled("open"));fresh.activate("open");assertEquals("other-dimension:home",p.selected);
    }
}
