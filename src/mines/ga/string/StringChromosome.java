package mines.ga.string;

import mines.ga.chrom.RollingChromosome;
import java.util.*;

/**
 * Chromosome for array-based genotypes.
 */
public class StringChromosome extends RollingChromosome {

	private int[] string;	//array genotype.

	/**
	 * Chromosome constructor.
	 *
	 * @param	string	the array genotype.
	 */
	public StringChromosome(int[] string) {
		this.string = Arrays.copyOf(string,string.length);
	}

	/**
	 * Tests for equality by comparing arrays.
	 *
	 * @param	other	the Object to test for equality.
	 * @return	true if the other object is a StringChromosome,
	 *			and the strings are equivalent,
	 *			false otherwise.
	 */
	public boolean equals(Object other) {
		if (other instanceof StringChromosome) {
			return Arrays.equals(((StringChromosome) other).string,this.string);
		}
		return false;
	}

	/**
	 * Creates a copy of this chromosome with the same genotype but without any fitness.
	 *
	 * @return	a StringChromosome copy.
	 */
	public StringChromosome clone() {
		return new StringChromosome(string);
	}

	/**
	 * Returns the hash code of the array genotype.
	 *
	 * @return	the hash code.
	 */
	public int hashCode() {
		return Arrays.hashCode(string);
	}

	/**
	 * Provides a string description of this chromosome,
	 * in the form Aa-Ff-Cs,
	 * where a is the age,
	 * f is the fitness,
	 * and s is the string form of the genotype array.
	 *
	 * @return	a String form of this chromosome.
	 */
	public String toString() {
		return String.format("A%d-F%f-C%s",getAge(),getFitness(),Arrays.toString(string));
	}

	/**
	 * Returns the genotype.
	 *
	 * @return	the genotype array.
	 */
	public int[] getString() {
		return Arrays.copyOf(string,string.length);
	}
}