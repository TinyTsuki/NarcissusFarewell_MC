package xin.vanilla.narcissus;

import xin.vanilla.banira.common.util.IIdentifier;

/**
 * 资源创建器接口
 */
public final class Identifier implements IIdentifier {
    private static final Identifier INSTANCE = new Identifier();

    private Identifier() {
    }

    @Override
    public String modId() {
        return NarcissusFarewell.MODID;
    }

    @Override
    public IIdentifier instance() {
        return INSTANCE;
    }

    public static IIdentifier id() {
        return INSTANCE;
    }
}
