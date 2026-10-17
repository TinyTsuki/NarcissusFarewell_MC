package xin.vanilla.narcissus.integration.map;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class JourneyMapApiContract {
    private JourneyMapApiContract() { }

    static Method method(Class<?> type, String name, Class<?> result, Class<?>... arguments)
            throws NoSuchMethodException {
        Method method = type.getMethod(name, arguments);
        if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != result)
            throw new NoSuchMethodException(type.getName() + "." + name);
        return method;
    }
}
