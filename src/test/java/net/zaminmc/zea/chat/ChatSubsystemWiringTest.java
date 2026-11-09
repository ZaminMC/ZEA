package net.zaminmc.zea.chat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins ChatSubsystem's construction order against its compiled bytecode.
 * The dev.23 live crash: the {@code feedFilter} field initializer called
 * {@code classifications()} while the {@code classifications} field was
 * still null — Java runs field initializers before the constructor body,
 * so every FeedFilter ever built held a null cache, and the first tab
 * click or incoming message dereferenced it inside vanilla's chat
 * (NPE in the click handler; "Network Protocol Error" disconnect on the
 * packet path). Javac happily compiles an initializer that calls an
 * accessor; only the compiled order tells the truth, and
 * ChatSubsystem cannot be constructed in a unit-test JVM (it registers
 * Fabric events and binds Minecraft), so this test reads the compiled
 * constructor instead — the same approach WrapOperationSignatureTest
 * takes for mixin handler signatures.
 *
 * <p>The rule: the constructor must not read the classification cache —
 * directly or through its accessor — before it assigns it.
 */
class ChatSubsystemWiringTest {
	/** The compiled main classes, relative to the project Gradle runs tests from. */
	private static final Path MAIN_CLASSES = Paths.get("build", "classes", "java", "main");

	@Test
	void theConstructorNeverReadsTheClassificationCacheBeforeAssigningIt() throws IOException {
		assumeTrue(Files.isDirectory(MAIN_CLASSES),
				"compiled classes not found — run through Gradle");
		ClassNode node = new ClassNode(Opcodes.ASM9);
		new ClassReader(Files.readAllBytes(MAIN_CLASSES.resolve(
				"net/zaminmc/zea/chat/ChatSubsystem.class"))).accept(node, 0);
		MethodNode constructor = node.methods.stream()
				.filter(method -> "<init>".equals(method.name))
				.reduce((first, second) -> {
					throw new IllegalStateException("multiple constructors");
				})
				.orElseThrow(() -> new IllegalStateException("ChatSubsystem has no constructor"));

		int assignment = -1;
		int firstRead = -1;
		for (int i = 0; i < constructor.instructions.size(); i++) {
			AbstractInsnNode insn = constructor.instructions.get(i);
			if (insn instanceof FieldInsnNode field
					&& "classifications".equals(field.name)
					&& node.name.equals(field.owner)) {
				if (field.getOpcode() == Opcodes.PUTFIELD && assignment < 0) {
					assignment = i;
				} else if (field.getOpcode() == Opcodes.GETFIELD && firstRead < 0) {
					firstRead = i;
				}
			} else if (insn instanceof MethodInsnNode call
					&& "classifications".equals(call.name)
					&& node.name.equals(call.owner)) {
				// The accessor reads the field on the caller's behalf.
				if (firstRead < 0) {
					firstRead = i;
				}
			}
		}

		assertTrue(assignment >= 0,
				"the constructor never assigns the classification cache");
		assertTrue(firstRead < 0 || firstRead > assignment,
				"the constructor reads the classification cache (directly or through"
						+ " classifications()) before assigning it — a field initializer"
						+ " is running before the constructor body; the feed filter it"
						+ " builds will hold a null cache and crash the chat in game"
						+ " (the dev.23 live defect)");
	}
}
