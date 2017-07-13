package mines.util.times;

/**
 * Random value generator.
 */
public interface TimeDistribution {

	/**
	 * Generate a random value with the given mean and standard deviation.
	 *
	 * @param	mean	the mean.
	 * @param	sd		the standard deviation.
	 * @return	a random value.
	 */
	public double nextTime(double mean, double sd);
	
}