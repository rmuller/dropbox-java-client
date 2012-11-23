package eu.infomas.dropbox;

import java.io.IOException;
import java.util.Map;
import static eu.infomas.dropbox.Utils.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

public final class AccountTest {

    private static String JSON;

    @BeforeClass
    public static void loadTestData() throws IOException {
        JSON = Utils.toString(
            Utils.getResourceAsStream("classpath:/Account.json", EntryTest.class),
            Utils.UTF8);
        //Log.info(JSON);
    }

    @Test
    public void test() throws IOException {
        Map map = parseJson(JSON, Map.class);
        Account account = Account.valueOf(map);

        assertEquals(174L, account.getUid());
        assertEquals("", account.getCountry());
        assertEquals("John Q. User", account.getDisplayName());
        assertEquals(62277025792L, account.getQuota());
    }
}
