package mines.ga.string.multi.op;

import mines.ga.op.*;
import mines.ga.string.multi.MultiStringChromosome;
import java.util.*;
import java.util.concurrent.*;

/**
 * Mutation and crossover operator for multi-array-based chromosomes.
 */

public class ClassicalMultiStringOperatorF implements GeneticOperator<MultiStringChromosome> {

	/*
	 * Chromosome parameters.
	 */
	private int[] maxValues;		//the maximum values, exclusive, for each string.
	private int numStrings;			//the number of strings per chromosome.
	private int[] fixedLength;		//the length of each string if fixed, 0 indicates variable length.

	private int variable;			//not sure what this was for, it doesn't appear to do anything.

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
	private double spcSFactor;		//expected number of crossed strings per complete crossover.
	private int vmN;				//number of mutations per complete value mutation.
	private int invN;				//number of mutations per complete inversion mutation.
	private int swapN;				//number of mutations per complete swap mutation.
	private int moveN;				//number of mutations per complete move mutation.
	private int insN;				//number of mutations per complete insertion mutation.
	private int delN;				//number of mutations per complete deletion mutation.

	private boolean initialised; //whether the operator has been initialised.

	/**
	 * Constructor to set fundamental variables.
	 * All strings are set to variable length.
	 *
	 * @see	the other constructor.
	 */
	public ClassicalMultiStringOperatorF(int[] maxValues, int numStrings) {
		this(maxValues,numStrings,new int[numStrings]);
	}

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	maxValues	an array of maximum values, exclusive, for each string.
	 * @param	numStrings	the number of strings per chromosome.
	 * @param	fixedLength	an array of length of each string if fixed, 0 indicates variable length.
	 */
	public ClassicalMultiStringOperatorF(int[] maxValues, int numStrings, int[] fixedLength) {
		this.maxValues = maxValues;
		this.numStrings = numStrings;
		this.fixedLength = fixedLength;
		variable = 0;
		for (int i=0; i<numStrings; i++) {
			if (fixedLength[i] == 0) {
				variable++;
			}
		}
		this.spcProb = 0;
		this.vmProb = 0;
		this.inversionProb = 0;
		this.swapProb = 0;
		this.moveProb = 0;
		this.insertProb = 0;
		this.deleteProb = 0;
		this.spcSFactor = 0;
		this.vmN = 0;
		this.invN = 0;
		this.swapN = 0;
		this.moveN = 0;
		this.insN = 0;
		this.delN = 0;
		initialised = false;
	}

	/**
	 * Set the crossover parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	spcProb		the crossover probability.
	 * @param	spcSFactor	the expected number of crossed strings per complete crossover.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalMultiStringOperatorF setSinglePointXOParams(double spcProb, double spcSFactor) {
		if (!initialised) {
			this.spcProb = spcProb;
			this.spcSFactor = spcSFactor;
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
	public ClassicalMultiStringOperatorF setValueMutationParams(double vmProb, int vmN) {
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
	 * Set the inversion mutation parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	inversionProb	the inversion mutation probability.
	 * @param	invN			the number of mutations per complete inversion mutation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalMultiStringOperatorF setInversionParams(double inversionProb, int invN) {
		if (!initialised) {
			this.inversionProb = inversionProb;
			this.invN = invN;
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
	public ClassicalMultiStringOperatorF setSwapMutationParams(double swapProb, int swapN) {
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
	 * Set the move mutation parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	moveProb	the move mutation probability.
	 * @param	moveN		the number of mutations per complete move mutation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ClassicalMultiStringOperatorF setMoveMutationParams(double moveProb, int moveN) {
		if (!initialised) {
			this.moveProb = moveProb;
			this.moveN = moveN;
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
	public ClassicalMultiStringOperatorF setInsertionParams(double insertProb, int insN) {
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
	public ClassicalMultiStringOperatorF setDeletionParams(double deleteProb, int delN) {
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
	public ClassicalMultiStringOperatorF initialise() {
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
	 * Chromosome crossover is performed as follows:
	 * For each pair of strings, 
	 * with a certain probability perform crossover,
	 * alternatively take one of the complete strings.
	 * String crossover is single-point--
	 * for variable length strings the crossover point is chosen independently for each string,
	 * thus the crossed string can be variable length.
	 *
	 * Value mutation alters the value of n elements.
	 * Inversion mutation inverts the order of n element ranges.
	 * Swap mutation swaps n pairs of elements.
	 * Move mutation shifts n element ranges within strings.
	 * Insert mutation inserts n elements.
	 * Deletion mutation inserts n elements.
	 *
	 * @param	population		an ArrayList of parent chromosomes.
	 * @param	numOffspring	the number of children to create.
	 * @return	an ArrayList of children chromosomes.
	 * @throws	IllegalStateException if not initialised.
	 */
	public ArrayList<MultiStringChromosome> performOperation(ArrayList<MultiStringChromosome> population, int numOffspring) {
		if (initialised) {
			HashSet<MultiStringChromosome> seen = new HashSet<>(population);
			ArrayList<MultiStringChromosome> out = new ArrayList<>(numOffspring);
			int popSize = population.size();
			while (out.size() < numOffspring) {
				int i1 = ThreadLocalRandom.current().nextInt(popSize);
				MultiStringChromosome parent1 = population.get(i1);
				MultiStringChromosome child;
				if (ThreadLocalRandom.current().nextDouble() < spcProb) {
					int i2 = i1;
					while (i2 == i1) {
						i2 = ThreadLocalRandom.current().nextInt(popSize);
					}
					MultiStringChromosome parent2 = population.get(i2);
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
					child = performSwap(child);
				}
				if (ThreadLocalRandom.current().nextDouble() < moveProb) {
					child = performMove(child);
				}
				if (ThreadLocalRandom.current().nextDouble() < insertProb) {
					child = performInsertion(child);
				}
				if (ThreadLocalRandom.current().nextDouble() < deleteProb) {
					child = performDeletion(child);
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
	private MultiStringChromosome performSinglePointCrossover(MultiStringChromosome parent1, MultiStringChromosome parent2) {
		int[][] string = new int[numStrings][];
		for (int s=0; s<numStrings; s++) {
			int[] s1 = parent1.getString(s);
			int[] s2 = parent2.getString(s);
			if (s1.length > 1 && s2.length > 1 && ThreadLocalRandom.current().nextDouble() < (spcSFactor / numStrings)) {
				int l1 = ThreadLocalRandom.current().nextInt(s1.length - 1) + 1;
				int l2 = (fixedLength[s] > 0 ? fixedLength[s] - l1 : ThreadLocalRandom.current().nextInt(s2.length - 1) + 1);
				string[s] = new int[l1 + l2];
				for (int i=0; i<l1; i++) {
					string[s][i] = s1[i];
				}
				for (int i=0; i<l2; i++) {
					string[s][l1 + i] = s2[s2.length - l2 + i];
				}
			}
			else if (ThreadLocalRandom.current().nextBoolean()) {
				string[s] = s1;
			}
			else {
				string[s] = s2;
			}
		}
		return new MultiStringChromosome(string);
	}

	/**
	 * Performs value mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private MultiStringChromosome performValueMutation(MultiStringChromosome parent) {
		int[][] string = new int[numStrings][];
		for (int s=0; s<numStrings; s++) {
			string[s] = parent.getString(s);
		}
		for (int m=0; m<vmN; m++) {
			int s = ThreadLocalRandom.current().nextInt(numStrings);
			int i = ThreadLocalRandom.current().nextInt(string[s].length);
			string[s][i] = (string[s][i] + 1 + ThreadLocalRandom.current().nextInt(maxValues[s] - 1)) % maxValues[s];
		}
		return new MultiStringChromosome(string);
	}

	/**
	 * Performs inversion mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private MultiStringChromosome performInversion(MultiStringChromosome parent) {
		int[][] string = new int[numStrings][];
		for (int s=0; s<numStrings; s++) {
			string[s] = parent.getString(s);
		}
		for (int m=0; m<invN; m++) {
			int s = ThreadLocalRandom.current().nextInt(numStrings);
			if (string[s].length > 1) {
				int i1 = ThreadLocalRandom.current().nextInt(string[s].length);
				int i2 = i1;
				while (i1 == i2) {
					i2 = ThreadLocalRandom.current().nextInt(string[s].length);
				}
				if (i1 > i2) {
					int temp = i1;
					i1 = i2;
					i2 = temp;
				}
				for (int i=0; i1 + i < i2 - i; i++) {
					int temp = string[s][i1 + i];
					string[s][i1 + i] = string[s][i2 - i];
					string[s][i2 - i] = temp;
				}
			}
		}
		return new MultiStringChromosome(string);
	}

	/**
	 * Performs swap mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private MultiStringChromosome performSwap(MultiStringChromosome parent) {
		int[][] string = new int[numStrings][];
		for (int s=0; s<numStrings; s++) {
			string[s] = parent.getString(s);
		}
		for (int m=0; m<swapN; m++) {
			int s = ThreadLocalRandom.current().nextInt(numStrings);
			if (string[s].length > 1) {
				int i = ThreadLocalRandom.current().nextInt(string[s].length);
				int j = i;
				while (i == j) {
					j = ThreadLocalRandom.current().nextInt(string[s].length);
				}
				int temp = string[s][i];
				string[s][i] = string[s][j];
				string[s][j] = temp;
			}
		}
		return new MultiStringChromosome(string);
	}

	/**
	 * Performs move mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private MultiStringChromosome performMove(MultiStringChromosome parent) {
		int[][] string = new int[numStrings][];
		for (int s=0; s<numStrings; s++) {
			string[s] = parent.getString(s);
		}
		for (int m=0; m<moveN; m++) {
			int s = ThreadLocalRandom.current().nextInt(numStrings);
			if (string[s].length > 1) {
				int[] pstring = Arrays.copyOf(string[s],string[s].length);
				int start = ThreadLocalRandom.current().nextInt(pstring.length);
				int end = ThreadLocalRandom.current().nextInt(pstring.length);
				if (start > end) {
					int temp = start;
					start = end;
					end = temp;
				}
				if (end - start == pstring.length - 1) {
					string[s] = pstring;
					continue;
				}
				int dest = start;
				while (dest == start) {
					dest = ThreadLocalRandom.current().nextInt(pstring.length - (end - start));
				}
				string[s] = new int[pstring.length];
				for (int i=0; i<=end - start; i++) {
					string[s][dest + i] = pstring[start + i];
				}
				for (int i=0; i<pstring.length - (end - start + 1); i++) {
					string[s][(i < dest ? i : i + (end - start + 1))] = pstring[(i < start ? i : i + (end - start + 1))];
				}
			}
		}
		return new MultiStringChromosome(string);
	}

	/**
	 * Performs insertion mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private MultiStringChromosome performInsertion(MultiStringChromosome parent) {
		int[][] string = new int[numStrings][];
		for (int s=0; s<numStrings; s++) {
			string[s] = parent.getString(s);
		}
		for (int m=0; m<insN; m++) {
			int s = ThreadLocalRandom.current().nextInt(numStrings);
			while (fixedLength[s] > 0) {
				s = ThreadLocalRandom.current().nextInt(numStrings);
			}
			int[] pstring = Arrays.copyOf(string[s],string[s].length);
			int insert = ThreadLocalRandom.current().nextInt(pstring.length + 1);
			string[s] = new int[pstring.length + 1];
			for (int i=0; i<insert; i++) {
				string[s][i] = pstring[i];
			}
			string[s][insert] = ThreadLocalRandom.current().nextInt(maxValues[s]);
			for (int i=insert; i<pstring.length; i++) {
				string[s][i + 1] = pstring[i];
			}
		}
		return new MultiStringChromosome(string);
	}

	/**
	 * Performs deletion mutation of a parent chromosome and produces a child chromosome.
	 *
	 * @param 	parent	the parent MultiStringChromosome.
	 * @return	a child MultiStringChromosome.
	 * @see	performOperation for a complete description.
	 */
	private MultiStringChromosome performDeletion(MultiStringChromosome parent) {
		int[][] string = new int[numStrings][];
		for (int s=0; s<numStrings; s++) {
			string[s] = parent.getString(s);
		}
		for (int m=0; m<delN; m++) {
			int s = ThreadLocalRandom.current().nextInt(numStrings);
			while (fixedLength[s] > 0) {
				s = ThreadLocalRandom.current().nextInt(numStrings);
			}
			if (string[s].length > 1) {
				int[] pstring = parent.getString(s);
				int delete = ThreadLocalRandom.current().nextInt(pstring.length);
				string[s] = new int[pstring.length - 1];
				for (int i=0; i<delete; i++) {
					string[s][i] = pstring[i];
				}
				for (int i=delete + 1; i<pstring.length; i++) {
					string[s][i - 1] = pstring[i];
				}
			}
		}
		return new MultiStringChromosome(string);
	}
}