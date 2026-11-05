package xin.vanilla.narcissus.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

import static org.junit.Assert.*;

public class Fabric26PlayerMixinTargetTest {
    @Test
    public void playerHooksMatchNativeMethodsAndCallbackParameters() throws Exception {
        assertTarget("ServerPlayerTeleportMixin", "teleport");
        assertTarget("ServerPlayerDamageMixin", "hurtServer");
    }

    private void assertTarget(String mixinName, String targetName) throws Exception {
        ClassNode mixin = new ClassNode();
        try (var input = getClass().getResourceAsStream(mixinName + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(mixin, 0);
        }
        var hook = mixin.methods.stream().filter(method -> method.name.startsWith("narcissus$")).findFirst().orElseThrow();
        var annotations = hook.visibleAnnotations == null ? List.<org.objectweb.asm.tree.AnnotationNode>of() : hook.visibleAnnotations;
        var inject = annotations.stream().filter(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;"))
                .findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        List<String> selectors = (List<String>) inject.values.get(inject.values.indexOf("method") + 1);
        var arguments = Type.getArgumentTypes(hook.desc);
        assertEquals("org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable",
                arguments[arguments.length - 1].getClassName());
        for (var target : ServerPlayer.class.getDeclaredMethods()) {
            if (!target.getName().equals(targetName) || target.isBridge()) continue;
            String descriptor = Type.getMethodDescriptor(target);
            if (!selectors.contains(targetName) && !selectors.contains(targetName + descriptor)) continue;
            assertArrayEquals(Type.getArgumentTypes(descriptor), java.util.Arrays.copyOf(arguments, arguments.length - 1));
            return;
        }
        fail("No Minecraft player method matched " + selectors);
    }
}
