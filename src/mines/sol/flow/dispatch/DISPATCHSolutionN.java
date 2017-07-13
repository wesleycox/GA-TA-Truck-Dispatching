package mines.sol.flow.dispatch;

import mines.sol.SolutionN;
import mines.sim.MineParametersN;
import mines.lp.LPFlowConstructor;

/**
 * Class for creating DISPATCH-based controllers for complex road networks.
 */
public class DISPATCHSolutionN extends SolutionN {

	private double runtime;				//shift time.
	private String modelName;			//output filename for the LP model.
	private boolean oneWayRestriction;	//whether to enable the one-way restriction in the LP model.

	/**
	 * Constructs the solution with a null LP model name.
	 *
	 * @see	the other constructor.
	 */
	public DISPATCHSolutionN(MineParametersN params, double runtime, boolean oneWayRestriction) {
		this(params,runtime,oneWayRestriction,null);
	}

	/**
	 * Solution constructor.
	 *
	 * @param	params				the simulation parameters.
	 * @param	runtime				the shift length.
	 * @param	oneWayRestriction	whether to enable the one-way restriction in the LP model.
	 * @param	modelName			the output filename for the LP model.
	 * @see	LPFlowConstructor
	 */
	public DISPATCHSolutionN(MineParametersN params, double runtime, boolean oneWayRestriction, String modelName) {
		super(params);
		this.runtime = runtime;
		this.modelName = modelName;
		this.oneWayRestriction = oneWayRestriction;
	}

	/**
	 * Construct a DISPATCH-based controller.
	 *
	 * @return	a DISPATCHControllerN.
	 */
	public DISPATCHControllerN getController() {
		LPFlowConstructor construct = new LPFlowConstructor(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,fillTimesMean,
			roadTravelTimesMean,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,routeShovels)
			.setOneWayRestriction(oneWayRestriction);
		double[][] flow = construct.getFlow(modelName);
		return new DISPATCHControllerN(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,
			routeShovels,runtime,flow,oneWayRestriction);
	}

	/**
	 * Get the solution name.
	 *
	 * @return	"DISPATCH-owr" if the one-way restriction is enabled,
	 *			"DISPATCH-scale" otherwise.
	 */
	public String getSolutionName() {
		return String.format("DISPATCH-%s",(oneWayRestriction ? "owr" : "scale"));
	}
}