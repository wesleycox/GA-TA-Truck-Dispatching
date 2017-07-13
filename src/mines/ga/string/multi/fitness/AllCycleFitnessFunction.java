package mines.ga.string.multi.fitness;

import mines.util.times.TimeDistribution;
import mines.ga.sim.SimpleSimFitnessFunctionMCNWTL;
import mines.ga.string.multi.MultiStringChromosome;
import mines.util.IntList;
import mines.sim.TruckLocation;
import mines.ga.FitnessFunction;

/**
 * Fitness function for cyclic schedules on complex road networks.
 */

public class AllCycleFitnessFunction extends SimpleSimFitnessFunctionMCNWTL implements FitnessFunction<MultiStringChromosome> {

	private double runtime;						//shift time.
	private int numSamples;						//number of samples per fitness evaluation.
	private double crusherDiscountFactor;		//discount factor for penalising long crusher schedules.
	private double shovelDiscountFactor;		//discount factor for penalising long shovel schedules.
	private static final int IDEALSCLEN = 4;	//length of 'long' shovel schedule.
	private boolean initialised;				//whether the function has been initialised.

	private IntList[] routesFromCrusher;	//lists of routes from each crusher.
	private IntList[] routesFromShovel;		//lists of routes from each shovel.

	private int[] numAssignments;	//current schedule positions.
	private int[][] routeCycle;		//current schedules.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param numTrucks				the number of trucks.
	 * @param numCrushers			the number of crushers.
	 * @param numShovels			the number of shovels.
	 * @param numRoads				the number of roads.
	 * @param emptyTimesMean		an array of average emptying times for each crusher.
	 * @param emptyTimesSD			an array of standard deviations of emptying times for each crusher.
	 * @param fillTimesMean			an array of average filling times for each shovel.
	 * @param fillTimesSD			an array of standard deviations of filling times for each shovel.
	 * @param roadTravelTimesMean	an array of average travelling times on each road.
	 * @param roadTravelTimesSD		an array of standard deviations of travelling time on each road.
	 * @param fullSlowdown			the travel time increase for travelling full.
	 * @param isOneWay				an array specifying whether each road is one-lane.
	 * @param numRoutes				the number of routes.
	 * @param routeRoads			a 2D array listing the roads comprising each route.
	 * @param routeDirections		a 2D array listing the directions travelled on each road in each route.
	 * @param routeLengths			an array of the number of roads in each route.
	 * @param routeCrushers			an array of the crusher at the start of each route.
	 * @param routeShovels			an array of the shovel at the end of each route.
	 * @param tgen					a TimeDistrubtion specifying the distribution used for generating all stochastic values.
	 * @param runtime				the length of a shift.
	 */
	public AllCycleFitnessFunction(int numTrucks, int numCrushers, int numShovels, int numRoads, double[] emptyTimesMean, 
		double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[] roadTravelTimesMean, double[] roadTravelTimesSD,
		double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections, int[] routeLengths,
		int[] routeCrushers, int[] routeShovels, TimeDistribution tgen, double runtime) {
		super(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,
			roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,routeShovels,tgen);
		this.runtime = runtime;
		crusherDiscountFactor = Math.pow(0.995,1.0 / numTrucks);
		// shovelDiscountFactor = Math.pow(0.995,1.0 / IDEALSCLEN);
		shovelDiscountFactor = 0.995;
		numSamples = 1;
		numAssignments = new int[numCrushers + numShovels];
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
		}
		routeCycle = new int[numCrushers + numShovels][];
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
	public AllCycleFitnessFunction setNumSamples(int numSamples) {
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
	public AllCycleFitnessFunction initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Function already initialised");
		}
	}

	/**
	 * Evaluates the fitness of a cyclic schedule specified by a multi-array-based genotype.
	 * Genotype values correspond to indexes of route lists,
	 * not route indexes themselves,
	 * hence all valid values range from 0, inclusive, 
	 * to the maximum number of routes for a location, exclusive.
	 * The fitness is the average number of empties over multiple shift simulations,
	 * with penalties for excessive length.
	 *
	 * @param	chrom	a MultiStringChromosome representing the schedules.
	 * @return	the fitness.
	 * @throws	IllegalStateException if not initialised.
	 */
	public double getFitness(MultiStringChromosome chrom) {
		if (initialised) {
			double penalty = 1;
			for (int i=0; i<numCrushers; i++) {
				int[] cycle = chrom.getString(i);
				routeCycle[i] = new int[cycle.length];
				for (int j=0; j<cycle.length; j++) {
					routeCycle[i][j] = routesFromCrusher[i].get(cycle[j]);
				}
				if (cycle.length == 0) {
					return 0;
				}
				else if (cycle.length > numTrucks) {
					penalty *= Math.pow(crusherDiscountFactor,cycle.length - numTrucks);
				}
			}
			for (int i=0; i<numShovels; i++) {
				int[] cycle = chrom.getString(i + numCrushers);
				routeCycle[i + numCrushers] = new int[cycle.length];
				for (int j=0; j<cycle.length; j++) {
					routeCycle[i + numCrushers][j] = routesFromShovel[i].get(cycle[j]);
				}
				if (cycle.length == 0) {
					return 0;
				}
				else if (cycle.length > IDEALSCLEN) {
					penalty *= Math.pow(shovelDiscountFactor,cycle.length - IDEALSCLEN);
				}
			}
			double total = 0;
			for (int i=0; i<numSamples; i++) {
				reReady();
				for (int j=0; j<numCrushers + numShovels; j++) {
					numAssignments[j] = 0;
				}
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
	 * Get the next route in the cyclic schedule for the required location.
	 * 
	 * @param	tid	the index of the truck requiring routing.
	 * @return	a route index.
	 */
	protected int nextRoute(int tid) {
		if (truckLocs[tid] == TruckLocation.FILLING) {
			int sid = assignedShovel[tid];
			int out = routeCycle[numCrushers + sid][numAssignments[numCrushers + sid]];
			numAssignments[numCrushers + sid] = (numAssignments[numCrushers + sid] + 1) % routeCycle[numCrushers + sid].length;
			return out;
		}
		else {
			int cid = assignedCrusher[tid];
			int out = routeCycle[cid][numAssignments[cid]];
			numAssignments[cid] = (numAssignments[cid] + 1) % routeCycle[cid].length;
			return out;
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

}