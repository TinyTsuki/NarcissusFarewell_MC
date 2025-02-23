package xin.vanilla.narcissus.command.concise;

import lombok.NonNull;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import xin.vanilla.narcissus.command.FarewellCommand;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class HereYesCommand extends CommandBase {

    @Override
    @NonNull
    public String getName() {
        return ServerConfig.COMMAND_TP_HERE_YES;
    }

    @Override
    @NonNull
    public String getUsage(@NonNull ICommandSender sender) {
        return "/" + ServerConfig.COMMAND_PREFIX + " help " + this.getName();
    }

    @Override
    @NonNull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return FarewellCommand.getSuggestions(server, sender, Stream.concat(Stream.of(this.getName()), Arrays.stream(args)).toArray(String[]::new), targetPos);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(@NonNull MinecraftServer server, @NonNull ICommandSender sender, @ParametersAreNonnullByDefault String[] args) throws PlayerNotFoundException {
        FarewellCommand.verifyExecuteResult(sender, FarewellCommand.executeCommand(server, sender, Stream.concat(Stream.of(this.getName()), Arrays.stream(args)).toArray(String[]::new)));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return NarcissusUtils.getCommandPermissionLevel(ECommandType.TP_HERE_YES);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return NarcissusUtils.hasCommandPermission(sender, ECommandType.TP_HERE_YES);
    }
}
