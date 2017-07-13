package mines.sol.string;

import mines.sim.*;
import mines.sol.RouteController;
import java.util.*;

/**
 * Cycle based controller for complex road networks.
 * Each location has a cyclic schedule to dispatch trucks to.
 */
public class MultiStringController implements RouteController {

	private int numTrucks;			//the number of trucks.
	private int numShovels;			//the number of shovels.
	private int numCrushers;		//the number of crushers.
	private int[] routeCrushers;	//the crusher connected to each route.
	private int[] routeShovels;		//the shovel connected to each route.

	private boolean[] atCrusher;	//whether each truck is at a crusher for routing purposes.
	private int[] assignedCrusher;	//the currently assigned crusher of each truck in simulation.
	private int[] assignedShovel;	//the currently assigned shovel of each truck in simulation.

	private int[][] crusherCycles;		//the schedules for each crusher.
	private int[][] shovelCycles;		//the schedules for each shovel.
	private int[] crusherAssignments;	//the current schedule index for each crusher.
	private int[] shovelAssignments;	//the current schedule index for each shovel.

	/**
	 * Controller constructor.
	 *
	 * @param	numTrucks		the number of trucks.
	 * @param	numCrushers		the number of crushers.
	 * @param	numShovels		the number of shovels.
	 * @param	routeCrushers	an array of the crusher connected to each route.
	 * @param	routeShovels	an array of the shovel connected to each route.
	 * @param	crusherCycles	a 2D array of the schedule for each crusher.
	 * @param	shovelCycles	a 2D array of the schedule for each shovel.
	 */
	public MultiStringController(int numTrucks, int numCrushers, int numShovels, int[] routeCrushers, int[] routeShovels, 
		int[][] crusherCycles, int[][] shovelCycles) {
		this.numTrucks = numTrucks;
		this.numCrushers = numCrushers;
		this.numShovels = numShovels;
		this.routeCrushers = routeCrushers;
		this.routeShovels = routeShovels;
		this.crusherCycles = crusherCycles;
		this.shovelCycles = shovelCycles;

		atCrusher = new boolean[numTrucks];
		assignedCrusher = new int[numTrucks];
		assignedShovel = new int[numTrucks];
		crusherAssignments = new int[numCrushers];
		shovelAssignments = new int[numShovels];

		reset();
	}

	/**
	 * Get the next destination in the cyclic schedule for a truck's current location.
	 *
	 * @param	tid	the requesting truck.
	 * @return	a route index.
	 */
	public int nextRoute(int tid) {
		if (atCrusher[tid]) {
			int cid = assignedCrusher[tid];
			int out = crusherCycles[cid][crusherAssignments[cid]];
			crusherAssignments[cid] = (crusherAssignments[cid] + 1) % crusherCycles[cid].length;
			return out;
		}
		else {
			int sid = assignedShovel[tid];
			int out = shovelCycles[sid][shovelAssignments[sid]];
			shovelAssignments[sid] = (shovelAssignments[sid] + 1) % shovelCycles[sid].length;
			return out;
		}
	}

	/**
	 * Updates stored truck state parameters based on the state change.
	 *
	 * @param	change	the StateChange received from the simulator.
	 */
	public void event(StateChange change) {
		int tid = change.getTruck();
		TruckLocation target = change.getTarget();
		int route = change.getFirstIndex();
		switch (target) {
			case FILLING: {
				atCrusher[tid] = false;
				break;
			}
			case EMPTYING: {
				atCrusher[tid] = true;
				break;
			}
			case TRAVEL_TO_SHOVEL: {
				assignedShovel[tid] = routeShovels[route];
				break;
			}
			case TRAVEL_TO_CRUSHER: {
				assignedCrusher[tid] = routeCrushers[route];
				break;
			}
		}
	}

	/**
	 * Updates stored light state parameters based on the state change.
	 *
	 * @param	light	the road which has changed light state.
	 * @param	change	the new light state.
	 */
	public void lightEvent(int light, TrafficLight change) {}

	/**
	 * Resets the stored state parameters to the start of a shift.
	 */
	public void reset() {
		for (int i=0; i<numTrucks; i++) {
			atCrusher[i] = true;
			assignedCrusher[i] = i % numCrushers;
			assignedShovel[i] = -1;
		}
		for (int i=0; i<numCrushers; i++) {
			crusherAssignments[i] = 0;
		}
		for (int i=0; i<numShovels; i++) {
			shovelAssignments[i] = 0;
		}
	}

	/**
	 * Get the text form of the schedules.
	 */
	public String toString() {
		return Arrays.deepToString(crusherCycles) + "," + Arrays.deepToString(shovelCycles);
	}
}