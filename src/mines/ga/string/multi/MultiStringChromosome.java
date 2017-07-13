package mines.ga.string.multi;

import mines.ga.chrom.RollingChromosome;
import java.util.*;

/**
 * Chromosome for multi-array-based genotypes.
 */
public class MultiStringChromosome extends RollingChromosome {

	private int[][] string; //2D array genotype.
	private static boolean cyclic = false; //whether to test equivalence of cycles when testing equality.

	/**
	 * Chromosome constructor.
	 *
	 * @param	string	the 2D array genotype.
	 */
	public MultiStringChromosome(int[][] string) {
		this.string = new int[string.length][];
		for (int i=0; i<string.length; i++) {
			this.string[i] = Arrays.copyOf(string[i],string[i].length);
		}
	}

	/**
	 * Equality test.
	 * If cyclic testing is enabled,
	 * arrays with different lengths are tested for equality when considered as rings,
	 * otherwise equality is based solely on equality of arrays.
	 *
	 * @param	other	the Object to test for equality.
	 * @return	true if the other object is a MultiStringChromosome,
	 *			and each pair of strings is equivalent,
	 *			false otherwise.
	 */
	public boolean equals(Object other) {
		if (other instanceof MultiStringChromosome) {
			MultiStringChromosome mo = (MultiStringChromosome) other;
			if (cyclic) {
				for (int i=0; i<string.length; i++) {
					if (!cyclicEqual(this.string[i],mo.string[i])) {
						return false;
					}
				}
				return true;
			}
			else {
				return Arrays.deepEquals(mo.string,this.string);
			}
		}
		return false;
	}

	/**
	 * Cyclic equality test of two arrays.
	 * Arrays are treated as rings and tested for equality.
	 * For example, [0,1] is equivalent to [0,1,0,1].
	 *
	 * @param	array1	the first array.
	 * @param	array2	the second array.
	 * @return	true if equivalent, false otherwise.
	 */
	private boolean cyclicEqual(int[] array1, int[] array2) {
		int len1 = array1.length;
		int len2 = array2.length;
		if (len1 == 0) {
			return (len2 == 0);
		}
		else if (len2 == 0) {
			return false;
		}
		int l = lcm(len1,len2);
		for (int i=0; i<l; i++) {
			if (array1[i % len1] != array2[i % len2]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Lowest common multiple.
	 *
	 * @param	a	an integer.
	 * @param	b	an integer.
	 * @return	the lowest common multiple of a and b.
	 */
	private int lcm(int a, int b) {
		return (a * b) / gcd(a,b);
	}

	/**
	 * Greatest common divisor.
	 *
	 * @param	a	an integer.
	 * @param	b	an integer.
	 * @return	the greatest common divisor of a and b.
	 */
	private int gcd(int a, int b) {
		return (b == 0 ? a : gcd(b,a % b));
	}

	/**
	 * Creates a copy of this chromosome with the same genotype but without any fitness.
	 *
	 * @return	a MultiStringChromosome copy.
	 */
	public MultiStringChromosome clone() {
		return new MultiStringChromosome(string);
	}

	/**
	 * Returns the hash code of the 2D array genotype.
	 *
	 * Note that if cyclic equality testing is active,
	 * then this method violates the general contract of hashCode,
	 * i.e. two chromosomes considered equal by equals can have different hash codes.
	 *
	 * @return	the hash code.
	 */
	public int hashCode() {
		return Arrays.deepHashCode(string);
	}

	/**
	 * Provides a string description of this chromosome,
	 * in the form Aa-Ff-Cs,
	 * where a is the age,
	 * f is the fitness,
	 * and s is the string form of the 2D genotype array.
	 *
	 * @return	a String form of this chromosome.
	 */
	public String toString() {
		return String.format("A%d-F%f-C%s",getAge(),getFitness(),Arrays.deepToString(string));
	}

	/**
	 * Returns the ith string in the genotype.
	 *
	 * @param	i	the index of the array to return.
	 * @return	the ith array.
	 */
	public int[] getString(int i) {
		return Arrays.copyOf(string[i],string[i].length);
	}

	/**
	 * Returns the number of strings in the genotype.
	 *
	 * @return	the size of the genotype.
	 */
	public int numStrings() {
		return string.length;
	}

	/**
	 * Set whether cyclic equality testing is active for all objects of this class.
	 *
	 * @param	c	whether cyclic equality testing should be active or not.
	 */
	public static void setCyclic(boolean c) {
		cyclic = c;
	}
}