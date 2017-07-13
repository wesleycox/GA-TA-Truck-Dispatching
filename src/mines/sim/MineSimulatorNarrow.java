package mines.sim;

import mines.util.times.TimeDistribution;
import mines.util.*;
import mines.sol.Controller;
import mines.system.Debugger;
import java.util.*;

/**
 * Simulator class for a simple road network that simulates entire shifts.
 * Relies on a controller for routing decisions.
 */

public class MineSimulatorNarrow {

	public static final double FULLSLOWDOWN = 1.2;	//full slowdown penalty.

	private static boolean ALLOWSTOPS = false;	//whether to allow trucks to transition out of use.

	/*
	 * Simulation parameters.
	 */
	private int numTrucks;				//number of trucks.
	private int numShovels;				//number of shovels.
	private double emptyTimeMean;		//average emptying time of the crusher.
	private double emptyTimeSD;			//standard deviation of the emptying time of the crusher.
	private double[] fillTimesMean;		//average filling times for each shovel.
	private double[] fillTimesSD;		//standard deviations of filling times for each shovel.
	private double[] travelTimesMean;	//average travelling times to each shovel.
	private double[] travelTimesSD;		//standard deviations of travelling time to each shovel.

	private TimeDistribution tgen;	//the distribution used for generating all stochastic values.
	private Controller con;			//controller for routing decision.

	/*
	 * Current simulation variables.
	 */
	private int numEmpties;									//number of empties.
	private double currTime;								//current simulation time.
	private PriorityQueue<Transition> eventQueue;			//upcoming non-instant transitions in simulation.
	private ShortPriorityQueue<Transition> instantQueue;	//upcoming instant transitions in simulation.
	private TruckLocation[] truckLocs;						//current locations of each truck in simulation.
	private int[] assignedShovel;							//current assigned shovel of each truck in simulation.
	private IntQueue crusherQueue;							//queue for crusher in simulation.
	private IntQueue[] shovelQueues;						//queues for each shovel in simulaton.
	private int[][] roadPriority;							//priority values used for transitions to preserve order.
	private double[] arrivalTime;							//most recent transition time for each truck in simulation.
	private double[] intendedArrival;						//expected transition times before considering slowdowns from lack of overtaking.
	private double[][] roadAvailable;						//the minimum possible arrival time for each road end in simulation.
	private IntQueue[][] roadQueues;						//order of trucks on each road in current simulation.

	private double active;			//total time spent emptying at the crusher.
	private double currentActivity;	//emptying time of most recently started empty.

	/**
	 * Simulator constructor.
	 *
	 * @param	params	the simulation parameters structure.
	 * @param	tgen	the distribution used for generating all stochastic values.
	 */
	public MineSimulatorNarrow(MineParameters params, TimeDistribution tgen) {
		numTrucks = params.getNumTrucks();
		numShovels = params.getNumShovels();
		int numCrushers = params.getNumCrushers();
		if (numCrushers != 1) {
			throw new IllegalArgumentException(String.format("Too many crushers: %d",numCrushers));
		}
		emptyTimeMean = params.getMeanEmptyTime(0);
		emptyTimeSD = params.getEmptyTimeSD(0);
		fillTimesMean = params.getMeanFillTimes();
		fillTimesSD = params.getFillTimesSD();
		travelTimesMean = params.getMeanTravelTimes(0);
		travelTimesSD = params.getTravelTimesSD(0);
		this.tgen = tgen;

		eventQueue = new PriorityQueue<>();
		instantQueue = new ShortPriorityQueue<>();
		truckLocs = new TruckLocation[numTrucks];
		assignedShovel = new int[numTrucks];
		crusherQueue = new IntQueue();
		shovelQueues = new IntQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i] = new IntQueue();
		}
		roadPriority = new int[numShovels][2];
		arrivalTime = new double[numTrucks];
		roadAvailable = new double[numShovels][2];
		intendedArrival = new double[numTrucks];
		roadQueues = new IntQueue[numShovels][2];
		for (int i=0; i<numShovels; i++) {
			for (int j=0; j<2; j++) {
				roadQueues[i][j] = new IntQueue();
			}
		}
		initialise();
	}

	/**
	 * Resets the simulator and controller to the start of a shift.
	 */
	public void initialise() {
		numEmpties = 0;
		currTime = 0;
		active = 0;
		currentActivity = 0;
		eventQueue.clear();
		instantQueue.clear();
		crusherQueue.clear();
		for (int i=numTrucks - 1; i>=0; i--) {
			instantQueue.add(new Transition(i,currTime,TruckLocation.WAITING,TruckLocation.WAITING,getPriority(i,TruckLocation.WAITING)));
			truckLocs[i] = TruckLocation.WAITING;
			assignedShovel[i] = -1;
			arrivalTime[i] = currTime;
		}
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i].clear();
			for (int j=0; j<2; j++) {
				roadPriority[i][j] = Integer.MIN_VALUE;
				roadAvailable[i][j] = 0;
				roadQueues[i][j].clear();
			}
		}
		if (con != null) {
			con.reset();
		}
	}

	/**
	 * Load a controller.
	 *
	 * @param	con	the controller.
	 */
	public void loadController(Controller con) {
		this.con = con;
		con.reset();
	}

	/**
	 * Runs a simulation.
	 * 
	 * @param	runtime	the termination time of the simulation.
	 */
	public void simulate(double runtime) {
		// con.reset();
		while (peekNextEvent().getTime() <= runtime) {
			singleEvent();
		}
	}

	/**
	 * Move the simulation forward by one transition.
	 * 
	 * @throws	IllegalStateException	if a simulation error occurred,
	 *									e.g. illegal transition.
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
					if (tOrigin == TruckLocation.EMPTYING) {
						crusherQueue.poll();
						if (!crusherQueue.isEmpty()) {
							int head = crusherQueue.peek();
							instantQueue.add(new Transition(head,currTime,TruckLocation.WAITING_AT_CRUSHER,TruckLocation.EMPTYING,
								getPriority(head,TruckLocation.EMPTYING)));
						}
						numEmpties++;
						active += currentActivity;
						Debugger.print(String.format("%d %f\n",numEmpties,currTime));
					}
					assignedShovel[tid] = con.nextShovel(tid);
					Debugger.print(String.format("%d A%d\n",tid,assignedShovel[tid]));
					if (assignedShovel[tid] < 0 && ALLOWSTOPS) {
						instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.UNUSED,getPriority(tid,TruckLocation.UNUSED)));
					}
					else {
						instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.TRAVEL_TO_SHOVEL,getPriority(tid,
							TruckLocation.TRAVEL_TO_SHOVEL)));
					}
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					double travelTime = tgen.nextTime(travelTimesMean[assignedShovel[tid]],travelTimesSD[assignedShovel[tid]]);
					eventQueue.add(preventCollisions(travelTime,tid,assignedShovel[tid],true));
					roadQueues[assignedShovel[tid]][0].add(tid);
					break;
				}
				case APPROACHING_SHOVEL: {
					TruckLocation nextLoc = (shovelQueues[assignedShovel[tid]].isEmpty() ? TruckLocation.FILLING : 
						TruckLocation.WAITING_AT_SHOVEL);
					shovelQueues[assignedShovel[tid]].add(tid);
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
					roadQueues[assignedShovel[tid]][0].poll();
					break;
				}
				case WAITING_AT_SHOVEL: {
					break;
				}
				case FILLING: {
					double fillTime = tgen.nextTime(fillTimesMean[assignedShovel[tid]],fillTimesSD[assignedShovel[tid]]);
					eventQueue.add(new Transition(tid,currTime + fillTime,tDest,TruckLocation.LEAVING_SHOVEL,getPriority(tid,
						TruckLocation.LEAVING_SHOVEL)));
					intendedArrival[tid] = fillTime + currTime;
					break;
				}
				case LEAVING_SHOVEL: {
					if (tOrigin == TruckLocation.FILLING) {
						shovelQueues[assignedShovel[tid]].poll();
						if (!shovelQueues[assignedShovel[tid]].isEmpty()) {
							int head = shovelQueues[assignedShovel[tid]].peek();
							instantQueue.add(new Transition(head,currTime,TruckLocation.WAITING_AT_SHOVEL,TruckLocation.FILLING,
								getPriority(head,TruckLocation.FILLING)));
						}
					}
					instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.TRAVEL_TO_CRUSHER,getPriority(tid,
						TruckLocation.TRAVEL_TO_CRUSHER)));
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					double travelTime = tgen.nextTime(travelTimesMean[assignedShovel[tid]],travelTimesSD[assignedShovel[tid]]) * FULLSLOWDOWN;
					eventQueue.add(preventCollisions(travelTime,tid,assignedShovel[tid],false));
					roadQueues[assignedShovel[tid]][1].add(tid);
					break;
				}
				case APPROACHING_CRUSHER: {
					TruckLocation nextLoc = (crusherQueue.isEmpty() ? TruckLocation.EMPTYING : TruckLocation.WAITING_AT_CRUSHER);
					crusherQueue.add(tid);
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
					roadQueues[assignedShovel[tid]][1].poll();
					break;
				}
				case WAITING_AT_CRUSHER: {
					break;
				}
				case EMPTYING: {
					double emptyTime = tgen.nextTime(emptyTimeMean,emptyTimeSD);
					eventQueue.add(new Transition(tid,currTime + emptyTime,tDest,TruckLocation.WAITING,getPriority(tid,
						TruckLocation.WAITING)));
					currentActivity = emptyTime;
					intendedArrival[tid] = currTime + emptyTime;
					break;
				}
				case UNUSED: {
					if (ALLOWSTOPS) {
						break;
					}
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
				return numTrucks * 2 + tid;
			}
			case TRAVEL_TO_SHOVEL: {
				return tid;
			}
			case APPROACHING_SHOVEL: {
				return numTrucks * 3 + tid;
			}
			case WAITING_AT_SHOVEL: {
				return numTrucks + tid;
			}
			case FILLING: {
				return numTrucks + tid;
			}
			case LEAVING_SHOVEL: {
				return numTrucks * 2 + tid;
			}
			case TRAVEL_TO_CRUSHER: {
				return tid;
			}
			case APPROACHING_CRUSHER: {
				return numTrucks * 3 + tid;
			}
			case WAITING_AT_CRUSHER: {
				return numTrucks + tid;
			}
			case EMPTYING: {
				return numTrucks + tid;
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
	 * @param	sid			the index of the assigned shovel.
	 * @param	toShovel	whether the truck is heading to a shovel or not.
	 * @return	a Transition.
	 */
	private Transition preventCollisions(double travelTime, int tid, int sid, boolean toShovel) {
		int to = (toShovel ? 0 : 1);
		intendedArrival[tid] = travelTime + currTime;
		double actualArrival = Math.max(intendedArrival[tid],roadAvailable[sid][to]);
		roadAvailable[sid][to] = actualArrival;
		TruckLocation travelLoc = (toShovel ? TruckLocation.TRAVEL_TO_SHOVEL : TruckLocation.TRAVEL_TO_CRUSHER);
		TruckLocation targetLoc = (toShovel ? TruckLocation.APPROACHING_SHOVEL : TruckLocation.APPROACHING_CRUSHER);
		roadPriority[sid][to]++;
		return new Transition(tid,actualArrival,travelLoc,targetLoc,roadPriority[sid][to]);
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
		return active / runtime;
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
		for (int i=0; i<numShovels; i++) {
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
				case APPROACHING_SHOVEL:
				case LEAVING_SHOVEL:
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
				case WAITING_AT_SHOVEL:
				case WAITING_AT_CRUSHER: {
					progress[i] = currTime - arrivalTime[i];
					break;
				}
				case UNUSED: {
					if (ALLOWSTOPS) {
						break;
					}
				}
				default: {
					throw new IllegalStateException(String.format("Truck has entered illegal state: %s",truckLocs[i]));
				}
			}
		}
		return new StateChange(next,assignedShovel[truck],0,progress);
	}

}