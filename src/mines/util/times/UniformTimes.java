package mines.util.times;

// import java.util.*;
import java.util.concurrent.*;

/**
 * Uniform distribution.
 */
public final class UniformTimes implements TimeDistribution {

	// private Random rng;

	public UniformTimes() {
		// rng = new Random(System.nanoTime());
	}

	/**
	 * Generate a uniform random value with the given mean and standard deviation.
	 *
	 * @param	mean	the mean.
	 * @param	sd		the standard deviation.
	 * @return	a random value.
	 */
	public double nextTime(final double mean, final double stdev) {
		double min = mean - Math.sqrt(3) * stdev;
		if (min < 0) {
			throw new IllegalArgumentException();
		}
		// return min + 2 * Math.sqrt(3) * stdev * rng.nextDouble();
		// return mean + Math.sqrt(3) * stdev * (2 * rng.nextDouble() - 1);
		return mean + Math.sqrt(3) * stdev * (2 * ThreadLocalRandom.current().nextDouble() - 1);
	}

}