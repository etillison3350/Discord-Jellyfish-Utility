package utility.commands;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import utility.Util;

public class Brainf__k extends ListenerAdapter {

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
		}
		if (params != null) {
			try {
			event.getChannel().sendTyping().queue();

			////////////////////////////////////////////////////
			try {
				Thread.sleep(128);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}

			String code = message.replaceAll("^\\#(?:bf|brainfuck)\\s", "");

			String input = "";

			boolean printUnprintables = false;
			boolean resultOnly = false;
			boolean codesOnly = false;
			boolean escapeMarkdown = false;
			boolean printTape = false;
			boolean wrapToByte = false;
			Matcher m = Pattern.compile("^\\$([a-z]+)").matcher(code);
			while (m.find()) {
				final char[] flags = m.group(1).toCharArray();
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
					}
				}
			}
			if (resultOnly) codesOnly = false;

			m = Pattern.compile("\\\\([inc])\\s([\\s\\S]*)$").matcher(code);
			if (m.find()) {
				if (m.group(1).charAt(0) == 'n' || m.group(1).charAt(0) == 'c') {
					final StringBuffer sb = new StringBuffer();
					final String[] codes = m.group(2).split("/");
					boolean oob = false;
					for (final String s : codes) {
						try {
							final BigInteger i = new BigInteger(s);
							final BigInteger mod = i.mod(BigInteger.valueOf(65536));
							if (!i.equals(mod)) {
								oob = true;
							}

							sb.append((char) mod.intValue());
						} catch (final NumberFormatException e) {
							send(event.getChannel(), String.format("%s\nError: Invalid character code: \"%s\"", event.getAuthor().getAsMention(), s));
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
						send(event.getChannel(), String.format("%s\nWarning: Out of bounds character codes in input will be wrapped to %s (%s)", event.getAuthor().getAsMention(), real.toString(), input));
					}
				} else {
					input = m.group(2);
				}
				code = code.substring(0, m.start());
			}
			code = code.replaceAll("[^\\<\\>\\+\\-\\,\\.\\[\\]]+", "");

			final List<Integer> tape = new ArrayList<>(1);
			tape.add(0);
			int ptr = 0;
			int inputIndex = 0;

			final StringBuffer out = codesOnly ? null : new StringBuffer(), outCodes = resultOnly ? null : new StringBuffer();

			final String interpret = interpretBf ? "Interpreting brainfuck-like input as brainfuck code.\n" : "";

			long n = 0;
			for (int i = 0; i < code.length(); i++) {
				if (n++ > maxInstructions.getValue()) {
					send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Program exceeded 268,435,455 instructions!");
					return;
				}

				if (printTape && tape.size() > 667 || out != null && out.length() >= 2000 || out == null && outCodes != null && outCodes.length() > 2000) {
					send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Output exceeded 2000 characters!");
					return;
				} else if (tape.size() > 2000) {
					send(event.getChannel(), event.getAuthor(), out, outCodes, null, interpret + "Error: Tape length exceeded 2000!");
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
							send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Invalid pointer location: " + ptr);
							return;
						}
						tape.set(ptr, wrapToByte ? (tape.get(ptr) + 1) % 256 : tape.get(ptr) + 1);
						break;
					case '-':
						if (ptr < 0) {
							send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Invalid pointer location: " + ptr);
							return;
						}
						tape.set(ptr, wrapToByte ? (tape.get(ptr) - 1) % 256 : tape.get(ptr) - 1);
						break;
					case '.':
						if (ptr < 0) {
							send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Invalid pointer location: " + ptr);
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
							send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Invalid pointer location: " + ptr);
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
							send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Invalid pointer location: " + ptr);
							return;
						}
						if (tape.get(ptr) == 0) {
							i++;
							for (int bkt = 1; bkt > 0; i++) {
								if (i >= code.length()) {
									send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Unmatched brackets!");
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
							send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Invalid pointer location: " + ptr);
							return;
						}
						if (tape.get(ptr) != 0) {
							i--;
							for (int bkt = 1; bkt > 0; i--) {
								if (i < 0) {
									send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret + "Error: Unmatched brackets!");
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

			send(event.getChannel(), event.getAuthor(), out, outCodes, printTape ? tape : null, interpret);
		} else if (message.matches("^\\#(?:bfhelp)(?:$|[^a-z])[\\s\\S]*")) {
			send(event.getChannel(),
					String.format("%s\n" + //
							"**Syntax: #*<bf|brainfuck>* *[$flags]* *code* *[\\inFlags input]***" + //
							"\n**$flags**" + //
							"\n$p: print unprintable characters (e.g. NUL) as unicode control pictures" + //
							"\n$r: print only results (no ASCII char codes)" + //
							"\n$c: print only ASCII char codes. If both $c and $r are used, $c is ignored" + //
							"\n$m: escape markdown characters" + //
							"\n$t: print the tape" + //
							"\n$w: wrap numbers in tape to byte range (`[0, 256)`)" + //
							"\n**\\inFlags**" + //
							"\n\\i: Standard input\u2014all characters following are read as input." + //
							"\n\\n or \\c: Character code input\u2014input is read as a series of slash separated character codes." + //
							"\nReact with <:justin:302279919222784000> to remove your message.", event.getAuthor().getAsMention()));
		} else if (message.matches("^\\#(?:tobf)(?:$|[^a-z])[\\s\\S]*")) {
			final String m = message.replaceAll("^\\#(?:tobf)\\s", "");

			final Map<Integer, Integer> sixteens = new TreeMap<>();
			for (final char c : m.toCharArray()) {
				sixteens.put(c / 16, 0);
			}

			int index = 1;
			final StringBuffer code = new StringBuffer("++++++++++++++++[");
			for (final Integer c : sixteens.keySet().toArray(new Integer[sixteens.size()])) {
				code.append('>');
				if (c < 16) {
					for (int n = 0; n < c; n++) {
						code.append('+');
					}
				} else {
					for (int n = 0; n < 8; n++) {
						code.append('+');
					}
					code.append("[>");
					for (int n = 0; n < c / 8; n++) {
						code.append('+');
					}
					code.append("<-]>");
					for (int n = 0; n < c % 8; n++) {
						code.append('+');
					}
					index++;
				}
				sixteens.put(c, index++ << 4);
			}
			for (; index > 1; index--) {
				code.append('<');
			}
			code.append("-]");

			index = 0;

			for (final char c : m.toCharArray()) {
				final int sixteen = c / 16;
				final int one = c % 16;
				int currentOne = sixteens.get(sixteen) & 0b1111;
				final int targetIndex = sixteens.get(sixteen) >> 4;
				if (targetIndex > index) {
					for (; targetIndex > index; index++)
						code.append('>');
				} else if (targetIndex < index) {
					for (; targetIndex < index; index--)
						code.append('<');
				}

				if (currentOne > one) {
					for (; currentOne > one; currentOne--)
						code.append('-');
				} else if (currentOne < one) {
					for (; currentOne < one; currentOne++)
						code.append('+');
				}

				sixteens.put(sixteen, one | targetIndex << 4);

				code.append('.');
			}

			send(event.getChannel(), String.format("%s\n%s", event.getAuthor().getAsMention(), code.toString()));
		}
	} catch (final Exception e) {
		send(event.getChannel(), "Error: " + e.toString());
		e.printStackTrace();
	}
	//////////////////////////////////////////////////////////////////

		}
}

}
