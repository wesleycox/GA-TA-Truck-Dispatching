package mines.ga.chrom;

import mines.util.DoubleQueue;

/**
 * A chromosome that sets fitness based on a rolling average of fitness values.
 */

public abstract class RollingChromosome extends Chromosome {

	protected DoubleQueue fitnesses;	//the current fitness bucket.
	protected double totalFitness;		//the sum of the fitness bucket.
	private static int bucketSize = 10;	//the size of the fitness bucket.

	public RollingChromosome() {
		super();
		fitnesses = new DoubleQueue();
		totalFitness = 0;
	}

	/**
	 * Empty the fitness bucket and insert a single value.
	 *
	 * @param	f	the new fitness.
	 */
	@Override
	public void setFitness(double f) {
		fitnesses.clear();
		fitnesses.add(f);
		totalFitness = f;
	}

	/**
	 * Enter a single value into the fitness bucket.
	 * An old fitness value will be removed if the bucket is full.
	 *
	 * @param	f	the new value.
	 */
	@Override
	public void giveFitness(double f) {
		fitnesses.add(f);
		totalFitness += f;
		while (fitnesses.size() > bucketSize) {
			double old = fitnesses.poll();
			totalFitness -= old;
		}
	}

	/**
	 * Get whether the fitness bucket is not empty.
	 *
	 * @return	false if empty, true otherwise.
	 */
	@Override
	public boolean isFitnessSet() {
		return !fitnesses.isEmpty();
	}

	/**
	 * Get the average of the fitness bucket.
	 *
	 * @return	the current fitness.
	 */
	@Override
	public double getFitness() {
		return totalFitness / fitnesses.size();
	}

	/**
	 * Set the bucket size for all instances of this class.
	 *
	 * @param	size						the new bucket size.
	 * @throws	IllegalArgumentException	if non-positive value is supplied.
	 */
	public static void setBucketSize(int size) {
		if (size > 0) {
			bucketSize = size;
		}
		else {
			throw new IllegalArgumentException("Bucket size must be positive");
		}
	}
}