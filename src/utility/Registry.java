package utility;

import java.util.Arrays;
import java.util.stream.Collectors;

import utility.commands.Brainf__k;
import utility.commands.DigitReader;
import utility.commands.MessageTransfer;
import utility.commands.StandardCommandListener;
import utility.commands.Webhook;

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
			}
			Util.sendError(e.getChannel(), "No command could be found named '" + params[0] + "'.");
		}
	})),
	ECHO("echo", "echo(<_string_>)", "Sends the given string", new StandardCommandListener("echo", e -> {
		final String[] params = Util.parseParams(e.getMessage().getContent());
		if (params.length == 0) {
			Util.sendError(e.getChannel(), "Not enough arguments (got 0, expected 1)");
		} else if (params.length > 0) {
			Util.send(e.getChannel(), Util.escape(params[0]));
		}
	})),
	BF("bf", "<bf|brainfuck>(<_code_>[, _input_])",
			"Executes Brainfuck code\n" + //
					"**Flags:**" + //
					"\n$n: accept ASCII char codes as input. Will automatically be true if more than two parameters are passed" + //
					"\n$p: print unprintable characters (e.g. NUL) as unicode control pictures" + //
					"\n$r: print only results (no ASCII char codes)" + //
					"\n$c: print only ASCII char codes. If both $c and $r are used, $c is ignored" + //
					"\n$m: escape markdown characters" + //
					"\n$t: print the tape" + //
					"\n$w: wrap numbers in tape to byte range (`[0, 256)`)",
			new Brainf__k()),
	DIGIT("digit", "digit([_hidden\\_node\\_count_][, _bias_])", "Guesses the digit that the attached 28x28 image represents. Number of hidden nodes and bias can be supplied, and will only be used if the correct file exists.\n$o also prints the output layer.", new DigitReader()),
	WEBHOOK("webhook", "webhook(<_url_>, <_content_>[, _username_[, _icon\\_url_]])", "Posts the given message to the given URL", new Webhook()),
	TRANSFER("transfer", "transfer(<_channel_>, <_start\\_id_>[, _end\\_id_])", "Transfers the messages from the given ID range to the given channel. Use the $k flag to keep the original messages", new MessageTransfer());

	public final String command, usage, description;
	public final Object listener;

	private Registry(final String command, final String usage, final String description, final Object listener) {
		this.command = command;
		this.usage = usage;
		this.description = description;
		this.listener = listener;
	}

}
