package mines.sol.greedy.network;

import mines.sol.*;
import mines.sim.MineParametersN;
import mines.util.times.TimeDistribution;
import mines.sol.greedy.HeuristicKind;

/**
 * Class for creating greedy heuristic based controllers for complex road networks.
 */
public class HeuristicSolutionN extends SolutionN {

	private HeuristicKind hKind;	//the heuristic type.
	private TimeDistribution tgen;	//the distribution used for generating times.
	private int numSamples;			//the number of samples per heuristic evaluation.
	private double endtime;			//the shift length.

	/**
	 * Solution constructor.
	 *
	 * @param	params		the simulation parameters.
	 * @param	hKind		the heuristic type.
	 * @param	tgen		the distribution used for generating times.
	 * @param	numSamples	the number of samples per heuristic evaluation.
	 * @param	endtime		the shift length.
	 */
	public HeuristicSolutionN(MineParametersN params, HeuristicKind hKind, TimeDistribution tgen, int numSamples, double endtime) {
		super(params);
		this.hKind = hKind;
		this.tgen = tgen;
		this.numSamples = numSamples;
		this.endtime = endtime;
	}

	/**
	 * Creates a heuristic based controller,
	 * either two-stage for MTST, MTWT, MSWT,
	 * or single-stage for MTCT.
	 *
	 * @return	a two-stage HeuristicControllerNLC,
	 *			or a single-stage MTCTControllerN.
	 */
	public RouteController getController() {
		switch (hKind) {
			case MTST:
			case MTWT:
			case MSWT: {
				return new HeuristicControllerNLC(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,
					fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,
					routeCrushers,routeShovels,tgen,endtime,numSamples,hKind);
			}
			case MTCT: {
				return new MTCTControllerN(numTrucks,numCrushers,numShovels,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,
					fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,
					routeCrushers,routeShovels,tgen,endtime,numSamples);
			}
			default: {
				throw new IllegalArgumentException(String.format("Unsupported heuristic: %s",hKind));
			}
		}
		
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