package net.zaminmc.zea.mixin;

import net.zaminmc.zea.VanillaBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the MixinExtras {@code @WrapOperation} handler contract against the
 * compiled mixins and the mapped Minecraft jar, so a wrong handler
 * signature fails the build instead of the game. The dev.18 build crashed
 * at startup — before the title screen — because MentionHighlightMixin's
 * handler captured the enclosing lambda's arguments before the wrapped
 * call's and omitted the receiver; MixinExtras validates the layout only
 * when the mixin applies, which never happens in a unit-test build.
 *
 * <p>The contract, read off the MixinExtras 0.5.5 sources
 * ({@code WrapOperationInjector#invokeHandler} and
 * {@code OperationUtils#generateSyntheticBridge}) and its wiki: the
 * handler takes the wrapped invocation's receiver (unless the wrapped
 * member is static) and arguments, then the {@code Operation}, then
 * optionally a prefix of the enclosing method's arguments; the handler's
 * return type is the wrapped call's; a static enclosing method requires
 * a static handler; and {@code original.call} must be passed the receiver
 * together with the wrapped arguments — the generated bridge counts them
 * exactly and throws {@code IncorrectArgumentCountException} otherwise.
 */
class WrapOperationSignatureTest {
	/** The mixin package this mod owns, as an internal (slash) name prefix. */
	private static final String MIXIN_PREFIX = "net/zaminmc/zea/mixin/";

	/** The compiled main classes, relative to the project Gradle runs tests from. */
	private static final Path MAIN_CLASSES = Paths.get("build", "classes", "java", "main");

	private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
	private static final String WRAP_OPERATION_DESC =
			"Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
	private static final String OPERATION_DESC =
			"Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;";
	private static final String OPERATION_OWNER =
			"com/llamalad7/mixinextras/injector/wrapoperation/Operation";
	private static final String OPERATION_CALL_DESC = "([Ljava/lang/Object;)Ljava/lang/Object;";

	/** A member reference in Mixin's {@code @At target} form: {@code Lowner;name(desc)ret}. */
	private static final Pattern TARGET = Pattern.compile("^L([^;]+);([^.]+)\\((.*)\\)(.+)$");

	/** A compiled {@code @WrapOperation} handler, plus everything needed to judge it. */
	private record Handler(String mixin, String targetClass, String enclosing, String atTarget, MethodNode method) {
		String name() {
			return method.name;
		}

		String descriptor() {
			return method.desc;
		}

		boolean isStatic() {
			return (method.access & Opcodes.ACC_STATIC) != 0;
		}
	}

	private static final List<Handler> handlers = new ArrayList<>();

	@BeforeAll
	static void collectHandlers() throws IOException {
		assumeTrue(Files.isDirectory(MAIN_CLASSES),
				"compiled classes not found — run through Gradle");
		VanillaBootstrap.ensure();
		try (Stream<Path> paths = Files.walk(MAIN_CLASSES)) {
			for (Path file : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
				String internal = MAIN_CLASSES.relativize(file).toString()
								.replace('\\', '/').replace(".class", "");
				if (!internal.startsWith(MIXIN_PREFIX)) {
					continue;
				}
				ClassNode node = new ClassNode(Opcodes.ASM9);
				new ClassReader(Files.readAllBytes(file)).accept(node, 0);
				AnnotationNode mixin = annotationOf(node.visibleAnnotations, node.invisibleAnnotations, MIXIN_DESC);
				Object target = mixin == null ? null : annotationValue(mixin, "value");
				if (!(target instanceof List<?> classes) || classes.isEmpty()
								|| !(classes.get(0) instanceof Type mixinTarget)) {
					continue;
				}
				for (MethodNode method : node.methods) {
					AnnotationNode wrap = annotationOf(
									method.visibleAnnotations, method.invisibleAnnotations, WRAP_OPERATION_DESC);
					if (wrap == null) {
						continue;
					}
					Object methodValue = annotationValue(wrap, "method");
					Object atValue = annotationValue(wrap, "at");
					if (!(methodValue instanceof List<?> methods) || methods.size() != 1
									|| !(methods.get(0) instanceof String enclosing)
									|| !(atValue instanceof List<?> ats) || ats.size() != 1
									|| !(ats.get(0) instanceof AnnotationNode at)
									|| !(annotationValue(at, "target") instanceof String atTarget)) {
						throw new IllegalStateException("unsupported @WrapOperation shape in " + internal);
					}
					handlers.add(new Handler(internal, mixinTarget.getInternalName(), enclosing, atTarget, method));
				}
			}
		}
	}

	@Test
	void wrapOperationHandlersMatchTheMixinExtrasContract() {
		assertFalse(handlers.isEmpty(),
				"no @WrapOperation handlers found — the scan is broken, not the mixins");
		List<String> violations = new ArrayList<>();
		for (Handler handler : handlers) {
			violations.addAll(checkSignature(handler));
			violations.addAll(checkOriginalCall(handler));
		}
		assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
	}

	/** The handler's signature against receiver + wrapped args + Operation + enclosing prefix. */
	private static List<String> checkSignature(Handler handler) {
		String where = handler.mixin() + "#" + handler.name();
		List<String> violations = new ArrayList<>();
		Matcher target = TARGET.matcher(handler.atTarget());
		if (!target.matches()) {
			return List.of(where + ": unparseable @At target " + handler.atTarget());
		}
		String owner = target.group(1);
		String wrappedDesc = "(" + target.group(3) + ")" + target.group(4);

		Method wrapped = findMethod(classFor(owner), target.group(2), wrappedDesc);
		if (wrapped == null) {
			return List.of(where + ": @At target " + handler.atTarget()
							+ " does not name a declared method of the mapped jar");
		}

		// Receiver (unless the wrapped member is static) then the wrapped
		// call's arguments, exactly as WrapOperationInjector pushes them.
		List<String> expected = new ArrayList<>();
		if (!Modifier.isStatic(wrapped.getModifiers())) {
			expected.add("L" + owner + ";");
		}
		for (Type argument : Type.getArgumentTypes(wrappedDesc)) {
			expected.add(argument.getDescriptor());
		}
		expected.add(OPERATION_DESC);

		Type[] actual = Type.getArgumentTypes(handler.descriptor());
		if (actual.length < expected.size()) {
			violations.add(where + ": handler has " + actual.length + " arguments but the contract needs at least "
							+ expected.size() + " (receiver + wrapped arguments + Operation)");
			return violations;
		}
		for (int i = 0; i < expected.size(); i++) {
			if (!actual[i].getDescriptor().equals(expected.get(i))) {
				violations.add(where + ": handler argument " + i + " is " + actual[i].getDescriptor()
								+ " but the contract expects " + expected.get(i)
								+ " (the wrapped call's receiver and arguments come first, then Operation,"
								+ " then a prefix of the enclosing method's arguments)");
			}
		}

		// The trailing arguments must be a prefix of the enclosing method's.
		Method enclosing = resolveEnclosing(handler);
		if (enclosing == null) {
			violations.add(where + ": method \"" + handler.enclosing() + "\" does not name exactly one declared"
							+ " method of " + handler.targetClass() + " in the mapped jar");
			return violations;
		}
		if (Modifier.isStatic(enclosing.getModifiers()) && !handler.isStatic()) {
			violations.add(where + ": handler must be static because the enclosing "
							+ enclosing.getName() + " is");
		}
		int trailing = actual.length - expected.size();
		Type[] enclosingArguments = Arrays.stream(enclosing.getParameterTypes())
						.map(Type::getType).toArray(Type[]::new);
		if (trailing > enclosingArguments.length) {
			violations.add(where + ": handler captures " + trailing
							+ " enclosing arguments but the enclosing method only takes "
							+ enclosingArguments.length);
		}
		for (int i = 0; i < Math.min(trailing, enclosingArguments.length); i++) {
			if (!actual[expected.size() + i].getDescriptor().equals(enclosingArguments[i].getDescriptor())) {
				violations.add(where + ": handler argument " + (expected.size() + i) + " is "
								+ actual[expected.size() + i].getDescriptor() + " but the enclosing method's argument "
								+ i + " is " + enclosingArguments[i].getDescriptor());
			}
		}

		Type expectedReturn = Type.getReturnType(wrappedDesc);
		if (!Type.getReturnType(handler.descriptor()).equals(expectedReturn)) {
			violations.add(where + ": handler returns " + Type.getReturnType(handler.descriptor())
							+ " but the wrapped call returns " + expectedReturn);
		}
		return violations;
	}

	/** That {@code original.call} receives exactly the receiver and the wrapped arguments. */
	private static List<String> checkOriginalCall(Handler handler) {
		String where = handler.mixin() + "#" + handler.name();
		Matcher target = TARGET.matcher(handler.atTarget());
		if (!target.matches()) {
			return List.of();
		}
		String owner = target.group(1);
		String wrappedDesc = "(" + target.group(3) + ")" + target.group(4);
		Method wrapped = findMethod(classFor(owner), target.group(2), wrappedDesc);
		if (wrapped == null) {
			return List.of(); // checkSignature already reports the missing member
		}
		int expected = Type.getArgumentTypes(wrappedDesc).length;
		if (!Modifier.isStatic(wrapped.getModifiers())) {
			expected++; // the receiver is the first call argument
		}
		List<String> violations = new ArrayList<>();
		boolean buildingArray = false;
		int arguments = 0;
		for (AbstractInsnNode insn : handler.method().instructions) {
			if (insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.ANEWARRAY
							&& "java/lang/Object".equals(type.desc)) {
				buildingArray = true;
				arguments = 0;
			} else if (insn.getOpcode() == Opcodes.AASTORE && buildingArray) {
				arguments++;
			} else if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEINTERFACE
							&& OPERATION_OWNER.equals(call.owner) && "call".equals(call.name)
							&& OPERATION_CALL_DESC.equals(call.desc)) {
				if (!buildingArray || arguments != expected) {
					violations.add(where + ": original.call is passed " + (buildingArray ? arguments : "no")
									+ " argument(s) but the wrapped call needs " + expected
									+ " — the receiver counts too; fewer or more throws"
									+ " IncorrectArgumentCountException in game");
				}
				buildingArray = false;
			}
		}
		return violations;
	}

	/** The enclosing method the annotation names, by bare name (must be unique) or by descriptor. */
	private static Method resolveEnclosing(Handler handler) {
		Class<?> target = classFor(handler.targetClass());
		int descriptorStart = handler.enclosing().indexOf('(');
		String name = descriptorStart < 0
				? handler.enclosing()
				: handler.enclosing().substring(0, descriptorStart);
		String descriptor = descriptorStart < 0 ? null
				: handler.enclosing().substring(descriptorStart);
		List<Method> candidates = new ArrayList<>();
		for (Method declared : target.getDeclaredMethods()) {
			if (!declared.getName().equals(name)) {
				continue;
			}
			if (descriptor == null || descriptorOf(declared).equals(descriptor)) {
				candidates.add(declared);
			}
		}
		return candidates.size() == 1 ? candidates.get(0) : null;
	}

	private static String descriptorOf(Method method) {
		return "(" + Arrays.stream(method.getParameterTypes())
				.map(Type::getType).map(Type::getDescriptor)
				.reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append) + ")"
				+ Type.getDescriptor(method.getReturnType());
	}

	private static Method findMethod(Class<?> owner, String name, String descriptor) {
		for (Method declared : owner.getDeclaredMethods()) {
			if (declared.getName().equals(name) && descriptorOf(declared).equals(descriptor)) {
				return declared;
			}
		}
		return null;
	}

	private static Class<?> classFor(String internal) {
		try {
			return Class.forName(internal.replace('/', '.'));
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("mapped class not on the test classpath: " + internal, e);
		}
	}

	private static AnnotationNode annotationOf(List<AnnotationNode> visible, List<AnnotationNode> invisible,
			String descriptor) {
		if (visible != null) {
			for (AnnotationNode annotation : visible) {
				if (annotation.desc.equals(descriptor)) {
					return annotation;
				}
			}
		}
		if (invisible != null) {
			for (AnnotationNode annotation : invisible) {
				if (annotation.desc.equals(descriptor)) {
					return annotation;
				}
			}
		}
		return null;
	}

	private static Object annotationValue(AnnotationNode annotation, String key) {
		if (annotation.values == null) {
			return null;
		}
		for (int i = 0; i < annotation.values.size(); i += 2) {
			if (key.equals(annotation.values.get(i))) {
				return annotation.values.get(i + 1);
			}
		}
		return null;
	}
}
