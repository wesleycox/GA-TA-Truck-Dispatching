package mines.ga;

import mines.ga.chrom.Chromosome;

/**
 * Genetic algorithm class.
 */
public interface GeneticAlgorithm<E extends Chromosome> {

	/**
	 * Run the genetic algorithm.
	 * 
	 * @return	the best chromosome.
	 */
	public E run();

}