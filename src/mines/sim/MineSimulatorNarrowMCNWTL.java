package mines.sim;

import mines.util.times.TimeDistribution;
import mines.util.*;
import mines.system.Debugger;
import mines.sol.RouteController;
import java.util.*;

/**
 * Simulator class for a complex road network that simulates entire shifts.
 * Relies on a controller for routing decisions.
 */
public class MineSimulatorNarrowMCNWTL {

	/*
	 * Simulation parameters.
	 */
	private int numTrucks;					//number of trucks.
	private int numShovels;					//number of shovels.
	private int numCrushers;				//number of crushers.
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
	private RouteController con;	//the controller for routing decisions.

	/*
	 * Current simulation variables.
	 */
	private int numEmpties;									//number of empties completed in simulation.
	private double currTime;								//current time in simulation.
	private ShortPriorityQueue<Transition> eventQueue;		//upcoming non-instant transitions in simulation.
	private ShortPriorityQueue<Transition> instantQueue;	//upcoming instant transitions in simulation.
	private TruckLocation[] truckLocs;						//current locations of each truck in simulation.
	private int[] assignedShovel;							//current assigned shovel for each truck in simulation.
	private int[] assignedCrusher;							//current assigned crusher for each truck in simulation.
	private int[] assignedRoute;							//current assigned route for each truck in simulation.
	private IntQueue[] crusherQueues;						//queues for each crusher in simulation.
	private IntQueue[] shovelQueues;						//queues for each shovel in simulation.
	private double[] arrivalTime;							//most recent transition time for each truck in simulation.
	private IntQueue[][] lightQueues;						//queues for each traffic light in simulation.
	private TrafficLight[] lights;							//state of each traffic light in simulation.
	private double[][] roadAvailable;						//the minimum possible arrival time for each road end in simulation.
	private int[][] roadPriority;							//priority values used for transitions to preserve order.
	private double[] intendedArrival;						//expected transition times before considering slowdowns from lack of overtaking.
	private IntQueue[][] roadQueues;						//order of trucks on each road in simulation.
	private int[] routePoint;								//current route index for each truck in simulation.

	private ArrayList<double[]> activity;	//list of empty completion and duration times.

	private boolean initialised;	//whether the simulator has been initialised.

	/**
	 * Simulator constructor.
	 *
	 * @param	params	the simulation parameters structure.
	 * @param	tgen	the distribution used for generating all stochastic values.
	 */
	public MineSimulatorNarrowMCNWTL(MineParametersN params, TimeDistribution tgen) {
		numTrucks = params.getNumTrucks();
		numShovels = params.getNumShovels();
		numCrushers = params.getNumCrushers();
		numRoads = params.getNumRoads();
		emptyTimesMean = params.getMeanEmptyTimes();
		emptyTimesSD = params.getEmptyTimesSD();
		fillTimesMean = params.getMeanFillTimes();
		fillTimesSD = params.getFillTimesSD();
		roadTravelTimesMean = params.getMeanTravelTimes();
		roadTravelTimesSD = params.getTravelTimesSD();
		fullSlowdown = params.getFullSlowdown();
		isOneWay = params.getIsOneWay();

		numRoutes = params.getNumRoutes();
		routeRoads = params.getRouteRoads();
		routeLengths = params.getRouteLengths();
		routeShovels = params.getRouteShovels();
		routeCrushers = params.getRouteCrushers();
		routeDirections = params.getRouteDirections();

		this.tgen = tgen;

		eventQueue = new ShortPriorityQueue<>();
		instantQueue = new ShortPriorityQueue<>();
		truckLocs = new TruckLocation[numTrucks];
		assignedShovel = new int[numTrucks];
		assignedCrusher = new int[numTrucks];
		assignedRoute = new int[numTrucks];
		crusherQueues = new IntQueue[numCrushers];
		for (int i=0; i<numCrushers; i++) {
			crusherQueues[i] = new IntQueue();
		}
		shovelQueues = new IntQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i] = new IntQueue();
		}
		arrivalTime = new double[numTrucks];
		lightQueues = new IntQueue[numRoads][2];
		for (int i=0; i<numRoads; i++) {
			if (isOneWay[i]) {
				for (int j=0; j<2; j++) {
					lightQueues[i][j] = new IntQueue();
				}
			}
		}
		lights = new TrafficLight[numRoads];
		roadAvailable = new double[numRoads][2];
		roadPriority = new int[numRoads][2];
		intendedArrival = new double[numTrucks];
		roadQueues = new IntQueue[numRoads][2];
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				roadQueues[i][j] = new IntQueue();
			}
		}
		routePoint = new int[numTrucks];

		activity = new ArrayList<>();

		initialised = false;
		// initialise();
	}

	/**
	 * Resets the simulator and controller to the start of a shift.
	 */
	public void initialise() {
		numEmpties = 0;
		currTime = 0;
		eventQueue.clear();
		instantQueue.clear();
		for (int i=0; i<numCrushers; i++) {
			crusherQueues[i].clear();
		}
		int[] initialCrushers = con.getInitialCrushers();
		for (int i=numTrucks - 1; i>=0; i--) {
			instantQueue.add(new Transition(i,currTime,TruckLocation.WAITING,TruckLocation.WAITING,getPriority(i,TruckLocation.WAITING)));
			truckLocs[i] = TruckLocation.WAITING;
			assignedCrusher[i] = (initialCrushers == null ? i % numCrushers : initialCrushers[i]);
			for (int j=0; j<numRoutes; j++) {
				if (routeCrushers[j] == assignedCrusher[i]) {
					assignedRoute[i] = j;
					assignedShovel[i] = routeShovels[j];
					break;
				}
				if (j == numRoutes - 1) {
					throw new IllegalStateException("No routes out of crusher");
				}
			}			
			arrivalTime[i] = currTime;
			intendedArrival[i] = currTime;
			routePoint[i] = 0;
		}
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i].clear();
		}
		for (int i=0; i<numRoads; i++) {
			if (isOneWay[i]) {
				lights[i] = TrafficLight.GR;
			}
			for (int j=0; j<2; j++) {
				roadPriority[i][j] = Integer.MIN_VALUE;
				roadAvailable[i][j] = currTime;
				roadQueues[i][j].clear();
				if (isOneWay[i]) {
					lightQueues[i][j].clear();
				}
			}
		}
		activity.clear();
		// if (con != null) {
			con.reset();
		// }
		initialised = true;
	}

	/**
	 * Load a controller.
	 *
	 * @param	con	the controller.
	 */
	public void loadController(RouteController con) {
		this.con = con;
		con.reset();
	}

	/**
	 * Runs a simulation.
	 * 
	 * @param	runtime	the termination time of the simulation.
	 * @throws	IllegalStateException if the simulator hasn't been initialised.
	 */
	public void simulate(double runtime) {
		if (initialised) {
			while (peekNextEvent().getTime() <= runtime) {
				singleEvent();
			}
		}
		else {
			throw new IllegalStateException("Simulator not initialised");
		}
	}

	/**
	 * Move the simulation forward by one transition.
	 * 
	 * @throws	IllegalStateException	if a simulation error occurred,
	 *									e.g. illegal route index, 
	 *									approaching non-existent traffic lights,
	 *									illegal traffic light state,
	 *									illegal transition.
	 */
	private void singleEvent() {
		Transition next = getNextEvent();
		currTime = next.getTime();
		int tid = next.getIndex();
		con.event(getStateChange(next));
		TruckLocation tOrigin = next.getSource();
		TruckLocation tDest = next.getTarget();
		arrivalTime[tid] = currTime;
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
						Debugger.print(String.format("%d empties at %f\n",numEmpties,currTime));
					}
					int route = con.nextRoute(tid);
					if (routeCrushers[route] != cid) {
						throw new IllegalStateException(String.format("Illegal route supplied: %d at crusher %d",route,cid));
					}
					assignedRoute[tid] = route;
					assignedShovel[tid] = routeShovels[route];
					Debugger.print(String.format("Truck %d dispatched on route %d from crusher %d at %f\n",tid,route,cid,currTime));
					TruckLocation nextLoc = (isOneWay[routeRoads[route][0]] ? TruckLocation.APPROACHING_TL_CS : 
						TruckLocation.TRAVEL_TO_SHOVEL);
					routePoint[tid] = 0;
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
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
								con.lightEvent(road,lights[road]);
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
								con.lightEvent(road,lights[road]);
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
					checkLights(routeRoads[assignedRoute[tid]][routePoint[tid]]);
					break;
				}
				case APPROACHING_SHOVEL: {
					clearedRoad(tid,true);
					int sid = assignedShovel[tid];
					TruckLocation nextLoc = (shovelQueues[sid].isEmpty() ? TruckLocation.FILLING : TruckLocation.WAITING_AT_SHOVEL);
					shovelQueues[sid].add(tid);
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
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
					intendedArrival[tid] = currTime + fillTime;
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
					}
					int route = con.nextRoute(tid);
					if (routeShovels[route] != sid) {
						throw new IllegalStateException(String.format("Illegal route supplied: %d at shovel %d",route,sid));
					}
					assignedRoute[tid] = route;
					assignedCrusher[tid] = routeCrushers[route];
					Debugger.print(String.format("Truck %d dispatched on route %d from shovel %d at %f\n",tid,route,sid,currTime));
					int point = routeLengths[route] - 1;
					TruckLocation nextLoc = (isOneWay[routeRoads[route][point]] ? TruckLocation.APPROACHING_TL_SS : 
						TruckLocation.TRAVEL_TO_CRUSHER);
					routePoint[tid] = point;
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
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
								con.lightEvent(road,lights[road]);
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
								con.lightEvent(road,lights[road]);
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
					checkLights(routeRoads[assignedRoute[tid]][routePoint[tid]]);
					break;
				}
				case APPROACHING_CRUSHER: {
					clearedRoad(tid,false);
					int cid = assignedCrusher[tid];
					TruckLocation nextLoc = (crusherQueues[cid].isEmpty() ? TruckLocation.EMPTYING : TruckLocation.WAITING_AT_CRUSHER);
					crusherQueues[cid].add(tid);
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
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
					intendedArrival[tid] = currTime + emptyTime;
					activity.add(new double[]{currTime,emptyTime});
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
			}
			con.lightEvent(road,lights[road]);
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
		intendedArrival[tid] = currTime + travelTime;
		double actualArrival = Math.max(intendedArrival[tid],roadAvailable[road][to]);
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
	 * Get the number of completed empties at the crusher.
	 *
	 * @param	the number of empties.
	 */
	public int getEmpties() {
		return numEmpties;
	}

	/**
	 * Get the fraction of time the crusher was active for a shift.
	 *
	 * @param	runtime	the shift time.
	 * @return	the fraction of time the crusher was active.
	 */
	public double getActive(double runtime) {
		double active = 0;
		for (double[] empty : activity) {
			double start = Math.min(empty[0],runtime);
			double end = Math.min(empty[1] + empty[0],runtime);
			active += end - start;
		}
		return active / (runtime * numCrushers);
	}

	/**
	 * Get the change in simulation state as defined by the current transition,
	 * and the progress of each truck in its current task,
	 * i.e. the fractional completion of each truck's transition based on the current time.
	 * For stationary trucks, the progress is set to the waiting time.
	 *
	 * @return	a StateChange structure containing the current transition and the progress of each truck.
	 */
	private StateChange getStateChange(Transition next) {
		double[] progress = new double[numTrucks];
		int truck = next.getIndex();
		boolean[] marked = new boolean[numTrucks];
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				double minProgress = 1;
				for (int k=0; k<roadQueues[i][j].size(); k++) {
					int t = roadQueues[i][j].get(k);
					double intendedProgress = (currTime - arrivalTime[t]) / (intendedArrival[t] - arrivalTime[t]);
					minProgress = Math.min(minProgress,intendedProgress);
					progress[t] = minProgress;
					marked[t] = true;
				}
			}
		}
		for (int i=0; i<numTrucks; i++) {
			if (i == truck) {
				continue;
			}
			switch (truckLocs[i]) {
				case WAITING:
				case APPROACHING_TL_CS:
				case APPROACHING_SHOVEL:
				case LEAVING_SHOVEL:
				case APPROACHING_TL_SS:
				case APPROACHING_CRUSHER: {
					break;
				}
				case TRAVEL_TO_SHOVEL:
				case TRAVEL_TO_CRUSHER: {
					if (!marked[i]) {
						throw new IllegalStateException("Road queues are incorrect");
					}
					break;
				}
				case FILLING:
				case EMPTYING: {
					progress[i] = (currTime - arrivalTime[i]) / (intendedArrival[i] - arrivalTime[i]);
					break;
				}
				case STOPPED_AT_TL_CS:
				case WAITING_AT_SHOVEL:
				case STOPPED_AT_TL_SS:
				case WAITING_AT_CRUSHER: {
					progress[i] = currTime - arrivalTime[i];
					break;
				}
				default: {
					throw new IllegalStateException(String.format("Truck has entered illegal state: %s",truckLocs[i]));
				}
			}
		}
		return new StateChange(next,assignedRoute[truck],routePoint[truck],progress);
	}

}