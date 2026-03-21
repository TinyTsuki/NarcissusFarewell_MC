package xin.vanilla.narcissus;

import lombok.NonNull;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.data.AbstractComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;


public final class NarcissusComponent extends AbstractComponent {

    public static final NarcissusComponent INSTANCE = new NarcissusComponent();

    private NarcissusComponent() {
    }

    @Override
    protected @NonNull String modId() {
        return NarcissusFarewell.MODID;
    }

    public static NarcissusComponent get() {
        return INSTANCE;
    }

    public Component translatable(String languageCode, EnumI18nType type, String key, Object... args) {
        return transLang(languageCode, type, key, args);
    }

    public Component translatable(ServerPlayer player, EnumI18nType type, String key, Object... args) {
        return trans(player, type, key, args);
    }

    public Component translatable(EnumI18nType type, String key, Object... args) {
        return trans(type, key, args);
    }
}
