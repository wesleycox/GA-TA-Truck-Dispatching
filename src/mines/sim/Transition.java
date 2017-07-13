package mines.sim;

/**
 * Structure for representing transitions in TA model.
 */
public class Transition implements Comparable<Transition> {

	private double arrivalTime;		//transition time.
	private int index;				//truck index.
	private TruckLocation source;	//transition origin state.
	private TruckLocation target;	//transition target state.
	private int priority;			//transition priority for ordering transitions.

	/**
	 * Transition constructor.
	 *
	 * @param	index		the transitioning truck index.
	 * @param	time		the time of the transition.
	 * @param	source		the source state of the transition.
	 * @param	target		the target state of the transition.
	 * @param	priority	the priority used for ordering transitions--
	 *						only relevant when transitions share transition time.
	 */
	public Transition(int index, double time, TruckLocation source, TruckLocation target, int priority) {
		this.index = index;
		this.arrivalTime = time;
		this.source = source;
		this.target = target;
		this.priority = priority;
	}
	
	/**
	 * Get the transition time.
	 *
	 * @return	the transition time.
	 */
	public double getTime() {
		return arrivalTime;
	}

	/**
	 * Get the associated index of this transition,
	 * intended as the index of the transitioning truck.
	 *
	 * @return	the index.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Get the source state of the transition.
	 * 
	 * @return	the TruckLocation.
	 */
	public TruckLocation getSource() {
		return source;
	}

	/**
	 * Get the target state of the transition.
	 * 
	 * @return	the TruckLocation.
	 */
	public TruckLocation getTarget() {
		return target;
	}
	
	/**
	 * Get the priority value of the transition.
	 * This value is only used when comparing transitions with equal transition times.
	 *
	 * @return	the priority.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Compare with another transition.
	 * Comparison if first by transition time,
	 * secondarily by priority.
	 *
	 * @return	negative if this transition should occur first,
	 *			positive if this should occur after the other,
	 *			0 if equal in time and priority.
	 */
	public int compareTo(Transition other) {
		int timeDiffSgn = (int) Math.signum(this.arrivalTime - other.arrivalTime);
		return (timeDiffSgn == 0 ? Integer.compare(this.priority,other.priority) : timeDiffSgn);
	}

	/**
	 * Formats the transition as a String,
	 * in the form i-t-S-T-p
	 * where i is the index,
	 * t is the time,
	 * S is the source,
	 * T is the target,
	 * and p is the priority.
	 *
	 * @return	the String form.
	 */
	public String toString() {
		return String.format("%d-%f-%s-%s-%d",index,arrivalTime,source,target,priority);
	}
}