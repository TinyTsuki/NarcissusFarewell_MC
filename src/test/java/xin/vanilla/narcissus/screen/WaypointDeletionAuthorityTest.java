package xin.vanilla.narcissus.screen;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.*;

public class WaypointDeletionAuthorityTest {
    @Test
    public void deleteRequestDoesNotMutateRowsOrSelectionBeforeServerConfirmation() throws Exception {
        int[] sendsAndMutations = new int[2];
        try (InputStream input = getClass().getResourceAsStream("/xin/vanilla/narcissus/screen/WaypointScreen.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (!name.equals("doActualDelete")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (owner.equals("xin/vanilla/banira/common/util/PacketUtils") && name.equals("sendPacketToServer")) {
                                sendsAndMutations[0]++;
                            }
                            if (name.equals("removeIf") || name.equals("remove") || name.equals("clear")
                                    || name.equals("applySearchFilter") || name.equals("selectionAfterDelete")) {
                                sendsAndMutations[1]++;
                            }
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            if (opcode == Opcodes.PUTFIELD && (name.equals("selectedItem") || name.equals("lastSelectedItem"))) {
                                sendsAndMutations[1]++;
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals("The original delete packet is still sent once", 1, sendsAndMutations[0]);
        assertEquals("Rejected deletion must retain rows and selection until authoritative sync", 0, sendsAndMutations[1]);
    }

    @Test
    public void authoritativeRefreshRetainsRejectedTargetAndClearsAcceptedDeletion() {
        WaypointSelectionState.Key selected = new WaypointSelectionState.Key("STAGE", "stage", "minecraft:overworld", null);
        WaypointSelectionState.Key synced = new WaypointSelectionState.Key("STAGE", "stage", "minecraft:overworld", null);
        assertSame(synced, WaypointSelectionState.afterRefresh(selected, null, Collections.singletonList(synced), key -> key));
        assertNull(WaypointSelectionState.afterRefresh(selected, null, Collections.<WaypointSelectionState.Key>emptyList(), key -> key));
    }


}
