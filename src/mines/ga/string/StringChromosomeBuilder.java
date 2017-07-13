package mines.ga.string;

import mines.ga.chrom.ChromosomeBuilder;
import java.util.*;

/**
 * Builder class for variable length array-based chromosomes.
 */
public class StringChromosomeBuilder implements ChromosomeBuilder<StringChromosome> {

	private int maxValue;	//maximum gene value, exclusive.
	private int avSize;		//average string length.

	/**
	 * Builder constructor.
	 * Sets the average length equal to maxValue.
	 *
	 * @see	the other constructor
	 */
	public StringChromosomeBuilder(int maxValue) {
		this(maxValue,maxValue);
	}

	/**
	 * Builder constructor.
	 *
	 * @param	maxValue	the maximum gene value.
	 * @param	avSize		the average array length.
	 */
	public StringChromosomeBuilder(int maxValue, int avSize) {
		this.maxValue = maxValue;
		this.avSize = avSize;
	}

	/**
	 * Creates a random chromosome.
	 * Each value is uniformly chosen between 0, inclusive,
	 * and the max value, exclusive.
	 * String length is generated by an exponential distribution.
	 *
	 * @param	rng	the RNG.
	 * @return	a randomly generated StringChromosome
	 */
	public StringChromosome getRandomChromosome(Random rng) {
		int length = Math.max((int) (avSize * -Math.log(rng.nextDouble())),1);
		int[] string = new int[length];
		for (int i=0; i<length; i++) {
			string[i] = rng.nextInt(maxValue);
		}
		return new StringChromosome(string);
	}

}