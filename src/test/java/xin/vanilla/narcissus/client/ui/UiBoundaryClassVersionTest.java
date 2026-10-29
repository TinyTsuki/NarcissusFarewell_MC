package xin.vanilla.narcissus.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.Map;

import static org.junit.Assert.*;

public class UiBoundaryClassVersionTest {
    private Map<String, byte[]> archive(int version) {
        String name = "xin/vanilla/narcissus/BoundaryFixture";
        ClassWriter writer = new ClassWriter(0);
        writer.visit(version, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        writer.visitEnd();
        return Map.of(name + ".class", writer.toByteArray());
    }

    @Test
    public void minecraft21ArchiveAcceptsJava21Classes() {
        assertTrue(UiBoundaryChecks.artifact(archive(Opcodes.V21), "xin/vanilla/narcissus/", false).isEmpty());
    }

    @Test
    public void minecraft21ArchiveStillRejectsFutureJavaClasses() {
        assertTrue(UiBoundaryChecks.artifact(archive(Opcodes.V22), "xin/vanilla/narcissus/", false)
                .stream().anyMatch(error -> error.startsWith("Future class ")));
    }
}
