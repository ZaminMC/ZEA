package net.zaminmc.zea.share;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasteKeyTest {
	@Test
	void bareKeysParse() throws ShareException {
		assertEquals("wozajojele", PasteKey.parse("wozajojele").key());
	}

	@Test
	void presentationUrlsParse() throws ShareException {
		PasteKey key = PasteKey.parse("https://paste.helpcha.at/wozajojele.java");

		assertEquals("wozajojele", key.key());
		assertEquals("https://paste.helpcha.at/wozajojele", key.url());
	}

	@Test
	void rawUrlsParse() throws ShareException {
		assertEquals("wozajojele", PasteKey.parse("https://paste.helpcha.at/raw/wozajojele").key());
	}

	@Test
	void jsonExtensionsAreStrippedLikeJavaOnes() throws ShareException {
		assertEquals("abcdef", PasteKey.parse("https://paste.helpcha.at/abcdef.json").key());
		assertEquals("abcdef", PasteKey.parse("abcdef.json").key());
	}

	@Test
	void inputIsTrimmed() throws ShareException {
		assertEquals("wozajojele", PasteKey.parse("  wozajojele \n").key());
	}

	@Test
	void hostIsCaseInsensitive() throws ShareException {
		assertEquals("wozajojele", PasteKey.parse("https://PASTE.HELPCHA.AT/wozajojele").key());
	}

	@Test
	void otherHostsAreRejected() {
		ShareException e = assertThrows(ShareException.class, () ->
				PasteKey.parse("https://evil.example/wozajojele"));

		assertTrue(e.getMessage().contains("paste.helpcha.at"), e.getMessage());
	}

	@Test
	void httpIsRejected() {
		assertThrows(ShareException.class, () ->
				PasteKey.parse("http://paste.helpcha.at/wozajojele"));
	}

	@Test
	void schemelessUrlLikeInputIsRejected() {
		assertThrows(ShareException.class, () ->
				PasteKey.parse("paste.helpcha.at/wozajojele"));
	}

	@Test
	void lookalikeHostsAreRejected() {
		assertThrows(ShareException.class, () ->
				PasteKey.parse("https://paste.helpcha.at.evil.example/wozajojele"));
	}

	@Test
	void queriesAndFragmentsAreRejected() {
		assertThrows(ShareException.class, () ->
				PasteKey.parse("https://paste.helpcha.at/wozajojele?x=1"));
		assertThrows(ShareException.class, () ->
				PasteKey.parse("https://paste.helpcha.at/wozajojele#top"));
	}

	@Test
	void traversalShapedKeysAreRejected() {
		assertThrows(ShareException.class, () -> PasteKey.parse("../etc/passwd"));
		assertThrows(ShareException.class, () -> PasteKey.parse("https://paste.helpcha.at/../other"));
		assertThrows(ShareException.class, () -> PasteKey.parse("https://paste.helpcha.at/raw/../secret"));
		assertThrows(ShareException.class, () -> PasteKey.parse("a/b"));
		assertThrows(ShareException.class, () -> PasteKey.parse("a%2Fb"));
	}

	@Test
	void emptyAndNullInputIsRejected() {
		assertThrows(ShareException.class, () -> PasteKey.parse(""));
		assertThrows(ShareException.class, () -> PasteKey.parse("   "));
		assertThrows(ShareException.class, () -> PasteKey.parse(null));
	}

	@Test
	void overlongKeysAreRejected() {
		assertThrows(ShareException.class, () -> PasteKey.parse("a".repeat(65)));
	}

	@Test
	void keysWithDotsOnlyInTheExtensionAreAccepted() throws ShareException {
		// A dot at position 0 leaves an empty key — rejected.
		assertThrows(ShareException.class, () -> PasteKey.parse(".java"));
		// A dot after valid key characters is the extension.
		assertEquals("ab", PasteKey.parse("ab.txt").key());
	}

	@Test
	void underscoreAndDashKeysAreAccepted() throws ShareException {
		assertEquals("a-b_c", PasteKey.parse("a-b_c").key());
	}

	@Test
	void deepPathsAreRejected() {
		assertThrows(ShareException.class, () ->
				PasteKey.parse("https://paste.helpcha.at/a/b/c"));
		assertThrows(ShareException.class, () ->
				PasteKey.parse("https://paste.helpcha.at/raw/a/b"));
	}

	@Test
	void theRecordConstructorValidatesToo() {
		assertThrows(IllegalArgumentException.class, () -> new PasteKey("not a key"));
		assertThrows(IllegalArgumentException.class, () -> new PasteKey(null));
	}
}
