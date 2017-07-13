package mines.sim;

import java.util.Arrays;

/**
 * Class representing a change in state after a transition occurs.
 * A StateChange object is given to the controller after each transition,
 * to allow it to maintain information about the current state of the mine,
 * for dispatching purposes.
 * 
 * Provides information about the currently transitioning truck,
 * as well as updates on the progress of the other trucks on their transitions.
 */
public class StateChange {

	private Transition trans;	//the last transition.
	private double[] progress;	//the current completion of each truck of its current task.
	private int sDest;			//the first information index.
	private int cDest;			//the second information index.

	/**
	 * State change constructor.
	 *
	 * @param	trans		the most recent transition.
	 * @param	sDest		the first information index--
	 *						for simple road networks,
	 *						this is set to the assigned shovel of the transitioning truck;
	 *						for complex road networks,
	 *						this is set to the assigned route of the transitioning truck.
	 * @param	cDest		the second information index--
	 *						for simple road networks,
	 *						this is unused;
	 *						for complex road networks,
	 *						this is set to the route point,
	 *						i.e. the number of roads passed in the current route,
	 *						or the number of roads to pass on the return route.
	 * @param	progress	the fractional completion of each truck for its current transition;
	 *						for stationary trucks,
	 *						this is the waiting time.
	 */
	public StateChange(Transition trans, int sDest, int cDest, double[] progress) {
		this.trans = trans;
		this.sDest = sDest;
		this.cDest = cDest;
		this.progress = Arrays.copyOf(progress,progress.length);
	}

	/**
	 * Get the time of the transition,
	 * i.e. the current simulation time.
	 *
	 * @return	the transition time.
	 */
	public double getTime() {
		return trans.getTime();
	}

	/**
	 * Get the index of the transitioning truck.
	 *
	 * @return	a truck index.
	 */
	public int getTruck() {
		return trans.getIndex();
	}

	/**
	 * Get the new location of the transitioning truck.
	 *
	 * @return	a state from the TA model.
	 */
	public TruckLocation getTarget() {
		return trans.getTarget();
	}

	/**
	 * Get the first information index,
	 * which for simple road networks is used as the assigned shovel of the transitioning truck.
	 *
	 * @return	an int value.
	 * @see	getFirstIndex.
	 */
	public int getShovel() {
		return sDest;
	}

	/**
	 * Get the progress value for a single truck.
	 *
	 * @param	the truck index.
	 * @return	a double value.
	 */
	public double getProgress(int tid) {
		return progress[tid];
	}

	/**
	 * Get the first information index
	 * For simple road networks, 
	 * this is used as the assigned shovel of the transitioning truck.
	 * For complex road networks,
	 * this is used as the assigned route of the transitioning truck.
	 *
	 * @return	an int value.
	 * @see	getShovel.
	 */
	public int getFirstIndex() {
		return sDest;
	}

	/**
	 * Get the second information index
	 * For simple road networks, 
	 * this is unused.
	 * For complex road networks,
	 * this is the route point,
	 * i.e. the number of roads passed in the current route,
	 * or the number of roads to pass on the return route.
	 *
	 * @return	an int value.
	 */
	public int getSecondIndex() {
		return cDest;
	}
}