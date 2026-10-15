package xin.vanilla.narcissus.integration.map;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import journeymap.api.v2.common.waypoint.WaypointGroup;
import net.minecraft.core.BlockPos;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

public class JourneyMapApiWaypointTest {
    private final Map<String, Waypoint> points = new LinkedHashMap<>();
    private final Map<String, WaypointGroup> groups = new LinkedHashMap<>();
    private Object previousFactory;
    private Field factoryField;
    private JourneyMap.Adapter adapter;
    private IClientAPI api;

    @BeforeClass public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before public void setup() throws Exception {
        factoryField = WaypointFactory.class.getDeclaredField("INSTANCE");
        factoryField.setAccessible(true);
        previousFactory = factoryField.get(null);
        Class<?> store = Class.forName(WaypointFactory.class.getName() + "$WaypointStore");
        Object factory = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{store}, (p, m, a) -> {
            if (m.getName().equals("createWaypoint")) return new PointState((String)a[0], (BlockPos)a[1], (String)a[2], (String)a[3]).proxy();
            if (m.getName().equals("createWaypointGroup")) return new GroupState((String)a[0], (String)a[1]).proxy();
            throw new UnsupportedOperationException(m.getName());
        });
        WaypointFactory.class.getConstructor(store).newInstance(factory);
        api = (IClientAPI)Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IClientAPI.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "getDataPath": return new File("owned-world");
                case "getWaypoints": return points.values().stream().filter(w -> w.getModId().equals(a[0])).toList();
                case "getWaypointGroups": return groups.values().stream().filter(g -> g.getModId().equals(a[0])).toList();
                case "addWaypointGroup": { WaypointGroup g=(WaypointGroup)a[0]; groups.put(g.getGuid(),g); return null; }
                case "removeWaypoint": points.remove(((Waypoint)a[1]).getGuid()); return null;
                case "addWaypoint": {
                    Waypoint w=(Waypoint)a[1];
                    // Native 6.0.7 silently accepts a duplicate without saving it, regardless of owner/name.
                    if(points.values().stream().noneMatch(v -> v.getGroupId().equals(w.getGroupId())
                            && v.getBlockPos().equals(w.getBlockPos()) && v.getDimensions().equals(w.getDimensions())))
                        points.put(w.getGuid(),w);
                    return null;
                }
                default: throw new UnsupportedOperationException(m.getName());
            }
        });
        adapter = adapter();
    }

    @After public void restoreFactory() throws Exception { factoryField.set(null,previousFactory); }

    private JourneyMap.Adapter adapter() throws Exception {
        Constructor<?> c=Class.forName(JourneyMapWaypointPlugin.class.getName()+"$ApiAdapter").getDeclaredConstructor(IClientAPI.class);
        c.setAccessible(true);
        return (JourneyMap.Adapter)c.newInstance(api);
    }

    @Test public void samePositionManualHomeAndStageRemainIndependent() throws Exception {
        Waypoint manual = new PointState("journeymap",new BlockPos(-1,64,-2),"home","minecraft:overworld").proxy();
        points.put(manual.getGuid(),manual);
        JourneyMap.Point home=new JourneyMap.Point("HOME","home","minecraft:overworld",-1,64,-2);
        JourneyMap.Point stage=new JourneyMap.Point("STAGE","home","minecraft:overworld",-1,64,-2);
        adapter.add(home); adapter.add(stage); adapter.add(home);
        assertEquals(3,points.size());
        assertEquals(2,groups.size());
        adapter=adapter();
        adapter.add(home);
        assertEquals(3,points.size());
        adapter.remove(home);
        assertEquals(2,points.size());
        assertSame(manual,points.get(manual.getGuid()));
        adapter.remove(stage);
        assertEquals(List.of(manual),List.copyOf(points.values()));
    }

    @Test public void sameKindAliasesAtOnePositionAreNotLostToNativeDuplicatePolicy() throws Exception {
        JourneyMap.Point a=new JourneyMap.Point("HOME","a","minecraft:overworld",-1,64,-2);
        JourneyMap.Point b=new JourneyMap.Point("HOME","b","minecraft:overworld",-1,64,-2);
        adapter.add(a); adapter.add(b);
        assertEquals(2,points.size());
        assertEquals(1,groups.size());
        adapter.remove(a);
        assertEquals("b",points.values().iterator().next().getName());
    }

    private final class GroupState implements InvocationHandler {
        final String owner,name,id=UUID.randomUUID().toString();
        final Map<String,String> data=new HashMap<>();
        GroupState(String owner,String name){this.owner=owner;this.name=name;}
        WaypointGroup proxy(){return (WaypointGroup)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{WaypointGroup.class},this);}
        public Object invoke(Object p,Method m,Object[] a) {
            switch(m.getName()) {
                case "getGuid": return id;
                case "getModId": return owner;
                case "getName": return name;
                case "getCustomData": return data.get(a[0]);
                case "setCustomData": data.put((String)a[0],(String)a[1]); return null;
                case "setPersistent": return null;
                case "addWaypoint": {
                    Waypoint w=(Waypoint)a[0];
                    ((PointState)Proxy.getInvocationHandler(w)).group=id;
                    points.put(w.getGuid(),w);
                    // Native 6.0.7 transfers/saves successfully but always returns false.
                    return false;
                }
                default: throw new UnsupportedOperationException(m.getName());
            }
        }
    }

    private static final class PointState implements InvocationHandler {
        final String owner,name,dimension,id=UUID.randomUUID().toString();
        final BlockPos pos;
        final Map<String,String> data=new HashMap<>();
        String group="default";
        PointState(String owner,BlockPos pos,String name,String dimension){this.owner=owner;this.pos=pos;this.name=name;this.dimension=dimension;}
        Waypoint proxy(){return (Waypoint)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Waypoint.class},this);}
        public Object invoke(Object p,Method m,Object[] a) {
            switch(m.getName()) {
                case "getGuid": return id;
                case "getModId": return owner;
                case "getName": return name;
                case "getDimensions": return new TreeSet<>(List.of(dimension));
                case "getBlockPos": return pos;
                case "getX": return pos.getX();
                case "getY": return pos.getY();
                case "getZ": return pos.getZ();
                case "getGroupId": return group;
                case "getCustomData": return data.get(a[0]);
                case "setCustomData": data.put((String)a[0],(String)a[1]); return null;
                case "setColor": return null;
                default: throw new UnsupportedOperationException(m.getName());
            }
        }
    }
}
