package xin.vanilla.narcissus.command;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigCommandStructureTest {

    @Test
    public void exposesGenericCommonConfigEditing() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/narcissus/command/impl/ConfigCommand.java")), StandardCharsets.UTF_8);

        assertEquals(1, occurrences(source, "Commands.literal(\"common\")"));
        assertTrue(source.contains("CommandUtils.configKeySuggestion(commonConfig()"));
        assertTrue(source.contains("CommandUtils.configValueSuggestion(commonConfig()"));
        assertTrue(source.contains("CommandUtils.executeModifyConfig(commonConfig()"));
    }

    private static int occurrences(String value, String target) {
        int count = 0;
        for (int index = 0; (index = value.indexOf(target, index)) >= 0; index += target.length()) {
            count++;
        }
        return count;
    }
}
