package mines.sol;

import mines.sim.*;

/**
 * Controller for making routing decisions in complex road networks.
 * Complex is defined as having multi-road routes,
 * and combinations of one-lane and two-lane roads.
 * Routes are predefined,
 * and not every crusher-shovel pair is guaranteed to be connected by a route.
 */
public interface RouteController {

	/**
	 * Get the next route for the current truck.
	 * The set of valid routes is dependent on the location of the truck.
	 *
	 * @param	tid	the requesting truck.
	 * @return	a route index.
	 */
	public int nextRoute(int tid);

	/**
	 * Update stored state truck information about the current simulation.
	 *
	 * @param	change	a StateChange.
	 */
	public void event(StateChange change);

	/**
	 * Update stored state light information about the current simulation.
	 *
	 * @param	light	the index of the road that changed light state.
	 * @param	change	the new light state.
	 */
	public void lightEvent(int light, TrafficLight change);

	/**
	 * Reset the stored state information to the start of the shift.
	 * All trucks should be at a crusher waiting empty for dispatch.
	 */
	public void reset();

	/**
	 * Get the initial crusher locations of each truck at shift start.
	 * null results in the default:
	 * a truck with index i starts at the crusher with index (i % NC),
	 * for NC crushers.
	 *
	 * @return	an array of crusher indexes.
	 */
	public default int[] getInitialCrushers() {
		return null;
	}
	
}