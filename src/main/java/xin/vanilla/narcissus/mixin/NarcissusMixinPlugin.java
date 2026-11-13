package xin.vanilla.narcissus.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public final class NarcissusMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> mine, Set<String> others) {
    }

    @Override
    public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {
    }

    @Override
    public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {
    }

    public static boolean supportsFtb(ClassNode node) {
        if (node.methods.stream().noneMatch(method -> method.name.equals("draw") && method.desc.endsWith(")V")))
            return false;
        return has(node, "onMousePressed", "(Ldev/ftb/mods/ftblibrary/ui/BaseScreen;Ldev/ftb/mods/ftblibrary/ui/input/MouseButton;)Z")
                && has(node, "onKeyPressed", "(Ldev/ftb/mods/ftblibrary/ui/BaseScreen;Ldev/ftb/mods/ftblibrary/ui/input/Key;)Z")
                && has(node, "isVisible", "(Ldev/ftb/mods/ftbchunks/api/client/icon/MapType;DZ)Z");
    }

    private static boolean has(ClassNode node, String name, String descriptor) {
        return node.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor));
    }

    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        if (!mixin.endsWith(".FtbWaypointIconMixin")) return true;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(target.replace('.', '/') + ".class")) {
            if (input == null) return false;
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            return supportsFtb(node);
        } catch (Exception | LinkageError failure) {
            return false;
        }
    }
}

