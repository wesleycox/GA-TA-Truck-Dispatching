package mines.sol;

import mines.sim.MineParameters;

/**
 * Solutions create controllers.
 * All simulation parameters are stored,
 * so the parameters necessary for a particular controller can be used.
 */
public abstract class Solution {

	protected final int numTrucks;
	protected final int numShovels;
	protected final int numCrushers;
	protected final double[] emptyTimesMean;
	protected final double[] emptyTimesSD;
	protected final double[] fillTimesMean;
	protected final double[] fillTimesSD;
	protected final double[][] travelTimesMean;
	protected final double[][] travelTimesSD;

	/**
	 * Constructor for simple road network solution.
	 *
	 * @param	params	the simulation parameters.
	 */
	public Solution(MineParameters params) {
		numTrucks = params.getNumTrucks();
		numShovels = params.getNumShovels();
		numCrushers = params.getNumCrushers();
		emptyTimesMean = new double[numCrushers];
		emptyTimesSD = new double[numCrushers];
		for (int i=0; i<numCrushers; i++) {
			emptyTimesMean[i] = params.getMeanEmptyTime(i);
			emptyTimesSD[i] = params.getEmptyTimeSD(i);
		}
		fillTimesMean = params.getMeanFillTimes();
		fillTimesSD = params.getFillTimesSD();
		travelTimesMean = new double[numCrushers][numShovels];
		travelTimesSD = new double[numCrushers][numShovels];
		for (int i=0; i<numCrushers; i++) {
			for (int j=0; j<numShovels; j++) {
				travelTimesMean[i][j] = params.getMeanTravelTime(i,j);
				travelTimesSD[i][j] = params.getTravelTimeSD(i,j);
			}
		}
	}

	/**
	 * Creates a controller.
	 *
	 * @return	a simple-road-network controller.
	 */
	public abstract Controller getController();

	/**
	 * Get the solution name.
	 *
	 * @return	the solution name.
	 */
	public abstract String getSolutionName();
}