package mines.sol.greedy;

import mines.sol.Solution;
import mines.util.times.TimeDistribution;
import mines.sim.MineParameters;

/**
 * Class for creating alternative MTCT-based controllers for simple road networks.
 */
public class METSolution extends Solution {

	private TimeDistribution tgen;	//the distribution used for generating times.
	private int numSamples;			//the number of samples per heuristic evaluation.

	/**
	 * Solution constructor.
	 *
	 * @param	params		the simulation parameters.
	 * @param	tgen		the distribution used for generating times.
	 * @param	numSamples	the number of samples per heuristic evaluation.
	 */
	public METSolution(MineParameters params, TimeDistribution tgen, int numSamples) {
		super(params);
		this.tgen = tgen;
		this.numSamples = numSamples;
	}

	/**
	 * Creates a MTCT-based controller.
	 *
	 * @return	an METController.
	 */
	public METController getController() {
		return new METController(tgen,numSamples,numTrucks,numShovels,travelTimesMean[0],travelTimesSD[0],fillTimesMean,fillTimesSD,
			emptyTimesMean[0],emptyTimesSD[0]);
	}

	/**
	 * Get the solution name.
	 *
	 * @return	"Greedy-MET (n samples)",
	 *			where n is the number of samples.
	 */
	public String getSolutionName() {
		return String.format("Greedy-MET (%d samples)",numSamples);
	}
}