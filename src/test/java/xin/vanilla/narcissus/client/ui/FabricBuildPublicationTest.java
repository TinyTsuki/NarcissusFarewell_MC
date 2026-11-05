package xin.vanilla.narcissus.client.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class FabricBuildPublicationTest {
    private String buildScript() throws Exception {
        return Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
    }

    @Test
    public void configurationCannotWriteClientFpsOptions() throws Exception {
        String build = buildScript();
        assertFalse(Pattern.compile("(?m)^\\s*prepareBaniraDevFpsOverlay\\(\\)\\s*$").matcher(build).find());
        assertTrue(build.contains("doLast prepareBaniraDevFpsOverlay"));
    }

    @Test
    public void clientRunStillPreparesTheDevelopmentOverlay() throws Exception {
        assertTrue(Pattern.compile("tasks\\.named\\('runClient'\\)\\.configure\\s*\\{\\s*dependsOn\\s+'prepareDevFpsOverlay'\\s*}")
                .matcher(buildScript()).find());
    }

    @Test
    public void publicationUsesTheUnobfuscatedArtifacts() throws Exception {
        String build = buildScript();
        assertTrue(build.contains("artifact tasks.named('jar')"));
        assertTrue(build.contains("artifact tasks.named('sourcesJar')"));
        assertFalse(build.contains("artifact tasks.named('remapJar')"));
    }

    @Test
    public void localPublicationWaitsForBothArchives() throws Exception {
        assertTrue(buildScript().contains("dependsOn 'jar', 'sourcesJar'"));
    }
}
