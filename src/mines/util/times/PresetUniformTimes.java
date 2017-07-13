package mines.util.times;

import java.util.concurrent.*;

/**
 * Uniform distribution with preset noise level.
 */
public class PresetUniformTimes implements TimeDistribution {

	private double noise;	//noise level.

	/**
	 * Distribution constructor.
	 *
	 * @param	noise	the noise level.
	 */
	public PresetUniformTimes(double noise) {
		this.noise = noise;
	}

	/**
	 * Returns a value from the uniform distribution on:
	 * [m * (1 - n),m * (1 + n)],
	 * for mean m,
	 * and noise level n.
	 *
	 * @param	mean	the mean.
	 * @param	stdev	unused.
	 * @return	the random value.
	 */
	public double nextTime(double mean, double stdev) {
		double min = mean * (1.0 - noise);
		double max = mean * (1.0 + noise);
		return min + (max - min) * ThreadLocalRandom.current().nextDouble();
	}

}