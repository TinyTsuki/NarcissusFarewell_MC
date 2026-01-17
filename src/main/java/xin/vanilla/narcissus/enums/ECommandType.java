package xin.vanilla.narcissus.enums;

import lombok.Getter;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandHelp;
import xin.vanilla.narcissus.command.concise.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

@Getter
public enum ECommandType {
    HELP(null, false, false),
    LANGUAGE(LanguageCommand::new, false, false),
    LANGUAGE_CONCISE(null),
    UUID(UuidCommand::new),
    UUID_CONCISE(null),
    DIMENSION(DimensionCommand::new),
    DIMENSION_CONCISE(null),
    CARD(CardCommand::new, false, false),
    CARD_CONCISE(null),
    SET_CARD(null, true),
    SET_CARD_CONCISE(null),
    SHARE(ShareCommand::new),
    SHARE_CONCISE(null),
    FEED(FeedCommand::new, false, false),
    FEED_CONCISE(null),
    FEED_OTHER(null, true),
    FEED_OTHER_CONCISE(null, true),
    TP_COORDINATE(CoordinateCommand::new),
    TP_COORDINATE_CONCISE(null),
    TP_STRUCTURE(StructureCommand::new),
    TP_STRUCTURE_CONCISE(null),
    TP_ASK(AskCommand::new),
    TP_ASK_CONCISE(null),
    TP_ASK_YES(AskYesCommand::new, false, false),
    TP_ASK_YES_CONCISE(null),
    TP_ASK_NO(AskNoCommand::new, false, false),
    TP_ASK_NO_CONCISE(null),
    TP_ASK_CANCEL(AskCancelCommand::new, false, false),
    TP_ASK_CANCEL_CONCISE(null),
    TP_HERE(HereCommand::new),
    TP_HERE_CONCISE(null),
    TP_HERE_YES(HereYesCommand::new, false, false),
    TP_HERE_YES_CONCISE(null),
    TP_HERE_NO(HereNoCommand::new, false, false),
    TP_HERE_NO_CONCISE(null),
    TP_HERE_CANCEL(HereCancelCommand::new, false, false),
    TP_HERE_CANCEL_CONCISE(null),
    TP_RANDOM(RandomCommand::new),
    TP_RANDOM_CONCISE(null),
    TP_SPAWN(SpawnCommand::new),
    TP_SPAWN_OTHER(null, true),
    TP_SPAWN_CONCISE(null),
    TP_SPAWN_OTHER_CONCISE(null, true),
    TP_WORLD_SPAWN(WorldSpawnCommand::new),
    TP_WORLD_SPAWN_CONCISE(null),
    TP_TOP(TopCommand::new),
    TP_TOP_CONCISE(null),
    TP_BOTTOM(BottomCommand::new),
    TP_BOTTOM_CONCISE(null),
    TP_UP(UpCommand::new),
    TP_UP_CONCISE(null),
    TP_DOWN(DownCommand::new),
    TP_DOWN_CONCISE(null),
    TP_VIEW(ViewCommand::new),
    TP_VIEW_CONCISE(null),
    TP_HOME(HomeCommand::new),
    TP_HOME_CONCISE(null),
    SET_HOME(SetHomeCommand::new, false, false),
    SET_HOME_CONCISE(null),
    DEL_HOME(DelHomeCommand::new, false, false),
    DEL_HOME_CONCISE(null),
    GET_HOME(GetHomeCommand::new, false, false),
    GET_HOME_CONCISE(null),
    TP_STAGE(StageCommand::new),
    TP_STAGE_CONCISE(null),
    SET_STAGE(SetStageCommand::new),
    SET_STAGE_CONCISE(null),
    DEL_STAGE(DelStageCommand::new),
    DEL_STAGE_CONCISE(null),
    GET_STAGE(GetStageCommand::new),
    GET_STAGE_CONCISE(null),
    TP_BACK(BackCommand::new),
    TP_BACK_CONCISE(null),
    VIRTUAL_OP(VirtualOpCommand::new),
    VIRTUAL_OP_CONCISE(null);

    /**
     * 在帮助信息内忽略
     */
    private final boolean ignore;
    /**
     * 是否简短指令
     */
    private final boolean concise = this.name().endsWith("_CONCISE");
    /**
     * 是否被虚拟权限管理
     */
    private final boolean op;

    @CheckForNull
    private final Supplier<? extends CommandBase> instance;

    ECommandType(@Nullable final Supplier<? extends CommandBase> instance) {
        this.instance = instance;
        this.ignore = false;
        this.op = !this.concise;
    }

    ECommandType(@Nullable final Supplier<? extends CommandBase> instance,
                 final boolean ig) {
        this.instance = instance;
        this.ignore = ig;
        this.op = !this.concise;
    }

    ECommandType(@Nullable Supplier<? extends CommandBase> instance,
                 final boolean ig,
                 final boolean op) {
        this.instance = instance;
        this.ignore = ig;
        this.op = !this.concise && op;
    }

    public int getSort() {
        return this.ordinal();
    }

    public ECommandType replaceConcise() {
        if (this.name().endsWith("_CONCISE")) {
            return ECommandType.valueOf(this.name().replace("_CONCISE", ""));
        }
        return this;
    }

    public ETeleportType toTeleportType() {
        switch (this) {
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ETeleportType.TP_COORDINATE;
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ETeleportType.TP_STRUCTURE;
            case TP_ASK:
                // case TP_ASK_YES:
                // case TP_ASK_NO:
                // case TP_ASK_CANCEL:
            case TP_ASK_CONCISE:
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                // case TP_ASK_CANCEL_CONCISE:
                return ETeleportType.TP_ASK;
            case TP_HERE:
                // case TP_HERE_YES:
                // case TP_HERE_NO:
                // case TP_HERE_CANCEL:
            case TP_HERE_CONCISE:
                // case TP_HERE_YES_CONCISE:
                // case TP_HERE_NO_CONCISE:
                // case TP_HERE_CANCEL_CONCISE:
                return ETeleportType.TP_HERE;
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ETeleportType.TP_RANDOM;
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return ETeleportType.TP_SPAWN;
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ETeleportType.TP_WORLD_SPAWN;
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ETeleportType.TP_TOP;
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ETeleportType.TP_BOTTOM;
            case TP_UP:
            case TP_UP_CONCISE:
                return ETeleportType.TP_UP;
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ETeleportType.TP_DOWN;
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ETeleportType.TP_VIEW;
            case TP_HOME:
                // case SET_HOME:
                // case DEL_HOME:
                // case GET_HOME:
            case TP_HOME_CONCISE:
                // case SET_HOME_CONCISE:
                // case DEL_HOME_CONCISE:
                // case GET_HOME_CONCISE:
                return ETeleportType.TP_HOME;
            case TP_STAGE:
                // case SET_STAGE:
                // case DEL_STAGE:
                // case GET_STAGE:
            case TP_STAGE_CONCISE:
                // case SET_STAGE_CONCISE:
                // case DEL_STAGE_CONCISE:
                // case GET_STAGE_CONCISE:
                return ETeleportType.TP_STAGE;
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ETeleportType.TP_BACK;
            default:
                return null;
        }
    }
}
