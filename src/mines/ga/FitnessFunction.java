package mines.ga;

import mines.ga.chrom.Chromosome;

/**
 * Fitness function class for use in genetic algorithms.
 */
public interface FitnessFunction<E extends Chromosome> {

	/**
	 * Evaluate the fitness of a chromosome.
	 * 
	 * @param	chrom	the chromosome to evaluate.
	 * @return	the fitness of the chromosome.
	 */
	public double getFitness(E chrom);

	/**
	 * Evaluate the fitness of a chromosome using some default parameters.
	 * For use in child classes that can evaluate different kinds of fitness.
	 * Intended for debugging purposes.
	 *
	 * @param	chrom	the chromosome to evaluate.
	 * @return	the fitness of the chromosome.
	 */
	public default double getDefaultFitness(E chrom) {
		// throw new UnsupportedOperationException();
		return getFitness(chrom);
	}

	/**
	 * Get whether the objective function is maximising or not.
	 *
	 * @return	true if high fitness is better,
	 *			false otherwise.
	 */
	public boolean isMaximising();
}