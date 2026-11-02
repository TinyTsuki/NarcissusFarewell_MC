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
        assertEquals("access_request_ignored", key("tpaKey", "a", com.google.common.collect.ImmutableSet.of("a"), com.google.common.collect.ImmutableSet.of("a"), com.google.common.collect.ImmutableSet.of("a"), com.google.common.collect.ImmutableSet.of("a")));
    }

    @Test
    public void nonemptyWhitelistDoesNotAdmitAnOutsider() throws Exception {
        assertEquals("access_request_ignored", key("tphKey", "b", com.google.common.collect.ImmutableSet.of(), com.google.common.collect.ImmutableSet.of("a"), com.google.common.collect.ImmutableSet.of("b"), com.google.common.collect.ImmutableSet.of("b")));
    }

    @Test
    public void emptyWhitelistAdmitsUnblockedManualRequests() throws Exception {
        assertEquals("access_request_manual", key("tpaKey", "b", com.google.common.collect.ImmutableSet.of("a"), com.google.common.collect.ImmutableSet.of(), com.google.common.collect.ImmutableSet.of(), com.google.common.collect.ImmutableSet.of()));
    }

    @Test
    public void noExtraModeIsManualRatherThanIgnored() throws Exception {
        assertEquals("access_request_manual", key("tpaKey", "a", com.google.common.collect.ImmutableSet.of(), com.google.common.collect.ImmutableSet.of("a"), com.google.common.collect.ImmutableSet.of(), com.google.common.collect.ImmutableSet.of()));
        assertEquals("access_request_manual", key("tphKey", "a", com.google.common.collect.ImmutableSet.of(), com.google.common.collect.ImmutableSet.of("a"), com.google.common.collect.ImmutableSet.of(), com.google.common.collect.ImmutableSet.of()));
    }

    @Test
    public void automaticModesAreIndependentAndDoNotMutateSets() throws Exception {
        Set<String> white = com.google.common.collect.ImmutableSet.of("a"), tpa = com.google.common.collect.ImmutableSet.of("a"), tph = com.google.common.collect.ImmutableSet.of();
        assertEquals("access_request_auto", key("tpaKey", "a", com.google.common.collect.ImmutableSet.of(), white, tpa, tph));
        assertEquals("access_request_manual", key("tphKey", "a", com.google.common.collect.ImmutableSet.of(), white, tpa, tph));
        assertEquals(com.google.common.collect.ImmutableSet.of("a"), white);
        assertEquals(com.google.common.collect.ImmutableSet.of("a"), tpa);
        assertTrue(tph.isEmpty());
    }
}
