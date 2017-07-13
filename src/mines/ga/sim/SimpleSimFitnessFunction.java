package mines.ga.sim;

import mines.util.times.TimeDistribution;
import mines.util.*;
import mines.sim.*;
import java.util.*;

/**
 * Simulator class for a simple road network that simulates entire shifts.
 * Intended to be extended as a fitness function.
 */

public abstract class SimpleSimFitnessFunction {

	/*
	 * Simulation parameters.
	 */
	protected final int numTrucks;				//number of trucks.
	protected final int numShovels;				//number of shovels.
	protected final double emptyTimeMean;		//average emptying time of the crusher.
	private final double emptyTimeSD;			//standard deviation of the emptying time of the crusher.
	protected final double[] fillTimesMean;		//average filling times for each shovel.
	private final double[] fillTimesSD;			//standard deviations of filling times for each shovel.
	protected final double[] travelTimesMean;	//average travelling times to each shovel.
	private final double[] travelTimesSD;		//standard deviations of travelling time to each shovel.

	private TimeDistribution tgen;	//the distribution used for generating all stochastic values.

	/*
	 * Current simulation variables.
	 */
	protected double currTime;								//current simulation time.
	private PriorityQueue<Transition> eventQueue;			//upcoming non-instant transitions in simulation.
	private ShortPriorityQueue<Transition> instantQueue;	//upcoming instant transitions in simulation.
	protected TruckLocation[] truckLocs;					//current locations of each truck in simulation.
	protected int[] assignedShovel;							//current assigned shovel of each truck in simulation.
	private IntQueue crusherQueue;							//queue for crusher in simulation.
	private IntQueue[] shovelQueues;						//queues for each shovel in simulaton.
	private int[][] roadPriority;							//priority values used for transitions to preserve order.
	protected double[] arrivalTime;							//most recent transition time for each truck in simulation.
	private double[] intendedArrival;						//expected transition times before considering slowdowns from lack of overtaking.
	private double[][] roadAvailable;						//the minimum possible arrival time for each road end in simulation.
	private IntQueue[][] roadQueues;						//order of trucks on each road in current simulation.

	protected double[] lastUsed;	//time of last service completion of each shovel in simulation.
	private int numEmpties;			//number of empties completed in simulation.
	private double[] lastEmpty;		//last emptying completion and time taken to complete.

	/**
	 * Simulator constructor.
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
	 */
	public SimpleSimFitnessFunction(int numTrucks, int numShovels, double[] travelTimesMean, double[] travelTimesSD, 
		double[] fillTimesMean, double[] fillTimesSD, double emptyTimeMean, double emptyTimeSD, TimeDistribution tgen) {
		this.numTrucks = numTrucks;
		this.numShovels = numShovels;
		this.travelTimesMean = Arrays.copyOf(travelTimesMean,travelTimesMean.length);
		this.travelTimesSD = Arrays.copyOf(travelTimesSD,travelTimesSD.length);
		this.fillTimesMean = Arrays.copyOf(fillTimesMean,fillTimesMean.length);
		this.fillTimesSD = Arrays.copyOf(fillTimesSD,fillTimesSD.length);
		this.emptyTimeMean = emptyTimeMean;
		this.emptyTimeSD = emptyTimeSD;
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
		lastUsed = new double[numShovels];
		roadAvailable = new double[numShovels][2];
		intendedArrival = new double[numTrucks];
		roadQueues = new IntQueue[numShovels][2];
		for (int i=0; i<numShovels; i++) {
			for (int j=0; j<2; j++) {
				roadQueues[i][j] = new IntQueue();
			}
		}
	}

	/**
	 * Get the next shovel for the given truck.
	 * 
	 * @param	tid	the index of the truck requiring routing.
	 * @return	a shovel index.
	 */
	protected abstract int nextShovel(int tid);

	/**
	 * Resets the simulator.
	 * Should be run before every simulation.
	 */
	protected void reReady() {
		numEmpties = 0;
		currTime = 0;
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
			lastUsed[i] = currTime;
		}
	}

	/**
	 * Runs a forward simulation.
	 * 
	 * @param	runtime	the termination time of the simulation.
	 * @throws	IllegalStateException if an illegal transition occurs.
	 */
	public void simulate(double runtime) {
		while (hasNextEvent() && peekNextEvent().getTime() <= runtime) {
			Transition next = getNextEvent();
			currTime = next.getTime();
			int tid = next.getIndex();
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
						}
						assignedShovel[tid] = nextShovel(tid);
						instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.TRAVEL_TO_SHOVEL,getPriority(tid,
							TruckLocation.TRAVEL_TO_SHOVEL)));
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
							lastUsed[assignedShovel[tid]] = currTime;
						}
						instantQueue.add(new Transition(tid,currTime,tDest,TruckLocation.TRAVEL_TO_CRUSHER,getPriority(tid,
							TruckLocation.TRAVEL_TO_CRUSHER)));
						break;
					}
					case TRAVEL_TO_CRUSHER: {
						double travelTime = MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[assignedShovel[tid]],
							travelTimesSD[assignedShovel[tid]]);
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
						intendedArrival[tid] = currTime + emptyTime;
						lastEmpty = new double[]{currTime,emptyTime};
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
	 * Determines whether the simulation can continue.
	 * 
	 * @return	true if a transition is available, false otherwise.
	 */
	private boolean hasNextEvent() {
		return (!instantQueue.isEmpty() || !eventQueue.isEmpty());
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
	 * Calculates the fractional completion of each truck's transition based on the current time.
	 * For stationary trucks, the waiting time is returned.
	 *
	 * @return	an array of progress values for each truck.
	 */
	protected double[] getProgress() {
		double[] progress = new double[numTrucks];
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
				default: {
					throw new IllegalStateException(String.format("Truck has entered illegal state: %s",truckLocs[i]));
				}
			}
		}
		return progress;
	}

	/**
	 * Get the number of empties.
	 * 
	 * @return	the number of empties.
	 */
	public int getNumEmpties() {
		return numEmpties;
	}

	/**
	 * Get the time and duration of the most recent empty at the crusher.
	 * 
	 * @return	an array containing the time and then duration of the empty.
	 */
	protected double[] getLastEmpty() {
		return lastEmpty;
	}

}