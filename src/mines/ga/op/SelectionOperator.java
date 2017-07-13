package mines.ga.op;

import mines.ga.chrom.Chromosome;
import java.util.ArrayList;

/**
 * Selection operator class for use in genetic algorithms
 */

public interface SelectionOperator<E extends Chromosome> {

	/**
	 * Load a new list of chromosomes for selection.
	 *
	 * @param	pool	an ArrayList of chromosomes for selection.
	 */
	public void loadPool(ArrayList<E> pool);

	/**
	 * Perform selection on the loaded pool.
	 *
	 * @param	popSize	the number of chromosomes to survive.
	 * @return	an ArrayList of survivors.
	 */
	public ArrayList<E> performSelection(int popSize);
	
}