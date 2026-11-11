package xin.vanilla.narcissus.client.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.*;

public class UiResourceBudgetTest {
    @Test
    public void everyOwnedResourceUsesLocalSharedStylesAndStaysWithinBudget() throws Exception {
        Map<String, byte[]> files = UiBoundaryChecks.ownResources("narcissus_farewell");
        assertFalse(files.isEmpty());
        assertEquals(com.google.common.collect.ImmutableList.of(), UiBoundaryChecks.resources(files, false));
        for (lombok.var entry : files.entrySet())
            if (entry.getKey().endsWith(".html"))
                assertTrue(entry.getKey(), new String(entry.getValue(), StandardCharsets.UTF_8).contains("../banira_codex/shared.css"));
    }

    @Test
    public void oversizedScriptsFontsBitmapsAndRemoteResourcesAreRejected() {
        for (lombok.var fixture : com.google.common.collect.ImmutableList.of(com.google.common.collect.ImmutableMap.of("page.html", "<script src='https://example.invalid/a.js'></script>".getBytes(StandardCharsets.UTF_8)),
                com.google.common.collect.ImmutableMap.of("large.html", new byte[12289]), com.google.common.collect.ImmutableMap.of("mod.css", new byte[5121]), com.google.common.collect.ImmutableMap.of("font.woff", new byte[1]),
                com.google.common.collect.ImmutableMap.of("large.png", new byte[65537]), com.google.common.collect.ImmutableMap.of("large.js", new byte[8193])))
            assertFalse(UiBoundaryChecks.resources(fixture, false).isEmpty());
    }

    @Test
    public void copiedSharedStylesAndUnsafeReferencesAreRejected() {
        assertFalse(UiBoundaryChecks.resources(com.google.common.collect.ImmutableMap.of("shared.css", new byte[1]), false).isEmpty());
        assertFalse(UiBoundaryChecks.resources(com.google.common.collect.ImmutableMap.of("page.html", "<link href='../foreign/file.css'>".getBytes(StandardCharsets.UTF_8)), false).isEmpty());
    }

    @Test
    public void finiteAccessStylesAcceptTheExactBudgetAndRejectOneByteMore() {
        assertEquals(com.google.common.collect.ImmutableList.of(), UiBoundaryChecks.resources(com.google.common.collect.ImmutableMap.of("mod.css", new byte[5120]), false));
        assertFalse(UiBoundaryChecks.resources(com.google.common.collect.ImmutableMap.of("mod.css", new byte[5121]), false).isEmpty());
    }
}
