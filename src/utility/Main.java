package utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Main {

	public static JDA jda;

	public static void main(final String[] args) {
		String token = null;
		try {
			token = Files.readAllLines(Paths.get("token.txt")).get(0);
		} catch (final IOException e) {}
		if (token == null) {
			System.err.println("Token could not be found. Enter token to continue:");
			final Scanner scanner = new Scanner(System.in);
			token = scanner.nextLine();
			scanner.close();
		}

		try {
			jda = new JDABuilder(AccountType.BOT).addEventListener(new ListenerAdapter() {

				@Override
				public void onMessageReceived(final MessageReceivedEvent event) {
					System.out.printf("[%s] %s\n", event.getAuthor().getName(), event.getMessage().getContent());
				}

			}).addEventListener(Arrays.stream(Registry.values()).map(r -> r.listener).toArray()).setToken(token).buildBlocking();
		} catch (LoginException | IllegalArgumentException | InterruptedException | RateLimitedException e) {
			e.printStackTrace();
		}
	}

}
