package net.zaminmc.zea.config;

import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code /zea} menu's preprocessing — the dev.24 live defect was that
 * the menu showed nothing: the editor was built over a processor that had
 * no editor factories registered and had never been driven, so it held
 * zero categories and zero options. These tests pin both halves of the
 * recipe ({@code withDefaults} and the driver) at the processor level;
 * the editor and screen themselves need a Minecraft runtime to render.
 */
class ConfigScreenTest {
	@Test
	void processingFinalizesTheProcessor() {
		MoulConfigProcessor<ZeaConfig> processor = ConfigScreen.process(new ZeaConfig());

		assertTrue(processor.isFinalized(),
				"the driver never ran — the editor would render an empty config");
	}

	@Test
	void processingExposesEveryConfigCategory() {
		MoulConfigProcessor<ZeaConfig> processor = ConfigScreen.process(new ZeaConfig());

		Map<String, ? extends ProcessedCategory> categories = processor.getAllCategories();

		assertFalse(categories.isEmpty(),
				"no categories survived processing — the /zea screen would be blank");
		assertTrue(categories.size() >= 7,
				"expected at least the seven config categories, found " + categories.keySet());
	}

	@Test
	void everyCategoryCarriesItsOptions() {
		MoulConfigProcessor<ZeaConfig> processor = ConfigScreen.process(new ZeaConfig());

		for (ProcessedCategory category : processor.getAllCategories().values()) {
			assertFalse(category.getOptions().isEmpty(),
					category.getIdentifier() + " lost all its options —"
							+ " no editor factory matched its fields");
		}
	}
}
