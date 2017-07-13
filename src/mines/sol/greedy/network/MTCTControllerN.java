package mines.sol.greedy.network;

import mines.sim.*;
import mines.util.IntList;
import mines.util.times.TimeDistribution;
import mines.sol.RouteController;
import mines.ga.sim.SimFitnessFunctionMCNWTL;
import java.util.*;

/**
 * Single-stage greedy heuristic controller for complex road networks.
 * Routing is performed by greedily minimising a heuristic value for a pair of routes,
 * (to and from a shovel).
 * Heuristics are calculated by performing forward simulations,
 * then averaging the results.
 *
 * For more information on these greedy heuristics see:
 *
 * Tan, S., & Ramani, R. V. (1992, February). 
 * Evaluation of computer truck dispatching criteria. 
 * In Proceedings of the SME/AIME annual meeting and exhibition, Arizona (pp. 192-215).
 */
public class MTCTControllerN implements RouteController {

	private static final double INFINITY = 1e9;

	/**
	 * Simulator for evaluating heuristic values.
	 *
	 * All references to the 'considered truck' refer to the truck currently requiring routing by this controller,
	 * (not this simulator).
	 */
	private static class GreedySimulator extends SimFitnessFunctionMCNWTL {

		private int[] assignedRoute;	//the schedule.
		private int returnRoute;		//the return route of the truck under consideration.
		private int currentDispatch;	//the index of the truck under consideration.
		private int dispatches;			//the number of times the considered truck has been routed.

		/**
		 * Simulator constructor.
		 *
		 * @param	numTrucks			the number of trucks.
		 * @param	numCrushers			the number of crushers.
		 * @param	numShovels			the number of shovels.
		 * @param	numRoads			the number of roads.
		 * @param	emptyTimesMean		an array of average emptying times for each crusher.
		 * @param	emptyTimesSD		an array of standard deviations of emptying times for each crusher.
		 * @param	fillTimesMean		an array of average filling times for each shovel.
		 * @param	fillTimesSD			an array of standard deviations of filling times for each shovel.
		 * @param	roadTravelTimesMean	an array of average travelling times on each road.
		 * @param	roadTravelTimesSD	an array of standard deviations of travelling time on each road.
		 * @param	fullSlowdown		the travel time increase for travelling full.
		 * @param	isOneWay			an array specifying whether each road is one-lane.
		 * @param	numRoutes			the number of routes.
		 * @param	routeRoads			a 2D array listing the roads comprising each route.
		 * @param	routeDirections		a 2D array listing the directions travelled on each road in each route.
		 * @param	routeLengths		an array of the number of roads in each route.
		 * @param	routeCrushers		an array of the crusher at the start of each route.
		 * @param	routeShovels		an array of the shovel at the end of each route.
		 * @param	tgen				the distribution for generating time values.
		 */
		public GreedySimulator(int numTrucks, int numCrushers, int numShovels, int numRoads, double[] emptyTimesMean, 
			double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[] roadTravelTimesMean, double[] roadTravelTimesSD,
			double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections, int[] routeLengths,
			int[] routeCrushers, int[] routeShovels, TimeDistribution tgen) {
			super(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,
				roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,routeShovels,tgen);
		}

		/**
		 * Get the next route for a truck from a fixed schedule.
		 * Returns a termination request if the considered truck has completed a cycle.
		 *
		 * @param	tid	the requesting truck.
		 * @return	a route index.
		 */
		protected int nextRoute(int tid) {
			if (tid != currentDispatch) {
				return assignedRoute[tid];
			}
			else if (dispatches == 0) {
				dispatches++;
				return assignedRoute[tid];
			}
			else if (dispatches == 1) {
				dispatches++;
				return returnRoute;
			}
			else if (dispatches == 2) {
				dispatches++;
				return -1;
			}
			else {
				throw new IllegalStateException(String.format("Current truck dispatch number is invalid: %d",dispatches));
			}
		}

		/**
		 * Get the cycle time for the considered truck given a schedule,
		 * averaged over multiple forward simulations.
		 *
		 * @param	currentDispatch	the considered truck.
		 * @param	assignedRoute	the FTA for all trucks.
		 * @param	returnRoute		the return route for the considered truck.
		 * @param	endtime			the shift length.
		 * @param	numSamples		the number of samples per evaluation.
		 * @return	the average cycle time.
		 */
		public double getValue(int currentDispatch, int[] assignedRoute, int returnRoute, double endtime, int numSamples) {
			this.currentDispatch = currentDispatch;
			this.assignedRoute = assignedRoute;
			this.returnRoute = returnRoute;
			if (!isReady) {
				ready();
			}
			double total = 0;
			for (int i=0; i<numSamples; i++) {
				reReady();
				dispatches = 0;
				simulate(endtime);
				if (dispatches == 3) {
					total += getLastServiceStart(currentDispatch) - simTime;
				}
				else {
					total += INFINITY;
				}
			}
			return total / numSamples;
		}
	}

	private int numTrucks;			//the number of trucks.
	private int numCrushers;		//the number of crushers.
	private int[] routeShovels;		//the shovel connected to each route.
	private int[] routeCrushers;	//the crusher connected to each route.

	private IntList[] routesFromCrushers;	//lists of routes out of each crusher.
	private IntList[] routesFromShovels;	//lists of routes out of each shovel.

	private int[] assignedCrusher;	//the currently assigned crusher for each truck in the simulation.
	private int[] assignedShovel;	//the currently assigned shovel for each truck in the simulation.
	private int[] assignedRoute;	//the currently assigned route for each truck in the simulation.
	private boolean[] atCrusher;	//whether each truck is at the crusher for routing purposes.

	private int[] scheduledRoute;	//the scheduled return routes for all trucks.

	private double endtime;		//the shift length.
	private int numSamples;		//the number of samples per heuristic evaluation.
	private GreedySimulator ff;	//the simulator for evaluating heuristic values.

	/**
	 * Controller constructor.
	 *
	 * @param	numTrucks			the number of trucks.
	 * @param	numCrushers			the number of crushers.
	 * @param	numShovels			the number of shovels.
	 * @param	numRoads			the number of roads.
	 * @param	emptyTimesMean		an array of average emptying times for each crusher.
	 * @param	emptyTimesSD		an array of standard deviations of emptying times for each crusher.
	 * @param	fillTimesMean		an array of average filling times for each shovel.
	 * @param	fillTimesSD			an array of standard deviations of filling times for each shovel.
	 * @param	roadTravelTimesMean	an array of average travelling times on each road.
	 * @param	roadTravelTimesSD	an array of standard deviations of travelling time on each road.
	 * @param	fullSlowdown		the travel time increase for travelling full.
	 * @param	isOneWay			an array specifying whether each road is one-lane.
	 * @param	numRoutes			the number of routes.
	 * @param	routeRoads			a 2D array listing the roads comprising each route.
	 * @param	routeDirections		a 2D array listing the directions travelled on each road in each route.
	 * @param	routeLengths		an array of the number of roads in each route.
	 * @param	routeCrushers		an array of the crusher at the start of each route.
	 * @param	routeShovels		an array of the shovel at the end of each route.
	 * @param	tgen				the distribution for generating time values.
	 * @param	endtime				the shift length.
	 * @param	numSamples			the number of samples per heuristic evaluation.
	 */
	public MTCTControllerN(int numTrucks, int numCrushers, int numShovels, int numRoads, double[] emptyTimesMean, 
		double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[] roadTravelTimesMean, double[] roadTravelTimesSD,
		double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections, int[] routeLengths,
		int[] routeCrushers, int[] routeShovels, TimeDistribution tgen, double endtime, int numSamples) {
		this.numTrucks = numTrucks;
		this.numCrushers = numCrushers;
		this.routeShovels = routeShovels;
		this.routeCrushers = routeCrushers;
		routesFromCrushers = new IntList[numCrushers];
		for (int i=0; i<numCrushers; i++) {
			routesFromCrushers[i] = new IntList();
		}
		routesFromShovels = new IntList[numShovels];
		for (int i=0; i<numShovels; i++) {
			routesFromShovels[i] = new IntList();
		}
		assignedCrusher = new int[numTrucks];
		assignedShovel = new int[numTrucks];
		assignedRoute = new int[numTrucks];
		atCrusher = new boolean[numTrucks];
		for (int i=0; i<numRoutes; i++) {
			routesFromShovels[routeShovels[i]].add(i);
			routesFromCrushers[routeCrushers[i]].add(i);
		}
		scheduledRoute = new int[numTrucks];
		this.endtime = endtime;
		this.numSamples = numSamples;
		ff = new GreedySimulator(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,
			routeShovels,tgen);
	}

	/**
	 * Determine the next route for the considered truck.
	 * 
	 * A pair of routes is chosen that minimises the cycle time of the considered truck.
	 * The return route is stored and not changed until the next cycle.
	 *
	 * @param	tid	the considered truck.
	 * @return	the next route.
	 */
	public int nextRoute(int tid) {
		if (!atCrusher[tid]) {
			return scheduledRoute[tid];
		}
		IntList availableRoutes = routesFromCrushers[assignedCrusher[tid]];
		int[] dispatch = new int[numTrucks];
		for (int i=0; i<numTrucks; i++) {
			dispatch[i] = scheduledRoute[i];
		}
		int numAvailable = availableRoutes.size();
		int[] best = new int[2];
		double bestValue = INFINITY + 1;
		for (int i=0; i<numAvailable; i++) {
			dispatch[tid] = availableRoutes.get(i);
			IntList returnRoutes = routesFromShovels[routeShovels[dispatch[tid]]];
			int rAvailable = returnRoutes.size();
			for (int j=0; j<rAvailable; j++) {
				int returnRoute = returnRoutes.get(j);
				double value = ff.getValue(tid,dispatch,returnRoute,endtime,numSamples);
				if (value < bestValue) {
					bestValue = value;
					best[0] = dispatch[tid];
					best[1] = returnRoute;
				}
			}
		}
		scheduledRoute[tid] = best[1];
		return best[0];
	}

	/**
	 * Updates stored truck state parameters based on the state change.
	 *
	 * @param	change	the StateChange received from the simulator.
	 */
	public void event(StateChange change) {
		ff.event(change);
		int truck = change.getTruck();
		TruckLocation target = change.getTarget();
		if (target != TruckLocation.WAITING && target != TruckLocation.LEAVING_SHOVEL) {
			assignedRoute[truck] = change.getFirstIndex();
			if (target == TruckLocation.FILLING) {
				atCrusher[truck] = false;
			}
			else if (target == TruckLocation.EMPTYING) {
				atCrusher[truck] = true;
			}
			else if (target == TruckLocation.TRAVEL_TO_SHOVEL) {
				assignedShovel[truck] = routeShovels[assignedRoute[truck]];
			}
			else if (target == TruckLocation.TRAVEL_TO_CRUSHER) {
				assignedCrusher[truck] = routeCrushers[assignedRoute[truck]];
			}
		}
	}

	/**
	 * Updates stored light state parameters based on the state change.
	 *
	 * @param	light	the road which has changed light state.
	 * @param	change	the new light state.
	 */
	public void lightEvent(int light, TrafficLight change) {
		ff.lightEvent(light,change);
	}

	/**
	 * Resets the stored state parameters to the start of a shift.
	 */
	public void reset() {
		ff.reset();
		for (int i=0; i<numTrucks; i++) {
			assignedCrusher[i] = i % numCrushers;
			assignedRoute[i] = routesFromCrushers[assignedCrusher[i]].get(0);
			scheduledRoute[i] = assignedRoute[i];
			assignedShovel[i] = routeShovels[assignedRoute[i]];
			atCrusher[i] = true;
		}
	}
}