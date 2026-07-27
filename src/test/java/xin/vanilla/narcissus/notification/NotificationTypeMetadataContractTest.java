package xin.vanilla.narcissus.notification;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 锁定通知说明通过 Banira 公共入口登记，并覆盖所有水仙辞通知类型。
 */
public class NotificationTypeMetadataContractTest {

    @Test
    public void clientRegistrationUsesPublicMetadataApi() throws Exception {
        String source = read("src/main/java/xin/vanilla/narcissus/event/ClientModEventHandler.java");
        assertTrue(source.contains("BaniraClientNotificationTypes.registerModDisplayName("));
        assertTrue(source.contains("BaniraClientNotificationTypes.register("));
        assertFalse(source.contains("client.notification.NotificationTypeRegistry"));

        String[] keys = {
                "notification_type_teleport_request",
                "notification_type_teleport_guard",
                "notification_type_teleport_search",
                "notification_type_teleport_error",
                "notification_type_waypoint",
                "notification_type_interactive_tp_flow",
                "notification_type_interactive_share",
                "notification_type_interactive_help",
                "notification_type_interactive_coordinate_list",
                "notification_type_interactive_query"
        };
        String zh = read("src/main/resources/assets/narcissus_farewell/lang/zh_cn.json");
        String en = read("src/main/resources/assets/narcissus_farewell/lang/en_us.json");
        for (String key : keys) {
            assertTrue("Missing client registration for " + key, source.contains("\"" + key + "\""));
            assertTrue("Missing zh_cn translation for " + key, zh.contains("\"word.narcissus_farewell." + key + "\""));
            assertTrue("Missing en_us translation for " + key, en.contains("\"word.narcissus_farewell." + key + "\""));
        }
    }

    @Test
    public void safeTeleportDefaultsUseOnlyResourceIdStrings() throws Exception {
        String source = read("src/main/java/xin/vanilla/narcissus/config/CommonConfig.java");
        assertTrue(source.contains("\"minecraft:lava\", \"minecraft:fire\""));
        assertTrue(source.contains("\"minecraft:lava\", \"minecraft:water\""));
        assertTrue(source.contains("\"minecraft:grass_block\", \"minecraft:grass_path\", \"minecraft:dirt\""));
        assertFalse(source.contains("Stream.of(Blocks."));
        assertFalse(source.contains("BlockUtils::getBlockRegistryString"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
