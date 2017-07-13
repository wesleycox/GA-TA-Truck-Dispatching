package mines.sol.string;

import mines.sim.StateChange;
import mines.sol.Controller;
import java.util.Arrays;

/**
 * Cycle based controller for simple road networks.
 */
public class StringController implements Controller {

	private int numInstructions;	//current schedule index.
	private int[] string;			//cyclic schedule.

	/**
	 * Controller constructor.
	 *
	 * @param	string	the schedule array.
	 */
	public StringController(int[] string) {
		this.string = Arrays.copyOf(string,string.length);
		reset();
	}

	/**
	 * Reset to the start of the schedule.
	 */
	public void reset() {
		numInstructions = 0;
	}

	/**
	 * Get the next shovel in the schedule.
	 *
	 * @param	tid	the requesting truck.
	 * @return	a shovel index.
	 */
	public int nextShovel(int tid) {
		numInstructions++;
		return string[(numInstructions - 1) % string.length];
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
	 * Nothing to update.
	 */
	public void event(StateChange change) {}
}