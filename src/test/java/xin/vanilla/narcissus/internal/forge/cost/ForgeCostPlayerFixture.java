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

/** Native player fixture shared by payment and quote regressions. */
public class ForgeCostPlayerFixture {
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
        Field position = net.minecraft.entity.Entity.class.getDeclaredField("position");
        position.setAccessible(true);
        position.set(player, net.minecraft.util.math.vector.Vector3d.ZERO);
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
