package utility;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.dv8tion.jda.core.entities.MessageChannel;

public class Util {

	private Util() {}

	public static char[] flags(final String command) {
		final Set<Character> chars = new TreeSet<>();
		for (final char c : command.replaceAll("[^\\(]*?\\$([A-Za-z]+)\\([\\S\\s]*$", "$1").toCharArray()) {
			chars.add(c);
		}
		final char[] ret = new char[chars.size()];
		int i = 0;
		for (final Iterator<Character> iterator = chars.iterator(); iterator.hasNext(); i++) {
			ret[i] = iterator.next();
		}
		return ret;
	}

	public static String[] parseParams(final String command) {
		if (!command.matches("^.*?(?<!\\\\)\\(.*?(?<!\\\\)\\).*")) return new String[0];
		final String[] parts = command.substring(command.indexOf('(') + 1).split("(?<!\\\\)\\)")[0].split("(?<!\\\\)\\,");

		for (int n = 0; n < parts.length; n++) {
			parts[n] = parts[n].trim().replaceFirst("\\\\$", " ").replaceAll("\\\\(.)", "$1");
		}
		return parts;
	}

	public static void send(final MessageChannel channel, final String message) {
		channel.sendMessage(message).queue();
	}

	public static void sendError(final MessageChannel channel, final String message) {
		send(channel, makeError(message));
	}

	public static String makeError(final String text) {
		return String.format("```diff\n- %s\n```", text);
	}

	public static String escape(final String text) {
		return text.replaceAll("[!\"#$%&'()*+,\\-./:;<=>?@[\\\\\\]^_`{|}~]", "\\$0");
	}
}
