package net.zaminmc.zea.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the contract the MoulConfig processor reads via reflection. The
 * processor itself needs a Minecraft runtime, so these tests verify the
 * structure it consumes instead: categories resolvable, every option
 * public and non-static, every option paired with an editor whose type
 * matches the field.
 */
class ZeaConfigStructureTest {
	private static final List<Class<? extends Annotation>> EDITOR_ANNOTATIONS = List.of(
			ConfigEditorBoolean.class, ConfigEditorSlider.class, ConfigEditorDropdown.class);

	@Test
	void extendsMoulConfigBase() {
		assertTrue(Config.class.isAssignableFrom(ZeaConfig.class));
	}

	@Test
	void categoryFieldsAreAnnotatedAndResolvable() {
		for (Field field : ZeaConfig.class.getDeclaredFields()) {
			if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (field.getType().getDeclaringClass() == null) {
				continue; // framework-owned fields
			}
			assertTrue(field.isAnnotationPresent(Category.class),
					field.getName() + " is a category holder and needs @Category");
			assertTrue(field.isAnnotationPresent(Category.class)
					&& !field.getAnnotation(Category.class).name().isBlank(),
					field.getName() + " needs a category name");
		}
	}

	@Test
	void everyOptionIsPublicNonStaticAndPairedWithAMatchingEditor() {
		for (Class<?> category : categoryTypes()) {
			for (Field field : category.getDeclaredFields()) {
				if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				assertTrue(field.isAnnotationPresent(ConfigOption.class),
						category.getSimpleName() + "." + field.getName() + " needs @ConfigOption");
				assertTrue(Modifier.isPublic(field.getModifiers()),
						category.getSimpleName() + "." + field.getName() + " must be public for the editor");
				List<Class<? extends Annotation>> editors = EDITOR_ANNOTATIONS.stream()
						.filter(a -> field.isAnnotationPresent(a)).toList();
				assertTrue(!editors.isEmpty(),
						category.getSimpleName() + "." + field.getName() + " has no editor annotation");
				if (editors.contains(ConfigEditorSlider.class)) {
					assertTrue(field.getType() == int.class || field.getType() == float.class
							|| field.getType() == double.class || field.getType() == long.class,
							category.getSimpleName() + "." + field.getName() + " is a slider but not numeric");
				}
				if (editors.contains(ConfigEditorBoolean.class)) {
					assertTrue(field.getType() == boolean.class,
							category.getSimpleName() + "." + field.getName() + " is a toggle but not boolean");
				}
				if (editors.contains(ConfigEditorDropdown.class)) {
					assertTrue(field.getType() == String.class,
							category.getSimpleName() + "." + field.getName() + " is a dropdown but not a string");
				}
			}
		}
	}

	@Test
	void sliderRangeCoversTheDocumentedBounds() {
		ConfigEditorSlider slider;
		try {
			Field field = ZeaConfig.Chat.class.getDeclaredField("dispatchIntervalMillis");
			slider = field.getAnnotation(ConfigEditorSlider.class);
		} catch (NoSuchFieldException e) {
			throw new AssertionError("dispatchIntervalMillis disappeared", e);
		}
		assertTrue(slider.minValue() <= ZeaConfig.MIN_DISPATCH_INTERVAL_MILLIS
				&& slider.maxValue() >= ZeaConfig.MAX_DISPATCH_INTERVAL_MILLIS,
				"slider bounds must match the clamped accessor range");
	}

	private static List<Class<?>> categoryTypes() {
		List<Class<?>> categories = new ArrayList<>();
		for (Class<?> nested : ZeaConfig.class.getDeclaredClasses()) {
			for (Field field : ZeaConfig.class.getDeclaredFields()) {
				if (!field.isSynthetic() && field.getType() == nested
						&& field.isAnnotationPresent(Category.class)) {
					categories.add(nested);
					break;
				}
			}
		}
		assertTrue(!categories.isEmpty(), "ZeaConfig must expose at least one category");
		return categories;
	}
}
