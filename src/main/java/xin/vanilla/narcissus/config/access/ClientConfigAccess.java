package xin.vanilla.narcissus.config.access;

import xin.vanilla.banira.common.config.ConfigCategoryViewProxy;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.narcissus.config.ClientConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * {@link ClientConfig} 运行时 {@link ClientConfig.RootView} 实现。
 */
public final class ClientConfigAccess {

    private static final ClientConfig.ClientRootCategory DEFAULT_CLIENT = new ClientConfig.ClientRootCategory();

    private ClientConfigAccess() {
    }

    public static ClientConfig.RootView root(ConfigHolder holder) {
        return (ClientConfig.RootView) Proxy.newProxyInstance(
                ClientConfig.class.getClassLoader(),
                new Class<?>[]{ClientConfig.RootView.class},
                (proxy, method, args) -> rootHandle(proxy, method, args, holder));
    }

    private static Object rootHandle(Object proxy, Method method, Object[] args, ConfigHolder holder) {
        if (method.getDeclaringClass() == Object.class) {
            return objectMethod(proxy, method, args, "ClientConfig.RootView");
        }
        switch (method.getName()) {
            case "client":
                return ConfigCategoryViewProxy.create(ClientConfig.ClientView.class, holder, "client", DEFAULT_CLIENT,
                        ClientConfigAccess::readClientLeaf);
            case "holder":
                return holder;
            case "save":
                if (holder != null) {
                    holder.save();
                }
                return null;
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }

    private static Object readClientLeaf(String leaf, Object raw, Object bean) throws Exception {
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object field(Object bean, String name) throws Exception {
        Field f = bean.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(bean);
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args, String tag) {
        String n = method.getName();
        switch (n) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return tag + "@" + System.identityHashCode(proxy);
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }
}
