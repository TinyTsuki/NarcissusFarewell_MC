package xin.vanilla.narcissus;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

/** 防止业务代码绕过 Banira 的稳定公共 API。 */
public class BaniraApiBoundaryTest {
    private static final String[] FORBIDDEN_SYMBOLS = {
            "BaniraServerUtils",
            "BaniraCodex.serverInstance()",
            "BaniraCodex.playerDataManager",
            "xin.vanilla.banira.internal"
    };

    @Test
    public void productionCodeUsesStableBaniraApi() throws IOException {
        Path root = Paths.get("src", "main", "java");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".java"))::iterator) {
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                for (String symbol : FORBIDDEN_SYMBOLS) {
                    if (source.contains(symbol)) {
                        violations.add(root.relativize(file) + " -> " + symbol);
                    }
                }
            }
        }
        if (!violations.isEmpty()) {
            fail("Production code must use Banira public APIs: " + String.join(", ", violations));
        }
    }
}
