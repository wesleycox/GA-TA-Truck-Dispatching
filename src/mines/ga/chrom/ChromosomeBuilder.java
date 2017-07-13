package mines.ga.chrom;

import java.util.Random;

/**
 * Builder class that provides random chromosomes to initialise a genetic algorithm.
 */

public interface ChromosomeBuilder<E extends Chromosome> {

	/**
	 * Get a random chromosome.
	 *
	 * @param	rng	the Random object used for any RNG.
	 * @return	a random chromosome.
	 */
	public E getRandomChromosome(Random rng);
}