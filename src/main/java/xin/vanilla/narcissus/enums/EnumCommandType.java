package xin.vanilla.narcissus.enums;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.Getter;
import net.minecraft.command.CommandSource;
import xin.vanilla.banira.api.permission.BaniraVirtualPermission;
import xin.vanilla.banira.command.BaniraCommand;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.command.impl.*;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Getter
public enum EnumCommandType implements BaniraVirtualPermission, IEnumDescribable {
    HELP(HelpCommand::create, false, false),
    LANGUAGE(() -> baniraNode(BaniraCommand.LANGUAGE), false, false),
    LANGUAGE_CONCISE(),
    UUID(UuidCommand::create),
    UUID_CONCISE(),
    DIMENSION(DimensionCommand::create),
    DIMENSION_CONCISE(),
    CARD(CardCommand::create, false, false),
    CARD_CONCISE(),
    SET_CARD(true),
    SET_CARD_CONCISE(),
    SHARE(ShareCommand::create),
    SHARE_CONCISE(),
    FEED(FeedCommand::create, false, false),
    FEED_CONCISE(),
    FEED_OTHER(true),
    FEED_OTHER_CONCISE(true),
    TP_COORDINATE(TpCoordinateCommand::create),
    TP_COORDINATE_CONCISE(),
    TP_STRUCTURE(TpStructureCommand::create),
    TP_STRUCTURE_CONCISE(),
    TP_ASK(TpAskCommand::create),
    TP_ASK_CONCISE(),
    TP_ASK_YES(TpAskYesCommand::create, false, false),
    TP_ASK_YES_CONCISE(),
    TP_ASK_NO(TpAskNoCommand::create, false, false),
    TP_ASK_NO_CONCISE(),
    TP_ASK_CANCEL(TpAskCancelCommand::create, false, false),
    TP_ASK_CANCEL_CONCISE(),
    TP_HERE(TpHereCommand::create),
    TP_HERE_CONCISE(),
    TP_HERE_YES(TpHereYesCommand::create, false, false),
    TP_HERE_YES_CONCISE(),
    TP_HERE_NO(TpHereNoCommand::create, false, false),
    TP_HERE_NO_CONCISE(),
    TP_HERE_CANCEL(TpHereCancelCommand::create, false, false),
    TP_HERE_CANCEL_CONCISE(),
    TP_RANDOM(TpRandomCommand::create),
    TP_RANDOM_CONCISE(),
    TP_SPAWN(TpSpawnCommand::create),
    TP_SPAWN_OTHER(true),
    TP_SPAWN_CONCISE(),
    TP_SPAWN_OTHER_CONCISE(true),
    TP_WORLD_SPAWN(TpWorldSpawnCommand::create),
    TP_WORLD_SPAWN_CONCISE(),
    TP_TOP(TpTopCommand::create),
    TP_TOP_CONCISE(),
    TP_BOTTOM(TpBottomCommand::create),
    TP_BOTTOM_CONCISE(),
    TP_UP(TpUpCommand::create),
    TP_UP_CONCISE(),
    TP_DOWN(TpDownCommand::create),
    TP_DOWN_CONCISE(),
    TP_VIEW(TpViewCommand::create),
    TP_VIEW_CONCISE(),
    TP_HOME(TpHomeCommand::create),
    TP_HOME_CONCISE(),
    SET_HOME(SetHomeCommand::create, false, false),
    SET_HOME_CONCISE(),
    DEL_HOME(DelHomeCommand::create, false, false),
    DEL_HOME_CONCISE(),
    GET_HOME(GetHomeCommand::create, false, false),
    GET_HOME_CONCISE(),
    TP_STAGE(TpStageCommand::create),
    TP_STAGE_CONCISE(),
    SET_STAGE(SetStageCommand::create),
    SET_STAGE_CONCISE(),
    DEL_STAGE(DelStageCommand::create),
    DEL_STAGE_CONCISE(),
    GET_STAGE(GetStageCommand::create),
    GET_STAGE_CONCISE(),
    TP_BACK(TpBackCommand::create),
    TP_BACK_CONCISE(),
    TP_GRAVE(TpGraveCommand::create),
    TP_GRAVE_CONCISE(),
    FLY(FlyCommand::create),
    FLY_CONCISE(),
    VIRTUAL_OP(() -> baniraNode(BaniraCommand.VIRTUAL_OP)),
    VIRTUAL_OP_CONCISE(),
    CONFIG(ConfigCommand::create, true),
    BLACKLIST(false, false),
    WHITELIST(false, false),
    ;

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

    @Nullable
    private final Supplier<LiteralArgumentBuilder<CommandSource>> instance;

    /**
     * Banira 对外以 Object 暴露版本相关命令节点，这里在 1.16.5 分支恢复 Brigadier 类型。
     */
    @SuppressWarnings("unchecked")
    private static LiteralArgumentBuilder<CommandSource> baniraNode(Object node) {
        return (LiteralArgumentBuilder<CommandSource>) node;
    }

    EnumCommandType() {
        this.instance = null;
        this.ignore = false;
        this.op = !this.concise;
    }

    EnumCommandType(boolean ig) {
        this.instance = null;
        this.ignore = ig;
        this.op = !this.concise;
    }

    EnumCommandType(boolean ig, boolean op) {
        this.instance = null;
        this.ignore = ig;
        this.op = !this.concise && op;
    }

    EnumCommandType(@Nullable Supplier<LiteralArgumentBuilder<CommandSource>> instance) {
        this.instance = instance;
        this.ignore = false;
        this.op = !this.concise;
    }

    EnumCommandType(@Nullable Supplier<LiteralArgumentBuilder<CommandSource>> instance, boolean ig) {
        this.instance = instance;
        this.ignore = ig;
        this.op = !this.concise;
    }

    EnumCommandType(@Nullable Supplier<LiteralArgumentBuilder<CommandSource>> instance, boolean ig, boolean op) {
        this.instance = instance;
        this.ignore = ig;
        this.op = !this.concise && op;
    }

    public int getSort() {
        return this.ordinal();
    }

    // region BaniraVirtualPermission
    @Override
    public String modId() {
        return NarcissusFarewell.MODID;
    }

    @Override
    public String id() {
        return this.replaceConcise().name();
    }

    @Override
    public boolean op() {
        return this.op;
    }

    @Override
    public int sort() {
        return getSort();
    }
    // endregion

    public EnumCommandType replaceConcise() {
        if (this.name().endsWith("_CONCISE")) {
            return EnumCommandType.valueOf(this.name().replace("_CONCISE", ""));
        }
        return this;
    }

    public EnumTeleportType toTeleportType() {
        switch (this) {
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return EnumTeleportType.TP_COORDINATE;
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return EnumTeleportType.TP_STRUCTURE;
            case TP_ASK:
                // case TP_ASK_YES:
                // case TP_ASK_NO:
                // case TP_ASK_CANCEL:
            case TP_ASK_CONCISE:
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                // case TP_ASK_CANCEL_CONCISE:
                return EnumTeleportType.TP_ASK;
            case TP_HERE:
                // case TP_HERE_YES:
                // case TP_HERE_NO:
                // case TP_HERE_CANCEL:
            case TP_HERE_CONCISE:
                // case TP_HERE_YES_CONCISE:
                // case TP_HERE_NO_CONCISE:
                // case TP_HERE_CANCEL_CONCISE:
                return EnumTeleportType.TP_HERE;
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return EnumTeleportType.TP_RANDOM;
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return EnumTeleportType.TP_SPAWN;
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return EnumTeleportType.TP_WORLD_SPAWN;
            case TP_TOP:
            case TP_TOP_CONCISE:
                return EnumTeleportType.TP_TOP;
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return EnumTeleportType.TP_BOTTOM;
            case TP_UP:
            case TP_UP_CONCISE:
                return EnumTeleportType.TP_UP;
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return EnumTeleportType.TP_DOWN;
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return EnumTeleportType.TP_VIEW;
            case TP_HOME:
                // case SET_HOME:
                // case DEL_HOME:
                // case GET_HOME:
            case TP_HOME_CONCISE:
                // case SET_HOME_CONCISE:
                // case DEL_HOME_CONCISE:
                // case GET_HOME_CONCISE:
                return EnumTeleportType.TP_HOME;
            case TP_STAGE:
                // case SET_STAGE:
                // case DEL_STAGE:
                // case GET_STAGE:
            case TP_STAGE_CONCISE:
                // case SET_STAGE_CONCISE:
                // case DEL_STAGE_CONCISE:
                // case GET_STAGE_CONCISE:
                return EnumTeleportType.TP_STAGE;
            case TP_BACK:
            case TP_BACK_CONCISE:
                return EnumTeleportType.TP_BACK;
            case TP_GRAVE:
            case TP_GRAVE_CONCISE:
                return EnumTeleportType.TP_GRAVE;
            default:
                return null;
        }
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
