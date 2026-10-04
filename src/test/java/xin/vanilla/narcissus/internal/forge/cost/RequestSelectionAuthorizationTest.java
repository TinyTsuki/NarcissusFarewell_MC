package xin.vanilla.narcissus.internal.forge.cost;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import org.junit.*;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class RequestSelectionAuthorizationTest {
    private final ForgeCostPlayerFixture fixture = new ForgeCostPlayerFixture();
    private ForgeCostPlayerFixture.RecordingPlayer requester;
    private TeleportRequest request;

    @Before public void setup() throws Exception {
        fixture.setup();
        requester = ForgeCostPlayerFixture.allocate(ForgeCostPlayerFixture.RecordingPlayer.class);
        requester.id = UUID.randomUUID();
        request = new TeleportRequest().setRequester(requester).setTarget(fixture.player()).setTeleportType(EnumTeleportType.TP_ASK);
        NarcissusFarewell.getTeleportRequest().put("request", request);
    }
    @After public void cleanup() throws Exception { NarcissusFarewell.getTeleportRequest().clear(); fixture.cleanup(); }

    private String select(EnumTeleportType type, boolean target) throws Exception {
        return select(type, target, "request");
    }

    private String select(EnumTeleportType type, boolean target, String id) throws Exception {
        AtomicReference<String> selection = new AtomicReference<>();
        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Commands.literal("select").then(Commands.argument("requestId", StringArgumentType.word())
                .executes(ctx -> { selection.set(CommandUtils.getRequestId(ctx, type, target)); return 1; })));
        CommandSource source = new CommandSource(null, Vector3d.ZERO, Vector2f.ZERO, null, 4,
                "fixture", StringTextComponent.EMPTY, null, fixture.player());
        dispatcher.execute("select " + id, source);
        return selection.get();
    }

    @Test public void unknownExplicitIdReturnsNoSelection() throws Exception {
        assertNull(select(EnumTeleportType.TP_ASK, true, "missing"));
    }

    @Test public void explicitIdRequiresTheExpectedRequestTypeAndActor() throws Exception {
        assertEquals("request", select(EnumTeleportType.TP_ASK, true));
        assertNull(select(EnumTeleportType.TP_HERE, true));
        assertNull(select(EnumTeleportType.TP_ASK, false));
        request.setTarget(requester);
        assertNull(select(EnumTeleportType.TP_ASK, true));
    }
}
