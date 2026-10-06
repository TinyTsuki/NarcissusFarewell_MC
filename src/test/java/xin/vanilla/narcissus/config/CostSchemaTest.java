package xin.vanilla.narcissus.config;

import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.narcissus.config.migration.CostConfigMigration;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CostSchemaTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void everyCostGroupHasTheSameRegisteredFields() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        Set<String> paths = fixture.defaults.keySet();
        Set<String> fields = new HashSet<>(Arrays.asList("type", "fixedAmount", "perBlockAmount", "minAmount",
                "maxAmount", "item", "command", "custom.file"));
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            String prefix = "cost." + CostConfigMigration.groupName(type) + ".";
            assertEquals(type.name(), fields, paths.stream().filter(p -> p.startsWith(prefix))
                    .map(p -> p.substring(prefix.length())).collect(Collectors.toSet()));
            assertEquals("NONE", fixture.defaults.get(prefix + "type").toString());
            assertEquals(0D, fixture.defaults.get(prefix + "fixedAmount"));
            assertEquals(.002D, fixture.defaults.get(prefix + "perBlockAmount"));
            assertEquals(0, fixture.defaults.get(prefix + "minAmount"));
            assertEquals(20, fixture.defaults.get(prefix + "maxAmount"));
            for (String field : Arrays.asList("item", "command", "custom.file"))
                assertEquals("", fixture.defaults.get(prefix + field));
        }
        assertFalse(paths.stream().anyMatch(p -> p.startsWith("cost.tp") || p.startsWith("base.teleportCard.")
                || p.startsWith("base.teleportLimit.teleportCost")));
        assertEquals("WAIVE_COST", fixture.defaults.get("cost.cards.mode").toString());
        assertEquals(10000, fixture.defaults.get("cost.distance.maxDistance"));
    }

    @Test
    public void smallRatesAndUnlimitedCapsAreRepresentable() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        ConfigEntryDescriptor rate = fixture.holder.getDescriptors().stream()
                .filter(d -> d.getPath().equals("cost.home.perBlockAmount")).findFirst().get();
        assertTrue(rate.getDecimalPlaces() >= 4);
        fixture.holder.set("cost.home.perBlockAmount", .0002);
        fixture.holder.set("cost.home.maxAmount", -1);
        assertEquals(.0002, ((Number) fixture.holder.get("cost.home.perBlockAmount")).doubleValue(), 0);
        assertEquals(-1, ((Number) fixture.holder.get("cost.home.maxAmount")).intValue());
    }
}
