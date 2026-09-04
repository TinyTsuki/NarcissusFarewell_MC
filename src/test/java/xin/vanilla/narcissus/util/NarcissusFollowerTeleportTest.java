package xin.vanilla.narcissus.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarcissusFollowerTeleportTest {

    @Test
    public void crossDimensionFollowersAreRecreatedAtTheDestination() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/xin/vanilla/narcissus/util/NarcissusUtils.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("EntityType.loadEntityRecursive"));
        assertTrue(source.contains("destination.addFromAnotherDimension(moved)"));
        assertFalse(source.contains("Entity moved = entity.changeDimension(level)"));
    }
}
