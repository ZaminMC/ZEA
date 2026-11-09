package net.zaminmc.zea.share;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The operand of {@code /zea import}: a bare paste key or a paste URL,
 * read as the whole remaining input (like a greedy string).
 *
 * <p>The literal siblings {@code confirm} and {@code cancel} live on the
 * same node, so Brigadier's ambiguity scan compares their example words
 * against this argument's {@link #parse}. Rejecting exactly those two
 * reserved words here tells the scan the truth the parser already
 * implements — the words belong to the literals — and silences the
 * {@code Ambiguity: [zea, import, confirm] vs [zea, import, paste]}
 * warning. At parse time Brigadier tries an exactly-matching literal
 * first, so the rejection is never reached for real input; a paste key
 * that spelled "confirm" anyway would still import via its full URL.
 */
public final class ImportPasteArgument implements ArgumentType<String> {
	/** Words owned by the sibling literals under {@code /zea import}. */
	private static final Set<String> RESERVED_WORDS = Set.of("confirm", "cancel");

	private static final SimpleCommandExceptionType RESERVED_WORD = new SimpleCommandExceptionType(
			new LiteralMessage("'confirm' and 'cancel' are reserved — paste a key or the full paste URL instead"));

	private static final Collection<String> EXAMPLES = List.of(
			"wozajojele",
			PasteKey.BASE_URL + "/wozajojele");

	/** Reads the operand from the context; use instead of greedy-string helpers. */
	public static String get(CommandContext<?> context, String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		while (reader.canRead()) {
			reader.skip();
		}
		String operand = reader.getString().substring(start, reader.getCursor());
		if (RESERVED_WORDS.contains(operand)) {
			throw RESERVED_WORD.createWithContext(reader);
		}
		return operand;
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
			SuggestionsBuilder builder) {
		return Suggestions.empty();
	}
}
