package xin.vanilla.narcissus.integration.map;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

final class WaypointOwnership {
    private static final List<WeakReference<Object>> OWNED = new ArrayList<>();

    private WaypointOwnership() {
    }

    static synchronized void remember(Object waypoint) {
        OWNED.removeIf(reference -> reference.get() == null);
        if (owns(waypoint)) return;
        if (OWNED.size() >= 4096) OWNED.remove(0);
        OWNED.add(new WeakReference<>(waypoint));
    }

    static synchronized boolean owns(Object waypoint) {
        if (waypoint == null) return false;
        for (WeakReference<Object> reference : OWNED) if (reference.get() == waypoint) return true;
        return false;
    }

    static synchronized void forget(Object waypoint) {
        OWNED.removeIf(reference -> reference.get() == null || reference.get() == waypoint);
    }
}
