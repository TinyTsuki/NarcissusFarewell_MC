package xin.vanilla.narcissus;

import lombok.NonNull;
import xin.vanilla.banira.common.data.AbstractComponent;


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
}
