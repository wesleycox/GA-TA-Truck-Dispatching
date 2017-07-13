package mines.ga.string.fitness;

import mines.ga.sim.SimpleSimFitnessFunction;
import mines.ga.string.StringChromosome;
import mines.util.times.TimeDistribution;
import mines.ga.FitnessFunction;
import java.util.*;

/**
 * Fitness function for cyclic schedules on simple road networks.
 */

public class CycleFitnessFunction extends SimpleSimFitnessFunction implements FitnessFunction<StringChromosome> {

	private double discountFactor;	//discount factor for penalising long schedules.

	private int numSamples;			//number of samples per fitness evaluation.
	private final double runtime;	//shift time.

	private int[] cycle;		//current schedule.
	private int numAssignments;	//current schedule position.

	private boolean initialised;	//whether the function has been initialised.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param numTrucks			the number of trucks.
	 * @param numShovels		the number of shovels.
	 * @param travelTimesMean	an array of average travelling times to each shovel.
	 * @param travelTimesSD		an array of standard deviations of travelling time to each shovel.
	 * @param fillTimesMean		an array of average filling times for each shovel.
	 * @param fillTimesSD		an array of standard deviations of filling times for each shovel.
	 * @param emptyTimeMean		the average emptying time of the crusher.
	 * @param emptyTimeSD		the standard deviation of the emptying time of the crusher.
	 * @param tgen				a TimeDistrubtion specifying the distribution used for generating all stochastic values.
	 * @param runtime			the length of a shift.
	 */
	public CycleFitnessFunction(int numTrucks, int numShovels, double[] travelTimesMean, double[] travelTimesSD, double[] fillTimesMean,
		double[] fillTimesSD, double emptyTimeMean, double emptyTimeSD, TimeDistribution tgen, double runtime) {
		super(numTrucks,numShovels,travelTimesMean,travelTimesSD,fillTimesMean,fillTimesSD,emptyTimeMean,emptyTimeSD,tgen);
		this.numSamples = 1;
		this.runtime = runtime;
		discountFactor = Math.pow(0.995,1.0 / numTrucks);
		initialised = false;
	}

	/**
	 * Set the number of samples per fitness evaluation.
	 * Can only be used before initialisation.
	 *
	 * @param	numSamples	the number of samples.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public CycleFitnessFunction setNumSamples(int numSamples) {
		if (!initialised) {
			this.numSamples = numSamples;
			return this;
		}
		else {
			throw new IllegalStateException("Function already initialised");
		}
	}

	/**
	 * Initialise this object for use.
	 * Can only be used once.
	 *
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public CycleFitnessFunction initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Function already initialised");
		}
	}

	/**
	 * Evaluates the fitness of a cyclic schedule specified by an array-based genotype.
	 * The fitness is the average number of empties over multiple shift simulations,
	 * with penalties for excessive length.
	 *
	 * @param	chrom	a StringChromosome containing the schedule.
	 * @return	the fitness.
	 * @throws	IllegalStateException if not initialised.
	 */
	public double getFitness(StringChromosome chrom) {
		if (initialised) {
			cycle = chrom.getString();
			double penalty = 1;
			if (cycle.length == 0) {
				return 0;
			}
			else if (cycle.length > numTrucks) {
				penalty = Math.pow(discountFactor,cycle.length - numTrucks);
			}
			double total = 0;
			for (int i=0; i<numSamples; i++) {
				reReady();
				numAssignments = 0;
				simulate(runtime);
				total += getNumEmpties();
			}
			return total * penalty / numSamples;
		}
		else {
			throw new IllegalStateException("Function not initialised");
		}
	}

	/**
	 * Returns whether the objective is to maximise.
	 *
	 * @return	true.
	 */
	public boolean isMaximising() {
		return true;
	}

	/**
	 * Get the next shovel in the cyclic schedule.
	 * 
	 * @param	tid	the index of the truck requiring routing.
	 * @return	a shovel index.
	 */
	protected int nextShovel(int tid) {
		int out = cycle[numAssignments];
		numAssignments = (numAssignments + 1) % cycle.length;
		return out;
	}
	
}