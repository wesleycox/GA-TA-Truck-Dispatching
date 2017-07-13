package mines.sol.greedy;

import mines.sol.Controller;
import mines.util.times.TimeDistribution;
import mines.sim.*;
import mines.util.DoubleList;
import java.util.*;

/**
 * An alternative MTCT controller for simple road networks.
 * The HeuristicController class minimises the return time to the crusher for MTCT.
 * This controller instead minimises complete cycle time, 
 * (including waiting at the crusher).
 * Routing is performed by greedily minimising cycle time for a route.
 * Cycle time is calculated by performing simple forward simulations,
 * then averaging the results.
 *
 * For more information on these greedy heuristics see:
 *
 * Tan, S., & Ramani, R. V. (1992, February). 
 * Evaluation of computer truck dispatching criteria. 
 * In Proceedings of the SME/AIME annual meeting and exhibition, Arizona (pp. 192-215).
 */
public class METController implements Controller {

	private static class Item implements Comparable<Item> {

		boolean b;
		double p;
		int s;

		public Item(double p, boolean b, int s) {
			this.p = p;
			this.b = b;
			this.s = s;
		}

		public int compareTo(Item other) {
			return (int) Math.signum(this.p - other.p);
		}
	}

	private static class ItemList extends ArrayList<Item> {}

	private static final double EPSILON = 1e-6;

	private TimeDistribution tgen;		//the distribution used to generate times.
	private int numSamples;				//the number of samples per heuristic evaluation.
	private int numTrucks;				//the number of trucks.
	private int numShovels;				//the number of shovels.
	private double[] travelTimesMean;	//average travelling times to each shovel.
	private double[] travelTimesSD;		//standard deviations of travelling time to each shovel.
	private double[] fillTimesMean;		//average filling times for each shovel.
	private double[] fillTimesSD;		//standard deviations of filling times for each shovel.
	private double emptyTimeMean;		//the mean emptying time at the crusher.
	private double emptyTimeSD;			//the standard deviation of the emptying time at the crusher.

	private TruckLocation[] truckLocs;	//the current locations of each truck in the simulation.
	private double[] progress;			//the current completion of each truck on its current task.
	private int[] sDest;				//the current shovel assignment for each truck in the simulation.
	private double[] lastUsed;			//the last service completion for each shovel in the simulation.
	private double currTime;			//the current simulation time.

	/**
	 * Controller constructor.
	 *
	 * @param	tgen			the distribution used to generate times.
	 * @param	numSamples		the number of samples per heuristic evaluation.
	 * @param	numTrucks		the number of trucks.
	 * @param	numShovels		the number of shovels.
	 * @param	travelTimesMean	an array of average travelling times to each shovel.
	 * @param	travelTimesSD	an array of standard deviations of travelling time to each shovel.
	 * @param	fillTimesMean	an array of average filling times for each shovel.
	 * @param	fillTimesSD		an array of standard deviations of filling times for each shovel.
	 * @param	emptyTimeMean	the mean emptying time at the crusher.
	 * @param	emptyTimeSD		the standard deviation of the emptying time at the crusher.
	 */
	public METController(TimeDistribution tgen, int numSamples, int numTrucks, int numShovels, double[] travelTimesMean, 
		double[] travelTimesSD, double[] fillTimesMean, double[] fillTimesSD, double emptyTimeMean, double emptyTimeSD) {
		this.tgen = tgen;
		this.numSamples = numSamples;
		this.numTrucks = numTrucks;
		this.numShovels = numShovels;
		this.travelTimesMean = Arrays.copyOf(travelTimesMean,travelTimesMean.length);
		this.travelTimesSD = Arrays.copyOf(travelTimesSD,travelTimesSD.length);
		this.fillTimesMean = Arrays.copyOf(fillTimesMean,fillTimesMean.length);
		this.fillTimesSD = Arrays.copyOf(fillTimesSD,fillTimesSD.length);
		this.emptyTimeMean = emptyTimeMean;
		this.emptyTimeSD = emptyTimeSD;

		truckLocs = new TruckLocation[numTrucks];
		progress = new double[numTrucks];
		sDest = new int[numTrucks];
		lastUsed = new double[numShovels];
	}

	/**
	 * Resets the stored state parameters to the start of a shift.
	 */
	public void reset() {
		for (int i=0; i<numTrucks; i++) {
			truckLocs[i] = TruckLocation.WAITING;
			progress[i] = 0;
			sDest[i] = -1;
		}
		for (int i=0; i<numShovels; i++) {
			lastUsed[i] = 0;
		}
		currTime = 0;
	}

	/**
	 * Updates stored truck state parameters based on the state change.
	 *
	 * @param	change	the StateChange received from the simulator.
	 */
	public void event(StateChange change) {
		currTime = change.getTime();
		int truck = change.getTruck();
		truckLocs[truck] = change.getTarget();
		sDest[truck] = change.getShovel();
		if (truckLocs[truck] == TruckLocation.LEAVING_SHOVEL) {
			lastUsed[sDest[truck]] = currTime;
		}
		for (int i=0; i<numTrucks; i++) {
			progress[i] = change.getProgress(i);
		}
	}

	/**
	 * Determine the next route for the considered truck.
	 * 
	 * A route is chosen that minimises the cycle time of the considered truck.
	 *
	 * @param	tid	the considered truck.
	 * @return	the next route.
	 */
	public int nextShovel(int tid) {
		if (truckLocs[tid] != TruckLocation.WAITING) {
			throw new IllegalArgumentException("Requested truck is not ready for assignment");
		}
		DoubleList[] travelList = new DoubleList[numShovels];
		int[] fillCount = new int[numShovels];
		double[] filling = new double[numShovels];
		DoubleList[] returnList = new DoubleList[numShovels];
		for (int i=0; i<numShovels; i++) {
			travelList[i] = new DoubleList();
			filling[i] = -1;
			returnList[i] = new DoubleList();
		}
		int emptyCount = 0;
		double emptying = -1;
		for (int i=0; i<numTrucks; i++) {
			switch (truckLocs[i]) {
				case WAITING: {
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					travelList[sDest[i]].add(progress[i]);
					break;
				}
				case APPROACHING_SHOVEL:
				case WAITING_AT_SHOVEL: {
					fillCount[sDest[i]]++;
					break;
				}
				case FILLING: {
					filling[sDest[i]] = progress[i];
					break;
				}
				case LEAVING_SHOVEL:
				case TRAVEL_TO_CRUSHER: {
					returnList[sDest[i]].add(progress[i]);
					break;
				}
				case APPROACHING_CRUSHER:
				case WAITING_AT_CRUSHER: {
					emptyCount++;
					break;
				}
				case EMPTYING: {
					emptying = progress[i];
					break;
				}
				default: {
					throw new IllegalStateException(String.format("Truck is in illegal state: %s",truckLocs[i]));
				}
			}
		}
		double[] totalCycleTime = new double[numShovels];
		for (int i=0; i<numShovels; i++) {
			travelList[i].add(0);
			travelList[i].sort();
			returnList[i].sort();
		}
		for (int sa=0; sa<numSamples; sa++) {
			ItemList returning = new ItemList();
			for (int i=0; i<numShovels; i++) {
				DoubleList arriving = new DoubleList();
				double maxArr = 0;
				double lastP = 1.1;
				for (int k=travelList[i].size() - 1; k>=0; k--) {
					double p = travelList[i].get(k);
					if (lastP - p < EPSILON) {
						arriving.add(maxArr);
						continue;
					}
					double arr = Math.max(maxArr,tgen.nextTime(travelTimesMean[i],travelTimesSD[i]) * (1 - p));
					maxArr = arr;
					lastP = p;
					arriving.add(arr);
				}
				maxArr = 0;
				lastP = 1.1;
				for (int k=returnList[i].size() - 1; k>=0; k--) {
					double p = returnList[i].get(k);
					if (lastP - p < EPSILON) {
						returning.add(new Item(maxArr,false,i));
						continue;
					}
					double arr = Math.max(maxArr,MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[i],
						travelTimesSD[i]) * (1 - p));
					maxArr = arr;
					lastP = p;
					returning.add(new Item(arr,false,i));
				}
				double sAvailable = lastUsed[i] - currTime;
				if (filling[i] >= 0) {
					sAvailable = tgen.nextTime(fillTimesMean[i],fillTimesSD[i]) * (1 - filling[i]);
					double arr = Math.max(maxArr,sAvailable + MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[i],
						travelTimesSD[i]));
					maxArr = arr;
					returning.add(new Item(arr,false,i));
				}
				for (int k=0; k<fillCount[i]; k++) {
					sAvailable += tgen.nextTime(fillTimesMean[i],fillTimesSD[i]);
					double arr = Math.max(maxArr,sAvailable + MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[i],
						travelTimesSD[i]));
					maxArr = arr;
					returning.add(new Item(arr,false,i));
				}
				for (int k=0; k<arriving.size(); k++) {
					double prevSA = sAvailable;
					sAvailable = Math.max(sAvailable,arriving.get(k)) + tgen.nextTime(fillTimesMean[i],fillTimesSD[i]);
					double arr = Math.max(maxArr,sAvailable + MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[i],
						travelTimesSD[i]));
					maxArr = arr;
					returning.add(new Item(arr,k == arriving.size() - 1,i));
				}
			}
			Collections.sort(returning);
			double cAvailable = 0;
			if (emptying >= 0) {
				cAvailable = tgen.nextTime(emptyTimeMean,emptyTimeSD) * (1 - emptying);
			}
			for (int k=0; k<emptyCount; k++) {
				cAvailable += tgen.nextTime(emptyTimeMean,emptyTimeSD);
			}
			for (Item item : returning) {
				if (item.b) {
					totalCycleTime[item.s] += Math.max(cAvailable,item.p);
				}
				else {
					cAvailable = Math.max(cAvailable,item.p) + tgen.nextTime(emptyTimeMean,emptyTimeSD);
				}
			}
		}
		return getMinIndex(totalCycleTime);
	}

	/**
	 * Multi crusher networks are unsupported by this controller.
	 *
	 * @throws	UnsupportedOperationException
	 * @see		HeuristicControllerNLC and MTCTControllerN for complex network routing.
	 */
	public int nextCrusher(int tid) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the index of the minimum value of an array.
	 *
	 * @param	array	an array.
	 * @return	the index of the min value.
	 */
	private int getMinIndex(double[] array) {
		int length = array.length;
		int best = 0;
		double bestVal = array[0];
		for (int i=1; i<length; i++) {
			if (array[i] < bestVal) {
				bestVal = array[i];
				best = i;
			}
		}
		return best;
	}

	/**
	 * Get the index of the maximum value of an array.
	 *
	 * @param	array	an array.
	 * @return	the index of the max value.
	 */
	private int getMaxIndex(double[] array) {
		int length = array.length;
		int best = 0;
		double bestVal = array[0];
		for (int i=1; i<length; i++) {
			if (array[i] > bestVal) {
				bestVal = array[i];
				best = i;
			}
		}
		return best;
	}

}