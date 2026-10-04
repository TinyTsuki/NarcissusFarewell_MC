package xin.vanilla.narcissus.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import xin.vanilla.narcissus.command.impl.CostReloadCommand;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;

import static org.junit.Assert.*;

public class CostReloadCommandTest {
    private CommandSourceStack console(int permission) {
        return new CommandSourceStack(null, Vec3.ZERO, Vec2.ZERO, null, permission,
                "console", net.minecraft.network.chat.Component.empty(), null, null);
    }

    @Test public void unauthorizedSourceCannotReachReloadHandler() throws Exception {
        assertNull(NarcissusCostService.get());
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(CostReloadCommand.create());
        assertFalse(CostReloadCommand.permitted(console(3)));
        try { dispatcher.execute("cost reload", console(3)); fail("Unauthorized reload was accepted"); }
        catch (CommandSyntaxException expected) { }
        assertNull(NarcissusCostService.get());
    }

    @Test public void administratorConsoleCanAccessReloadCommand() {
        assertTrue(CostReloadCommand.permitted(console(4)));
        assertTrue(CostReloadCommand.create().build().canUse(console(4)));
    }
}
