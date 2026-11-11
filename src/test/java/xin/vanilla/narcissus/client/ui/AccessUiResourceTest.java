package xin.vanilla.narcissus.client.ui;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class AccessUiResourceTest {
    private String resource(String name) throws Exception {
        return Files.readString(Path.of("src/main/resources/assets/apricityui/apricity/narcissus_farewell/" + name));
    }


    @Test
    public void absoluteRowTextReservesTheProfileIconColumn() throws Exception {
        String css = resource("access.css");
        assertTrue(css.contains(".access .row-action{padding-left:32px}"));
        assertTrue(css.contains(".access .row .label{left:32px}"));
    }


}
