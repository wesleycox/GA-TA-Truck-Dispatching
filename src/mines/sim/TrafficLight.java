package mines.sim;

/**
 * Enum for traffic light states.
 * 
 * States are used exclusively for two-light systems for managing access to one-lane roads.
 * G indicates green,
 * providing access.
 * Y indicates yellow,
 * denying access but indicating some trucks may still be on the road.
 * R indicates red,
 * denying all access.
 * 
 * RR is an unused state.
 */
public enum TrafficLight {
	RR, GR, YR, RG, RY
}