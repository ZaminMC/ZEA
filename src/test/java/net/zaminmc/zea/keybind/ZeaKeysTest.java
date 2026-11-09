package net.zaminmc.zea.keybind;

import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins the registration contract: every {@link KeyMapping} ZEA defines
 * must be handed to {@link ZeaKeys#register()}. A consumed-but-unregistered
 * mapping stays unbound forever and its feature is unreachable — exactly
 * the bug this test exists to prevent (EDIT_BUTTONS was checked in
 * ContainerScreenMixin but never registered, leaving button editing
 * keyless).
 *
 * <p>{@code KeyMappingHelper.registerKeyMapping} needs the Fabric runtime,
 * so the test verifies the delegation list {@link ZeaKeys#registered()}
 * instead of calling the loader itself.
 */
class ZeaKeysTest {
	@Test
	void everyKeyMappingIsRegistered() {
		Set<KeyMapping> defined = new HashSet<>();
		for (Field field : ZeaKeys.class.getDeclaredFields()) {
			if (!field.isSynthetic() && Modifier.isStatic(field.getModifiers())
					&& field.getType() == KeyMapping.class) {
				try {
					field.setAccessible(true);
					defined.add((KeyMapping) field.get(null));
				} catch (ReflectiveOperationException e) {
					throw new AssertionError("cannot read " + field.getName(), e);
				}
			}
		}
		List<KeyMapping> registered = ZeaKeys.registered();
		assertNotNull(registered, "registration list must exist after class load");
		assertEquals(defined, new HashSet<>(registered),
				"every defined key mapping must be in the registration list");
	}
}
