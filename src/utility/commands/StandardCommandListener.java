package utility.commands;

import java.util.function.Consumer;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class StandardCommandListener extends ListenerAdapter {
	private final Consumer<MessageReceivedEvent> action;
	private final String regex;

	public StandardCommandListener(final String command, final Consumer<MessageReceivedEvent> action) {
		this.action = action;
		regex = "^ju." + command + "\\b[\\s\\S]*";
	}

	@Override
	public void onMessageReceived(final MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;

		if (event.getMessage().getContent().matches(regex)) action.accept(event);
	}

}
