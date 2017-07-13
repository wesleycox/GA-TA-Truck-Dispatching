package mines.sol;

import mines.sim.StateChange;

/**
 * Controller for making routing decisions in simple road networks.
 * Simple is defined as a single two-lane road between each crusher and shovel,
 * typically with only one crusher.
 */
public interface Controller {

	/**
	 * Get the next shovel destination for the current truck.
	 *
	 * @param	tid	the requesting truck.
	 * @return	a shovel index.
	 */
	public int nextShovel(int tid);

	/**
	 * Get the next crusher destination for the current truck.
	 *
	 * NOTE that no simulator currently supports this method.
	 * @see RouteController	for routing in complex road networks.
	 */
	public int nextCrusher(int tid);

	/**
	 * Update stored state information about the current simulation.
	 *
	 * @param	change	a StateChange.
	 */
	public void event(StateChange change);

	/**
	 * Reset the stored state information to the start of the shift.
	 * All trucks should be at the crusher waiting empty for dispatch.
	 */
	public void reset();
}