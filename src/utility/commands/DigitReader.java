package utility.commands;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import net.dv8tion.jda.core.entities.Message.Attachment;
import utility.Util;

public class DigitReader extends StandardCommandListener {

	private static final String DEFAULT_NODES = "500b";
	private static final boolean DEFAULT_BIAS = true;

	public DigitReader() {
		super("digit", event -> {
			try {
				if (event.getMessage().getAttachments().isEmpty()) {
					Util.sendError(event.getChannel(), "No image attached");
					return;
				}

				for (final Attachment a : event.getMessage().getAttachments()) {
					if (a.isImage() && a.getHeight() == 28 && a.getWidth() == 28) {
						event.getChannel().sendTyping();
						try {
							Thread.sleep(128);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}

						a.download(Paths.get("digit-weights/digit.png").toFile());

						final String command = event.getMessage().getContent();
						final String[] params = Util.parseParams(command);

						String suffix = null;
						boolean bias = DEFAULT_BIAS;

						if (params.length == 2) {
							bias = Boolean.parseBoolean(params[1]);
							suffix = params[0] + (bias ? "b" : "");
						} else if (params.length == 1) {
							suffix = params[0] + (bias ? "b" : "");
						} else if (params.length != 0) {
							Util.sendError(event.getChannel(), "Invalid number of parameters");
						}

						if (!Files.exists(Paths.get("digit-weights/weights0_" + suffix + ".txt")) && Files.exists(Paths.get("digit-weights/weights1_" + suffix + ".txt"))) {
							Util.sendWarning(event.getChannel(), "Could not find the weights specified; using default weights instead.");

							suffix = DEFAULT_NODES;
							bias = DEFAULT_BIAS;
						}
						if (suffix == null) suffix = DEFAULT_NODES;

						final List<String> weight0 = Files.readAllLines(Paths.get("digit-weights/weights0_" + suffix + ".txt"));

						final float[][] arr0 = new float[weight0.size()][weight0.get(0).length() / 8];

						for (int r = 0; r < weight0.size(); r++) {
							final String s = weight0.get(r);
							for (int c = 0; c < arr0[r].length; c++) {
								arr0[r][c] = Float.intBitsToFloat((int) Long.parseLong(s.substring(c * 8, c * 8 + 8), 16));
							}
						}

						final List<String> weight1 = Files.readAllLines(Paths.get("digit-weights/weights1_" + suffix + ".txt"));
						final float[][] arr1 = new float[weight1.size()][weight1.get(0).length() / 8];

						for (int r = 0; r < weight1.size(); r++) {
							final String s = weight1.get(r);
							for (int c = 0; c < arr1[r].length; c++) {
								arr1[r][c] = Float.intBitsToFloat((int) Long.parseLong(s.substring(c * 8, c * 8 + 8), 16));
							}
						}

						final BufferedImage read = ImageIO.read(Paths.get("digit-weights/digit.png").toFile());
						final int w = read.getWidth();
						final int h = read.getHeight();
						final float[][] in = new float[1][w * h];
						for (int y = 0; y < h; y++) {
							for (int x = 0; x < w; x++) {
								final Color c = new Color(read.getRGB(x, y));
								in[0][y * w + x] = 1 - (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 255000F;
							}
						}

						Files.delete(Paths.get("digit-weights/digit.png"));

						final float[] out;
						if (bias)
							out = sigmoid(multiply(addBias(sigmoid(multiply(addBias(in), arr0))), arr1))[0];
						else
							out = sigmoid(multiply(sigmoid(multiply(in, arr0)), arr1))[0];
						float max = 0;
						int best = 0;
						for (int n = 0; n < out.length; n++) {
							if (out[n] > max) {
								max = out[n];
								best = n;
							}
						}

						Util.send(event.getChannel(), "Most likely digit: " + best);
						for (final char c : Util.flags(command)) {
							if (c == 'o') {
								Util.send(event.getChannel(), "Outputs: " + Arrays.toString(out));
							}
						}

						return;
					}
				}
				Util.sendError(event.getChannel(), "No image attached");
			} catch (NumberFormatException | IOException e) {
				e.printStackTrace();
			}
		});
	}

	public static float[][] multiply(final float[][] a, final float[][] b) {
		if (a[0].length != b.length) throw new IllegalArgumentException(String.format("Matrix size mismatch (%d x %d), (%d x %d)", a.length, a[0].length, b.length, b[0].length));

		final float[][] ret = new float[a.length][b[0].length];
		for (int r = 0; r < ret.length; r++) {
			for (int c = 0; c < ret[r].length; c++) {
				for (int n = 0; n < b.length; n++) {
					ret[r][c] += a[r][n] * b[n][c];
				}
			}
		}

		return ret;
	}

	public static float[][] addBias(final float[][] matrix) {
		final float[][] ret = new float[matrix.length][];
		for (int r = 0; r < matrix.length; r++) {
			ret[r] = new float[matrix[r].length + 1];
			System.arraycopy(matrix[r], 0, ret[r], 0, matrix[r].length);
			ret[r][ret[r].length - 1] = 1;
		}

		return ret;
	}

	/**
	 * Note: does not create a new array
	 */
	public static float[][] sigmoid(final float[][] x) {
		for (int r = 0; r < x.length; r++) {
			for (int c = 0; c < x[r].length; c++) {
				x[r][c] = 1 / (1 + (float) Math.exp(-x[r][c]));
			}
		}

		return x;
	}

	private static void print(final List<String> out, final float[][] arr) {
		Arrays.stream(arr).map(a -> {
			final StringBuffer sb = new StringBuffer("[");
			for (final float n : a)
				sb.append(String.format("%.02f, ", n));
			sb.append("],");
			return sb.toString();
		}).forEach(out::add);
	}

}
