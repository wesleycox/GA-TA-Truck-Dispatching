package mines.sol.greedy;

import mines.sol.Controller;
import mines.util.times.TimeDistribution;
import mines.sim.*;
import mines.util.DoubleList;
import java.util.Arrays;

/**
 * Greedy heuristic controller for simple road networks.
 * Routing is performed by greedily minimising a heuristic value for a route.
 * Heuristics are calculated by performing simple forward simulations,
 * then averaging the results.
 *
 * For more information on these greedy heuristics see:
 *
 * Tan, S., & Ramani, R. V. (1992, February). 
 * Evaluation of computer truck dispatching criteria. 
 * In Proceedings of the SME/AIME annual meeting and exhibition, Arizona (pp. 192-215).
 */
public class HeuristicController implements Controller {

	private static final double EPSILON = 1e-6;

	private HeuristicKind hKind;		//the heuristic type.
	private TimeDistribution tgen;		//the distribution used to generate times.
	private int numSamples;				//the number of samples per heuristic evaluation.
	private int numTrucks;				//the number of trucks.
	private int numShovels;				//the number of shovels.
	private double[] travelTimesMean;	//average travelling times to each shovel.
	private double[] travelTimesSD;		//standard deviations of travelling time to each shovel.
	private double[] fillTimesMean;		//average filling times for each shovel.
	private double[] fillTimesSD;		//standard deviations of filling times for each shovel.

	private TruckLocation[] truckLocs;	//the current locations of each truck in the simulation.
	private double[] progress;			//the current completion of each truck on its current task.
	private int[] sDest;				//the current shovel assignment for each truck in the simulation.
	private double[] lastUsed;			//the last service completion for each shovel in the simulation.
	private double currTime;			//the current simulation time.

	private double minTravelTimeMean;	//the minimum average travel time.
	private double totalDigRate;		//the net service rate of all shovels.

	/**
	 * Controller constructor.
	 *
	 * @param	hKind			the heuristic type.
	 * @param	tgen			the distribution used to generate times.
	 * @param	numSamples		the number of samples per heuristic evaluation.
	 * @param	numTrucks		the number of trucks.
	 * @param	numShovels		the number of shovels.
	 * @param	travelTimesMean	an array of average travelling times to each shovel.
	 * @param	travelTimesSD	an array of standard deviations of travelling time to each shovel.
	 * @param	fillTimesMean	an array of average filling times for each shovel.
	 * @param	fillTimesSD		an array of standard deviations of filling times for each shovel.
	 */
	public HeuristicController(HeuristicKind hKind, TimeDistribution tgen, int numSamples, int numTrucks, int numShovels, 
		double[] travelTimesMean, double[] travelTimesSD, double[] fillTimesMean, double[] fillTimesSD) {
		this.hKind = hKind;
		this.tgen = tgen;
		this.numSamples = numSamples;
		this.numTrucks = numTrucks;
		this.numShovels = numShovels;
		this.travelTimesMean = Arrays.copyOf(travelTimesMean,travelTimesMean.length);
		this.travelTimesSD = Arrays.copyOf(travelTimesSD,travelTimesSD.length);
		this.fillTimesMean = Arrays.copyOf(fillTimesMean,fillTimesMean.length);
		this.fillTimesSD = Arrays.copyOf(fillTimesSD,fillTimesSD.length);

		truckLocs = new TruckLocation[numTrucks];
		progress = new double[numTrucks];
		sDest = new int[numTrucks];
		lastUsed = new double[numShovels];

		minTravelTimeMean = travelTimesMean[getMinIndex(travelTimesMean)];
		totalDigRate = 0;
		for (int i=0; i<numShovels; i++) {
			totalDigRate += 1.0 / fillTimesMean[i];
		}
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
	 * A route is chosen that minimises the set heuristic value of the considered truck.
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
		for (int i=0; i<numTrucks; i++) {
			switch (truckLocs[i]) {
				case WAITING:
				case APPROACHING_CRUSHER:
				case WAITING_AT_CRUSHER:
				case EMPTYING: {
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
				default: {
					throw new IllegalStateException(String.format("Truck is in illegal state: %s",truckLocs[i]));
				}
			}
		}
		double[] totalCycleTime = new double[numShovels];
		double[] totalServiceTime = new double[numShovels];
		double[] totalTWaitingTime = new double[numShovels];
		double[] totalSWaitingTime = new double[numShovels];
		for (int i=0; i<numShovels; i++) {
			travelList[i].add(0);
			travelList[i].sort();
			returnList[i].sort();
			DoubleList arriving = new DoubleList();
			DoubleList returning = new DoubleList();
			for (int j=0; j<numSamples; j++) {
				arriving.clear();
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
				returning.clear();
				maxArr = 0;
				lastP = 1.1;
				for (int k=returnList[i].size() - 1; k>=0; k--) {
					double p = returnList[i].get(k);
					if (lastP - p < EPSILON) {
						returning.add(maxArr);
						continue;
					}
					double arr = Math.max(maxArr,MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[i],travelTimesSD[i]) * 
						(1 - p));
					maxArr = arr;
					lastP = p;
					returning.add(arr);
				}
				double sAvailable = lastUsed[i] - currTime;
				if (filling[i] >= 0) {
					sAvailable = tgen.nextTime(fillTimesMean[i],fillTimesSD[i]) * (1 - filling[i]);
					double arr = Math.max(maxArr,sAvailable + MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[i],
						travelTimesSD[i]));
					maxArr = arr;
					returning.add(arr);
				}
				for (int k=0; k<fillCount[i]; k++) {
					sAvailable = Math.max(0,sAvailable) + tgen.nextTime(fillTimesMean[i],fillTimesSD[i]);
					double arr = Math.max(maxArr,sAvailable + MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[i],
						travelTimesSD[i]));
					maxArr = arr;
					returning.add(arr);
				}
				for (int k=0; k<arriving.size(); k++) {
					if (k == arriving.size() - 1) {
						totalServiceTime[i] += Math.max(sAvailable,arriving.get(k));
						totalTWaitingTime[i] += Math.max(0,sAvailable - arriving.get(k));
						totalSWaitingTime[i] += sAvailable;
					}
					sAvailable = Math.max(sAvailable,arriving.get(k)) + tgen.nextTime(fillTimesMean[i],fillTimesSD[i]);
					double arr = Math.max(maxArr,sAvailable + MineSimulatorNarrow.FULLSLOWDOWN * tgen.nextTime(travelTimesMean[i],
						travelTimesSD[i]));
					maxArr = arr;
					returning.add(arr);
				}
				totalCycleTime[i] += returning.get(returning.size() - 1);
			}
		}
		switch (hKind) {
			case MTCT: {
				return getMinIndex(totalCycleTime);
			}
			case MTST: {
				return getMinIndex(totalServiceTime);
			}
			case MTWT: {
				return getMinIndex(totalTWaitingTime);
			}
			case MSWT: {
				return getMinIndex(totalSWaitingTime);
			}
			default: {
				throw new IllegalStateException("Invalid heuristic kind");
			}
		}
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