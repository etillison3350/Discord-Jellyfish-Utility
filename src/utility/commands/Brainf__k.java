package utility.commands;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import utility.Util;

public class Brainf__k extends ListenerAdapter {

	public static final String ESC = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

	@Override
	public void onMessageReceived(final MessageReceivedEvent event) {
		final String message = event.getMessage().getContent();

		String[] params = null;
		char[] flags = null;
		if (message.matches("^ju.(?:brainfuck|bf)\\b[\\s\\S]*")) {
			params = Util.parseParams(message);
			flags = Util.flags(message);
		} else if (message.length() > 10 && message.replaceAll("[\\<\\>\\+\\-\\,\\.\\[\\]]", "").isEmpty()) {
			params = new String[] {message};
			Util.send(event.getChannel(), "Interpreting BF-like message as BF code");
		}
		if (params != null) {
			try {
				event.getChannel().sendTyping().queue();

				try {
					Thread.sleep(128);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}

				String code = params[0];
				String input = "";

				boolean printUnprintables = false;
				boolean resultOnly = false;
				boolean codesOnly = false;
				boolean escapeMarkdown = false;
				boolean printTape = false;
				boolean wrapToByte = false;
				boolean numericInput = params.length > 2;
				final Matcher m = Pattern.compile("^\\$([a-z]+)").matcher(code);
				while (m.find()) {
					for (final char f : flags) {
						switch (f) {
							case 'p':
								printUnprintables = true;
								break;
							case 'r':
								resultOnly = true;
								break;
							case 'c':
								codesOnly = true;
								break;
							case 'm':
								escapeMarkdown = true;
								break;
							case 't':
								printTape = true;
								break;
							case 'w':
								wrapToByte = true;
								break;
							case 'n':
								numericInput = true;
								break;
						}
					}
				}
				if (resultOnly) codesOnly = false;

				if (numericInput) {
					final StringBuffer sb = new StringBuffer();
					boolean oob = false;
					for (int n = 1; n < params.length; n++) {
						try {
							final BigInteger i = new BigInteger(params[n]);
							final BigInteger mod = i.mod(BigInteger.valueOf(65536));
							if (!i.equals(mod)) {
								oob = true;
							}

							sb.append((char) mod.intValue());
						} catch (final NumberFormatException e) {
							Util.sendError(event.getChannel(), String.format("Invalid character code: \"%s\"", params[n]));
							return;
						}
					}
					input = sb.toString();

					if (oob) {
						final StringBuffer real = new StringBuffer();
						for (final char c : input.toCharArray()) {
							real.append((int) c);
							real.append('/');
						}
						real.deleteCharAt(real.length() - 1);
						Util.sendWarning(event.getChannel(), String.format("Warning: Out of bounds character codes in input will be wrapped to %s (%s)", real.toString(), input));
					}
				} else if (params.length > 1) {
					input = params[1];
				}
				code = code.replaceAll("[^\\<\\>\\+\\-\\,\\.\\[\\]]+", "");

				final List<Integer> tape = new ArrayList<>(1);
				tape.add(0);
				int ptr = 0;
				int inputIndex = 0;

				final StringBuffer out = codesOnly ? null : new StringBuffer();
				final StringBuffer outCodes = resultOnly ? null : new StringBuffer();
				//////////////////////////////////////////

				long n = 0;
				for (int i = 0; i < code.length(); i++) {
					if (n++ > 268435455) {
						Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Program exceeded 268,435,455 instructions"));
						return;
					}

					if (printTape && tape.size() > 667 || !codesOnly && out.length() >= 2000 || codesOnly && outCodes.length() > 2000) {
						Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Output exceeded 2,000 characters"));
						return;
					} else if (tape.size() > 2000) {
						Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Tape length exceeded 2,000"));
						return;
					}

					switch (code.charAt(i)) {
						case '>':
							if (++ptr >= tape.size()) tape.add(0);
							break;
						case '<':
							ptr--;
							break;
						case '+':
							if (ptr < 0) {
								Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Invalid pointer location: " + ptr));
								return;
							}
							tape.set(ptr, wrapToByte ? (tape.get(ptr) + 1) % 256 : tape.get(ptr) + 1);
							break;
						case '-':
							if (ptr < 0) {
								Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Invalid pointer location: " + ptr));
								return;
							}
							tape.set(ptr, wrapToByte ? (tape.get(ptr) - 1) % 256 : tape.get(ptr) - 1);
							break;
						case '.':
							if (ptr < 0) {
								Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Invalid pointer location: " + ptr));
								return;
							}
							final int o = tape.get(ptr).intValue();
							if (out != null) {
								if (escapeMarkdown && ESC.indexOf(o) > 0) out.append('\\');
								out.append(printUnprintables && o >= 0 && o < 32 ? (char) (9216 + o) : (char) o);
							}
							if (outCodes != null) outCodes.append(String.format("%d, ", o));
							break;
						case ',':
							if (ptr < 0) {
								Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Invalid pointer location: " + ptr));
								return;
							}
							try {
								final int val = input.charAt(inputIndex++);
								tape.set(ptr, wrapToByte ? val % 256 : val);
							} catch (final IndexOutOfBoundsException e) {
								tape.set(ptr, 0);
							}
							break;
						case '[':
							if (ptr < 0) {
								Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Invalid pointer location: " + ptr));
								return;
							}
							if (tape.get(ptr) == 0) {
								i++;
								for (int bkt = 1; bkt > 0; i++) {
									if (i >= code.length()) {
										Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Invalid pointer location: " + ptr));
										return;
									}

									if (code.charAt(i) == '[')
										bkt++;
									else if (code.charAt(i) == ']') bkt--;
								}
								i--;
							}
							break;
						case ']':
							if (ptr < 0) {
								Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Invalid pointer location: " + ptr));
								return;
							}
							if (tape.get(ptr) != 0) {
								i--;
								for (int bkt = 1; bkt > 0; i--) {
									if (i < 0) {
										Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null, "Unmatched brackets"));
										return;
									}

									if (code.charAt(i) == '[')
										bkt--;
									else if (code.charAt(i) == ']') bkt++;
								}
								i++;
							}
							break;
					}
				}

				Util.send(event.getChannel(), message(out, outCodes, printTape ? tape : null));
			} catch (final Exception e) {
				e.printStackTrace();
				Util.sendError(event.getChannel(), e.toString());
			}
		}

	}

	private String message(final StringBuffer out, final StringBuffer outCodes, final List<Integer> tape) {
		return message(out, outCodes, tape, null);
	}

	private String message(final StringBuffer out, final StringBuffer outCodes, final List<Integer> tape, final String error) {
		final StringBuffer message = new StringBuffer(error == null ? "" : String.format("```diff\n- %s\n```", error));
		if (tape != null) {
			if (message.length() > 0) message.append('\n');
			message.append("Tape: ");
			for (int n = 0; n < tape.size() && message.length() < 2005; n++) {
				message.append(tape.get(n));
				if (n < tape.size() - 1) message.append(", ");
			}

			if (message.length() >= 2000) {
				message.delete(message.lastIndexOf(",", 1995), Integer.MAX_VALUE);
				message.append("...");
				return message.toString();
			}
		}

		if (out != null) {
			if (message.length() > 0) message.append('\n');
			message.append("Result: ");
			message.append(out.length() == 0 ? "[None]" : out);
			if (message.length() >= 2000) {
				message.delete(1995, Integer.MAX_VALUE);
				message.append("...");
				return message.toString();
			}
		}

		if (outCodes != null && outCodes.length() > 0) {
			if (message.length() > 0) message.append('\n');
			message.append("Char codes: ");
			if (message.length() > 1995) {
				message.delete(1995, Integer.MAX_VALUE);
				message.append("...");
				return message.toString();
			}

			message.append(outCodes);
			message.delete(message.length() - 2, Integer.MAX_VALUE);
			if (message.length() >= 2000) {
				message.delete(message.lastIndexOf(",", 1995), Integer.MAX_VALUE);
				message.append("...");
				return message.toString();
			}
		}
		return message.toString();
	}

}
