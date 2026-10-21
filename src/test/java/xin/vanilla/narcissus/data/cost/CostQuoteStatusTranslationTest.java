package xin.vanilla.narcissus.data.cost;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.Assert.*;

public class CostQuoteStatusTranslationTest {
    @Test
    public void statusDescriptionUsesSideAwareTranslationWithItsExistingKey() throws Exception {
        String source = Files.readString(Path.of("src/main/java/xin/vanilla/narcissus/data/cost/CostQuote.java"));
        String status = source.substring(source.indexOf("public enum Status"), source.indexOf("public CostQuote("));
        assertTrue("Status must follow the client language and retain server translation",
                status.contains("EnumDescriptionHelper.transAutoDesc("));
        assertTrue(status.contains("\"enum_cost_quote_status_\" + name().toLowerCase(Locale.ROOT)"));
        assertFalse("The nested enum needs its explicit key prefix", status.contains("describeEnum("));
        assertFalse(status.contains(".trans("));
    }

    @Test
    public void everyStatusRetainsBothLanguageEntries() throws Exception {
        for (String language : new String[]{"zh_cn", "en_us"}) {
            JsonObject translations = JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/assets/narcissus_farewell/lang/" + language + ".json"))).getAsJsonObject();
            for (CostQuote.Status status : CostQuote.Status.values()) {
                String key = "word.narcissus_farewell.enum_cost_quote_status_" + status.name().toLowerCase(Locale.ROOT);
                assertTrue(language + ": " + key, translations.has(key));
                assertFalse(translations.get(key).getAsString().isEmpty());
            }
        }
    }

    @Test
    public void statusWireOrderRemainsUnchanged() {
        assertArrayEquals(new String[]{"READY", "UNKNOWN", "UNAVAILABLE", "REJECTED", "RATE_LIMITED"},
                java.util.Arrays.stream(CostQuote.Status.values()).map(Enum::name).toArray(String[]::new));
    }
}
