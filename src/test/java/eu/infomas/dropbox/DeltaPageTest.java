package eu.infomas.dropbox;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONValue;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

public final class DeltaPageTest {

    private static String JSON;

    @BeforeClass
    public static void loadTestData() throws IOException {
        JSON = Utils.toString(
            Utils.getResourceAsStream("classpath:/Delta.json", DeltaPageTest.class),
            Utils.UTF8);
    }

    @Test
    public void test() {
        Map map = (Map) JSONValue.parse(JSON);
        DeltaPage page = new DeltaPage(map);

        assertEquals("AlwtMrpkbdJ8Z8379YMlQlxsGh2GuHty0afhxOh5zTpQeJxjrXv8mYHR4yzHlxOn" +
            "GBjYdRmAAABTJAYy", page.getCursor());
        assertTrue(page.isReset());
        final List<DeltaEntry> entries = page.getEntries();
        final DeltaEntry e0 = entries.get(0);
        assertEquals("/digipub", e0.getLowerCasedPath());
        assertEquals("/digipub", e0.getMetadata().getPath());
        assertEquals(0, e0.getMetadata().getBytes());
        assertEquals("1208f4c8ca", e0.getMetadata().getRev());
        assertFalse(page.hasMore());
    }
}
