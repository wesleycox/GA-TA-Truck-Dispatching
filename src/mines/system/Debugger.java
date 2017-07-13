package mines.system;

import java.io.PrintStream;

/**
 * The debugger prints output only if debugging mode is active.
 */
public final class Debugger {

	private Debugger() {}

	private static StringBuilder builder = new StringBuilder();	//the current unprinted output stream.
	private static final int BUFFERSIZE = 4096;					//the maximum stream size.
	private static boolean DEBUG = false;						//whether debugging mode is active.
	private static PrintStream out = System.err;				//output.
	public static final double EPSILON = 1e-6;					//unused.

	/**
	 * Enable or disable debugging mode.
	 *
	 * @param	debug	whether to enable debugging mode.
	 */
	public static void setDebug(boolean debug) {
		flush();
		DEBUG = debug;
	}

	/**
	 * If debugging mode is active,
	 * append the message to the stream,
	 * and flush the stream.
	 * Otherwise do nothing.
	 *
	 * Equivalent to print(message,true).
	 *
	 * @param	message	the text to print.
	 */
	public static void print(String message) {
		if (DEBUG) {
			print(message,true);
		}
	}

	/**
	 * Flushes the current stream,
	 * then changes the output.
	 *
	 * @param	stream	the new output.
	 */
	public static void setOut(PrintStream stream) {
		flush();
		out = stream;
	}

	/**
	 * If debugging mode is active,
	 * append the message to the stream,
	 * and flush the stream if requested.
	 * Otherwise do nothing.
	 *
	 * @param	message	the text to print.
	 * @param	flush	whether to flush the stream now.
	 */
	public static void print(String message, boolean flush) {
		if (DEBUG) {
			builder.append(message);
			if (flush || builder.length() >= BUFFERSIZE) {
				flush();
			}
		}
	}

	/**
	 * Flush and clear the output stream.
	 * Prints everything that been appended to the stream,
	 * but not yet printed.
	 */
	public static void flush() {
		if (DEBUG) {
			out.print(builder.toString());
			builder = new StringBuilder();
		}
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				Debugger.flush();
			}
		},"Shutdown"));
	}
}