package mines.ga.string;

import mines.ga.op.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Mutation and crossover operator for array-based chromosomes.
 */

public class ClassicalStringOperatorF implements GeneticOperator<StringChromosome> {

	private int maxValue;	//maximum gene value, exclusive.

	/*
	 * Operator probabilities.
	 */
	private double spcProb;			//crossover probability.
	private double vmProb;			//value mutation probability.
	private double inversionProb;	//inversion mutation probability.
	private double swapProb;		//swap mutation probability.
	private double moveProb;		//move mutation probability.
	private double insertProb;		//insertion mutation probability.
	private double deleteProb;		//deletion mutation probability.
	private int vmN;				//number of mutations per complete value mutation.
	private int swapN;				//number of mutations per complete swap mutation.
	private int insN;				//number of mutations per complete insertion mutation.
	private int delN;				//number of mutations per complete deletion mutation.

	private boolean initialised; //whether the operator has been initialised.

	/**
	 * Constructor to set fundamental variable.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	maxValue	the maximum gene value, exclusive.
	 */
	public ClassicalStringOperatorF(int maxValue) {
		this.maxValue = maxValue;
		this.spcProb = 0;
		this.vmProb = 0;
		this.inversionProb = 0;
		this.swapProb = 0;
		this.moveProb = 0;
		this.insertProb = 0;
		this.deleteProb = 0;
		this.vmN = 0;
		this.swapN = 0;
		this.insN = 0;
		this.delN = 0;

		initialised = false;
	}

	/**
	 * Set the crossover probability.
	 * Can only be used before initialisation.
	 *
	 * @param	spcProb		the crossover probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalStringOperatorF setSinglePointXOProb(double spcProb) {
		if (!initialised) {
			this.spcProb = spcProb;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set the value mutation parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	vmProb	the value mutation probability.
	 * @param	vmN		the number of mutations per complete value mutation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalStringOperatorF setValueMutationParams(double vmProb, int vmN) {
		if (!initialised) {
			this.vmProb = vmProb;
			this.vmN = vmN;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set the inversion mutation probability.
	 * Can only be used before initialisation.
	 *
	 * @param	inversionProb	the inversion mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalStringOperatorF setInversionProb(double inversionProb) {
		if (!initialised) {
			this.inversionProb = inversionProb;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set the swap mutation parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	swapProb	the swap mutation probability.
	 * @param	swapN		the number of mutations per complete swap mutation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalStringOperatorF setSwapMutationParams(double swapProb, int swapN) {
		if (!initialised) {
			this.swapProb = swapProb;
			this.swapN = swapN;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set the move mutation probability.
	 * Can only be used before initialisation.
	 *
	 * @param	moveProb	the move mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalStringOperatorF setMoveMutationProb(double moveProb) {
		if (!initialised) {
			this.moveProb = moveProb;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set the insertion mutation parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	insertProb	the insertion mutation probability.
	 * @param	insN		the number of mutations per complete insertion mutation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalStringOperatorF setInsertionParams(double insertProb, int insN) {
		if (!initialised) {
			this.insertProb = insertProb;
			this.insN = insN;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set the deletion mutation parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	deleteProb	the deletion mutation probability.
	 * @param	delN		the number of mutations per complete deletion mutation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalStringOperatorF setDeletionParams(double deleteProb, int delN) {
		if (!initialised) {
			this.deleteProb = deleteProb;
			this.delN = delN;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Initialise this object for use.
	 * Can only be used once.
	 *
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalStringOperatorF initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Produce a list of offspring from a list of parents by applying crossover and mutation.
	 * Crossover and each form of mutation are performed independently with specified probabilities.
	 * Selection is uniform from the population.
	 *
	 * String crossover is single-point--
	 * for variable length strings the crossover point is chosen independently for each string,
	 * thus the crossed string can be variable length.
	 *
	 * Value mutation alters the value of n elements.
	 * Inversion mutation inverts the order of a random element ranges.
	 * Swap mutation swaps n pairs of elements.
	 * Move mutation shifts a random element ranges within the string.
	 * Insert mutation inserts n elements.
	 * Deletion mutation inserts n elements.
	 *
	 * @param	population		an ArrayList of parent chromosomes.
	 * @param	numOffspring	the number of children to create.
	 * @return	an ArrayList of children chromosomes.
	 * @throws	IllegalStateException if not initialised.
	 */
	public ArrayList<StringChromosome> performOperation(ArrayList<StringChromosome> population, int numOffspring) {
		if (initialised) {
			HashSet<StringChromosome> seen = new HashSet<>(population);
			ArrayList<StringChromosome> out = new ArrayList<>(numOffspring);
			int popSize = population.size();
			while (out.size() < numOffspring) {
				int i1 = ThreadLocalRandom.current().nextInt(popSize);
				StringChromosome parent1 = population.get(i1);
				StringChromosome child;
				if (ThreadLocalRandom.current().nextDouble() < spcProb) {
					int i2 = i1;
					while (i2 == i1) {
						i2 = ThreadLocalRandom.current().nextInt(popSize);
					}
					StringChromosome parent2 = population.get(i2);
					child = performSinglePointCrossover(parent1,parent2);
				}
				else {
					child = parent1.clone();
				}
				if (ThreadLocalRandom.current().nextDouble() < vmProb) {
					child = performValueMutation(child);
				}
				if (ThreadLocalRandom.current().nextDouble() < inversionProb) {
					child = performInversion(child);
				}
				if (ThreadLocalRandom.current().nextDouble() < swapProb) {
					child = performSwaps(child);
				}
				if (ThreadLocalRandom.current().nextDouble() < moveProb) {
					child = performMove(child);
				}
				if (ThreadLocalRandom.current().nextDouble() < insertProb) {
					child = performInsertions(child);
				}
				if (ThreadLocalRandom.current().nextDouble() < deleteProb) {
					child = performDeletions(child);
				}
				if (!seen.contains(child)) {
					out.add(child);
					seen.add(child);
				}
			}
			return out;
		}
		else {
			throw new IllegalStateException("Operator not initialised");
		}
	}

	/**
	 * Performs single point crossover on two parent chromosomes and produces a single child chromosome.
	 *
	 * @param 	parent1	the first parent MultiStringChromosome.
	 * @param	parent2 the second parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private StringChromosome performSinglePointCrossover(StringChromosome parent1, StringChromosome parent2) {
		int[] s1 = parent1.getString();
		int[] s2 = parent2.getString();
		if (s1.length <= 1 || s2.length <= 1) {
			return parent1;
		}
		int l1 = ThreadLocalRandom.current().nextInt(s1.length - 1) + 1;
		int l2 = ThreadLocalRandom.current().nextInt(s2.length - 1) + 1;
		int[] s3 = new int[l1 + l2];
		for (int i=0; i<l1; i++) {
			s3[i] = s1[i];
		}
		for (int i=0; i<l2; i++) {
			s3[l1 + i] = s2[s2.length - l2 + i];
		}
		return new StringChromosome(s3);
	}

	/**
	 * Performs value mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private StringChromosome performValueMutation(StringChromosome parent) {
		int[] string = parent.getString();
		for (int m=0; m<vmN; m++) {
			int i = ThreadLocalRandom.current().nextInt(string.length);
			string[i] = (string[i] + 1 + ThreadLocalRandom.current().nextInt(maxValue - 1)) % maxValue;
		}
		return new StringChromosome(string);
	}

	/**
	 * Performs inversion mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private StringChromosome performInversion(StringChromosome parent) {
		int[] string = parent.getString();
		if (string.length <= 1) {
			return parent;
		}
		int i1 = ThreadLocalRandom.current().nextInt(string.length);
		int i2 = i1;
		while (i1 == i2) {
			i2 = ThreadLocalRandom.current().nextInt(string.length);
		}
		if (i1 > i2) {
			int temp = i1;
			i1 = i2;
			i2 = temp;
		}
		for (int i=0; i1 + i < i2 - i; i++) {
			int temp = string[i1 + i];
			string[i1 + i] = string[i2 - i];
			string[i2 - i] = temp;
		}
		return new StringChromosome(string);
	}

	/**
	 * Performs swap mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private StringChromosome performSwaps(StringChromosome parent) {
		int[] string = parent.getString();
		if (string.length > 1) {
			for (int m=0; m<swapN; m++) {
				int i = ThreadLocalRandom.current().nextInt(string.length);
				int j = i;
				while (j == i) {
					j = ThreadLocalRandom.current().nextInt(string.length);
				}
				int temp = string[i];
				string[i] = string[j];
				string[j] = temp;
			}
		}
		return new StringChromosome(string);
	}

	/**
	 * Performs move mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private StringChromosome performMove(StringChromosome parent) {
		int[] pstring = parent.getString();
		if (pstring.length <= 1) {
			return parent;
		}
		int start = ThreadLocalRandom.current().nextInt(pstring.length);
		int end = ThreadLocalRandom.current().nextInt(pstring.length);
		if (start > end) {
			int temp = start;
			start = end;
			end = temp;
		}
		if (end - start == pstring.length - 1) {
			return parent;
		}
		int dest = start;
		while (dest == start) {
			dest = ThreadLocalRandom.current().nextInt(pstring.length - (end - start));
		}
		int[] cstring = new int[pstring.length];
		for (int i=0; i<=end - start; i++) {
			cstring[dest + i] = pstring[start + i];
		}
		for (int i=0; i<pstring.length - (end - start + 1); i++) {
			cstring[(i < dest ? i : i + (end - start + 1))] = pstring[(i < start ? i : i + (end - start + 1))];
		}
		return new StringChromosome(cstring);
	}

	/**
	 * Performs insertion mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private StringChromosome performInsertions(StringChromosome parent) {
		int[] pstring = parent.getString();
		ArrayList<Integer> clist = new ArrayList<>();
		for (int i=0; i<pstring.length; i++) {
			clist.add(pstring[i]);
		}
		for (int m=0; m<insN; m++) {
			int i = ThreadLocalRandom.current().nextInt(clist.size() + 1);
			int v = ThreadLocalRandom.current().nextInt(maxValue);
			clist.add(i,v);
		}
		int[] cstring = new int[clist.size()];
		for (int i=0; i<cstring.length; i++) {
			cstring[i] = clist.get(i);
		}
		return new StringChromosome(cstring);
	}

	/**
	 * Performs deletion mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private StringChromosome performDeletions(StringChromosome parent) {
		int[] pstring = parent.getString();
		ArrayList<Integer> clist = new ArrayList<>();
		for (int i=0; i<pstring.length; i++) {
			clist.add(pstring[i]);
		}
		for (int m=0; m<delN && clist.size() > 1; m++) {
			int i = ThreadLocalRandom.current().nextInt(clist.size());
			clist.remove(i);
		}
		int[] cstring = new int[clist.size()];
		for (int i=0; i<cstring.length; i++) {
			cstring[i] = clist.get(i);
		}
		return new StringChromosome(cstring);
	}
}