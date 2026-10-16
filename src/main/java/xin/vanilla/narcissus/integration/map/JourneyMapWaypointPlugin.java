package xin.vanilla.narcissus.integration.map;

import com.google.gson.JsonArray;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import journeymap.api.v2.common.waypoint.WaypointGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapWaypointPlugin implements IClientPlugin {
    private static final String KIND = "narcissus_farewell.kind";

    @Override
    public String getModId() {
        return JourneyMap.MOD_ID;
    }

    @Override
    public void initialize(IClientAPI api) {
        try {
            validateContract();
            JourneyMap.install(new ApiAdapter(api));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            JourneyMap.fail(error);
        }
    }

    private static void validateContract() throws ReflectiveOperationException {
        Method factory = WaypointFactory.class.getMethod("createClientWaypoint", String.class, BlockPos.class,
                String.class, String.class, boolean.class);
        if (!Modifier.isStatic(factory.getModifiers()) || factory.getReturnType() != Waypoint.class)
            throw new NoSuchMethodException("WaypointFactory.createClientWaypoint");
        Method groupFactory = WaypointFactory.class.getMethod("createWaypointGroup", String.class, String.class);
        if (!Modifier.isStatic(groupFactory.getModifiers()) || groupFactory.getReturnType() != WaypointGroup.class)
            throw new NoSuchMethodException("WaypointFactory.createWaypointGroup");
        LegacyJourneyMapAdapter.method(IClientAPI.class, "getDataPath", File.class, String.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "getWaypoints", List.class, String.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "getWaypoint", Waypoint.class, String.class, String.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "addWaypoint", void.class, String.class, Waypoint.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "removeWaypoint", void.class, String.class, Waypoint.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "getWaypointGroups", List.class, String.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "addWaypointGroup", void.class, WaypointGroup.class);
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "getGuid", String.class);
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "getModId", String.class);
        ApiData.bind(WaypointGroup.class);
        groupPersistence();
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "addWaypoint", boolean.class, Waypoint.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getGroupId", String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getGuid", String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getModId", String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getName", String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getDimensions", TreeSet.class);
        for (String getter : new String[]{"getX", "getY", "getZ"})
            LegacyJourneyMapAdapter.method(Waypoint.class, getter, int.class);
        ApiData.bind(Waypoint.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "setColor", void.class, int.class);
    }

    private static Method groupPersistence() throws ReflectiveOperationException {
        try {
            return LegacyJourneyMapAdapter.method(WaypointGroup.class, "setPersistent", void.class, boolean.class);
        } catch (NoSuchMethodException absent) {
            WaypointGroup.class.getMethod("getCustomData");
            return null;
        }
    }

    private record ApiData(Method read, Method write, boolean keyed) {
        static ApiData bind(Class<?> contract) throws ReflectiveOperationException {
            try {
                contract.getMethod("getCustomData", String.class);
            } catch (NoSuchMethodException absent) {
                return new ApiData(LegacyJourneyMapAdapter.method(contract, "getCustomData", String.class),
                        LegacyJourneyMapAdapter.method(contract, "setCustomData", void.class, String.class), false);
            }
            return new ApiData(LegacyJourneyMapAdapter.method(contract, "getCustomData", String.class, String.class),
                    LegacyJourneyMapAdapter.method(contract, "setCustomData", void.class, String.class, String.class), true);
        }

        String get(Object target) throws ReflectiveOperationException {
            String value = (String) LegacyJourneyMapAdapter.call(read, target, keyed ? new Object[]{KIND} : new Object[0]);
            return keyed ? value : value != null && value.startsWith(KIND + "=") ? value.substring(KIND.length() + 1) : null;
        }

        void set(Object target, String value) throws ReflectiveOperationException {
            LegacyJourneyMapAdapter.call(write, target, keyed ? new Object[]{KIND, value} : new Object[]{KIND + "=" + value});
        }
    }

    private static final class ApiAdapter implements JourneyMap.Adapter {
        private final IClientAPI api;
        private final ApiData points;
        private final ApiData groups;
        private final Method persistent;

        private ApiAdapter(IClientAPI api) throws ReflectiveOperationException {
            this.api = api;
            points = ApiData.bind(Waypoint.class);
            groups = ApiData.bind(WaypointGroup.class);
            persistent = groupPersistence();
        }
        @Override
        public boolean ready() {
            return api.getDataPath(JourneyMap.MOD_ID) != null;
        }

        @Override
        public void add(JourneyMap.Point point) throws ReflectiveOperationException {
            for (Waypoint waypoint : api.getWaypoints(JourneyMap.MOD_ID)) if (matches(waypoint, point)) return;
            Waypoint waypoint = WaypointFactory.createClientWaypoint(JourneyMap.MOD_ID,
                    new BlockPos(point.x(), point.y(), point.z()), point.name(), point.dimension(), true);
            if (!JourneyMap.MOD_ID.equals(waypoint.getModId())) throw new IllegalStateException("Waypoint owner mismatch");
            waypoint.setColor(0x00FF00);
            points.set(waypoint, point.kind());
            WaypointGroup group = group(point);
            try {
                if (points.keyed()) {
                    group.addWaypoint(waypoint);
                } else {
                    Method assign = LegacyJourneyMapAdapter.method(waypoint.getClass(), "setGroupId", void.class, String.class);
                    LegacyJourneyMapAdapter.call(assign, waypoint, group.getGuid());
                }
                // Native transfer return values are inconsistent; verify identity and registration.
                if (group.getGuid() == null || group.getGuid().isEmpty() || !group.getGuid().equals(waypoint.getGroupId()))
                    throw new IllegalStateException("JourneyMap waypoint group transfer rejected");
                api.addWaypoint(JourneyMap.MOD_ID, waypoint);
                Waypoint registered = api.getWaypoint(JourneyMap.MOD_ID, waypoint.getGuid());
                if (registered == null || !group.getGuid().equals(registered.getGroupId()) || !matches(registered, point))
                    throw new IllegalStateException("JourneyMap waypoint registration rejected");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                try {
                    api.removeWaypoint(JourneyMap.MOD_ID, waypoint);
                } catch (RuntimeException | LinkageError cleanup) {
                    if (cleanup != error) error.addSuppressed(cleanup);
                }
                throw error;
            }
        }

        private WaypointGroup group(JourneyMap.Point point) throws ReflectiveOperationException {
            String kind = point.kind();
            String identity = kind;
            if (!points.keyed()) {
                JsonArray value = new JsonArray();
                value.add(kind);
                value.add(point.name());
                identity = value.toString();
            }
            for (WaypointGroup group : api.getWaypointGroups(JourneyMap.MOD_ID)) {
                if (JourneyMap.MOD_ID.equals(group.getModId()) && identity.equals(groups.get(group))) return group;
            }
            String key = switch (kind) {
                case "HOME" -> "enum_teleport_type_tp_home";
                case "STAGE" -> "enum_teleport_type_tp_stage";
                default -> "mod_name";
            };
            WaypointGroup group = WaypointFactory.createWaypointGroup(JourneyMap.MOD_ID,
                    Component.translatable("word.narcissus_farewell." + key).getString()
                            + (points.keyed() ? "" : " - " + point.name()));
            if (!JourneyMap.MOD_ID.equals(group.getModId())) throw new IllegalStateException("Waypoint group owner mismatch");
            if (persistent != null) LegacyJourneyMapAdapter.call(persistent, group, true);
            groups.set(group, identity);
            api.addWaypointGroup(group);
            return group;
        }

        @Override
        public void remove(JourneyMap.Point point) throws ReflectiveOperationException {
            for (Waypoint waypoint : new ArrayList<>(api.getWaypoints(JourneyMap.MOD_ID))) {
                if (matches(waypoint, point)) api.removeWaypoint(JourneyMap.MOD_ID, waypoint);
            }
        }

        private boolean matches(Waypoint waypoint, JourneyMap.Point point) throws ReflectiveOperationException {
            return point.kind().equals(points.get(waypoint))
                    && JourneyMap.matches(waypoint.getModId(), waypoint.getName(), waypoint.getDimensions(),
                    waypoint.getX(), waypoint.getY(), waypoint.getZ(),
                    point.name(), point.dimension(), point.x(), point.y(), point.z());
        }
    }
}
