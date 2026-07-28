package xin.vanilla.narcissus;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import org.junit.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModMetadataDependencyContractTest {
    private static final String MOD_ID = "narcissus_farewell";
    private static final Path PROCESSED_METADATA =
            Paths.get("build/resources/main/META-INF/neoforge.mods.toml");

    @Test
    public void dependencyGraphDoesNotContainCurrentMod() throws Exception {
        List<? extends Config> dependencies = dependencies();

        assertFalse("A mod dependency on itself creates a cyclic loader graph",
                dependencies.stream().anyMatch(this::isCurrentMod));
        assertTrue("Banira Codex must remain a required dependency",
                dependencies.stream().anyMatch(this::isRequiredBaniraDependency));
    }

    private List<? extends Config> dependencies() throws Exception {
        // Parse the processed metadata so variable expansion is covered by the contract.
        try (Reader reader = Files.newBufferedReader(PROCESSED_METADATA, StandardCharsets.UTF_8)) {
            Config metadata = new TomlParser().parse(reader);
            return metadata.get("dependencies." + MOD_ID);
        }
    }

    private boolean isCurrentMod(Config dependency) {
        return MOD_ID.equals(dependency.<String>get("modId"));
    }

    private boolean isRequiredBaniraDependency(Config dependency) {
        return "banira_codex".equals(dependency.<String>get("modId"))
                && "required".equals(dependency.<String>get("type"));
    }
}
