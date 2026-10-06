package xin.vanilla.narcissus.internal.server.teleport;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class RidingTransferTest {
    @Test
    public void soloMovesExactlyOnce() {
        World world = new World();
        Node player = world.node("player");
        assertTrue(RidingTransfer.transfer(player, player, world));
        assertEquals(Collections.singletonList("player"), world.calls);
    }

    @Test
    public void clonesRebuildEveryEdgeAndKeepPassengerOrder() {
        World world = new World();
        Node root = world.node("root"), first = world.child(root, "first"), second = world.child(first, "second");
        Node player = world.child(second, "player"), sibling = world.child(first, "sibling");
        assertTrue(RidingTransfer.transfer(root, player, world));
        assertSame(world.current.get("root"), world.current.get("first").parent);
        assertSame(world.current.get("first"), world.current.get("second").parent);
        assertSame(world.current.get("second"), world.current.get("player").parent);
        assertSame(world.current.get("first"), world.current.get("sibling").parent);
        assertEquals(Arrays.asList(world.current.get("second"), world.current.get("sibling")), world.current.get("first").children);
        assertNotSame(root, world.current.get("root"));
        assertEquals(5, new HashSet<>(world.calls).size());
        assertEquals("player", world.calls.get(4));
        assertTrue(root.children.isEmpty());
        assertFalse(root.alive);
    }

    @Test
    public void rootNullReturnStopsBeforePlayerAndRestoresSourceEdges() {
        World world = new World();
        Node root = world.node("root"), player = world.child(root, "player");
        world.deny = "root";
        assertFalse(RidingTransfer.transfer(root, player, world));
        assertEquals(Collections.singletonList("root"), world.calls);
        assertSame(root, player.parent);
    }

    @Test
    public void middleNullReturnNeverLinksAcrossWorlds() {
        World world = new World();
        Node root = world.node("root"), middle = world.child(root, "middle"), player = world.child(middle, "player");
        world.deny = "middle";
        assertFalse(RidingTransfer.transfer(root, player, world));
        assertEquals(Arrays.asList("root", "middle"), world.calls);
        assertSame(middle, player.parent);
        assertNull(middle.parent);
        assertTrue(world.current.get("root").children.isEmpty());
    }

    @Test
    public void sourceObjectReturnIsNotSuccess() {
        World world = new World();
        Node player = world.node("player");
        world.stays = "player";
        assertFalse(RidingTransfer.transfer(player, player, world));
        assertEquals(0, player.world);
    }

    @Test
    public void dismountVetoPreventsAllMigration() {
        World world = new World();
        Node root = world.node("root"), player = world.child(root, "player");
        world.veto = "player";
        assertFalse(RidingTransfer.transfer(root, player, world));
        assertTrue(world.calls.isEmpty());
        assertSame(root, player.parent);
    }

    @Test
    public void attachmentVetoCannotReportSuccess() {
        World world = new World();
        Node root = world.node("root"), player = world.child(root, "player");
        world.attachVeto = "player";
        assertFalse(RidingTransfer.transfer(root, player, world));
        assertNull(world.current.get("player").parent);
    }

    @Test
    public void moveExceptionRestoresValidSourceGraphAndPropagates() {
        World world = new World();
        Node root = world.node("root"), player = world.child(root, "player");
        world.error = "root";
        try {
            RidingTransfer.transfer(root, player, world);
            fail("exception missing");
        } catch (IllegalStateException expected) {
            assertEquals("move failed", expected.getMessage());
        }
        assertSame(root, player.parent);
    }

    @Test
    public void repeatedNodeRejectsGraphWithoutMoving() {
        World world = new World();
        Node root = world.node("root"), player = world.child(root, "player");
        root.children.add(player);
        assertFalse(RidingTransfer.transfer(root, player, world));
        assertTrue(world.calls.isEmpty());
        assertSame(root, player.parent);
    }

    @Test
    public void removedCloneCannotReportSuccess() {
        World world = new World();
        Node player = world.node("player");
        world.removed = "player";
        assertFalse(RidingTransfer.transfer(player, player, world));
    }

    @Test
    public void nativePrependingPlayersKeepTheirOriginalOrder() {
        World world = new World();
        world.prependPlayers = true;
        Node root = world.node("root"), first = world.child(root, "player-a"), second = world.child(root, "player-b");
        Node item = world.child(root, "item");
        assertEquals(Arrays.asList(second, first, item), root.children);
        assertTrue(RidingTransfer.transfer(root, first, world));
        assertEquals(Arrays.asList(world.current.get("player-b"), world.current.get("player-a"), world.current.get("item")), world.current.get("root").children);
    }

    private static final class Node {
        final String name;
        int world;
        boolean alive = true;
        Node parent;
        final List<Node> children = new ArrayList<>();

        Node(String name) {
            this.name = name;
        }
    }

    private static final class World implements RidingTransfer.Backend<Node> {
        final Map<String, Node> current = new LinkedHashMap<>();
        final List<String> calls = new ArrayList<>();
        String deny, stays, veto, attachVeto, error, removed;
        boolean prependPlayers;

        Node node(String name) {
            Node node = new Node(name);
            current.put(name, node);
            return node;
        }

        Node child(Node parent, String name) {
            Node node = node(name);
            attach(node, parent);
            return node;
        }

        public List<Node> passengers(Node node) {
            return node.children;
        }

        public List<Node> attachmentOrder(Node vehicle, List<Node> passengers) {
            if (!prependPlayers) return passengers;
            List<Node> order = new ArrayList<>();
            for (Node passenger : passengers) {
                if (passenger.name.startsWith("player-")) order.add(0, passenger);
                else order.add(passenger);
            }
            return order;
        }

        public Node vehicle(Node node) {
            return node.parent;
        }

        public void detach(Node node) {
            if (node.name.equals(veto)) return;
            if (node.parent != null) node.parent.children.remove(node);
            node.parent = null;
        }

        public boolean alive(Node node) {
            return node != null && node.alive;
        }

        public boolean atDestination(Node node) {
            return node.world == 1;
        }

        public boolean sameWorld(Node first, Node second) {
            return first.world == second.world;
        }

        public boolean attach(Node node, Node parent) {
            if (node.name.equals(attachVeto)) return false;
            if (node.parent != null) detach(node);
            node.parent = parent;
            if (prependPlayers && node.name.startsWith("player-")) parent.children.add(0, node);
            else parent.children.add(node);
            return true;
        }

        public Node move(Node node) {
            calls.add(node.name);
            assertNull("must detach before native migration", node.parent);
            assertTrue("must detach all passengers", node.children.isEmpty());
            if (node.name.equals(error)) throw new IllegalStateException("move failed");
            if (node.name.equals(deny)) return null;
            if (node.name.equals(stays)) return node;
            Node clone = new Node(node.name);
            clone.world = 1;
            clone.alive = !node.name.equals(removed);
            node.alive = false;
            current.put(node.name, clone);
            return clone;
        }
    }
}
