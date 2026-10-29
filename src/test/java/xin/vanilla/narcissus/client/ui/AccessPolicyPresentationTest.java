package xin.vanilla.narcissus.client.ui;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class AccessPolicyPresentationTest {
    private String key(String method, String id, Set<String> black, Set<String> white, Set<String> tpa, Set<String> tph) throws Exception {
        Class<?> type;
        try {
            type = Class.forName("xin.vanilla.narcissus.client.ui.AccessPolicyPresentation");
        } catch (ClassNotFoundException failure) {
            fail("Missing display-only access policy");
            return "";
        }
        Object result = type.getMethod("of", String.class, Set.class, Set.class, Set.class, Set.class).invoke(null, id, black, white, tpa, tph);
        return (String) type.getMethod(method).invoke(result);
    }

    @Test
    public void blacklistWinsEvenWithAutomaticFlags() throws Exception {
        assertEquals("access_request_ignored", key("tpaKey", "a", Set.of("a"), Set.of("a"), Set.of("a"), Set.of("a")));
    }

    @Test
    public void nonemptyWhitelistDoesNotAdmitAnOutsider() throws Exception {
        assertEquals("access_request_ignored", key("tphKey", "b", Set.of(), Set.of("a"), Set.of("b"), Set.of("b")));
    }

    @Test
    public void emptyWhitelistAdmitsUnblockedManualRequests() throws Exception {
        assertEquals("access_request_manual", key("tpaKey", "b", Set.of("a"), Set.of(), Set.of(), Set.of()));
    }

    @Test
    public void noExtraModeIsManualRatherThanIgnored() throws Exception {
        assertEquals("access_request_manual", key("tpaKey", "a", Set.of(), Set.of("a"), Set.of(), Set.of()));
        assertEquals("access_request_manual", key("tphKey", "a", Set.of(), Set.of("a"), Set.of(), Set.of()));
    }

    @Test
    public void automaticModesAreIndependentAndDoNotMutateSets() throws Exception {
        Set<String> white = Set.of("a"), tpa = Set.of("a"), tph = Set.of();
        assertEquals("access_request_auto", key("tpaKey", "a", Set.of(), white, tpa, tph));
        assertEquals("access_request_manual", key("tphKey", "a", Set.of(), white, tpa, tph));
        assertEquals(Set.of("a"), white);
        assertEquals(Set.of("a"), tpa);
        assertTrue(tph.isEmpty());
    }
}
