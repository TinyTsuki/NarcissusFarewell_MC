package xin.vanilla.narcissus.internal.forge.cost;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RequestSelectionAuthorizationTest {
    private final ForgeCostPlayerFixture fixture = new ForgeCostPlayerFixture();
    private ForgeCostPlayerFixture.RecordingPlayer requester;
    private TeleportRequest request;

    @Before
    public void setup() throws Exception {
        fixture.setup();
        requester = ForgeCostPlayerFixture.allocate(ForgeCostPlayerFixture.RecordingPlayer.class);
        requester.id = UUID.randomUUID();
        request = new TeleportRequest().setRequester(requester).setTarget(fixture.player()).setTeleportType(EnumTeleportType.TP_ASK);
        NarcissusFarewell.getTeleportRequest().put("request", request);
    }

    @After
    public void cleanup() throws Exception {
        NarcissusFarewell.getTeleportRequest().clear();
        fixture.cleanup();
    }

    private String select(EnumTeleportType type, boolean target) throws Exception {
        return select(type, target, "request");
    }

    private String select(EnumTeleportType type, boolean target, String id) throws Exception {
        AtomicReference<String> selection = new AtomicReference<>();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Commands.literal("select").then(Commands.argument("requestId", StringArgumentType.word())
                .executes(ctx -> {
                    selection.set(CommandUtils.getRequestId(ctx, type, target));
                    return 1;
                })));
        CommandSourceStack source = new CommandSourceStack(null, Vec3.ZERO, Vec2.ZERO, null, 4,
                "fixture", net.minecraft.network.chat.Component.empty(), null, fixture.player());
        dispatcher.execute("select " + id, source);
        return selection.get();
    }

    @Test
    public void unknownExplicitIdReturnsNoSelection() throws Exception {
        assertNull(select(EnumTeleportType.TP_ASK, true, "missing"));
    }

    @Test
    public void explicitIdRequiresTheExpectedRequestTypeAndActor() throws Exception {
        assertEquals("request", select(EnumTeleportType.TP_ASK, true));
        assertNull(select(EnumTeleportType.TP_HERE, true));
        assertNull(select(EnumTeleportType.TP_ASK, false));
        request.setTarget(requester);
        assertNull(select(EnumTeleportType.TP_ASK, true));
    }
}
