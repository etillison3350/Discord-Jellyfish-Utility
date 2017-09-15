package utility;

import java.util.Arrays;
import java.util.stream.Collectors;

import utility.commands.StandardCommandListener;

public enum Registry {

	HELP("help", "help() OR help(<_command_>)", "Shows the list of commands, or information on a specific command", new StandardCommandListener("help", e -> {
		final String[] params = Util.parseParams(e.getMessage().getContent());
		if (params.length == 0) {
			Util.send(e.getChannel(), "General command usage: ju.<_command_>[$_flags_]([_param\\_1_], [_param\\_2_], _etc._)\n" + //
					"Available Commands: \n" + Arrays.stream(Registry.values()).map(r -> r.command).sorted().collect(Collectors.joining(", ")));
		} else if (params.length == 1) {
			for (final Registry r : Registry.values()) {
				if (r.command.equalsIgnoreCase(params[0])) {
					Util.send(e.getChannel(), String.format("**%s**\nUsage: %s\n%s", r.command, r.usage, r.description));
					return;
				}
				Util.sendError(e.getChannel(), "No command could be found named '" + params[0] + "'.");
			}
		}
	})),
	ECHO("echo", "echo(<_string_>)", "Sends the given string", new StandardCommandListener("echo", e -> {
		final String[] params = Util.parseParams(e.getMessage().getContent());
		if (params.length == 0) {
			Util.sendError(e.getChannel(), "Not enough arguments (got 0, expected 1)");
		} else if (params.length > 0) {
			Util.send(e.getChannel(), Util.escape(params[0]));
		}
	}));

	public final String command, usage, description;
	public final Object listener;

	private Registry(final String command, final String usage, final String description, final Object listener) {
		this.command = command;
		this.usage = usage;
		this.description = description;
		this.listener = listener;
	}

}
