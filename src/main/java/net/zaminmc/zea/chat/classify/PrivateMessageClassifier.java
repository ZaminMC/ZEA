package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.PrivateMessage;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Recognizes private messages.
 *
 * <p>Observed shapes: {@code [Sender → me] content} (incoming) and
 * {@code [me → Recipient] content} (outgoing). Both the header structure
 * and valid usernames are required — a message merely containing an
 * arrow is not a private message.
 *
 * <p>The self side is stored as the literal "me"; the PM feed (Stage 5)
 * resolves it to the local player's name.
 */
public final class PrivateMessageClassifier implements MessageClassifier {
	private static final Pattern INCOMING = Pattern.compile("^\\[(\\w{1,16}) → me\\] (.*)$");
	private static final Pattern OUTGOING = Pattern.compile("^\\[me → (\\w{1,16})\\] (.*)$");

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		String text = message.text();

		var incoming = INCOMING.matcher(text);
		if (incoming.matches()) {
			PrivateMessage pm = new PrivateMessage(incoming.group(1), "me",
					PrivateMessage.Direction.INCOMING, incoming.group(2));
			return Optional.of(Result.of(MessageCategory.PRIVATE_MESSAGE, pm));
		}
		var outgoing = OUTGOING.matcher(text);
		if (outgoing.matches()) {
			PrivateMessage pm = new PrivateMessage("me", outgoing.group(1),
					PrivateMessage.Direction.OUTGOING, outgoing.group(2));
			return Optional.of(Result.of(MessageCategory.PRIVATE_MESSAGE, pm));
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "private-message";
	}
}
