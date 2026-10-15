package xin.vanilla.narcissus.integration.map;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
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
        Method factory = WaypointFactory.class.getMethod("createWaypoint", String.class, BlockPos.class,
                String.class, String.class, boolean.class);
        if (!Modifier.isStatic(factory.getModifiers()) || factory.getReturnType() != Waypoint.class)
            throw new NoSuchMethodException("WaypointFactory.createWaypoint");
        Method groupFactory = WaypointFactory.class.getMethod("createWaypointGroup", String.class, String.class);
        if (!Modifier.isStatic(groupFactory.getModifiers()) || groupFactory.getReturnType() != WaypointGroup.class)
            throw new NoSuchMethodException("WaypointFactory.createWaypointGroup");
        LegacyJourneyMapAdapter.method(IClientAPI.class, "getDataPath", File.class, String.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "getWaypoints", List.class, String.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "addWaypoint", void.class, String.class, Waypoint.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "removeWaypoint", void.class, String.class, Waypoint.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "getWaypointGroups", List.class, String.class);
        LegacyJourneyMapAdapter.method(IClientAPI.class, "addWaypointGroup", void.class, WaypointGroup.class);
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "getGuid", String.class);
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "getModId", String.class);
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "getCustomData", String.class, String.class);
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "setCustomData", void.class, String.class, String.class);
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "setPersistent", void.class, boolean.class);
        LegacyJourneyMapAdapter.method(WaypointGroup.class, "addWaypoint", boolean.class, Waypoint.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getGroupId", String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getModId", String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getName", String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getDimensions", TreeSet.class);
        for (String getter : new String[]{"getX", "getY", "getZ"})
            LegacyJourneyMapAdapter.method(Waypoint.class, getter, int.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "getCustomData", String.class, String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "setCustomData", void.class, String.class, String.class);
        LegacyJourneyMapAdapter.method(Waypoint.class, "setColor", void.class, int.class);
    }

    private record ApiAdapter(IClientAPI api) implements JourneyMap.Adapter {
        @Override
        public boolean ready() {
            return api.getDataPath(JourneyMap.MOD_ID) != null;
        }

        @Override
        public void add(JourneyMap.Point point) {
            for (Waypoint waypoint : api.getWaypoints(JourneyMap.MOD_ID)) if (matches(waypoint, point)) return;
            Waypoint waypoint = WaypointFactory.createWaypoint(JourneyMap.MOD_ID,
                    new BlockPos(point.x(), point.y(), point.z()), point.name(), point.dimension(), true);
            if (!JourneyMap.MOD_ID.equals(waypoint.getModId())) throw new IllegalStateException("Waypoint owner mismatch");
            waypoint.setColor(0x00FF00);
            waypoint.setCustomData(KIND, point.kind());
            WaypointGroup group = group(point.kind());
            group.addWaypoint(waypoint);
            // 6.0.7 returns false even after a successful transfer; verify the resulting group instead.
            if (group.getGuid() == null || group.getGuid().isEmpty() || !group.getGuid().equals(waypoint.getGroupId()))
                throw new IllegalStateException("JourneyMap waypoint group transfer rejected");
            api.addWaypoint(JourneyMap.MOD_ID, waypoint);
        }

        private WaypointGroup group(String kind) {
            for (WaypointGroup group : api.getWaypointGroups(JourneyMap.MOD_ID)) {
                if (JourneyMap.MOD_ID.equals(group.getModId()) && kind.equals(group.getCustomData(KIND))) return group;
            }
            String key = switch (kind) {
                case "HOME" -> "enum_teleport_type_tp_home";
                case "STAGE" -> "enum_teleport_type_tp_stage";
                default -> "mod_name";
            };
            WaypointGroup group = WaypointFactory.createWaypointGroup(JourneyMap.MOD_ID,
                    Component.translatable("word.narcissus_farewell." + key).getString());
            if (!JourneyMap.MOD_ID.equals(group.getModId())) throw new IllegalStateException("Waypoint group owner mismatch");
            group.setPersistent(true);
            group.setCustomData(KIND, kind);
            api.addWaypointGroup(group);
            return group;
        }

        @Override
        public void remove(JourneyMap.Point point) {
            for (Waypoint waypoint : new ArrayList<>(api.getWaypoints(JourneyMap.MOD_ID))) {
                if (matches(waypoint, point)) api.removeWaypoint(JourneyMap.MOD_ID, waypoint);
            }
        }

        private boolean matches(Waypoint waypoint, JourneyMap.Point point) {
            return point.kind().equals(waypoint.getCustomData(KIND))
                    && JourneyMap.matches(waypoint.getModId(), waypoint.getName(), waypoint.getDimensions(),
                    waypoint.getX(), waypoint.getY(), waypoint.getZ(),
                    point.name(), point.dimension(), point.x(), point.y(), point.z());
        }
    }
}
