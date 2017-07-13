package mines.sol.flow.dispatch;

import mines.sim.*;
import mines.util.ShortPriorityQueue;
import mines.sol.Controller;
import java.util.*;

/**
 * Controller class for simple road networks,
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
public class DISPATCHController implements Controller {

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

	/*
	 * Stored state variables.
	 */
	private TruckLocation[] simLocs;	//current location of all trucks in the simulation.
	private double[] simProgress;		//current progress of each truck on current task in the simulation.
	private double[] simLastUsed;		//last service completion at each shovel in the simulation.
	private double simTime;				//current time in the simulation.
	private double[] simLastDispatch;	//last dispatch to each shovel in the simulation.
	private double[] simAllocated;		//allocation variables used in the algorithm -- see references.
	private int[] sDest;				//current assigned shovel for each truck in the simulation.

	/*
	 * Algorithm variables.
	 */
	private double[] flow;				//designated truck flow to each shovel -- see references.
	private double totalDiggingRate;	//net digging rate of all shovels.
	private double requiredTrucks;		//required number of trucks to satisfy network flow -- see references.
	private double minTravelTime;		//minimum average travel time to a shovel.

	/*
	 * Simulation parameters.
	 */
	private int numTrucks;				//number of trucks.
	private int numShovels;				//number of shovels.
	private double[] travelTimesMean;	//mean travel times to each shovel.
	private double[] fillTimesMean;		//mean fillling times at each shovel.
	private double emptyTimeMean;		//mean emptying time at the crusher.

	/**
	 * Constructor for DISPATCH-based controller.
	 *
	 * @param	numTrucks		the number of trucks.
	 * @param	numShovels		the number of shovels.
	 * @param	travelTimesMean	an array of mean travelling times to each shovel.
	 * @param	fillTimesMean	an array of mean filling times at each shovel.
	 * @param	emptyTimeMean	the mean emptying time at the crusher.
	 * @param	flow			the expected truck flow in the network.
	 */
	public DISPATCHController(int numTrucks, int numShovels, double[] travelTimesMean, double[] fillTimesMean, double emptyTimeMean, 
		double[] flow) {
		this.numTrucks = numTrucks;
		this.numShovels = numShovels;
		this.travelTimesMean = travelTimesMean;
		this.fillTimesMean = fillTimesMean;
		this.emptyTimeMean = emptyTimeMean;
		this.flow = flow;
		totalDiggingRate = 0;
		requiredTrucks = 0;
		minTravelTime = travelTimesMean[0];
		for (int i=0; i<numShovels; i++) {
			totalDiggingRate += flow[i];
			requiredTrucks += flow[i] * (travelTimesMean[i] * (1 + MineSimulatorNarrow.FULLSLOWDOWN) + fillTimesMean[i] + emptyTimeMean);
			minTravelTime = Math.min(minTravelTime,travelTimesMean[i]);
		}

		simLocs = new TruckLocation[numTrucks];
		simProgress = new double[numTrucks];
		simLastUsed = new double[numShovels];
		simLastDispatch = new double[numShovels];
		simAllocated = new double[numShovels];
		sDest = new int[numTrucks];

		reset();
	}

	/**
	 * Determine the next shovel assignment for a given truck.
	 * Uses the DISPATCH algorithm,
	 * which involves sorting trucks by expected dispatch time at the crusher,
	 * sorting shovels by a priority function,
	 * and assigning trucks to shovels to minimise a lost-tons function.
	 * See the references for more information.
	 *
	 * @param	tid	the requesting truck index.
	 * @return	the shovel assignment.
	 * @throws	IllegalArgumentException if the truck is not at the crusher.
	 */
	public int nextShovel(int tid) {
		if (simLocs[tid] != TruckLocation.WAITING) {
			throw new IllegalArgumentException("Requested truck is not ready for assignment");
		}
		PairList progressList = new PairList();
		for (int t=0; t<numTrucks; t++) {
			progressList.add(new Pair(t,simProgress[t]));
		}
		Collections.sort(progressList);
		PairList dispatchTime = new PairList();
		PairList[] travelling = new PairList[numShovels];
		PairList[] fillList = new PairList[numShovels];
		Pair[] filling = new Pair[numShovels];
		PairList[] returning = new PairList[numShovels];
		PairList emptyList = new PairList();
		Pair emptying = null;
		for (int s=0; s<numShovels; s++) {
			travelling[s] = new PairList();
			fillList[s] = new PairList();
			returning[s] = new PairList();
		}
		for (int ind=numTrucks - 1; ind>=0; ind--) {
			Pair p = progressList.get(ind);
			switch (simLocs[p.i]) {
				case WAITING: {
					dispatchTime.add(new Pair(p.i,0));
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					travelling[sDest[p.i]].add(p);
					break;
				}
				case APPROACHING_SHOVEL:
				case WAITING_AT_SHOVEL: {
					fillList[sDest[p.i]].add(p);
					break;
				}
				case FILLING: {
					filling[sDest[p.i]] = p;
					break;
				}
				case LEAVING_SHOVEL:
				case TRAVEL_TO_CRUSHER: {
					returning[sDest[p.i]].add(p);
					break;
				}
				case APPROACHING_CRUSHER:
				case WAITING_AT_CRUSHER: {
					emptyList.add(p);
					break;
				}
				case EMPTYING: {
					emptying = p;
					break;
				}
				default: {
					throw new IllegalStateException(String.format("Truck is in illegal state: %s",simLocs[p.i]));
				}
			}
		}
		double[] sAvailable = new double[numShovels];
		PairList arriveAtCrusher = new PairList();
		for (int s=0; s<numShovels; s++) {
			sAvailable[s] = simLastUsed[s] - simTime;
			PairList arriveAtShovel = new PairList();
			for (Pair p : travelling[s]) {
				double travelTime = travelTimesMean[s] * (1 - p.d);
				arriveAtShovel.add(new Pair(p.i,travelTime));
			}
			double returnTime = travelTimesMean[s] * MineSimulatorNarrow.FULLSLOWDOWN;
			if (filling[s] != null) {
				double fillTime = fillTimesMean[s] * (1 - filling[s].d);
				sAvailable[s] = fillTime;
				arriveAtCrusher.add(new Pair(filling[s].i,fillTime + returnTime));
			}
			for (Pair p : fillList[s]) {
				sAvailable[s] = Math.max(sAvailable[s],0) + fillTimesMean[s];
				arriveAtCrusher.add(new Pair(p.i,sAvailable[s] + returnTime));
			}
			for (Pair p : arriveAtShovel) {
				sAvailable[s] = Math.max(sAvailable[s],p.d) + fillTimesMean[s];
				arriveAtCrusher.add(new Pair(p.i,sAvailable[s] + returnTime));
			}
			for (Pair p : returning[s]) {
				double travelTime = returnTime * (1 - p.d);
				arriveAtCrusher.add(new Pair(p.i,travelTime));
			}
		}
		Collections.sort(arriveAtCrusher);
		double cAvailable = 0;
		if (emptying != null) {
			double emptyTime = emptyTimeMean * (1 - emptying.d);
			cAvailable = emptyTime;
			dispatchTime.add(new Pair(emptying.i,emptyTime));
		}
		for (Pair p : emptyList) {
			cAvailable += emptyTimeMean;
			dispatchTime.add(new Pair(p.i,cAvailable));
		}
		for (Pair p : arriveAtCrusher) {
			cAvailable = Math.max(cAvailable,p.d) + emptyTimeMean;
			dispatchTime.add(new Pair(p.i,cAvailable));
		}
		Collections.sort(dispatchTime);
		int out = getByNeed(tid,dispatchTime,Arrays.copyOf(sAvailable,numShovels));
		return (out < 0 ? getByGreedy(sAvailable) : out);
	}

	/**
	 * Assign trucks to shovels by examining shovels in order of neediness.
	 * Terminates when the requesting truck has been assigned.
	 *
	 * @param	tid				the requesting truck index.
	 * @param	dispatchTime	a list of estimated upcoming dispatch times for trucks.
	 * @param	sAvailable		an array of estimated availability times for each shovel.
	 * @return	the shovel to route the requesting truck to.
	 */
	private int getByNeed(int tid, PairList dispatchTime, double[] sAvailable) {
		ShortPriorityQueue<Pair> shovelNeed = new ShortPriorityQueue<>();
		double[] lastDispatch = new double[numShovels];
		double[] allocated = new double[numShovels];
		for (int s=0; s<numShovels; s++) {
			if (flow[s] > 0) {
				lastDispatch[s] = simLastDispatch[s] - simTime;
				allocated[s] = simAllocated[s];
				double needTime = lastDispatch[s] + allocated[s] / flow[s] - travelTimesMean[s];
				shovelNeed.add(new Pair(s,needTime));
			}
		}
		boolean[] assigned = new boolean[numTrucks];
		while (true) {
			Pair neediest = shovelNeed.poll();
			int sid = neediest.i;
			Pair bestPair = null;
			double bestValue = 1e9;
			double baseLoss = totalDiggingRate * (travelTimesMean[sid] - minTravelTime) / requiredTrucks;
			for (Pair p : dispatchTime) {
				if (!assigned[p.i]) {
					double dispatch = Math.max(p.d,lastDispatch[sid]);
					double arrivalTime = dispatch + travelTimesMean[sid];
					double lostTons = baseLoss;
					if (arrivalTime > sAvailable[sid]) {
						lostTons += (arrivalTime - sAvailable[sid]) * flow[sid];
					}
					else {
						lostTons += (sAvailable[sid] - arrivalTime) * totalDiggingRate / requiredTrucks;
					}
					if (bestPair == null || lostTons < bestValue) {
						bestPair = p;
						bestValue = lostTons;
					}
				}
			}
			double dispatch = Math.max(bestPair.d,lastDispatch[sid]);
			if (dispatch > bestPair.d) {
				return -1;
			}
			if (bestPair.i == tid) {
				return sid;
			}
			assigned[bestPair.i] = true;
			allocated[sid] = Math.max(0,allocated[sid] - (dispatch - lastDispatch[sid]) * flow[sid]) + 1;
			lastDispatch[sid] = dispatch;
			sAvailable[sid] = Math.max(dispatch + travelTimesMean[sid],sAvailable[sid]) + fillTimesMean[sid];
			double needTime = lastDispatch[sid] + allocated[sid] / flow[sid] - travelTimesMean[sid];
			shovelNeed.add(new Pair(sid,needTime));
		}
	}

	/**
	 * Assign the next to the truck greedily to the shovel which will minimise the lost-tons function.
	 * Used when the algorithm decides a truck should be dispatched out of order,
	 * because it will spend too much time waiting wherever it arrives otherwise.
	 *
	 * @param	sAvailable		an array of estimated availability times for each shovel.
	 * @return	the shovel to route the requesting truck to.
	 */
	private int getByGreedy(double[] sAvailable) {
		int best = -1;
		double bestValue = 1e9;
		for (int s=0; s<numShovels; s++) {
			double lostTons = totalDiggingRate * (travelTimesMean[s] - minTravelTime) / requiredTrucks;
			double arrivalTime = travelTimesMean[s];
			if (arrivalTime > sAvailable[s]) {
				lostTons += (arrivalTime - sAvailable[s]) * flow[s];
			}
			else {
				lostTons += (sAvailable[s] - arrivalTime) * totalDiggingRate / requiredTrucks;
			}
			if (best < 0 || lostTons < bestValue) {
				best = s;
				bestValue = lostTons;
			}
		}
		// mines.system.Debugger.print(String.format("X %d\n",best));
		return best;
	}

	/**
	 * Multi crusher networks are unsupported by this controller.
	 *
	 * @throws	UnsupportedOperationException
	 * @see		DISPATCHControllerN for complex network routing.
	 */
	public int nextCrusher(int tid) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Updates stored state parameters based on the state change.
	 *
	 * @param	change	the StateChange received from the simulator.
	 */
	public void event(StateChange change) {
		simTime = change.getTime();
		int truck = change.getTruck();
		simLocs[truck] = change.getTarget();
		int sid = change.getShovel();
		sDest[truck] = sid;
		for (int i=0; i<numTrucks; i++) {
			simProgress[i] = change.getProgress(i);
		}
		switch (simLocs[truck]) {
			case LEAVING_SHOVEL: {
				simLastUsed[sid] = simTime;
				break;
			}
			case TRAVEL_TO_SHOVEL: {
				simAllocated[sid] = Math.max(0,simAllocated[sid] - (simTime - simLastDispatch[sid]) * flow[sid]) + 1;
				simLastDispatch[sid] = truck;
				break;
			}
		}
	}

	/**
	 * Resets the stored state parameters to the start of a shift.
	 */
	public void reset() {
		simTime = 0;
		for (int i=0; i<numTrucks; i++) {
			simLocs[i] = TruckLocation.WAITING;
			simProgress[i] = 0;
			sDest[i] = -1;
		}
		for (int i=0; i<numShovels; i++) {
			simLastUsed[i] = simTime;
			simLastDispatch[i] = simTime;
			simAllocated[i] = 0;
		}
	}
}