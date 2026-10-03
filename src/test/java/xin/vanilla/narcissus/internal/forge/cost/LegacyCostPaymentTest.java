package xin.vanilla.narcissus.internal.forge.cost;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.platform.*;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ConfigBaselineFixture;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

/** Characterizes the legacy bugs until the coordinated T6 cutover; correct behavior lives in ForgeCostPaymentTest. */
public class LegacyCostPaymentTest {
    private RecordingPlayer player;
    private Object previousPlatform;
    private ConfigHolder holder;
    RecordingPlayer player() { return player; }

    @Before public void setup() throws Exception {
        Field platform = BaniraPlatforms.class.getDeclaredField("platform");
        platform.setAccessible(true);
        previousPlatform = platform.get(null);
        net.minecraft.util.registry.Bootstrap.bootStrap();
        player = allocate(RecordingPlayer.class);
        player.id = UUID.randomUUID();
        player.food = new FoodStats();
        player.food.setFoodLevel(10);
        player.health = 10;
        Field inventory = net.minecraft.entity.player.PlayerEntity.class.getDeclaredField("inventory");
        inventory.setAccessible(true);
        inventory.set(player, new net.minecraft.entity.player.PlayerInventory(player));
        player.totalExperience = 10;
        player.experienceLevel = 10;
        player.world = allocate(ServerWorld.class);
        Field dimension = World.class.getDeclaredField("dimension");
        dimension.setAccessible(true);
        dimension.set(player.world, World.OVERWORLD);
        player.connection = allocate(SilentConnection.class);
        Constructor<PlayerTeleportData> ctor = PlayerTeleportData.class.getDeclaredConstructor(net.minecraft.entity.player.PlayerEntity.class);
        ctor.setAccessible(true);
        PlayerTeleportData data = ctor.newInstance((Object) null);
        Field cache = PlayerTeleportData.class.getDeclaredField("CACHE");
        cache.setAccessible(true);
        ((Map<UUID, PlayerTeleportData>) cache.get(null)).put(player.id, data);
        CustomConfig.setPlayerLanguage(player.id.toString(), "en_us");
    }

    @After public void cleanup() throws Exception {
        PlayerTeleportData.clear();
        Field platform = BaniraPlatforms.class.getDeclaredField("platform");
        platform.setAccessible(true);
        platform.set(null, previousPlatform);
    }

    private void configure(EnumCostType type) throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put("base.teleportCard.teleportCard", true);
        values.put("base.teleportCard.teleportCardType", EnumCardType.NONE);
        values.put("cost.tpHome.costTpHomeType", type);
        values.put("cost.tpHome.costTpHomeNum", 5);
        values.put("cost.tpHome.costTpHomeExp", "5");
        values.put("cost.tpHome.costTpHomeConf", "test [num]");
        holder = ConfigBaselineFixture.holderWithValues(CommonConfig.class, values);
        BaniraConfigService configs = new BaniraConfigService() {
            public <T> void register(Class<T> config, String modId) { throw new UnsupportedOperationException(); }
            public <T> T view(Class<?> config, Class<T> view) { throw new UnsupportedOperationException(); }
            public BaniraConfigHandle handle(Class<?> config) { return config == CommonConfig.class ? holder : null; }
        };
        BaniraPlatforms.install((BaniraPlatform) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{BaniraPlatform.class}, (proxy, method, args) -> {
                    if (method.getName().equals("configService")) return configs;
                    if (method.getName().equals("networkService")) return Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[]{BaniraNetworkService.class}, (p, m, a) -> defaultValue(m.getReturnType()));
                    return defaultValue(method.getReturnType());
                }));
    }

    private boolean pay() throws Exception {
        Method method = NarcissusUtils.class.getDeclaredMethod("validateCost", ServerPlayerEntity.class,
                RegistryKey.class, double.class, EnumTeleportType.class, boolean.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, player, World.OVERWORLD, 10D, EnumTeleportType.TP_HOME, true);
    }

    @Test public void legacyMissingCardsIncorrectlyCreditExperiencePoints() throws Exception {
        configure(EnumCostType.EXP_POINT);
        assertTrue(pay());
        assertEquals(11, player.totalExperience);
    }

    @Test public void legacyMissingCardsIncorrectlyCreditExperienceLevels() throws Exception {
        configure(EnumCostType.EXP_LEVEL);
        assertTrue(pay());
        assertEquals(11, player.experienceLevel);
    }

    @Test public void legacyMissingCardsIncorrectlyCreditHealth() throws Exception {
        configure(EnumCostType.HEALTH);
        assertTrue(pay());
        assertEquals(11, player.health, 0);
    }

    @Test public void legacyMissingCardsIncorrectlyCreditFood() throws Exception {
        configure(EnumCostType.HUNGER);
        assertTrue(pay());
        assertEquals(11, player.food.getFoodLevel());
    }

    @Test public void legacyPositiveCommandFeeIsIncorrectlyRejected() throws Exception {
        configure(EnumCostType.COMMAND);
        PlayerTeleportData.getData(player).setTeleportCard(1);
        assertFalse(pay());
    }

    @Test public void legacyDisabledCardsIncorrectlyBypassCooldown() throws Exception {
        configure(EnumCostType.EXP_POINT);
        holder.set("base.teleportCard.teleportCard", false);
        holder.set("base.teleportCard.teleportCardType", EnumCardType.REFUND_COOLDOWN);
        PlayerTeleportData.getData(player).setTeleportCard(1);
        PlayerTeleportData.getData(player).setTeleportRecords(Collections.singletonList(
                new xin.vanilla.narcissus.data.TeleportRecord().setTeleportType(EnumTeleportType.TP_HOME)));
        assertEquals(0, NarcissusUtils.getTeleportCoolDown(player, EnumTeleportType.TP_HOME));
    }

    @Test public void legacyOffsetAndCooldownModeIsOmitted() throws Exception {
        configure(EnumCostType.EXP_POINT);
        holder.set("base.teleportCard.teleportCardType", EnumCardType.REFUND_COST_AND_COOLDOWN);
        PlayerTeleportData.getData(player).setTeleportCard(1);
        PlayerTeleportData.getData(player).setTeleportRecords(Collections.singletonList(
                new xin.vanilla.narcissus.data.TeleportRecord().setTeleportType(EnumTeleportType.TP_HOME)));
        assertTrue(NarcissusUtils.getTeleportCoolDown(player, EnumTeleportType.TP_HOME) > 0);
    }

    static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == String.class) return "fixture";
        if (type == Optional.class) return Optional.empty();
        return null;
    }

    static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    static class RecordingPlayer extends ServerPlayerEntity {
        UUID id;
        FoodStats food;
        float health;
        ServerWorld world;
        boolean cancelExperience;
        int experienceFactor;
        private RecordingPlayer() { super(null, null, null, null); }
        @Override public UUID getUUID() { return id; }
        @Override public ServerWorld getLevel() { return world; }
        @Override public FoodStats getFoodData() { return food; }
        @Override public float getHealth() { return health; }
        @Override public boolean isAlive() { return health > 0 && !removed; }
        @Override public void setHealth(float value) { health = value; }
        @Override public boolean isLocalPlayer() { return false; }
        @Override public void sendMessage(net.minecraft.util.text.ITextComponent message, UUID sender) { }
        @Override public void giveExperiencePoints(int amount) { if (!cancelExperience) totalExperience += amount * (experienceFactor == 0 ? 1 : experienceFactor); }
        @Override public void giveExperienceLevels(int amount) { if (!cancelExperience) experienceLevel += amount * (experienceFactor == 0 ? 1 : experienceFactor); }
        @Override public boolean hurt(DamageSource source, float amount) { health -= amount; return true; }
    }

    static class SilentConnection extends ServerPlayNetHandler {
        private SilentConnection() { super(null, null, null); }
        @Override public void send(IPacket<?> packet) { }
    }
}
