package mines.sol.greedy;

import mines.sol.Solution;
import mines.sim.MineParameters;
import mines.util.times.TimeDistribution;

/**
 * Class for creating greedy heuristic based controllers for simple road networks.
 */
public class HeuristicSolution extends Solution {

	private HeuristicKind hKind;	//the heuristic type.
	private TimeDistribution tgen;	//the distribution used for generating times.
	private int numSamples;			//the number of samples per heuristic evaluation.

	/**
	 * Solution constructor.
	 *
	 * @param	params		the simulation parameters.
	 * @param	hKind		the heuristic type.
	 * @param	tgen		the distribution used for generating times.
	 * @param	numSamples	the number of samples per heuristic evaluation.
	 */
	public HeuristicSolution(MineParameters params, HeuristicKind hKind, TimeDistribution tgen, int numSamples) {
		super(params);
		this.hKind = hKind;
		this.tgen = tgen;
		this.numSamples = numSamples;
	}

	/**
	 * Creates a heuristic based controller.
	 *
	 * @return	a HeuristicController.
	 */
	public HeuristicController getController() {
		return new HeuristicController(hKind,tgen,numSamples,numTrucks,numShovels,travelTimesMean[0],travelTimesSD[0],fillTimesMean,
			fillTimesSD);
	}

	/**
	 * Get the solution name.
	 *
	 * @return	"Greedy-H (n samples)",
	 *			where H is the heuristic type,
	 *			and n is the number of samples.
	 */
	public String getSolutionName() {
		return String.format("Greedy-%s (%d samples)",hKind,numSamples);
	}
}