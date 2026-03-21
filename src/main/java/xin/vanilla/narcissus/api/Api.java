package xin.vanilla.narcissus.api;

import xin.vanilla.banira.internal.config.CustomConfig;

public class Api implements IApi {
    @Override
    public void reloadCustomConfig() {
        CustomConfig.loadCustomConfig(false);
    }
}
