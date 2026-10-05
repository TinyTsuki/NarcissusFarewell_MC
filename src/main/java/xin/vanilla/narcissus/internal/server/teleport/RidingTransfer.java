package xin.vanilla.narcissus.internal.server.teleport;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Internal riding-graph transfer shared by loader-specific entity adapters. */
public final class RidingTransfer {
    private RidingTransfer() { }

    public interface Backend<E> {
        List<E> passengers(E entity);
        default List<E> attachmentOrder(E vehicle, List<E> passengers) { return passengers; }
        E vehicle(E entity);
        void detach(E entity);
        E move(E entity);
        boolean alive(E entity);
        boolean atDestination(E entity);
        boolean sameWorld(E first, E second);
        boolean attach(E entity, E vehicle);
    }

    public static <E> boolean transfer(E root, E player, Backend<E> backend) {
        List<E> order = new ArrayList<>();
        List<E> attachments = new ArrayList<>();
        Map<E, List<E>> children = new IdentityHashMap<>();
        Map<E, E> parents = new IdentityHashMap<>();
        Map<E, E> current = new IdentityHashMap<>();
        order.add(root);
        attachments.add(root);
        for (int index = 0; index < order.size(); index++) {
            E entity = order.get(index);
            if (!backend.alive(entity) || current.containsKey(entity)) return false;
            current.put(entity, entity);
            parents.put(entity, backend.vehicle(entity));
            List<E> passengers = new ArrayList<>(backend.passengers(entity));
            children.put(entity, passengers);
            attachments.addAll(backend.attachmentOrder(entity, passengers));
            for (E passenger : passengers) {
                if (backend.vehicle(passenger) != entity) return false;
                order.add(passenger);
            }
        }
        if (!current.containsKey(player)) return false;
        boolean success = false;
        RuntimeException failure = null;
        try {
            for (E entity : order) backend.detach(entity);
            for (E entity : order) {
                if (backend.vehicle(entity) != null || !backend.passengers(entity).isEmpty()) return false;
            }
            // Validate the vehicle migration before moving the initiating player.
            for (E entity : order) {
                if (entity != player && !move(entity, current, backend)) return false;
            }
            if (!move(player, current, backend)) return false;
            if (!restore(attachments, parents, current, backend, false)) return false;
            for (E entity : order) {
                List<E> actual = backend.passengers(current.get(entity));
                List<E> expected = children.get(entity);
                if (actual.size() != expected.size()) return false;
                for (int index = 0; index < expected.size(); index++) {
                    if (actual.get(index) != current.get(expected.get(index))) return false;
                }
            }
            success = true;
            return true;
        } catch (RuntimeException error) {
            failure = error;
            throw error;
        } finally {
            if (!success) {
                try {
                    restore(attachments, parents, current, backend, true);
                } catch (RuntimeException recovery) {
                    if (failure != null) failure.addSuppressed(recovery);
                    else throw recovery;
                }
            }
        }
    }

    private static <E> boolean move(E entity, Map<E, E> current, Backend<E> backend) {
        E moved = backend.move(entity);
        if (moved != null) current.put(entity, moved);
        return backend.alive(moved) && backend.atDestination(moved);
    }

    private static <E> boolean restore(List<E> order, Map<E, E> parents, Map<E, E> current,
                                       Backend<E> backend, boolean recovering) {
        boolean restored = true;
        for (E original : order) {
            E parent = parents.get(original);
            if (parent == null || !recovering && !current.containsKey(parent)) continue;
            E entity = current.get(original);
            E vehicle = current.containsKey(parent) ? current.get(parent) : parent;
            if (!backend.alive(entity) || !backend.alive(vehicle) || !backend.sameWorld(entity, vehicle)) {
                restored = false;
                continue;
            }
            if (backend.vehicle(entity) != vehicle) {
                if (backend.vehicle(entity) != null) backend.detach(entity);
                if (backend.vehicle(entity) != null || !backend.attach(entity, vehicle)) restored = false;
            }
            if (backend.vehicle(entity) != vehicle) restored = false;
        }
        return restored;
    }
}
