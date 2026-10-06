package xin.vanilla.narcissus.internal.forge.cost;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import org.junit.After;
import org.junit.Before;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Native player fixture shared by payment and quote regressions.
 */
public class ForgeCostPlayerFixture {
    private RecordingPlayer player;
    private Object previousPlatform;
    private ConfigHolder holder;

    public RecordingPlayer player() {
        return player;
    }

    @Before
    public void setup() throws Exception {
        Field platform = BaniraPlatforms.class.getDeclaredField("platform");
        platform.setAccessible(true);
        previousPlatform = platform.get(null);
        try {
            xin.vanilla.narcissus.test.ForgeUnitTestBootstrap.bootstrap();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        player = allocate(RecordingPlayer.class);
        player.id = UUID.randomUUID();
        player.food = new FoodData();
        player.food.setFoodLevel(10);
        player.health = 10;
        Field inventory = net.minecraft.world.entity.player.Player.class.getDeclaredField("inventory");
        inventory.setAccessible(true);
        inventory.set(player, new net.minecraft.world.entity.player.Inventory(player));
        player.totalExperience = 10;
        player.experienceLevel = 10;
        player.world = allocate(ServerLevel.class);
        Field position = net.minecraft.world.entity.Entity.class.getDeclaredField("position");
        position.setAccessible(true);
        position.set(player, net.minecraft.world.phys.Vec3.ZERO);
        Field dimension = Level.class.getDeclaredField("dimension");
        dimension.setAccessible(true);
        dimension.set(player.world, Level.OVERWORLD);
        player.connection = allocate(SilentConnection.class);
        Constructor<PlayerTeleportData> ctor = PlayerTeleportData.class.getDeclaredConstructor(net.minecraft.world.entity.player.Player.class);
        ctor.setAccessible(true);
        PlayerTeleportData data = ctor.newInstance((Object) null);
        Field cache = PlayerTeleportData.class.getDeclaredField("CACHE");
        cache.setAccessible(true);
        ((Map<UUID, PlayerTeleportData>) cache.get(null)).put(player.id, data);
        CustomConfig.setPlayerLanguage(player.id.toString(), "en_us");
    }

    @After
    public void cleanup() throws Exception {
        PlayerTeleportData.clear();
        Field platform = BaniraPlatforms.class.getDeclaredField("platform");
        platform.setAccessible(true);
        platform.set(null, previousPlatform);
    }

    static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == String.class) return "fixture";
        if (type == Optional.class) return Optional.empty();
        return null;
    }

    public static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    public static class RecordingPlayer extends ServerPlayer {
        public UUID id;
        FoodData food;
        float health;
        public ServerLevel world;
        boolean cancelExperience;
        int experienceFactor;

        private RecordingPlayer() {
            super(null, null, null, null);
        }

        @Override
        public UUID getUUID() {
            return id;
        }

        @Override
        public ServerLevel serverLevel() {
            return world;
        }

        @Override
        public net.minecraft.core.RegistryAccess registryAccess() {
            return net.minecraft.core.RegistryAccess.EMPTY;
        }

        @Override
        public FoodData getFoodData() {
            return food;
        }

        @Override
        public float getHealth() {
            return health;
        }

        @Override
        public boolean isAlive() {
            return health > 0 && !isRemoved();
        }

        @Override
        public void setHealth(float value) {
            health = value;
        }

        @Override
        public boolean isLocalPlayer() {
            return false;
        }

        @Override
        public void sendSystemMessage(net.minecraft.network.chat.Component message) {
        }

        @Override
        public void giveExperiencePoints(int amount) {
            if (!cancelExperience) totalExperience += amount * (experienceFactor == 0 ? 1 : experienceFactor);
        }

        @Override
        public void giveExperienceLevels(int amount) {
            if (!cancelExperience) experienceLevel += amount * (experienceFactor == 0 ? 1 : experienceFactor);
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            health -= amount;
            return true;
        }
    }

    static class SilentConnection extends ServerGamePacketListenerImpl {
        private SilentConnection() {
            super(null, null, null, null);
        }

        @Override
        public void send(Packet<?> packet) {
        }
    }
}
