package net.zaminmc.zea.chat.render;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the chat widgets out of 26.3's keyboard focus navigation. Vanilla
 * screens move focus with Tab and the arrow keys; with the tab strip and
 * search bar registered as ordinary focusable children, a history-recall
 * up-arrow while typing moved screen focus onto them and silently ate
 * every following keystroke — the live report of losing the caret
 * mid-sentence. Both widgets therefore override {@code nextFocusPath} to
 * return null. Instantiating them needs a real {@code Font} (the tab bar
 * measures its labels at construction), so this reads the compiled
 * bytecode instead — the same approach ChatSubsystemWiringTest takes —
 * and asserts the override exists and can only answer null.
 */
class ChatWidgetsFocusTest {
	private static final Path MAIN_CLASSES = Paths.get("build", "classes", "java", "main");

	@Test
	void theChatWidgetsNeverOfferThemselvesAsKeyboardFocusTargets() throws IOException {
		refusesFocusNavigation("net/zaminmc/zea/chat/render/ChatTabBar.class");
		refusesFocusNavigation("net/zaminmc/zea/chat/render/ChatSearchBar.class");
	}

	private static void refusesFocusNavigation(String classFile) throws IOException {
		assumeTrue(Files.isDirectory(MAIN_CLASSES), "compiled classes not found — run through Gradle");
		ClassNode node = new ClassNode(Opcodes.ASM9);
		new ClassReader(Files.readAllBytes(MAIN_CLASSES.resolve(classFile))).accept(node, 0);

		MethodNode override = null;
		for (MethodNode method : node.methods) {
			if ("nextFocusPath".equals(method.name)) {
				override = method;
				break;
			}
		}
		assertNotNull(override, classFile + " does not override nextFocusPath — keyboard focus"
				+ " navigation can move the caret onto it while the player types");

		boolean pushedNull = false;
		for (AbstractInsnNode insn : override.instructions) {
			int opcode = insn.getOpcode();
			if (opcode == Opcodes.ACONST_NULL) {
				pushedNull = true;
			} else if (opcode == Opcodes.ARETURN) {
				// The single return of a null-only method.
			} else if (opcode == -1) {
				// Frame and line-number pseudo-instructions.
			} else {
				fail(classFile + "#nextFocusPath does more than return null —"
						+ " it may answer a focus navigation request");
			}
		}
		assertTrue(pushedNull, classFile + "#nextFocusPath returns something other than null");
	}
}
