package mines.sol.flow.dispatch;

import mines.sim.*;
import mines.lp.LPFlowConstructor;
import mines.sol.Solution;

/**
 * Class for creating DISPATCH-based controllers for simple road networks.
 */
public class DISPATCHSolution extends Solution {

	private String modelName;	//output filename for the LP model.

	/**
	 * Constructs the solution with a null LP model name.
	 *
	 * @see	the other constructor.
	 */
	public DISPATCHSolution(MineParameters params) {
		this(params,null);
	}

	/**
	 * Solution constructor.
	 *
	 * @param	params		the simulation parameters.
	 * @param	modelName	the output filename for the LP model.
	 * @see	LPFlowConstructor
	 * @throws	IllegalArgumentException if the number of crushers is not 1.
	 */
	public DISPATCHSolution(MineParameters params, String modelName) {
		super(params);
		if (numCrushers != 1) {
			throw new IllegalArgumentException();
		}
		this.modelName = modelName;
	}

	/**
	 * Construct a DISPATCH-based controller.
	 *
	 * @return	a DISPATCHController.
	 */
	public DISPATCHController getController() {
		double fullSlowdown = MineSimulatorNarrow.FULLSLOWDOWN;
		boolean[] isOneWay = new boolean[numShovels];
		int[][] routeRoads = new int[numShovels][1];
		int[][] routeDirections = new int[numShovels][1];
		int[] routeLengths = new int[numShovels];
		int[] routeCrushers = new int[numShovels];
		int[] routeShovels = new int[numShovels];
		for (int i=0; i<numShovels; i++) {
			routeRoads[i][0] = i;
			routeDirections[i][0] = 0;
			routeLengths[i] = 1;
			routeCrushers[i] = 0;
			routeShovels[i] = i;
		}
		LPFlowConstructor construct = new LPFlowConstructor(numTrucks,numCrushers,numShovels,numShovels,emptyTimesMean,fillTimesMean,
			travelTimesMean[0],fullSlowdown,isOneWay,numShovels,routeRoads,routeDirections,routeLengths,routeCrushers,routeShovels);
		double[][] flowTW = construct.getFlow(modelName);
		double[] flow = new double[numShovels];
		for (int i=0; i<numShovels; i++) {
			flow[i] = flowTW[i][0];
		}
		return new DISPATCHController(numTrucks,numShovels,travelTimesMean[0],fillTimesMean,emptyTimesMean[0],flow);
	}

	/**
	 * Get the solution name.
	 *
	 * @return "DISPATCH".
	 */
	public String getSolutionName() {
		return "DISPATCH";
	}
}