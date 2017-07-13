package mines.sol.ga.cycle;

import mines.sol.SolutionN;
import mines.sim.MineParametersN;
import mines.util.times.TimeDistribution;
import mines.ga.string.multi.fitness.AllCycleFitnessFunction;
import mines.ga.op.*;
import mines.ga.chrom.ChromosomeBuilder;
import mines.ga.*;
import mines.ga.string.multi.*;
import mines.util.IntList;
import mines.sol.string.MultiStringController;
import mines.ga.string.multi.op.*;
import mines.system.Debugger;
import java.util.*;

/**
 * Class for constructing multi-cycle-based controllers using genetic algorithms.
 *
 * A genetic algorithm is run to optimise multiple variable length strings of integers,
 * representing cyclic schedules for each location,
 * where the fitness is the average number of truckloads returned to the crushers per shift.
 * Input parameters required are for the genetic operators,
 * and the genetic algorithm,
 * including the fitness resampling scheme.
 */
public class GACycleSolutionN extends SolutionN {

	private int[] numRoutesFrom;			//number of routes from each location.
	private IntList[] routesFromCrusher;	//lists of routes out of each crusher.
	private IntList[] routesFromShovel;		//lists of routes out of each shovel.
	private double runtime;					//shift length.
	private TimeDistribution tgen;			//distribution used in simulation.
	private int fixedShovelLength;			//length of schedules at shovels, 0 if variable length.

	private double deleteProb;		//deletion mutation probability.
	private double insertProb;		//insertion mutation probability.
	private double moveProb;		//move mutation probability.
	private double swapProb;		//swap mutation probability.
	private double inversionProb;	//inversion mutation probability.
	private double vmProb;			//value mutation probability.
	private double spcProb;			//crossover probability.
	private double smFactor;		//expected number of crossed strings per complete crossover.
	private int bucketSize;			//fitness bucket size.
	private int resampleRate;		//fitness resampling period.
	private int resampleSize;		//number of evaluations per resampling period.
	private int maxGen;				//maximum generation number.
	private double elitism;			//elitism proportion.
	private int popSize;			//population size.
	private int selectionSize;		//offspring size.
	private boolean allowSurvivors;	//whether to allow non-elite chromosomes to survive between generations.
	private int mutN;				//number of changes per complete mutation.

	private boolean initialised; //whether the solution has been initialised.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	params	the simulation parameters.
	 * @param	tgen	the distribution used in simulation.
	 * @param	runtime	the shift length.
	 */
	public GACycleSolutionN(MineParametersN params, TimeDistribution tgen, double runtime) {
		super(params);
		numRoutesFrom = new int[numShovels + numCrushers];
		routesFromCrusher = new IntList[numCrushers];
		for (int i=0; i<numCrushers; i++) {
			routesFromCrusher[i] = new IntList();
		}
		routesFromShovel = new IntList[numShovels];
		for (int i=0; i<numShovels; i++) {
			routesFromShovel[i] = new IntList();
		}
		for (int i=0; i<numRoutes; i++) {
			routesFromCrusher[routeCrushers[i]].add(i);
			routesFromShovel[routeShovels[i]].add(i);
			numRoutesFrom[routeCrushers[i]]++;
			numRoutesFrom[numCrushers + routeShovels[i]]++;
		}
		this.tgen = tgen;
		this.runtime = runtime;
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
		this.smFactor = numShovels + numCrushers;
		this.fixedShovelLength = 0;
		this.allowSurvivors = true;
		this.mutN = 1;
		initialised = false;
	}

	/**
	 * Set the crossover probability.
	 * Can only be used before initialisation.
	 *
	 * @param	spcProb		the crossover probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setSinglePointXOProb(double spcProb) {
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
	 * Can only be used before initialisation.
	 *
	 * @param	vmProb	the value mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setValueMutationProb(double vmProb) {
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
	 * Can only be used before initialisation.
	 *
	 * @param	inversionProb	the inversion mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setInversionProb(double inversionProb) {
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
	 * Can only be used before initialisation.
	 *
	 * @param	swapProb	the swap mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setSwapMutationProb(double swapProb) {
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
	 * Can only be used before initialisation.
	 *
	 * @param	moveProb	the move mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setMoveMutationProb(double moveProb) {
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
	 * Can only be used before initialisation.
	 *
	 * @param	insertProb	the insertion mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setInsertionProb(double insertProb) {
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
	 * Can only be used before initialisation.
	 *
	 * @param	deleteProb	the deletion mutation probability.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setDeletionProb(double deleteProb) {
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
	public GACycleSolutionN setPopulationParams(int popSize, int selectionSize) {
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
	public GACycleSolutionN setElitism(double elitism) {
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
	public GACycleSolutionN setMaximumGeneration(int maxGen) {
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
	public GACycleSolutionN setSamplingParams(int bucketSize, int resampleRate, int resampleSize) {
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
	 * Set the expected number of strings crossed per complete crossover.
	 * Can only be used before initialisation.
	 *
	 * @param	smFactor	the expected crossover number.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setStringMutationFactor(double smFactor) {
		if (!initialised) {
			this.smFactor = smFactor;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the length of schedules out of shovels.
	 * 0 indicates variable length.
	 * Can only be used before initialisation.
	 *
	 * @param	fixedShovelLength	the schedule length.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setShovelCycleLength(int fixedShovelLength) {
		if (!initialised) {
			this.fixedShovelLength = fixedShovelLength;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set whether to allow chromosomes to survive between generations.
	 * Can only be used before initialisation.
	 *
	 * @param	allowSurvivors	whether to allow non-elite chromosomes to survive between generations.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see RollingGeneticAlgorithm.
	 */
	public GACycleSolutionN setAllowSurvivors(boolean allowSurvivors) {
		if (!initialised) {
			this.allowSurvivors = allowSurvivors;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the number of changes per mutation event.
	 * Can only be used before initialisation.
	 *
	 * @param	mutN	the number of changes per mutation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	ClassicalMultiStringOperatorF
	 */
	public GACycleSolutionN setMutationN(int mutN) {
		if (!initialised) {
			this.mutN = mutN;
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
	public GACycleSolutionN initialise() {
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
	public MultiStringController getController() {
		if (initialised) {
			MultiStringChromosome.setCyclic(fixedShovelLength == 0);
			AllCycleFitnessFunction ff = new AllCycleFitnessFunction(numTrucks,numCrushers,numShovels,
				numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,
				numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,routeShovels,tgen,runtime)
				.setNumSamples(1)
				.initialise();
			boolean maximising = ff.isMaximising();
			int[] fixedLength = new int[numCrushers + numShovels];
			for (int i=0; i<numShovels; i++) {
				fixedLength[i + numCrushers] = fixedShovelLength;
			}
			ChromosomeBuilder<MultiStringChromosome> cBuilder = new MultiStringChromosomeBuilder(numRoutesFrom,numShovels + numCrushers,
				fixedLength);
			Comparator<MultiStringChromosome> comp = new Comparator<MultiStringChromosome>() {
				public int compare(MultiStringChromosome a1, MultiStringChromosome a2) {
					int diff = (maximising ? -1 : 1) * a1.compareTo(a2);
					return (diff == 0 ? (a2.getAge() - a1.getAge()) : diff);
				}
			};
			GeneticOperator<MultiStringChromosome> operator = new ClassicalMultiStringOperatorF(numRoutesFrom,numShovels + numCrushers,
				fixedLength)
				.setSinglePointXOParams(spcProb,smFactor)
				.setValueMutationParams(vmProb,mutN)
				.setInversionParams(inversionProb,mutN)
				.setSwapMutationParams(swapProb,mutN)
				.setMoveMutationParams(moveProb,mutN)
				.setInsertionParams(insertProb,mutN)
				.setDeletionParams(deleteProb,mutN)
				.initialise();
			SelectionOperator<MultiStringChromosome> selector = new KTournamentSelectionOperator<>(maximising,4);
			GeneticAlgorithm<MultiStringChromosome> ga = new RollingGeneticAlgorithm<>(ff,cBuilder,comp,operator,selector)
				.setPopulationParams(popSize,selectionSize)
				.setElitism(elitism)
				.setMaximumGeneration(maxGen)
				.setSamplingParams(bucketSize,resampleRate,resampleSize)
				.setAllowSurvivors(allowSurvivors)
				.initialise();
			Debugger.print("Running genetic algorithm...\n");
			MultiStringChromosome best = ga.run();
			Debugger.print("Genetic algorithm completed...\n");
			int[][] crusherCycles = new int[numCrushers][];
			int[][] shovelCycles = new int[numShovels][];
			for (int i=0; i<numCrushers; i++) {
				int[] cycle = best.getString(i);
				crusherCycles[i] = new int[cycle.length];
				for (int j=0; j<cycle.length; j++) {
					crusherCycles[i][j] = routesFromCrusher[i].get(cycle[j]);
				}
			}
			for (int i=0; i<numShovels; i++) {
				int[] cycle = best.getString(i + numCrushers);
				shovelCycles[i] = new int[cycle.length];
				for (int j=0; j<cycle.length; j++) {
					shovelCycles[i][j] = routesFromShovel[i].get(cycle[j]);
				}
			}
			return new MultiStringController(numTrucks,numCrushers,numShovels,routeCrushers,routeShovels,crusherCycles,shovelCycles);
		}
		else {
			throw new IllegalStateException("Solution not initialised");
		}
	}

	/**
	 * Get the solution name.
	 *
	 * @return	"Cycles by GA".
	 */
	public String getSolutionName() {
		return "Cycles by GA";
	}

}