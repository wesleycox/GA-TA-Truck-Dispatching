package mines.ga.sim;

import mines.util.times.TimeDistribution;
import mines.util.*;
import mines.sim.*;
import java.util.*;

/**
 * Simulator class for a complex road network that can initialise a simulation from a stored state.
 * Intended to be extended as a fitness function.
 */

public abstract class SimFitnessFunctionMCNWTL {

	private static class Pair implements Comparable<Pair> {

		int t;
		double p;

		public Pair(int t, double p) {
			this.t = t;
			this.p = p;
		}

		public int compareTo(Pair other) {
			int out = (int) Math.signum(other.p - this.p);
			return (out == 0 ? this.t - other.t : out);
		}
	}

	private static final double EPSILON = 1e-6;	//small value.
	private static final double INFINITY = 1e9;	//large value.

	/*
	 * Simulation parameters.
	 */
	protected int numTrucks;				//number of trucks.
	protected final int numShovels;			//number of shovels.
	protected final int numCrushers;		//number of crushers.
	private int numRoads;					//number of roads.
	private double[] emptyTimesMean;		//average emptying times for each crusher.
	private double[] emptyTimesSD;			//standard deviations of emptying times for each crusher.
	private double[] fillTimesMean;			//average filling times for each shovel.
	private double[] fillTimesSD;			//standard deviations of filling times for each shovel.
	private double[] roadTravelTimesMean;	//average travelling times on each road.
	private double[] roadTravelTimesSD;		//standard deviations of travelling time on each road.
	private double fullSlowdown;			//travel time increase for travelling full.
	private boolean[] isOneWay;				//whether each road is one-lane.

	/*
	 * Route specifications.
	 */
	private int numRoutes;				//number of routes.
	private int[][] routeRoads;			//list of roads comprising each route.
	private int[][] routeDirections;	//list of directions travelled on each road in each route.
	private int[] routeLengths;			//number of roads in each route.
	private int[] routeShovels;			//the shovel at the end of each route.
	private int[] routeCrushers;		//the crusher at the start of each route.

	private TimeDistribution tgen;	//the distribution used for generating all stochastic values.

	private int[] initialCrushers;	//location of each truck at start of shift.

	/*
	 * Stored state variables.
	 */
	protected double simTime;			//current time in stored state.
	private TruckLocation[] simLocs;	//current truck locations in stored state.
	private int[] simAShovel;			//current assigned shovel for each truck in stored state.
	private int[] simACrusher;			//current assigned crusher for each truck in stored state.
	private int[] simARoute;			//current assigned route for each truck in stored state.
	private int[] simRoutePoint;		//current route index for each truck in stored state.
	private double[] simProgress;		//current fractional completion of current task for each truck in stored state.
	private TrafficLight[] simLights;	//current state of each traffic light in stored state.

	/*
	 * Current simulation variables.
	 */
	private int numEmpties;									//number of empties completed in current simulation.
	protected double currTime;								//current time in current simulation.
	private ShortPriorityQueue<Transition> eventQueue;		//upcoming non-instant transitions in current simulation.
	private ShortPriorityQueue<Transition> instantQueue;	//upcoming instant transitions in current simulation.
	protected TruckLocation[] truckLocs;					//current locations of each truck in current simulation.
	protected int[] assignedShovel;							//current assigned shovel for each truck in current simulation.
	protected int[] assignedCrusher;						//current assigned crusher for each truck in current simulation.
	protected int[] assignedRoute;							//current assigned route for each truck in current simulation.
	private IntQueue[] crusherQueues;						//queues for each crusher in current simulation.
	private IntQueue[] shovelQueues;						//queues for each shovel in current simulation.
	private IntQueue[][] lightQueues;						//queues for each traffic light in current simulation.
	private TrafficLight[] lights;							//state of each traffic light in current simulation.
	private double[][] roadAvailable;						//the minimum possible arrival time for each road end in current simulation.
	private int[][] roadPriority;							//priority values used for transitions to preserve order.
	private IntQueue[][] roadQueues;						//order of trucks on each road in current simulation.
	private int[] routePoint;								//current route index for each truck in current simulation.

	/*
	 * Variables related to the stored state allowing for quick reinitialisation of simulation.
	 */
	private ArrayList<Transition> instantQueueStored;	//initial instant transitions from stored state.
	private IntQueue[] crusherQueuesStored;				//queues for each crusher from stored state.
	private IntQueue[] shovelQueuesStored;				//queues for each shovel from stored state.
	private IntQueue[][] lightQueuesStored;				//queues for each traffic light from stored state.
	private int[][] roadPriorityStored;					//initial priority values from stored state.
	private IntQueue[][] roadQueuesStored;				//order of trucks on each road from stored state.
	private ArrayList<Transition> eventQueueStored;		//initial non-instant transitions from stored state.

	protected boolean isReady;	//whether the stored state is unchanged since the last simulation.

	/*
	 * Statistics for current simulation.
	 */
	private double[] roadWaitingTime;		//total waiting time of each truck at traffic lights in current simulation.
	private double[] serviceWaitingTime;	//total waiting time of each truck at crushers and shovels in current simulation.
	private double[] lastServiceStart;		//time of last service start, i.e. emptying or filling, for each truck in current simulation.
	private double[] lastWaitStart;			//time of last queue entry for each truck in current simulation.
	private double[] lastFillEnd;			//time of last service completion of each shovel in current simulation.
	private double[] lastEmptyEnd;			//time of last service completion of each crusher in current simulation.
	private double[] serviceAvailableTime;	//time of last service completion before each truck started service in current simulation.
	private int[] serviced;					//number of completed services for each truck in current simulation.
	private double[] shovelWaitingTime;		//total waiting time of each shovel in current simulation.

	/**
	 * Simulator constructor.
	 * Sets the initial stored state as the default with all trucks at crushers.
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
	 */
	public SimFitnessFunctionMCNWTL(int numTrucks, int numCrushers, int numShovels, int numRoads, double[] emptyTimesMean, 
		double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[] roadTravelTimesMean, double[] roadTravelTimesSD,
		double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections, int[] routeLengths,
		int[] routeCrushers, int[] routeShovels, TimeDistribution tgen) {
		this.numTrucks = numTrucks;
		this.numShovels = numShovels;
		this.numCrushers = numCrushers;
		this.numRoads = numRoads;
		this.emptyTimesMean = emptyTimesMean;
		this.emptyTimesSD = emptyTimesSD;
		this.fillTimesMean = fillTimesMean;
		this.fillTimesSD = fillTimesSD;
		this.roadTravelTimesMean = roadTravelTimesMean;
		this.roadTravelTimesSD = roadTravelTimesSD;
		this.fullSlowdown = fullSlowdown;
		this.isOneWay = isOneWay;
		this.numRoutes = numRoutes;
		this.routeRoads = routeRoads;
		this.routeDirections = routeDirections;
		this.routeLengths = routeLengths;
		this.routeCrushers = routeCrushers;
		this.routeShovels = routeShovels;

		this.tgen = tgen;

		simLocs = new TruckLocation[numTrucks];
		simAShovel = new int[numTrucks];
		simACrusher = new int[numTrucks];
		simARoute = new int[numTrucks];
		simRoutePoint = new int[numTrucks];
		simProgress = new double[numTrucks];
		simLights = new TrafficLight[numRoads];

		initialCrushers = new int[numTrucks];
		for (int i=0; i<numTrucks; i++) {
			initialCrushers[i] = i % numCrushers;
		}

		eventQueue = new ShortPriorityQueue<>();
		instantQueue = new ShortPriorityQueue<>();
		truckLocs = new TruckLocation[numTrucks];
		assignedShovel = new int[numTrucks];
		assignedCrusher = new int[numTrucks];
		assignedRoute = new int[numTrucks];
		crusherQueues = new IntQueue[numCrushers];
		crusherQueuesStored = new IntQueue[numCrushers];
		for (int i=0; i<numCrushers; i++) {
			crusherQueues[i] = new IntQueue();
			crusherQueuesStored[i] = new IntQueue();
		}
		shovelQueues = new IntQueue[numShovels];
		shovelQueuesStored = new IntQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i] = new IntQueue();
			shovelQueuesStored[i] = new IntQueue();
		}
		lightQueues = new IntQueue[numRoads][2];
		lightQueuesStored = new IntQueue[numRoads][2];
		roadQueues = new IntQueue[numRoads][2];
		roadQueuesStored = new IntQueue[numRoads][2];
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				if (isOneWay[i]) {
					lightQueues[i][j] = new IntQueue();
					lightQueuesStored[i][j] = new IntQueue();
				}
				roadQueues[i][j] = new IntQueue();
				roadQueuesStored[i][j] = new IntQueue();
			}
		}
		lights = new TrafficLight[numRoads];
		roadAvailable = new double[numRoads][2];
		roadPriority = new int[numRoads][2];
		routePoint = new int[numTrucks];

		instantQueueStored = new ArrayList<>();
		roadPriorityStored = new int[numRoads][2];
		eventQueueStored = new ArrayList<>();

		isReady = false;

		roadWaitingTime = new double[numTrucks];
		serviceWaitingTime = new double[numTrucks];
		lastServiceStart = new double[numTrucks];
		lastWaitStart = new double[numTrucks];
		lastFillEnd = new double[numShovels];
		lastEmptyEnd = new double[numCrushers];
		serviceAvailableTime = new double[numTrucks];
		serviced = new int[numTrucks];
		shovelWaitingTime = new double[numShovels];
	}

	/**
	 * Set the initial locations of each truck for the starting state.
	 * 
	 * @param	initialCrushers	an array of crusher indexes for each truck.
	 */
	public void setInitialCrushers(int[] initialCrushers) {
		this.initialCrushers = initialCrushers;
	}

	/**
	 * Get the next route for the given truck.
	 * 
	 * @param	tid	the index of the truck requiring routing.
	 * @return	a positive route index,
	 *			or -2 to take the truck out of use,
	 *			otherwise any negative value to terminate the current simulation.
	 */
	protected abstract int nextRoute(int tid);

	/**
	 * Resets the stored state to the start of a shift.
	 */
	public void reset() {
		simTime = 0;
		for (int i=0; i<numTrucks; i++) {
			simLocs[i] = TruckLocation.WAITING;
			simACrusher[i] = initialCrushers[i];
			for (int j=0; j<numRoutes; j++) {
				if (routeCrushers[j] == simACrusher[i]) {
					simARoute[i] = j;
					simAShovel[i] = routeShovels[j];
					break;
				}
				if (j == numRoutes - 1) {
					throw new IllegalStateException("No routes out of crusher");
				}
			}
			simRoutePoint[i] = 0;
			simProgress[i] = 0;
		}
		for (int i=0; i<numRoads; i++) {
			simLights[i] = TrafficLight.GR;
		}
		isReady = false;
	}

	/**
	 * Update the stored state for the trucks.
	 * Should be used after each transition.
	 * 
	 * @param	change	a StateChange specifying the transition that occurred.
	 */
	public void event(StateChange change) {
		simTime = change.getTime();
		int truck = change.getTruck();
		simLocs[truck] = change.getTarget();
		simARoute[truck] = change.getFirstIndex();
		simRoutePoint[truck] = change.getSecondIndex();
		simAShovel[truck] = routeShovels[simARoute[truck]];
		simACrusher[truck] = routeCrushers[simARoute[truck]];
		for (int i=0; i<numTrucks; i++) {
			simProgress[i] = change.getProgress(i);
		}
		isReady = false;
	}

	/**
	 * Update the stored state for the traffic lights.
	 * Should be used after each change in traffic lights.
	 * 
	 * @param	light	the road which changed light state.
	 * @param	change	the new TrafficLight value.
	 */
	public void lightEvent(int light, TrafficLight change) {
		simLights[light] = change;
		isReady = false;
	}

	/**
	 * Readies the simulator for initialisation based on the stored state.
	 * Should be run once if the stored state has changed, as indicated by the isReady variable.
	 */
	protected void ready() {
		ArrayList<Pair> progressList = new ArrayList<>(numTrucks);
		for (int i=0; i<numTrucks; i++) {
			progressList.add(new Pair(i,simProgress[i]));
		}
		Collections.sort(progressList);
		instantQueueStored.clear();
		eventQueueStored.clear();
		for (int i=0; i<numCrushers; i++) {
			crusherQueuesStored[i].clear();
		}
		for (int i=0; i<numShovels; i++) {
			shovelQueuesStored[i].clear();
		}
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				if (isOneWay[i]) {
					lightQueuesStored[i][j].clear();
				}
				roadPriorityStored[i][j] = Integer.MIN_VALUE;
				roadQueuesStored[i][j].clear();
			}
		}
		for (Pair p : progressList) {
			int tid = p.t;
			int point = simRoutePoint[tid];
			int route = simARoute[tid];
			int road = -1;
			int dir = -1;
			if (point >= 0 && point < routeLengths[route]) {
				road = routeRoads[route][point];
				dir = routeDirections[route][point];
			}
			int sid = simAShovel[tid];
			int cid = simACrusher[tid];
			switch (simLocs[tid]) {
				case WAITING: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.WAITING,TruckLocation.WAITING,getPriority(tid,
						TruckLocation.WAITING)));
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					TruckLocation nextLoc;
					if (point == routeLengths[route] - 1) {
						nextLoc = TruckLocation.APPROACHING_SHOVEL;
					}
					else if (isOneWay[routeRoads[route][point + 1]]) {
						nextLoc = TruckLocation.APPROACHING_TL_CS;
					}
					else {
						nextLoc = TruckLocation.TRAVEL_TO_SHOVEL;
					}
					eventQueueStored.add(new Transition(tid,0,TruckLocation.TRAVEL_TO_SHOVEL,nextLoc,roadPriorityStored[road][dir]));
					roadPriorityStored[road][dir]++;
					roadQueuesStored[road][dir].add(tid);
					break;
				}
				case APPROACHING_TL_CS: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.APPROACHING_TL_CS,TruckLocation.APPROACHING_TL_CS,
						getPriority(tid,TruckLocation.APPROACHING_TL_CS)));
					break;
				}
				case STOPPED_AT_TL_CS: {
					lightQueuesStored[road][dir].add(tid);
					break;
				}
				case APPROACHING_SHOVEL: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.APPROACHING_SHOVEL,TruckLocation.APPROACHING_SHOVEL,
						getPriority(tid,TruckLocation.APPROACHING_SHOVEL)));
					break;
				}
				case WAITING_AT_SHOVEL: {
					shovelQueuesStored[sid].add(tid);
					break;
				}
				case FILLING: {
					shovelQueuesStored[sid].addFront(tid);
					eventQueueStored.add(new Transition(tid,0,TruckLocation.FILLING,TruckLocation.LEAVING_SHOVEL,getPriority(tid,
						TruckLocation.LEAVING_SHOVEL)));
					break;
				}
				case LEAVING_SHOVEL: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.LEAVING_SHOVEL,TruckLocation.LEAVING_SHOVEL,
						getPriority(tid,TruckLocation.LEAVING_SHOVEL)));
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					TruckLocation nextLoc;
					if (point == 0) {
						nextLoc = TruckLocation.APPROACHING_CRUSHER;
					}
					else if (isOneWay[routeRoads[route][point - 1]]) {
						nextLoc = TruckLocation.APPROACHING_TL_SS;
					}
					else {
						nextLoc = TruckLocation.TRAVEL_TO_CRUSHER;
					}
					eventQueueStored.add(new Transition(tid,0,TruckLocation.TRAVEL_TO_CRUSHER,nextLoc,roadPriorityStored[road][1 - dir]));
					roadPriorityStored[road][1 - dir]++;
					roadQueuesStored[road][1 - dir].add(tid);
					break;
				}
				case APPROACHING_TL_SS: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.APPROACHING_TL_SS,TruckLocation.APPROACHING_TL_SS,
						getPriority(tid,TruckLocation.APPROACHING_TL_SS)));
					break;
				}
				case STOPPED_AT_TL_SS: {
					lightQueuesStored[road][1 - dir].add(tid);
					break;
				}
				case APPROACHING_CRUSHER: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.APPROACHING_CRUSHER,TruckLocation.APPROACHING_CRUSHER,
						getPriority(tid,TruckLocation.APPROACHING_CRUSHER)));
					break;
				}
				case WAITING_AT_CRUSHER: {
					crusherQueuesStored[cid].add(tid);
					break;
				}
				case EMPTYING: {
					crusherQueuesStored[cid].addFront(tid);
					eventQueueStored.add(new Transition(tid,0,TruckLocation.EMPTYING,TruckLocation.WAITING,getPriority(tid,
						TruckLocation.WAITING)));
					break;
				}
			}
		}
		for (int i=0; i<numShovels; i++) {
			if (!shovelQueuesStored[i].isEmpty()) {
				int head = shovelQueuesStored[i].peek();
				if (simLocs[head] == TruckLocation.WAITING_AT_SHOVEL) {
					instantQueueStored.add(new Transition(head,simTime,TruckLocation.WAITING_AT_SHOVEL,TruckLocation.FILLING,getPriority(
						head,TruckLocation.FILLING)));
				}
			}
		}
		for (int i=0; i<numCrushers; i++) {
			if (!crusherQueuesStored[i].isEmpty()) {
				int head = crusherQueuesStored[i].peek();
				if (simLocs[head] == TruckLocation.WAITING_AT_CRUSHER) {
					instantQueueStored.add(new Transition(head,simTime,TruckLocation.WAITING_AT_CRUSHER,TruckLocation.EMPTYING,
						getPriority(head,TruckLocation.EMPTYING)));
				}
			}
		}
		Collections.sort(eventQueueStored);
		isReady = true;
	}

	/**
	 * Initialises the simulator.
	 * Should be run before every simulation, and only if isReady is true.
	 */
	protected void reReady() {
		numEmpties = 0;
		currTime = simTime;
		instantQueue.clear();
		instantQueue.addAll(instantQueueStored);
		for (int i=0; i<numTrucks; i++) {
			truckLocs[i] = simLocs[i];
			assignedShovel[i] = simAShovel[i];
			assignedCrusher[i] = simACrusher[i];
			assignedRoute[i] = simARoute[i];
			routePoint[i] = simRoutePoint[i];
			roadWaitingTime[i] = 0;
			serviceWaitingTime[i] = 0;
			lastServiceStart[i] = simTime;
			lastWaitStart[i] = simTime;
			serviceAvailableTime[i] = simTime;
			serviced[i] = 0;
		}
		for (int i=0; i<numCrushers; i++) {
			crusherQueues[i].clear();
			crusherQueues[i].addAll(crusherQueuesStored[i]);
			lastEmptyEnd[i] = simTime;
		}
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i].clear();
			shovelQueues[i].addAll(shovelQueuesStored[i]);
			lastFillEnd[i] = simTime;
			shovelWaitingTime[i] = 0;
		}
		double[][] roadProgress = new double[numRoads][2];
		for (int i=0; i<numRoads; i++) {
			if (isOneWay[i]) {
				for (int j=0; j<2; j++) {
					lightQueues[i][j].clear();
					lightQueues[i][j].addAll(lightQueuesStored[i][j]);
				}
				lights[i] = simLights[i];
			}
			for (int j=0; j<2; j++) {
				roadAvailable[i][j] = currTime;
				roadPriority[i][j] = roadPriorityStored[i][j];
				roadQueues[i][j].clear();
				roadQueues[i][j].addAll(roadQueuesStored[i][j]);
				roadProgress[i][j] = 1;
			}
		}
		eventQueue.clear();
		for (Transition t : eventQueueStored) {
			int tid = t.getIndex();
			TruckLocation source = t.getSource();
			TruckLocation target = t.getTarget();
			int priority = t.getPriority();
			int route = assignedRoute[tid];
			int point = routePoint[tid];
			int road = -1;
			int dir = -1;
			if (point >= 0 && point < routeLengths[route]) {
				road = routeRoads[route][point];
				dir = routeDirections[route][point];
			}
			int sid = assignedShovel[tid];
			int cid = assignedCrusher[tid];
			double progress = simProgress[tid];
			switch (source) {
				case TRAVEL_TO_SHOVEL: {
					if (roadProgress[road][dir] - progress > EPSILON) {
						double travelTime = tgen.nextTime(roadTravelTimesMean[road],roadTravelTimesSD[road]) * (1 - progress);
						roadAvailable[road][dir] = Math.max(roadAvailable[road][dir],currTime + travelTime);
					}
					roadProgress[road][dir] = progress;
					eventQueue.add(new Transition(tid,roadAvailable[road][dir],source,target,priority));
					routePoint[tid]++;
					break;
				}
				case FILLING: {
					double finish = currTime + tgen.nextTime(fillTimesMean[sid],fillTimesSD[sid]) * (1 - progress);
					eventQueue.add(new Transition(tid,finish,source,target,priority));
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					if (roadProgress[road][1 - dir] - progress > EPSILON) {
						double travelTime = tgen.nextTime(roadTravelTimesMean[road],roadTravelTimesSD[road]) * (1 - progress) * fullSlowdown;
						roadAvailable[road][1 - dir] = Math.max(roadAvailable[road][1 - dir],currTime + travelTime);
					}
					roadProgress[road][1 - dir] = progress;
					eventQueue.add(new Transition(tid,roadAvailable[road][1 - dir],source,target,priority));
					routePoint[tid]--;
					break;
				}
				case EMPTYING: {
					double emptyTime = tgen.nextTime(emptyTimesMean[cid],emptyTimesSD[cid]) * (1 - progress);
					double finish = currTime + emptyTime;
					eventQueue.add(new Transition(tid,finish,source,target,priority));
					break;
				}
				default: {
					throw new IllegalStateException(String.format("Stored non-instant event is invalid: %s",t));
				}
			}
		}
	}

	/**
	 * Runs a forward simulation.
	 * Will terminate if no trucks are in use, or a negative value other than -2 is assigned as the route.
	 * 
	 * @param	runtime	the termination time of the simulation.
	 */
	public void simulate(double runtime) {
		while (hasNextEvent() && peekNextEvent().getTime() <= runtime) {
			if (!singleEvent()) {
				break;
			}
		}
	}

	/**
	 * Move the simulation forward by one transition.
	 * 
	 * @return	false if a termination request is received, true otherwise.
	 * @throws	IllegalStateException	if a simulation error occurred,
	 *									e.g. illegal route index, 
	 *									approaching non-existent traffic lights,
	 *									illegal traffic light state,
	 *									illegal transition.
	 */
	private boolean singleEvent() {
		Transition next = getNextEvent();
		currTime = next.getTime();
		int tid = next.getIndex();
		TruckLocation tOrigin = next.getSource();
		TruckLocation tDest = next.getTarget();
		if (tOrigin == truckLocs[tid]) {
			switch (tDest) {
				case WAITING: {
					int cid = assignedCrusher[tid];
					if (tOrigin == TruckLocation.EMPTYING) {
						crusherQueues[cid].poll();
						if (!crusherQueues[cid].isEmpty()) {
							int head = crusherQueues[cid].peek();
							instantQueue.add(new Transition(head,currTime,TruckLocation.WAITING_AT_CRUSHER,TruckLocation.EMPTYING,
								getPriority(head,TruckLocation.EMPTYING)));
						}
						numEmpties++;
						serviced[tid]++;
						lastEmptyEnd[cid] = currTime;
					}
					int route = nextRoute(tid);
					if (route < 0) {
						if (route == -2) {
							instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.UNUSED,getPriority(tid,
								TruckLocation.UNUSED)));
						}
						else {
							return false;
						}
					}
					else if (routeCrushers[route] != cid) {
						throw new IllegalStateException(String.format("Illegal route supplied: %d at crusher %d",route,cid));
					}
					else {
						assignedRoute[tid] = route;
						assignedShovel[tid] = routeShovels[route];
						TruckLocation nextLoc = (isOneWay[routeRoads[route][0]] ? TruckLocation.APPROACHING_TL_CS : 
							TruckLocation.TRAVEL_TO_SHOVEL);
						routePoint[tid] = 0;
						instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
					}
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
					if (!isOneWay[road]) {
						clearedRoad(tid,true);
					}
					double travelTime = tgen.nextTime(roadTravelTimesMean[road],roadTravelTimesSD[road]);
					eventQueue.add(preventCollisions(travelTime,tid,true));
					routePoint[tid]++;
					break;
				}
				case APPROACHING_TL_CS: {
					clearedRoad(tid,true);
					int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
					if (!isOneWay[road]) {
						throw new IllegalStateException("Arrived at lights for two-way road");
					}
					int dir = routeDirections[assignedRoute[tid]][routePoint[tid]];
					switch (lights[road]) {
						case RR:
						case YR:
						case RY: {
							instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.STOPPED_AT_TL_CS,getPriority(tid,
								TruckLocation.STOPPED_AT_TL_CS)));
							lightQueues[road][dir].add(tid);
							break;
						}
						case RG: {
							if (dir == 0) {
								instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.STOPPED_AT_TL_CS,getPriority(tid,
									TruckLocation.STOPPED_AT_TL_CS)));
								lightQueues[road][dir].add(tid);
								lights[road] = TrafficLight.RY;
							}
							else {
								instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.TRAVEL_TO_SHOVEL,getPriority(tid,
									TruckLocation.TRAVEL_TO_SHOVEL)));
							}
							break;
						}
						case GR: {
							if (dir == 0) {
								instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.TRAVEL_TO_SHOVEL,getPriority(tid,
									TruckLocation.TRAVEL_TO_SHOVEL)));
							}
							else {
								instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.STOPPED_AT_TL_CS,getPriority(tid,
									TruckLocation.STOPPED_AT_TL_CS)));
								lightQueues[road][dir].add(tid);
								lights[road] = TrafficLight.YR;
							}
							break;
						}
						default: {
							throw new IllegalStateException(String.format("Illegal light configuration: %s",lights[road]));
						}
					}
					break;
				}
				case STOPPED_AT_TL_CS: {
					lastWaitStart[tid] = currTime;
					checkLights(routeRoads[assignedRoute[tid]][routePoint[tid]]);
					break;
				}
				case APPROACHING_SHOVEL: {
					clearedRoad(tid,true);
					int sid = assignedShovel[tid];
					TruckLocation nextLoc = (shovelQueues[sid].isEmpty() ? TruckLocation.FILLING : TruckLocation.WAITING_AT_SHOVEL);
					shovelQueues[sid].add(tid);
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
					lastWaitStart[tid] = currTime;
					break;
				}
				case WAITING_AT_SHOVEL: {
					break;
				}
				case FILLING: {
					int sid = assignedShovel[tid];
					double fillTime = tgen.nextTime(fillTimesMean[sid],fillTimesSD[sid]);
					eventQueue.add(new Transition(tid,currTime + fillTime,tDest,TruckLocation.LEAVING_SHOVEL,getPriority(tid,
						TruckLocation.LEAVING_SHOVEL)));
					lastServiceStart[tid] = currTime;
					serviceWaitingTime[tid] += currTime - lastWaitStart[tid];
					serviceAvailableTime[tid] = lastFillEnd[sid];
					shovelWaitingTime[sid] += currTime - lastFillEnd[sid];
					break;
				}
				case LEAVING_SHOVEL: {
					int sid = assignedShovel[tid];
					if (tOrigin == TruckLocation.FILLING) {
						shovelQueues[sid].poll();
						if (!shovelQueues[sid].isEmpty()) {
							int head = shovelQueues[sid].peek();
							instantQueue.add(new Transition(head,currTime,TruckLocation.WAITING_AT_SHOVEL,TruckLocation.FILLING,
								getPriority(head,TruckLocation.FILLING)));
						}
						serviced[tid]++;
						lastFillEnd[sid] = currTime;
					}
					int route = nextRoute(tid);
					if (route < 0) {
						if (route == -2) {
							instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.UNUSED,getPriority(tid,
								TruckLocation.UNUSED)));
						}
						else {
							return false;
						}
					}
					else if (routeShovels[route] != sid) {
						throw new IllegalStateException(String.format("Illegal route supplied: %d at shovel %d",route,sid));
					}
					else {
						assignedRoute[tid] = route;
						assignedCrusher[tid] = routeCrushers[route];
						int point = routeLengths[route] - 1;
						TruckLocation nextLoc = (isOneWay[routeRoads[route][point]] ? TruckLocation.APPROACHING_TL_SS : 
							TruckLocation.TRAVEL_TO_CRUSHER);
						routePoint[tid] = point;
						instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
					}
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
					if (!isOneWay[road]) {
						clearedRoad(tid,false);
					}
					double travelTime = tgen.nextTime(roadTravelTimesMean[road],roadTravelTimesSD[road]) * fullSlowdown;
					eventQueue.add(preventCollisions(travelTime,tid,false));
					routePoint[tid]--;
					break;
				}
				case APPROACHING_TL_SS: {
					clearedRoad(tid,false);
					int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
					if (!isOneWay[road]) {
						throw new IllegalStateException("Arrived at lights for two-way road");
					}
					int dir = 1 - routeDirections[assignedRoute[tid]][routePoint[tid]];
					switch (lights[road]) {
						case RR:
						case YR:
						case RY: {
							instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.STOPPED_AT_TL_SS,getPriority(tid,
								TruckLocation.STOPPED_AT_TL_SS)));
							lightQueues[road][dir].add(tid);
							break;
						}
						case RG: {
							if (dir == 0) {
								instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.STOPPED_AT_TL_SS,getPriority(tid,
									TruckLocation.STOPPED_AT_TL_SS)));
								lightQueues[road][dir].add(tid);
								lights[road] = TrafficLight.RY;
							}
							else {
								instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.TRAVEL_TO_CRUSHER,getPriority(tid,
									TruckLocation.TRAVEL_TO_CRUSHER)));
							}
							break;
						}
						case GR: {
							if (dir == 0) {
								instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.TRAVEL_TO_CRUSHER,getPriority(tid,
									TruckLocation.TRAVEL_TO_CRUSHER)));
							}
							else {
								instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.STOPPED_AT_TL_SS,getPriority(tid,
									TruckLocation.STOPPED_AT_TL_SS)));
								lightQueues[road][dir].add(tid);
								lights[road] = TrafficLight.YR;
							}
							break;
						}
						default: {
							throw new IllegalStateException(String.format("Illegal light configuration: %s",lights[road]));
						}
					}
					break;
				}
				case STOPPED_AT_TL_SS: {
					lastWaitStart[tid] = currTime;
					checkLights(routeRoads[assignedRoute[tid]][routePoint[tid]]);
					break;
				}
				case APPROACHING_CRUSHER: {
					clearedRoad(tid,false);
					int cid = assignedCrusher[tid];
					TruckLocation nextLoc = (crusherQueues[cid].isEmpty() ? TruckLocation.EMPTYING : TruckLocation.WAITING_AT_CRUSHER);
					crusherQueues[cid].add(tid);
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
					lastWaitStart[tid] = currTime;
					break;
				}
				case WAITING_AT_CRUSHER: {
					break;
				}
				case EMPTYING: {
					int cid = assignedCrusher[tid];
					double emptyTime = tgen.nextTime(emptyTimesMean[cid],emptyTimesSD[cid]);
					eventQueue.add(new Transition(tid,currTime + emptyTime,tDest,TruckLocation.WAITING,getPriority(tid,
						TruckLocation.WAITING)));
					lastServiceStart[tid] = currTime;
					serviceWaitingTime[tid] += currTime - lastWaitStart[tid];
					serviceAvailableTime[tid] = lastEmptyEnd[cid];
					break;
				}
				case UNUSED: {
					break;
				}
				default: {
					throw new IllegalStateException(String.format("Truck has entered illegal state: %s",tDest));
				}
			}
			truckLocs[tid] = tDest;
		}
		else {
			throw new IllegalStateException(String.format("Transition occurred from %s to %s when truck is in %s",tOrigin,tDest,
				truckLocs[tid]));
		}
		return true;
	}

	/**
	 * Used after a truck has completed a travel transition.
	 *
	 * @param	tid			the index of the truck that cleared a road.
	 * @param	toShovel	whether the truck was heading towards a shovel or not.
	 * @throws	IllegalStateException if this truck cleared a road before a truck ahead of it.
	 */
	private void clearedRoad(int tid, boolean toShovel) {
		int route = assignedRoute[tid];
		int startPoint = (toShovel ? 0 : routeLengths[route] - 1);
		int point = routePoint[tid];
		int off = (toShovel ? -1 : 1);
		if (point != startPoint) {
			int dir = routeDirections[route][point + off];
			int to = (toShovel ? dir : 1 - dir);
			int prevRoad = routeRoads[route][point + off];
			int front = roadQueues[prevRoad][to].poll();
			if (front != tid) {
				throw new IllegalStateException("Trucks out of order");
			}
			if (isOneWay[prevRoad]) {
				checkLights(prevRoad);
			}
		}
	}

	/**
	 * Get the priority for a transition.
	 * Should only affect event order when a non-random time distribution is used.
	 * Should not be used for travel transitions --
	 * those are controlled by using the roadPriority variable.
	 * 
	 * @param	tid		the index of the truck to transition.
	 * @param	dest	the destination TruckLocation of the transition.
	 * @return	the priority of the transition.
	 * @throws	IllegalArgumentException if dest is illegal.
	 */
	private int getPriority(int tid, TruckLocation dest) {
		switch (dest) {
			case WAITING: {
				return numTrucks * 4 + tid;
			}
			case APPROACHING_TL_CS: {
				return numTrucks * 3 + tid;
			}
			case STOPPED_AT_TL_CS: {
				return tid;
			}
			case TRAVEL_TO_SHOVEL: {
				return numTrucks + tid;
			}
			case APPROACHING_SHOVEL: {
				return numTrucks * 5 + tid;
			}
			case WAITING_AT_SHOVEL: {
				return numTrucks * 2 + tid;
			}
			case FILLING: {
				return numTrucks * 2 + tid;
			}
			case LEAVING_SHOVEL: {
				return numTrucks * 4 + tid;
			}
			case TRAVEL_TO_CRUSHER: {
				return numTrucks + tid;
			}
			case APPROACHING_TL_SS: {
				return numTrucks * 3 + tid;
			}
			case STOPPED_AT_TL_SS: {
				return tid;
			}
			case APPROACHING_CRUSHER: {
				return numTrucks * 5 + tid;
			}
			case WAITING_AT_CRUSHER: {
				return numTrucks * 2 + tid;
			}
			case EMPTYING: {
				return numTrucks * 2 + tid;
			}
			case UNUSED: {
				return -1;
			}
			default: {
				throw new IllegalArgumentException(String.format("Truck cannot transition to: %s",dest));
			}
		}
	}

	/**
	 * Check whether traffic lights on a road are yellow and should change state.
	 * Used after a truck clears a one-lane road or queues at a one-lane road.
	 * 
	 * @param	road	the road to check.
	 * @throws	IllegalStateException if information about traffic light queues is inconsistent.
	 */
	private void checkLights(int road) {
		if (roadQueues[road][0].isEmpty() && roadQueues[road][1].isEmpty()) {
			int side;
			switch (lights[road]) {
				case YR: {
					lights[road] = TrafficLight.RG;
					side = 1;
					if (!lightQueues[road][1 - side].isEmpty()) {
						lights[road] = TrafficLight.RY;
					}
					break;
				}
				case RY: {
					lights[road] = TrafficLight.GR;
					side = 0;
					if (!lightQueues[road][1 - side].isEmpty()) {
						lights[road] = TrafficLight.YR;
					}
					break;
				}
				default: {
					return;
				}
			}
			while (!lightQueues[road][side].isEmpty()) {
				int front = lightQueues[road][side].poll();
				roadPriority[road][side]++;
				TruckLocation origin;
				TruckLocation target;
				switch (truckLocs[front]) {
					case APPROACHING_TL_SS:
					case STOPPED_AT_TL_SS: {
						origin = TruckLocation.STOPPED_AT_TL_SS;
						target = TruckLocation.TRAVEL_TO_CRUSHER;
						break;
					}
					case APPROACHING_TL_CS:
					case STOPPED_AT_TL_CS: {
						origin = TruckLocation.STOPPED_AT_TL_CS;
						target = TruckLocation.TRAVEL_TO_SHOVEL;
						break;
					}
					default: {
						throw new IllegalStateException("Truck is at light queue but not stopped");
					}
				}
				instantQueue.add(new Transition(front,currTime,origin,target,roadPriority[road][side]));
				if (origin == truckLocs[front]) {
					roadWaitingTime[front] += currTime - lastWaitStart[front];
				}
			}
		}
	}

	/**
	 * Removes and returns the next upcoming transition.
	 * 
	 * @return	the next Transition.
	 */
	private Transition getNextEvent() {
		if (!instantQueue.isEmpty()) {
			return instantQueue.poll();
		}
		else {
			return eventQueue.poll();
		}
	}

	/**
	 * Returns but does not remove the next upcoming transition.
	 * 
	 * @return	the next Transition.
	 */
	private Transition peekNextEvent() {
		if (!instantQueue.isEmpty()) {
			return instantQueue.peek();
		}
		else {
			return eventQueue.peek();
		}
	}

	/**
	 * Determines whether the simulation can continue.
	 * 
	 * @return	true if a transition is available, false if no trucks are in use.
	 * @throws	IllegalStateException	if some trucks are in use but no transitions are available--
	 *									this indicates a bug or misuse that has caused a truck to get stuck.
	 */
	private boolean hasNextEvent() {
		if (!instantQueue.isEmpty() || !eventQueue.isEmpty()) {
			return true;
		}
		else {
			for (int i=0; i<numTrucks; i++) {
				if (truckLocs[i] != TruckLocation.UNUSED) {
					throw new IllegalStateException("No events scheduled despite active trucks");
				}
			}
			return false;
		}
	}

	/**
	 * Creates a transition with adjusted travel time and priority to prevent overtaking.
	 * 
	 * @param	travelTime	the travel time if no slower trucks are ahead.
	 * @param	tid			the index of the transitioning truck.
	 * @param	toShovel	whether the truck is heading to a shovel or not.
	 * @return	a Transition.
	 */
	private Transition preventCollisions(double travelTime, int tid, boolean toShovel) {
		int point = routePoint[tid];
		int route = assignedRoute[tid];
		int road = routeRoads[route][point];
		int dir = routeDirections[route][point];
		int to = (toShovel ? dir : 1 - dir);
		double actualArrival = Math.max(currTime + travelTime,roadAvailable[road][to]);
		roadAvailable[road][to] = actualArrival;
		TruckLocation travelLoc;
		TruckLocation targetLoc;
		if (toShovel) {
			travelLoc = TruckLocation.TRAVEL_TO_SHOVEL;
			if (point == routeLengths[route] - 1) {
				targetLoc = TruckLocation.APPROACHING_SHOVEL;
			}
			else if (isOneWay[routeRoads[route][point + 1]]) {
				targetLoc = TruckLocation.APPROACHING_TL_CS;
			}
			else {
				targetLoc = TruckLocation.TRAVEL_TO_SHOVEL;
			}
		}
		else {
			travelLoc = TruckLocation.TRAVEL_TO_CRUSHER;
			if (point == 0) {
				targetLoc = TruckLocation.APPROACHING_CRUSHER;
			}
			else if (isOneWay[routeRoads[route][point - 1]]) {
				targetLoc = TruckLocation.APPROACHING_TL_SS;
			}
			else {
				targetLoc = TruckLocation.TRAVEL_TO_CRUSHER;
			}
		}
		roadPriority[road][to]++;
		roadQueues[road][to].add(tid);
		return new Transition(tid,actualArrival,travelLoc,targetLoc,roadPriority[road][to]);
	}

	/**
	 * Get the total waiting time for a truck.
	 * 
	 * @param	tid	the index of the truck.
	 * @return	the total waiting time if the truck has been serviced,
	 *			a large value otherwise.
	 */
	public double getTotalWaitingTime(int tid) {
		if (serviced[tid] > 0) {
			return roadWaitingTime[tid] + serviceWaitingTime[tid];
		}
		else {
			return INFINITY;
		}
	}

	/**
	 * Get the time a truck started its most recent service.
	 * 
	 * @param	tid	the index of the truck.
	 * @return	the last service start if the truck has been serviced,
	 *			a large value otherwise.
	 */
	public double getLastServiceStart(int tid) {
		if (serviced[tid] > 0) {
			return lastServiceStart[tid];
		}
		else {
			return INFINITY;
		}
	}

	/**
	 * For the most recent service for a truck, get the time the servicing machine became available before that service.
	 * 
	 * @param	tid	the index of the truck.
	 * @return	the available time if the truck has been serviced,
	 *			a large value otherwise.
	 */
	public double getServiceAvailableTime(int tid) {
		if (serviced[tid] > 0) {
			return serviceAvailableTime[tid];
		}
		else {
			return INFINITY;
		}
	}

	/**
	 * Get the total waiting time for a shovel.
	 * 
	 * @param	tid	the index of the shovel.
	 * @return	the total waiting time.
	 */
	public double getTotalShovelWaitingTime(int sid) {
		return shovelWaitingTime[sid];
	}

	/**
	 * Get the number of empties.
	 * 
	 * @return	the number of empties.
	 */
	public int getNumEmpties() {
		return numEmpties;
	}

}