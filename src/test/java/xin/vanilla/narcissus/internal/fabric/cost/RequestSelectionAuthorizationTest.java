package xin.vanilla.narcissus.internal.fabric.cost;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.TextComponent;
import org.junit.*;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class RequestSelectionAuthorizationTest {
    private final FabricCostPlayerFixture fixture = new FabricCostPlayerFixture();
    private FabricCostPlayerFixture.RecordingPlayer requester;
    private TeleportRequest request;

    @Before public void setup() throws Exception {
        fixture.setup();
        requester = FabricCostPlayerFixture.allocate(FabricCostPlayerFixture.RecordingPlayer.class);
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
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Commands.literal("select").then(Commands.argument("requestId", StringArgumentType.word())
                .executes(ctx -> { selection.set(CommandUtils.getRequestId(ctx, type, target)); return 1; })));
        CommandSourceStack source = new CommandSourceStack(null, Vec3.ZERO, Vec2.ZERO, null, 4,
                "fixture", TextComponent.EMPTY, null, fixture.player());
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
