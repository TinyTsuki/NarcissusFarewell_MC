package xin.vanilla.narcissus;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/** 开发期同版本发布必须立即替换 Banira 依赖，避免 smoke 继续加载旧实现。 */
public class BaniraDependencyRefreshContractTest {
    @Test
    public void baniraDependenciesAreChangingAndUncached() throws Exception {
        String build = new String(Files.readAllBytes(Paths.get("build.gradle")), StandardCharsets.UTF_8);
        assertTrue(build.contains("cacheChangingModulesFor 0, 'seconds'"));
        assertChanging(build, "implementation");
        assertChanging(build, "baniraCodexObf");
    }

    private static void assertChanging(String build, String configuration) {
        Pattern declaration = Pattern.compile(
                configuration + "\\([^\\r\\n]*baniraCodexCoords[^\\r\\n]*\\)\\s*\\{[^}]*changing\\s*=\\s*true",
                Pattern.DOTALL);
        assertTrue(configuration + " Banira dependency must be changing", declaration.matcher(build).find());
    }
}
