package net.zaminmc.zea.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guards the mixin package's loadability rules, which Mixin enforces at
 * runtime and nothing else checks at build time:
 *
 * <ul>
 * <li>Mixin owns every class under its config package outright. When the
 * JVM loads one — even just to resolve a {@code checkcast} or a
 * superinterface — Mixin either strips it (registered <i>accessor</i>
 * mixins, via its accessor coprocessor) or throws
 * {@code IllegalClassLoadError: ... cannot be referenced directly}.
 * <li>A duck interface a mixin adds to a vanilla class is resolved
 * <i>eagerly</i>, when the JVM defines the transformed target: it sits in
 * the target's interface list. A duck interface inside the mixin package
 * therefore crashed every launch before the title screen until dev.16
 * (see WORKLOG 2026-11-09).
 * </ul>
 *
 * <p>These tests scan the compiled production classes, so a stray class
 * or an illegal reference fails the build instead of the game.
 */
class MixinPackageHygieneTest {
        /** The mixin package this mod owns, as an internal (slash) name prefix. */
        private static final String MIXIN_PREFIX = "net/zaminmc/zea/mixin/";

        /** The compiled main classes, relative to the project Gradle runs tests from. */
        private static final Path MAIN_CLASSES = Paths.get("build", "classes", "java", "main");

        /** The mixin config, whose lists define what "registered" means. */
        private static final Path MIXIN_CONFIG = Paths.get("src", "main", "resources", "zea.mixins.json");

        /** Class names the mixin config registers, in internal form. */
        private static Set<String> registered;

        /** Every production class file, keyed by internal name. */
        private static List<Path> classFiles;

        @BeforeAll
        static void readConfigAndClasses() throws IOException {
                assumeTrue(Files.isDirectory(MAIN_CLASSES),
                                "compiled classes not found — run through Gradle");
                try (Reader reader = Files.newBufferedReader(MIXIN_CONFIG, StandardCharsets.UTF_8)) {
                        JsonObject config = new Gson().fromJson(reader, JsonObject.class);
                        String pkg = config.get("package").getAsString();
                        registered = new HashSet<>();
                        for (String key : List.of("mixins", "client", "server")) {
                                if (config.has(key)) {
                                        for (var entry : config.getAsJsonArray(key)) {
                                                registered.add(pkg.replace('.', '/') + "/" + entry.getAsString());
                                        }
                                }
                        }
                }
                try (Stream<Path> paths = Files.walk(MAIN_CLASSES)) {
                        classFiles = paths.filter(p -> p.toString().endsWith(".class")).toList();
                }
        }

        @Test
        void everyClassInTheMixinPackageIsRegistered() {
                List<String> strays = new ArrayList<>();
                for (Path file : classFiles) {
                        String internal = internalNameOf(file);
                        if (internal.startsWith(MIXIN_PREFIX) && !registered.contains(internal)) {
                                strays.add(internal);
                        }
                }
                assertTrue(strays.isEmpty(), () -> "Classes in the mixin package that zea.mixins.json does not"
                                + " register: " + strays + ". Mixin refuses to load unregistered classes from its"
                                + " package — including nested classes — so any of them referenced by merged mixin"
                                + " bytecode, or merged into a target's interface list, crashes the game."
                                + " Move duck interfaces to net.zaminmc.zea.duck and helper types to a normal"
                                + " package.");
        }

        @Test
        void productionCodeReferencesOnlyLoadableMixinClasses() throws IOException {
                List<String> violations = new ArrayList<>();
                for (Path file : classFiles) {
                        String owner = internalNameOf(file);
                        for (String reference : referencesOf(file)) {
                                if (reference.equals(owner) || !reference.startsWith(MIXIN_PREFIX)) {
                                        continue;
                                }
                                if (!isLoadableMixinClass(reference)) {
                                        violations.add(owner + " -> " + reference);
                                }
                        }
                }
                assertTrue(violations.isEmpty(), () -> "Production classes referencing mixin-package classes"
                                + " Mixin would refuse to load: " + violations + ". Only registered accessor"
                                + " mixins (an @Mixin interface whose members are all @Accessor/@Invoker) may be"
                                + " referenced directly; everything else in the mixin package exists only to be"
                                + " merged into its target. This exact shape crashed dev.16 at startup.");
        }

        /**
         * Whether a direct classload of the mixin-package class would pass Mixin's
         * accessor carve-out: registered, an interface, annotated {@code @Mixin},
         * and free of members other than accessors and invokers.
         */
        private static boolean isLoadableMixinClass(String internal) throws IOException {
                if (!registered.contains(internal)) {
                        return false;
                }
                ClassNode node = readClass(internal);
                if (node == null || (node.access & Opcodes.ACC_INTERFACE) == 0) {
                        return false;
                }
                // Mixin's annotations have CLASS retention, so ASM reports them as
                // invisible (RetentionPolicy.CLASS), not visible (RUNTIME).
                if (!hasAnnotation(node.visibleAnnotations, node.invisibleAnnotations,
                                "Lorg/spongepowered/asm/mixin/Mixin;")) {
                        return false;
                }
                if (node.fields != null && !node.fields.isEmpty()) {
                        return false;
                }
                for (MethodNode method : node.methods) {
                        boolean accessorOrInvoker = hasAnnotation(method.visibleAnnotations, method.invisibleAnnotations,
                                        "Lorg/spongepowered/asm/mixin/gen/Accessor;")
                                        || hasAnnotation(method.visibleAnnotations, method.invisibleAnnotations,
                                                "Lorg/spongepowered/asm/mixin/gen/Invoker;");
                        if (!accessorOrInvoker) {
                                return false;
                        }
                }
                return true;
        }

        /** Whether either annotation list contains the descriptor. */
        private static boolean hasAnnotation(List<org.objectweb.asm.tree.AnnotationNode> visible,
                        List<org.objectweb.asm.tree.AnnotationNode> invisible, String descriptor) {
                return (visible != null && visible.stream().anyMatch(a -> a.desc.equals(descriptor)))
                                || (invisible != null && invisible.stream().anyMatch(a -> a.desc.equals(descriptor)));
        }

        /** Every class name the class's constant pool can reference. */
        private static Set<String> referencesOf(Path file) throws IOException {
                Set<String> references = new HashSet<>();
                new ClassReader(Files.readAllBytes(file)).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(int version, int access, String name, String signature,
                                        String superName, String[] interfaces) {
                                add(superName);
                                if (interfaces != null) {
                                        for (String implemented : interfaces) {
                                                add(implemented);
                                        }
                                }
                        }

                        @Override
                        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                                addFromDescriptor(desc);
                                return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                        String[] exceptions) {
                                addFromDescriptor(desc);
                                if (exceptions != null) {
                                        for (String thrown : exceptions) {
                                                add(thrown);
                                        }
                                }
                                return new MethodVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                                                add(owner);
                                                addFromDescriptor(desc);
                                        }

                                        @Override
                                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                                add(owner);
                                                addFromDescriptor(desc);
                                        }

                                        @Override
                                        public void visitTypeInsn(int opcode, String type) {
                                                add(type);
                                        }

                                        @Override
                                        public void visitInvokeDynamicInsn(String name, String desc, org.objectweb.asm.Handle bsm,
                                                        Object... bsmArgs) {
                                                addFromDescriptor(desc);
                                                addFromMethodType(bsm.getDesc());
                                                for (Object argument : bsmArgs) {
                                                        if (argument instanceof org.objectweb.asm.Type type) {
                                                                addFromDescriptor(type.getDescriptor());
                                                        }
                                                }
                                        }

                                        @Override
                                        public void visitLdcInsn(Object value) {
                                                if (value instanceof org.objectweb.asm.Type type && type.getSort() == org.objectweb.asm.Type.OBJECT) {
                                                        add(type.getInternalName());
                                                }
                                        }
                                };
                        }

                        private void add(String internal) {
                                if (internal != null) {
                                        references.add(internal);
                                }
                        }

                        private void addFromDescriptor(String descriptor) {
                                if (descriptor != null) {
                                        Matcher matcher = Pattern.compile("L([^;]+);").matcher(descriptor);
                                        while (matcher.find()) {
                                                references.add(matcher.group(1));
                                        }
                                }
                        }

                        private void addFromMethodType(String descriptor) {
                                addFromDescriptor(descriptor);
                        }
                }, ClassReader.SKIP_FRAMES);
                return references;
        }

        private static ClassNode readClass(String internal) throws IOException {
                Path file = MAIN_CLASSES.resolve(internal + ".class");
                if (!Files.isRegularFile(file)) {
                        return null;
                }
                ClassNode node = new ClassNode(Opcodes.ASM9);
                new ClassReader(Files.readAllBytes(file)).accept(node, ClassReader.SKIP_CODE);
                return node;
        }

        private static String internalNameOf(Path file) {
                return MAIN_CLASSES.relativize(file).toString().replace('\\', '/').replace(".class", "");
        }
}
