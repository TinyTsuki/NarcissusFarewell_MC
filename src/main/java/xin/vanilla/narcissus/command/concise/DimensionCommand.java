package xin.vanilla.narcissus.command.concise;

import lombok.NonNull;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import xin.vanilla.narcissus.command.FarewellCommand;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class DimensionCommand extends CommandBase {

    @Override
    @NonNull
    public String getCommandName() {
        return ServerConfig.COMMAND_DIMENSION;
    }

    @Override
    @NonNull
    public String getCommandUsage(@NonNull ICommandSender sender) {
        return "/" + ServerConfig.COMMAND_PREFIX + " help " + this.getCommandName();
    }

    @Override
    @NonNull
    @ParametersAreNonnullByDefault
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return FarewellCommand.getSuggestions(sender, Stream.concat(Stream.of(this.getCommandName()), Arrays.stream(args)).toArray(String[]::new));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void processCommand(@NonNull ICommandSender sender, @ParametersAreNonnullByDefault String[] args) throws PlayerNotFoundException {
        FarewellCommand.verifyExecuteResult(sender, FarewellCommand.executeCommand(sender, Stream.concat(Stream.of(this.getCommandName()), Arrays.stream(args)).toArray(String[]::new)));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return NarcissusUtils.getCommandPermissionLevel(ECommandType.DIMENSION);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return NarcissusUtils.hasCommandPermission(sender, ECommandType.DIMENSION);
    }
}
