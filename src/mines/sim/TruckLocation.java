package mines.sim;

/**
 * Enum for truck locations in the TA model.
 * Trucks exist in one of these states,
 * while transitioning to another (possibly identical) state.
 * Additional information is required to view the complete mine state.
 *
 * TL (traffic light) states are only used in complex road networks.
 */
public enum TruckLocation {
	WAITING, TRAVEL_TO_SHOVEL, APPROACHING_SHOVEL, WAITING_AT_SHOVEL, FILLING, LEAVING_SHOVEL, TRAVEL_TO_CRUSHER, APPROACHING_CRUSHER, 
	WAITING_AT_CRUSHER, EMPTYING, ERROR, UNUSED, REQUESTING,

	APPROACHING_TL_CS, STOPPED_AT_TL_CS, APPROACHING_TL_SS, STOPPED_AT_TL_SS
}