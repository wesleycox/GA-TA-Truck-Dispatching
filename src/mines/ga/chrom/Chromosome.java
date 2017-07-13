package mines.ga.chrom;

/**
 * Chromosome class for use in genetic algorithms.
 */

public abstract class Chromosome implements Comparable<Chromosome>, Cloneable {

	private double fitness;	//fitness of the chromosome.
	private int age;		//number of generations this chromosome has survived.
	private boolean isSet;	//whether the fitness has been set.

	/**
	 * Constructs a chromosome with unset 0 fitness and age 0.
	 */
	public Chromosome() {
		fitness = 0;
		age = 0;
		isSet = false;
	}

	/**
	 * Get the fitness value of this chromosome.
	 *
	 * @return	the current fitness.
	 */
	public double getFitness() {
		return fitness;
	}

	/**
	 * Get the age of this chromosome.
	 *
	 * @return	the current age.
	 */
	public int getAge() {
		return age;
	}

	/**
	 * Set the fitness value for this chromosome.
	 *
	 * @param	f	the new fitness value.
	 */
	public void setFitness(double f) {
		fitness = f;
		isSet = true;
	}

	/**
	 * Increment the age of this chromosome by 1.
	 */
	public void incrementAge() {
		age++;
	}

	/**
	 * Get whether fitness has been set yet.
	 *
	 * @return	true if the fitness has been set, false otherwise.
	 */
	public boolean isFitnessSet() {
		return isSet;
	}

	/**
	 * Equality test.
	 *
	 * @param	other	the Object to compare to.
	 * @return	true if equal, false otherwise.
	 */
	public abstract boolean equals(Object other);

	/**
	 * Clone this chromosome.
	 *
	 * @return	an equivalent chromosome.
	 */
	public abstract Chromosome clone();

	/**
	 * Get the hash code.
	 *
	 * @return	the hash code.
	 */
	public abstract int hashCode();

	/**
	 * Get a textual description of this chromosome.
	 *
	 * @return	a String description
	 */
	public abstract String toString();

	/**
	 * Compares this chromosome with another based on fitness.
	 *
	 * @param	other	the Chromosome to compare to
	 * @return	negative if this has lower fitness than the other, 
	 *			positive if this has higher fitness than the other, 
	 *			0 if fitness is equal.
	 */
	public int compareTo(Chromosome other) {
		return Double.compare(this.getFitness(),other.getFitness());
	}

	/**
	 * Give a fitness value.
	 * Unsupported; for use by child classes that set fitness based on multiple inputs.
	 *
	 * @param	f	the given fitness value.
	 * @throws	UnsupportedOperationException.
	 */
	public void giveFitness(double f) {
		throw new UnsupportedOperationException();
	}

}