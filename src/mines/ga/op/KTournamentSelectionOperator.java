package mines.ga.op;

import mines.ga.chrom.Chromosome;
import java.util.*;

/**
 * Tournament selection operator class with tournament size of k.
 */

public class KTournamentSelectionOperator<E extends Chromosome> implements SelectionOperator<E> {

	private Random rng;			//RNG.
	private boolean maximising;	//whether the fitness function maximises or minimises.
	private ArrayList<E> pool;	//the selection pool.
	private int poolSize;		//the number of elements in the selection pool.
	private int k;				//the tournament size.

	/**
	 * @param	maximising	whether the fitness function maximises or minimises.
	 * @param	k			the tournament size.
	 */
	public KTournamentSelectionOperator(boolean maximising, int k) {
		this.maximising = maximising;
		rng = new Random();
		this.k = k;
	}

	public void loadPool(ArrayList<E> pool) {
		this.pool = pool;
		poolSize = pool.size();
	}

	/**
	 * Perform tournament selection on the loaded pool.
	 *
	 * @param	popSize	the number of chromosomes to survive.
	 * @return	an ArrayList of survivors.
	 */
	public ArrayList<E> performSelection(int popSize) {
		if (popSize >= poolSize) {
			return new ArrayList<>(pool);
		}
		ArrayList<E> out = new ArrayList<>();
		int[] indexes = new int[poolSize];
		for (int i=0; i<poolSize; i++) {
			indexes[i] = i;
		}
		for (int i=0; i<popSize; i++) {
			for (int j=0; j<k; j++) {
				int r = rng.nextInt(poolSize - i - j) + i + j;
				int temp = indexes[r];
				indexes[r] = indexes[i + j];
				indexes[i + j] = temp;
			}
			E winner = pool.get(indexes[i]);
			double wFitness = winner.getFitness();
			int wInd = i;
			for (int j=1; j<k; j++) {
				E next = pool.get(indexes[i + j]);
				double nextFitness = next.getFitness();
				if (maximising == (nextFitness > wFitness)) {
					winner = next;
					wInd = i + j;
					wFitness = nextFitness;
				}
			}
			int temp = indexes[i];
			indexes[i] = indexes[wInd];
			indexes[wInd] = temp;
			out.add(winner);
		}
		return out;
	}
	
}