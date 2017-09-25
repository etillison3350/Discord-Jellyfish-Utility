package utility.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import utility.Util;

public class Webhook extends StandardCommandListener {

	public Webhook() {
		super("webhook", event -> {
			// event.getGuild().getMember(null).getNickname()
			final String content = event.getMessage().getContent();
			final String[] params = Util.parseParams(content);

			// https://discordapp.com/api/webhooks/302811724220727296/dMMBQOAs8c5gX8QBdqm8b4M6-s0NyuBiDziULCJ8o8UBgMWNZKRH60dSs1Z1iDx0nnn6
			if (params.length < 2) Util.sendError(event.getChannel(), "Not enough parameters");

			try {
				final URL url = new URL(params[0] + (params[0].endsWith("/slack") ? "" : "/slack"));
				final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

				final StringBuffer json = new StringBuffer(String.format("{\"text\":\"%s\"", params[1]));
				if (params.length > 2) json.append(String.format(",\"username\":\"%s\"", params[2]));
				if (params.length > 3) json.append(String.format(",\"icon_url\":\"%s\"", params[3]));
				json.append('}');

				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("Content-Length", String.valueOf(json.length()));
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
				final OutputStream os = conn.getOutputStream();
				os.write(json.toString().getBytes());
				os.flush();
				Util.send(event.getChannel(), "Sent '" + json.toString() + "'");
				final BufferedReader is = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String line;
				while ((line = is.readLine()) != null) {
					System.out.printf("[Webhook-Response] %s\n", line);
				}
				is.close();
				os.close();
			} catch (final IOException e) {
				Util.sendError(event.getChannel(), e.toString());
				e.printStackTrace();
			}
		});
	}

}
