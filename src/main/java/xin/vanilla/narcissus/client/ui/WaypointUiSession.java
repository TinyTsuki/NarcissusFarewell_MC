package xin.vanilla.narcissus.client.ui;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public final class WaypointUiSession extends NarcissusListUiSession {
    public WaypointUiSession(Port port, BooleanSupplier connection) { this(port, connection, System::nanoTime); }
    public WaypointUiSession(Port port, BooleanSupplier connection, LongSupplier clock) { super(port, connection, clock); }
}
