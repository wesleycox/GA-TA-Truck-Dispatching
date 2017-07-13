package mines.sol.ga.cycle;

import mines.util.times.*;
import mines.sim.MineParameters;
import mines.ga.*;
import mines.ga.string.*;
import mines.ga.string.fitness.*;
import mines.ga.chrom.ChromosomeBuilder;
import mines.ga.op.*;
import mines.system.Debugger;
import mines.sol.Solution;
import mines.sol.string.StringController;
import java.util.*;

/**
 * Class for constructing cycle-based controllers using genetic algorithms.
 *
 * A genetic algorithm is run to optimise a variable length string of integers,
 * representing a cyclic schedule,
 * where the fitness is the average number of truckloads returned to the crusher per shift.
 * Input parameters required are for the genetic operators,
 * and the genetic algorithm,
 * including the fitness resampling scheme.
 */
public class GACycleSolution extends Solution {

	private TimeDistribution tgen;	//the distribution used in simulation.
	private double endTime;			//shift length.
	
	private double spcProb;			//crossover probability.
	private double vmProb;			//value mutation probability.
	private double inversionProb;	//inversion mutation probability.
	private double swapProb;		//swap mutation probability.
	private double moveProb;		//move mutation probability.
	private double insertProb;		//insertion mutation probability.
	private double deleteProb;		//deletion mutation probability.
	private int popSize;			//population size.
	private int selectionSize;		//offspring size.
	private double elitism;			//elitism proportion.
	private int maxGen;				//maximum generation number.
	private int bucketSize;			//fitness bucket size.
	private int resampleRate;		//fitness resampling period.
	private int resampleSize;		//number of evaluations per resampling period.

	private boolean initialised; //whether the solution has been initialised.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	params	the simulation parameters.
	 * @param	tgen	the distribution used in simulation.
	 * @param	endTime	the shift length.
	 */
	public GACycleSolution(MineParameters params, TimeDistribution tgen, double endTime) {
		super(params);
		this.tgen = tgen;
		this.endTime = endTime;
		this.spcProb = 0;
		this.vmProb = 0;
		this.inversionProb = 0;
		this.swapProb = 0;
		this.moveProb = 0;
		this.insertProb = 0;
		this.deleteProb = 0;
		this.popSize = 100;
		this.selectionSize = popSize * 2;
		this.elitism = 0;
		this.maxGen = 999;
		this.bucketSize = 20;
		this.resampleRate = 1;
		this.resampleSize = 1;
		initialised = false;
	}

	/**
	 * Set the crossover probability.
	 * Can only be used before initialisation.
	 *
	 * @param	spcProb		the crossover probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalStringOperatorF
	 */
	public GACycleSolution setSinglePointXOProb(double spcProb) {
		if (!initialised) {
			this.spcProb = spcProb;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the value mutation probability.
	 * Each complete mutation is only a single change.
	 * Can only be used before initialisation.
	 *
	 * @param	vmProb	the value mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalStringOperatorF
	 */
	public GACycleSolution setValueMutationProb(double vmProb) {
		if (!initialised) {
			this.vmProb = vmProb;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the inversion mutation probability.
	 * Each complete mutation is only a single inversion.
	 * Can only be used before initialisation.
	 *
	 * @param	inversionProb	the inversion mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalStringOperatorF
	 */
	public GACycleSolution setInversionProb(double inversionProb) {
		if (!initialised) {
			this.inversionProb = inversionProb;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the swap mutation probability.
	 * Each complete mutation is only a single swap.
	 * Can only be used before initialisation.
	 *
	 * @param	swapProb	the swap mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalStringOperatorF
	 */
	public GACycleSolution setSwapMutationProb(double swapProb) {
		if (!initialised) {
			this.swapProb = swapProb;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the move mutation probability.
	 * Each complete mutation is only a single move.
	 * Can only be used before initialisation.
	 *
	 * @param	moveProb	the move mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalStringOperatorF
	 */
	public GACycleSolution setMoveMutationProb(double moveProb) {
		if (!initialised) {
			this.moveProb = moveProb;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the insertion mutation probability.
	 * Each complete mutation is only a single insertion.
	 * Can only be used before initialisation.
	 *
	 * @param	insertProb	the insertion mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalStringOperatorF
	 */
	public GACycleSolution setInsertionProb(double insertProb) {
		if (!initialised) {
			this.insertProb = insertProb;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the deletion mutation probability.
	 * Each complete mutation is only a single deletion.
	 * Can only be used before initialisation.
	 *
	 * @param	deleteProb	the deletion mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalStringOperatorF
	 */
	public GACycleSolution setDeletionProb(double deleteProb) {
		if (!initialised) {
			this.deleteProb = deleteProb;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the population and offspring sizes.
	 * Can only be used before initialisation.
	 *
	 * @param	popSize			the population size.
	 * @param	selectionSize	the number of offspring per generation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	RollingGeneticAlgorithm
	 */
	public GACycleSolution setPopulationParams(int popSize, int selectionSize) {
		if (!initialised) {
			this.popSize = popSize;
			this.selectionSize = selectionSize;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the elitism value.
	 * Can only be used before initialisation.
	 *
	 * @param	elitism	the portion of best chromosomes to survive by elitism.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	RollingGeneticAlgorithm
	 */
	public GACycleSolution setElitism(double elitism) {
		if (!initialised) {
			this.elitism = elitism;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the number of generations to run the GA.
	 * Early termination is disabled.
	 * Can only be used before initialisation.
	 *
	 * @param	maxGen		the maximum number of generations.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	RollingGeneticAlgorithm
	 */
	public GACycleSolution setMaximumGeneration(int maxGen) {
		if (!initialised) {
			this.maxGen = maxGen;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the resampling parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	bucketSize		the fitness bucket size.
	 * @param	resampleRate	the period between fitness resampling.
	 * @param	resampleSize	the number of reevaluations per resampling period.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	RollingGeneticAlgorithm
	 */
	public GACycleSolution setSamplingParams(int bucketSize, int resampleRate, int resampleSize) {
		if (!initialised) {
			this.bucketSize = bucketSize;
			this.resampleRate = resampleRate;
			this.resampleSize = resampleSize;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Initialise this object for use.
	 * Can only be used once.
	 *
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public GACycleSolution initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Run the GA to produce a cyclic controller using on the supplied settings.
	 *
	 * @return	a StringController optimised by a GA.
	 * @throws	IllegalStateException if not initialised.
	 */
	public StringController getController() {
		if (initialised) {
			FitnessFunction<StringChromosome> ff = new CycleFitnessFunction(numTrucks,numShovels,travelTimesMean[0],travelTimesSD[0],
				fillTimesMean,fillTimesSD,emptyTimesMean[0],emptyTimesSD[0],tgen,endTime)
				.setNumSamples(1)
				.initialise();
			ChromosomeBuilder<StringChromosome> cBuilder = new StringChromosomeBuilder(numShovels);
			boolean maximising = ff.isMaximising();
			Comparator<StringChromosome> comp = new Comparator<StringChromosome>() {
				public int compare(StringChromosome a1, StringChromosome a2) {
					int diff = (int) Math.signum((maximising ? -1 : 1) * (a1.compareTo(a2)));
					return (diff == 0 ? (a2.getAge() - a1.getAge()) : diff);
				}
			};
			GeneticOperator<StringChromosome> operator = new ClassicalStringOperatorF(numShovels)
				.setSinglePointXOProb(spcProb)
				.setValueMutationParams(vmProb,1)
				.setInversionProb(inversionProb)
				.setSwapMutationParams(swapProb,1)
				.setMoveMutationProb(moveProb)
				.setInsertionParams(insertProb,1)
				.setDeletionParams(deleteProb,1)
				.initialise();
			SelectionOperator<StringChromosome> selector = new KTournamentSelectionOperator<>(maximising,4);
			GeneticAlgorithm<StringChromosome> ga = new RollingGeneticAlgorithm<>(ff,cBuilder,comp,operator,selector)
					.setPopulationParams(popSize,selectionSize)
					.setElitism(elitism)
					.setMaximumGeneration(maxGen)
					.setSamplingParams(bucketSize,resampleRate,resampleSize)
					.initialise();
			StringChromosome best = ga.run();
			return new StringController(best.getString());
		}
		else {
			throw new IllegalStateException("Solution not initialised");
		}
	}

	/**
	 * Get the solution name.
	 *
	 * @return	"Cycle by GA".
	 */
	public String getSolutionName() {
		return "Cycle by GA";
	}

}