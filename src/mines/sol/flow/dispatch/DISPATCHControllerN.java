package mines.sol.flow.dispatch;

import mines.sim.*;
import mines.ga.sim.SimFitnessFunctionMCNWTL;
import mines.sol.RouteController;
import mines.util.*;
import mines.util.times.AverageTimes;
import java.util.*;

/**
 * Controller class for complex road networks,
 * based on the DISPATCH algorithm.
 *
 * Based on the algorithm presented in:
 * 
 * White, J. W., & Olson, J. P. (1986). 
 * Computer-based dispatching in mines with concurrent operating objectives. 
 * Min. Eng.(Littleton, Colo.);(United States), 38(11).
 *
 * White, J. W., Olson, J. P., & Vohnout, S. I. (1993). 
 * On improving truck/shovel productivity in open pit mines. 
 * CIM bulletin, 86, 43-43.
 */
public class DISPATCHControllerN implements RouteController {

	private static class Pair implements Comparable<Pair> {

		int i;
		double d;

		public Pair(int i, double d) {
			this.i = i;
			this.d = d;
		}

		public int compareTo(Pair other) {
			int dc = Double.compare(this.d,other.d);
			return (dc == 0 ? this.i - other.i : dc);
		}
	}

	private static class PairList extends ArrayList<Pair> {}

	/**
	 * Simulator required to run forward simulations and estimate certain variables.
	 */
	private static class Simulator extends SimFitnessFunctionMCNWTL {

		private double runtime;	//shift length.

		private int[] schedule;					//dispatch schedule to use when simulating.
		private PairList[] incoming;			//list of dispatch times for trucks arriving at the crusher.
		private double[] waitingUpToCrusher;	//total waiting time for each truck before leaving the crusher.

		/**
		 * Simulator constructor.
		 * The time distribution is set to non-random--
		 * all times use the mean value.
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
		 * @param	runtime				the shift length.
		 */
		public Simulator(int numTrucks, int numCrushers, int numShovels, int numRoads, double[] emptyTimesMean, double[] emptyTimesSD, 
			double[] fillTimesMean, double[] fillTimesSD, double[] roadTravelTimesMean, double[] roadTravelTimesSD, double fullSlowdown, 
			boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections, int[] routeLengths, int[] routeCrushers, 
			int[] routeShovels, double runtime) {
			super(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,
				roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,routeShovels,
				new AverageTimes());
			this.runtime = runtime;
			schedule = new int[numTrucks];
			waitingUpToCrusher = new double[numTrucks];
		}

		/**
		 * Get the next dispatch for a truck.
		 * Only dispatches from the crusher from a FTA,
		 * due to only requiring waiting times before arriving at the shovel,
		 * and dispatch times for trucks close to the shovel.
		 *
		 * @param	tid	the index of the requesting truck.
		 * @return	the preset assignment if leaving the crusher,
		 *			-2 otherwise,
		 *			which requests the truck move out of use.
		 */
		protected int nextRoute(int tid) {
			if (truckLocs[tid] == TruckLocation.FILLING || truckLocs[tid] == TruckLocation.LEAVING_SHOVEL) {
				return -2;
			}
			else {
				incoming[assignedCrusher[tid]].add(new Pair(tid,currTime));
				waitingUpToCrusher[tid] = getTotalWaitingTime(tid);
				return schedule[tid];
			}
		}

		/**
		 * Get the estimated upcoming dispatch times for trucks close to the crusher.
		 *
		 * @return	an array of lists of truck-time pairs,
		 *			one list for each crusher.
		 */
		public PairList[] getIncoming() {
			if (!isReady) {
				ready();
			}
			reReady();
			for (int i=0; i<numTrucks; i++) {
				schedule[i] = -2;
			}
			incoming = new PairList[numCrushers];
			for (int i=0; i<numCrushers; i++) {
				incoming[i] = new PairList();
			}
			simulate(runtime);
			return incoming;
		}

		/**
		 * Get the total waiting time between dispatch and filling for a truck-shovel pair,
		 * based on a given schedule.
		 *
		 * @param	schedule	a fixed truck assignment for all trucks.
		 * @param	tid			a truck index.
		 * @param	sid			a shovel index.
		 * @return	a double array containing the waiting of the truck between dispatch and filling,
		 *			and the total waiting time of the shovel.
		 */
		public double[] getWaitingTimes(int[] schedule, int tid, int sid) {
			if (!isReady) {
				ready();
			}
			reReady();
			for (int i=0; i<numTrucks; i++) {
				this.schedule[i] = schedule[i];
			}
			incoming = new PairList[numCrushers];
			for (int i=0; i<numCrushers; i++) {
				incoming[i] = new PairList();
			}
			simulate(runtime);
			return new double[]{getTotalWaitingTime(tid) - waitingUpToCrusher[tid],getTotalShovelWaitingTime(sid)};
		}

	}

	private int numTrucks;					//number of trucks.
	private int numCrushers;				//number of crushers.
	private int numRoutes;					//number of routes.
	private int[] routeShovels;				//shovels connected to each route.
	private int[] routeCrushers;			//crushers connected to each route.
	private IntList[] routesFromCrusher;	//lists of routes out of each crusher.
	private IntList[] routesFromShovel;		//lists of routes out of each shovel.
	private double fullSlowdown;			//full truck slowdown penalty.

	private Simulator ff;	//simulator for estimating waiting times and upcoming dispatch times.

	private double[][] flow;			//designated truck flow to each shovel -- see references.
	private double requiredTrucks;		//required number of trucks to satisfy network flow -- see references.
	private double totalDiggingRate;	//net digging rate of all shovels.
	private double[] minRouteTime;		//minimum average travel time to any shovel from each crusher.
	private double[][] meanRouteTime;	//mean route times
	private boolean[] flowOut;			//whether each crusher has any designated flow
	private int[] defaultOut;			//the default route out of a crusher
	private int[] crusherShare;			//the initial locations of each truck.

	private double simTime;				//the current time in the simulation.
	private int[] assignedShovel;		//the current assigned shovel for each truck in the simulation.
	private int[] assignedCrusher;		//the current assigned crusher for each truck in the simulation.
	private double[][] simAllocated;	//allocation variables used in the algorithm -- see references.
	private double[][] simLastDispatch;	//most recent dispatch along each route in the simulation.
	private boolean[] atCrusher;		//whether a truck is currently a crusher for dispatching purposes.
	private TruckLocation[] simLocs;	//the current state of each truck in the TA model.

	/**
	 * Constructor for DISPATCH-based controller.
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
	 * @param	runtime				the shift length.
	 * @param	flow				the expected truck flow for each route in each direction.
	 * @param	oneWayRestriction	whether the one way restriction was enabled when calculating the flow
	 * @see	LPFlowConstructor
	 */
	public DISPATCHControllerN(int numTrucks, int numCrushers, int numShovels, int numRoads, double[] emptyTimesMean, 
		double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[] roadTravelTimesMean, double[] roadTravelTimesSD, 
		double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections, int[] routeLengths, 
		int[] routeCrushers, int[] routeShovels, double runtime, double[][] flow, boolean oneWayRestriction) {
		this.numTrucks = numTrucks;
		this.numCrushers = numCrushers;
		this.numRoutes = numRoutes;
		this.routeShovels = routeShovels;
		this.routeCrushers = routeCrushers;
		this.fullSlowdown = fullSlowdown;
		routesFromCrusher = new IntList[numCrushers];
		routesFromShovel = new IntList[numShovels];
		for (int i=0; i<numCrushers; i++) {
			routesFromCrusher[i] = new IntList();
		}
		for (int i=0; i<numShovels; i++) {
			routesFromShovel[i] = new IntList();
		}
		for (int i=0; i<numRoutes; i++) {
			routesFromCrusher[routeCrushers[i]].add(i);
			routesFromShovel[routeShovels[i]].add(i);
		}

		ff = new Simulator(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,
			routeShovels,runtime);

		boolean[][] roadSuppliesShovel = new boolean[numRoads][numShovels];
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				roadSuppliesShovel[road][routeShovels[i]] = true;
			}
		}
		double[] maxExpRoadFlow = new double[numRoads];
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<numShovels; j++) {
				if (roadSuppliesShovel[i][j]) {
					maxExpRoadFlow[i] += 1.0 / fillTimesMean[j];
				}
			}
		}

		this.flow = flow;
		requiredTrucks = 0;
		totalDiggingRate = 0;
		minRouteTime = new double[numCrushers];
		meanRouteTime = new double[numRoutes][2];
		double[] trucksOnSide = new double[numCrushers];
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				double scale = 1;
				if (!oneWayRestriction && isOneWay[road]) {
					scale = (maxExpRoadFlow[road] > 0.5 / roadTravelTimesMean[road] ? 2 : 1 + 0.25 * maxExpRoadFlow[road] / (0.5 / 
						roadTravelTimesMean[road]));
				}
				for (int k=0; k<2; k++) {
					meanRouteTime[i][k] += roadTravelTimesMean[road] * (k == 0 ? 1 : fullSlowdown) * scale;
				}
			}
			requiredTrucks += (meanRouteTime[i][0] + fillTimesMean[routeShovels[i]]) * flow[i][0] + (meanRouteTime[i][1] + 
				emptyTimesMean[routeCrushers[i]]) * flow[i][1];
			totalDiggingRate += flow[i][0];
			trucksOnSide[routeCrushers[i]] += flow[i][0] * (meanRouteTime[i][0] + fillTimesMean[routeShovels[i]]) + flow[i][1] * 
				(meanRouteTime[i][1] + emptyTimesMean[routeCrushers[i]]);
		}
		for (int i=0; i<numCrushers; i++) {
			int min = routesFromCrusher[i].get(0);
			for (int j=1; j<routesFromCrusher[i].size(); j++) {
				int route = routesFromCrusher[i].get(j);
				if (meanRouteTime[min][0] > meanRouteTime[route][0]) {
					min = route;
				}
			}
			minRouteTime[i] = meanRouteTime[min][0];
		}
		flowOut = new boolean[numCrushers];
		defaultOut = new int[numCrushers];
		double[] shovelFlow = new double[numShovels];
		for (int i=0; i<numRoutes; i++) {
			if (flow[i][0] > 0) {
				flowOut[routeCrushers[i]] = true;
				shovelFlow[routeShovels[i]] += flow[i][0];
			}
		}
		int defaultShovel = 0;
		for (int i=1; i<numShovels; i++) {
			if (shovelFlow[i] > shovelFlow[defaultShovel]) {
				defaultShovel = i;
			}
		}
		for (int i=0; i<numCrushers; i++) {
			for (int j=0; j<routesFromCrusher[i].size(); j++) {
				int route = routesFromCrusher[i].get(j);
				if (routeShovels[route] == defaultShovel) {
					defaultOut[i] = route;
					break;
				}
			}
		}
		crusherShare = new int[numTrucks];
		int place = 0;
		for (int i=0; i<numCrushers - 1; i++) {
			int portion = (int) (numTrucks * trucksOnSide[i] / requiredTrucks);
			for (int j=0; j<portion; j++) {
				crusherShare[place] = i;
				place++;
			}
		}
		while (place < numTrucks) {
			crusherShare[place] = numCrushers - 1;
			place++;
		}
		ff.setInitialCrushers(crusherShare);

		assignedShovel = new int[numTrucks];
		assignedCrusher = new int[numTrucks];
		simAllocated = new double[numRoutes][2];
		simLastDispatch = new double[numRoutes][2];
		atCrusher = new boolean[numTrucks];
		simLocs = new TruckLocation[numTrucks];

		reset();
	}

	/**
	 * Determine the next route assignment for a given truck.
	 * Uses the DISPATCH algorithm,
	 * which involves sorting trucks by expected dispatch time at the crusher,
	 * sorting paths by a priority function,
	 * and assigning trucks to paths to minimise a lost-tons function.
	 * See the references for more information.
	 *
	 * @param	tid	the requesting truck index.
	 * @return	the route assignment.
	 * @throws	IllegalArgumentException if the truck is not at the crusher.
	 */
	public int nextRoute(int tid) {
		if (!atCrusher[tid]) {
			int sid = assignedShovel[tid];
			IntList routesOut = routesFromShovel[sid];
			int availableRoutes = routesOut.size();
			int best = -1;
			double bestValue = 1e9;
			for (int i=0; i<availableRoutes; i++) {
				int route = routesOut.get(i);
				if (flow[route][1] > 0) {
					double allocated = Math.max(0,simAllocated[route][1] - (simTime - simLastDispatch[route][1]) * flow[route][1]);
					double desired = meanRouteTime[route][1] * flow[route][1];
					double ratio = allocated / desired;
					if (best < 0 || ratio < bestValue) {
						best = route;
						bestValue = ratio;
					}
				}
			}
			return best;
		}
		else if (!flowOut[assignedCrusher[tid]]) {
			return defaultOut[assignedCrusher[tid]];
		}
		PairList[] incomingTrucks = ff.getIncoming();
		ShortPriorityQueue<Pair> pathNeed = new ShortPriorityQueue<>();
		double[] lastDispatch = new double[numRoutes];
		double[] allocated = new double[numRoutes];
		for (int r=0; r<numRoutes; r++) {
			if (flow[r][0] > 0) {
				lastDispatch[r] = simLastDispatch[r][0];
				allocated[r] = simAllocated[r][0];
				double needTime = lastDispatch[r] + allocated[r] / flow[r][0] - meanRouteTime[r][0];
				pathNeed.add(new Pair(r,needTime));
			}
		}
		int[] schedule = new int[numTrucks];
		for (int i=0; i<numTrucks; i++) {
			schedule[i] = -2;
		}
		while (true) {
			Pair neediest = pathNeed.poll();
			int route = neediest.i;
			int cid = routeCrushers[route];
			Pair bestPair = null;
			double bestValue = 1e9;
			double baseLoss = totalDiggingRate * (meanRouteTime[route][0] - minRouteTime[cid]) / requiredTrucks;
			for (Pair p : incomingTrucks[cid]) {
				if (schedule[p.i] < 0) {
					schedule[p.i] = route;
					double[] waitingTimes = ff.getWaitingTimes(schedule,p.i,routeShovels[route]);
					schedule[p.i] = -2;
					double lostTons = baseLoss + waitingTimes[0] * totalDiggingRate / requiredTrucks + waitingTimes[1] * flow[route][0];
					if (bestPair == null || lostTons < bestValue) {
						bestPair = p;
						bestValue = lostTons;
					}
				}
			}
			if (bestPair != null) {
				if (bestPair.d < lastDispatch[route]) {
					return getByGreedy(tid);
				}
				if (bestPair.i == tid) {
					return route;
				}
				schedule[bestPair.i] = route;
				allocated[route] = Math.max(0,allocated[route] - (bestPair.d - lastDispatch[route]) * flow[route][0]) + 1;
				lastDispatch[route] = bestPair.d;
				double needTime = lastDispatch[route] + allocated[route] / flow[route][0] - meanRouteTime[route][0];
				pathNeed.add(new Pair(route,needTime));
			}
		}
	}

	/**
	 * Assign the next to the truck greedily to the route which will minimise the lost-tons function.
	 * Used when the algorithm decides a truck should be dispatched out of order,
	 * because it will spend too much time waiting wherever it arrives otherwise.
	 *
	 * @param	tid	the requesting truck index.
	 * @return	the route assignment.
	 */
	private int getByGreedy(int tid) {
		int cid = assignedCrusher[tid];
		IntList routesOut = routesFromCrusher[cid];
		int availableRoutes = routesOut.size();
		int[] schedule = new int[numTrucks];
		for (int i=0; i<numTrucks; i++) {
			schedule[i] = -2;
		}
		int best = -1;
		double bestValue = 1e9;
		for (int i=0; i<availableRoutes; i++) {
			int route = routesOut.get(i);
			if (flow[route][0] > 0) {
				double baseLoss = totalDiggingRate * (meanRouteTime[route][0] - minRouteTime[cid]) / requiredTrucks;
				double baseSWT = ff.getWaitingTimes(schedule,tid,routeShovels[route])[1];
				schedule[tid] = route;
				double[] waitingTimes = ff.getWaitingTimes(schedule,tid,routeShovels[route]);
				schedule[tid] = -2;
				double lostTons = baseLoss + waitingTimes[0] * totalDiggingRate / requiredTrucks + (waitingTimes[1] - baseSWT) * 
					flow[route][0];
				if (best < 0 || lostTons < bestValue) {
					best = route;
					bestValue = lostTons;
				}
			}
		}
		return best;
	}

	/**
	 * Updates stored truck state parameters based on the state change.
	 *
	 * @param	change	the StateChange received from the simulator.
	 */
	public void event(StateChange change) {
		ff.event(change);
		simTime = change.getTime();
		int truck = change.getTruck();
		TruckLocation prevLoc = simLocs[truck];
		simLocs[truck] = change.getTarget();
		if (simLocs[truck] != TruckLocation.WAITING && simLocs[truck] != TruckLocation.LEAVING_SHOVEL) {
			int route = change.getFirstIndex();
			switch (simLocs[truck]) {
				case FILLING: {
					atCrusher[truck] = false;
					break;
				}
				case EMPTYING: {
					atCrusher[truck] = true;
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					assignedShovel[truck] = routeShovels[route];
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					assignedCrusher[truck] = routeCrushers[route];
					break;
				}
			}
			switch (prevLoc) {
				case WAITING: {
					simAllocated[route][0] = Math.max(0,simAllocated[route][0] - (simTime - simLastDispatch[route][0]) * flow[route][0]) + 1;
					simLastDispatch[route][0] = simTime;
					break;
				}
				case LEAVING_SHOVEL: {
					simAllocated[route][1] = Math.max(0,simAllocated[route][1] - (simTime - simLastDispatch[route][1]) * flow[route][1]) + 1;
					simLastDispatch[route][1] = simTime;
					break;
				}
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
		simTime = 0;
		for (int i=0; i<numTrucks; i++) {
			assignedCrusher[i] = crusherShare[i];
			assignedShovel[i] = routesFromCrusher[assignedCrusher[i]].get(0);
			atCrusher[i] = true;
			simLocs[i] = TruckLocation.WAITING;
		}
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<2; j++) {
				simAllocated[i][j] = 0;
				simLastDispatch[i][j] = 0;
			}
		}
	}

	/**
	 * Get the initial crusher locations of each truck at the start of the shift.
	 *
	 * @return	an array of crusher indexes.
	 */
	public int[] getInitialCrushers() {
		return crusherShare;
	}

}