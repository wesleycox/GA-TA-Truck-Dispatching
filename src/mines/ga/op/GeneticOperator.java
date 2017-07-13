package mines.ga.op;

import mines.ga.chrom.Chromosome;
import java.util.ArrayList;

/**
 * Genetic operator class for use in genetic algorithms
 */

public interface GeneticOperator<E extends Chromosome> {

	/**
	 * Produce a list of offspring from a list of parents by applying crossover, mutation, etc.
	 *
	 * @param	population		an ArrayList of parent chromosomes.
	 * @param	numOffspring	the number of children to create.
	 * @return	an ArrayList of children chromosomes.
	 */
	public ArrayList<E> performOperation(ArrayList<E> population, int numOffspring);
	
}