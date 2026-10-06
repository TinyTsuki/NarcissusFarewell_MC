package xin.vanilla.narcissus.config;

import com.google.gson.stream.JsonReader;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ConfigurationDeclarationContractTest {
    @Test
    public void declarationsLeaveRuntimeAccessToGeneratedViews() {
        verifyDeclaration(CommonConfig.class);
        verifyDeclaration(ClientConfig.class);
    }

    @Test
    public void englishKeysHaveThreeNaturallySortedSections() throws Exception {
        verifyLanguage("en_us");
    }

    @Test
    public void chineseKeysHaveThreeNaturallySortedSections() throws Exception {
        verifyLanguage("zh_cn");
    }

    @Test
    public void numericPartsUseNaturalOrder() {
        assertTrue(compare("word.example9_suffix", "word.example10_suffix") < 0);
        assertTrue(compare("word.example10_suffix", "word.example9_suffix") > 0);
        assertTrue(compare("word.example01", "word.example1") < 0);
        assertTrue(compare("format.z", "word.a") < 0);
        assertTrue(compare("key.z", "format.a") < 0);
    }

    private static void verifyDeclaration(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            assertTrue("Declaration bypasses the live configuration View: " + method,
                    !Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers()));
        }
        for (Class<?> nested : type.getDeclaredClasses()) verifyDeclaration(nested);
    }

    private static void verifyLanguage(String language) throws Exception {
        String resource = "/assets/narcissus_farewell/lang/" + language + ".json";
        try (InputStream stream = ConfigurationDeclarationContractTest.class.getResourceAsStream(resource)) {
            assertNotNull(resource, stream);
            try (JsonReader reader = new JsonReader(new InputStreamReader(stream,
                    StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)))) {
                Set<String> keys = new HashSet<>();
                String previous = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    assertTrue("Duplicate key: " + key, keys.add(key));
                    if (previous != null) {
                        assertTrue(language + ": " + previous + " must precede " + key,
                                compare(previous, key) < 0);
                    }
                    assertFalse("Replacement character in " + key, reader.nextString().contains("\uFFFD"));
                    previous = key;
                }
                reader.endObject();
                assertTrue(keys.size() > 200);
            }
        }
    }

    private static int group(String key) {
        return key.startsWith("format.") ? 1 : key.startsWith("word.") ? 2 : 0;
    }

    private static int compare(String left, String right) {
        int result = Integer.compare(group(left), group(right));
        if (result != 0) return result;
        int a = 0, b = 0;
        while (a < left.length() && b < right.length()) {
            char x = left.charAt(a), y = right.charAt(b);
            if (x >= '0' && x <= '9' && y >= '0' && y <= '9') {
                int endA = a, endB = b;
                while (endA < left.length() && left.charAt(endA) >= '0' && left.charAt(endA) <= '9') endA++;
                while (endB < right.length() && right.charAt(endB) >= '0' && right.charAt(endB) <= '9') endB++;
                result = new BigInteger(left.substring(a, endA)).compareTo(new BigInteger(right.substring(b, endB)));
                if (result != 0) return result;
                a = endA;
                b = endB;
            } else {
                result = Character.compare(x, y);
                if (result != 0) return result;
                a++;
                b++;
            }
        }
        result = Integer.compare(left.length() - a, right.length() - b);
        return result == 0 ? left.compareTo(right) : result;
    }
}
