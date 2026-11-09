package net.zaminmc.zea.share;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportPasteArgumentTest {
	private String parse(String input) throws CommandSyntaxException {
		return new ImportPasteArgument().parse(new StringReader(input));
	}

	@Test
	void bareKeysAndUrlsParseLikeAGreedyString() throws CommandSyntaxException {
		assertEquals("wozajojele", parse("wozajojele"));
		assertEquals("https://paste.helpch.at/wozajojele", parse("https://paste.helpch.at/wozajojele"));
		assertEquals("two words", parse("two words"));
		assertEquals("", parse(""));
	}

	@Test
	void exactlyTheReservedWordsAreRejected() {
		assertThrows(CommandSyntaxException.class, () -> parse("confirm"));
		assertThrows(CommandSyntaxException.class, () -> parse("cancel"));
	}

	@Test
	void onlyExactMatchesAreReserved() throws CommandSyntaxException {
		assertEquals("confirmation", parse("confirmation"));
		assertEquals("confirm this", parse("confirm this"));
		assertEquals("cancelled", parse("cancelled"));
	}

	@Test
	void examplesShowKeyAndUrl() {
		List<String> examples = new ArrayList<>(new ImportPasteArgument().getExamples());

		assertEquals(2, examples.size());
		assertTrue(examples.contains("wozajojele"));
		assertTrue(examples.contains("https://paste.helpch.at/wozajojele"));
	}

	/**
	 * The regression the reserved words exist for: Brigadier's ambiguity
	 * scan feeds each sibling's examples to the others' parsers, and used
	 * to report "[zea, import, confirm] vs [zea, import, paste]" when the
	 * paste operand was a plain greedy string. The real /zea import tree
	 * must scan clean.
	 */
	@Test
	void theImportTreeScansCleanOfAmbiguities() {
		List<String> reported = new ArrayList<>();
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		dispatcher.register(zeaRoot());

		dispatcher.findAmbiguities((parent, child, sibling, inputs) ->
				reported.add(child + " vs " + sibling + " on " + inputs));

		assertTrue(reported.isEmpty(), () -> "ambiguities: " + reported);
	}

	@Test
	void literalsStillWinOverThePasteOperand() throws CommandSyntaxException {
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		dispatcher.register(zeaRoot());

		assertEquals("confirm", lastNodeName(dispatcher, "zea import confirm"));
		assertEquals("cancel", lastNodeName(dispatcher, "zea import cancel"));
	}

	@Test
	void everythingElseStillReachesThePasteOperand() throws CommandSyntaxException {
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		dispatcher.register(zeaRoot());

		assertEquals("paste", lastNodeName(dispatcher, "zea import wozajojele"));
		assertEquals("wozajojele", operandOf(dispatcher, "zea import wozajojele"));
		assertEquals("paste", lastNodeName(dispatcher, "zea import confirmation"));
	}

	/**
	 * Builds the real ShareCommands tree. The coordinator is only invoked
	 * from execute-time lambdas, never during registration or parsing, so
	 * a null reference keeps the test on the pure-Brigadier path.
	 */
	private static LiteralArgumentBuilder<FabricClientCommandSource> zeaRoot() {
		LiteralArgumentBuilder<FabricClientCommandSource> zea = ClientCommands.literal("zea");
		ShareCommands.register(zea, null);
		return zea;
	}

	private static String lastNodeName(CommandDispatcher<FabricClientCommandSource> dispatcher,
			String input) throws CommandSyntaxException {
		List<ParsedCommandNode<FabricClientCommandSource>> nodes =
				dispatcher.parse(input, null).getContext().getNodes();
		return nodes.get(nodes.size() - 1).getNode().getName();
	}

	private static String operandOf(CommandDispatcher<FabricClientCommandSource> dispatcher,
			String input) throws CommandSyntaxException {
		CommandContext<FabricClientCommandSource> context =
				dispatcher.parse(input, null).getContext().build(input);
		return ImportPasteArgument.get(context, "paste");
	}
}
