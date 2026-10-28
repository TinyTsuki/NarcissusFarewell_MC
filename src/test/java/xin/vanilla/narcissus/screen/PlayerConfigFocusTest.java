package xin.vanilla.narcissus.screen;

import org.junit.Test;
import xin.vanilla.banira.internal.client.ui.UiViewState;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class PlayerConfigFocusTest {
    private Method declared(String name, Class<?>... parameters) {
        try {
            Method method = PlayerConfigScreen.class.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Native numeric focus handoff is missing: " + name, missing);
        }
    }

    @Test
    public void focusKeysUseOriginalConfigurableFieldOrder() throws Exception {
        Method key = declared("countdownFocusId", EnumTeleportType.class);
        var types = EnumTeleportType.countdownConfigurableTypes();
        assertEquals(16, types.size());
        for (int index = 0; index < types.size(); index++)
            assertEquals("field:" + index + "/input", key.invoke(null, types.get(index)));
        assertEquals("", key.invoke(null, (Object) null));
        assertEquals("", key.invoke(null, EnumTeleportType.DEATH));
        assertEquals("", key.invoke(null, EnumTeleportType.OTHER));
    }

    @Test
    public void nativeScreenOwnsBothHandoffLifecycleHooks() {
        assertEquals(PlayerConfigScreen.class, declared("prepareUiState", UiViewState.class).getDeclaringClass());
        assertEquals(PlayerConfigScreen.class, declared("afterPlayerConfigBuilt").getDeclaringClass());
    }
}
