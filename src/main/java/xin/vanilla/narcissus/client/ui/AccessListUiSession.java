package xin.vanilla.narcissus.client.ui;

import java.util.function.BooleanSupplier;

public final class AccessListUiSession extends NarcissusListUiSession {
    public AccessListUiSession(Port port, BooleanSupplier connection) { super(port, connection, System::nanoTime); }
}
