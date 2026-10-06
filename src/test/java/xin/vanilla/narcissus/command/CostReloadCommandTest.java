package xin.vanilla.narcissus.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import org.junit.Test;
import xin.vanilla.narcissus.command.impl.CostReloadCommand;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;

import static org.junit.Assert.*;

public class CostReloadCommandTest {
    private CommandSource console(int permission) {
        return new CommandSource(null, Vector3d.ZERO, Vector2f.ZERO, null, permission,
                "console", StringTextComponent.EMPTY, null, null);
    }

    @Test
    public void unauthorizedSourceCannotReachReloadHandler() throws Exception {
        assertNull(NarcissusCostService.get());
        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register(CostReloadCommand.create());
        assertFalse(CostReloadCommand.permitted(console(3)));
        try {
            dispatcher.execute("cost reload", console(3));
            fail("Unauthorized reload was accepted");
        } catch (CommandSyntaxException expected) {
        }
        assertNull(NarcissusCostService.get());
    }

    @Test
    public void administratorConsoleCanAccessReloadCommand() {
        assertTrue(CostReloadCommand.permitted(console(4)));
        assertTrue(CostReloadCommand.create().build().canUse(console(4)));
    }
}
