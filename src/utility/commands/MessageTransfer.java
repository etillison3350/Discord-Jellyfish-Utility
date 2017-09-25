package utility.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import utility.Util;

public class MessageTransfer extends StandardCommandListener {

	public MessageTransfer() {
		super("transfer", event -> {
			if (!event.getGuild().getMember(event.getAuthor()).hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {
				Util.sendError(event.getChannel(), "You must have the manage messages permission to use this command");
				return;
			}

			final String[] params = Util.parseParams(event.getMessage().getContent());
			final char[] flags = Util.flags(event.getMessage().getContent());
			boolean keep = false;
			for (final char f : flags) {
				if (f == 'k') keep = true;
			}

			String url = null;
			params[0] = params[0].replaceAll("^\\s*\\#", "");
			try {
				final List<String> webhooks = Files.readAllLines(Paths.get("webhooks.txt"));
				for (final String webhook : webhooks) {
					final String[] parts = webhook.split("\t");
					if (parts[0].equals(params[0])) {
						url = parts[1];
						break;
					}
				}
			} catch (final IOException e) {}
			if (url == null) {
				Util.sendError(event.getChannel(), "Could not find a webhook for the given channel name");
				return;
			}

			long start;
			try {
				start = Long.parseLong(params[1]);
			} catch (final NumberFormatException e) {
				Util.sendError(event.getChannel(), params[1] + " is not a valid long ID");
				return;
			}

			long end;
			try {
				end = Long.parseLong(params[2]);
			} catch (final NumberFormatException e) {
				end = event.getChannel().getLatestMessageIdLong();
			}

			final MessageHistory mh = event.getChannel().getHistoryAround(start, 1).complete();
			List<Message> history = mh.getRetrievedHistory();

			int messages = 0;
			while (messages++ < 100 && history.get(history.size() - 1).getIdLong() != end) {
				// System.out.println(mh.retrieveFuture(1).complete());
				// System.out.println(mh.retrievePast(1).complete());
				history = mh.getRetrievedHistory();
				// System.out.println(history);
			}

			try {
				for (int n = history.size() - 1; n >= 0; n--) {
					final Message m = history.get(n);
					post(m.getContent(), event.getGuild().getMember(m.getAuthor()).getNickname(), m.getAuthor().getAvatarUrl(), new URL(url + "/slack"));
					if (!keep) event.getChannel().deleteMessageById(m.getIdLong()).queue();
					Thread.sleep(512);
				}
			} catch (final IOException | InterruptedException e) {
				e.printStackTrace();
				Util.sendError(event.getChannel(), e.toString());
			}
			event.getChannel().deleteMessageById(event.getMessage().getIdLong()).queue();
			// System.out.println(mh.getRetrievedHistory().stream().map(m -> "Q: " +
			// m.getContent()).collect(Collectors.joining("\n")));

			// System.out.println(mh.retrievePast(5).complete().get(3));
		});
	}

	private static final String post(final String text, final String user, final String iconUrl, final URL webhook) throws IOException {
		// final URL url = new URL(params[0] + (params[0].endsWith("/slack") ? "" : "/slack"));
		final HttpsURLConnection conn = (HttpsURLConnection) webhook.openConnection();

		final StringBuffer json = new StringBuffer(String.format("{\"text\":\"%s\"", text));
		if (user != null) json.append(String.format(",\"username\":\"%s\"", user));
		if (iconUrl != null) json.append(String.format(",\"icon_url\":\"%s\"", iconUrl));
		json.append('}');

		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Content-Length", String.valueOf(json.length()));
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
		final OutputStream os = conn.getOutputStream();
		os.write(json.toString().getBytes());
		os.flush();
		final BufferedReader is = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = is.readLine()) != null) {
			System.out.printf("[Webhook-Response] %s\n", line);
		}
		is.close();
		os.close();

		return json.toString();
	}

}
