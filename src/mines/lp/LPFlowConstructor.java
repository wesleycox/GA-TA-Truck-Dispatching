package mines.lp;

import mines.util.IntList;
import lpsolve.*;
import java.util.*;

/**
 * Uses linear programming to determine 'optimal' flow in a network.
 * Inspired by LP models presented in:
 *
 * White, J. W., Arnold, M. J., & Clevenger, J. G. (1982). 
 * Automated open-pit truck dispatching at Tyrone. 
 * E&MJ-ENGINEERING AND MINING JOURNAL, 183(6), 76-84.
 *
 * Li, Z. (1990). 
 * A methodology for the optimum control of shovel and truck operations in open-pit mining. 
 * Mining Science and Technology, 10(3), 337-340.
 */
public class LPFlowConstructor {

	private static final double LARGECONSTANT = 1000;	//large constant for Big-M method.

	/*
	 * Simulation parameters.
	 */
	private int numTrucks;					//number of trucks.
	private int numShovels;					//number of shovels.
	private int numCrushers;				//number of crushers.
	private int numRoads;					//number of roads.
	private double[] emptyTimesMean;		//average emptying times for each crusher.
	private double[] fillTimesMean;			//average filling times for each shovel.
	private double[] roadTravelTimesMean;	//average travelling times on each road.
	private double fullSlowdown;			//travel time increase for travelling full.
	private boolean[] isOneWay;				//whether each road is one-lane.

	/*
	 * Route specifications.
	 */
	private int numRoutes;				//number of routes.
	private int[][] routeRoads;			//list of roads comprising each route.
	private int[][] routeDirections;	//list of directions travelled on each road in each route.
	private int[] routeLengths;			//number of roads in each route.
	private int[] routeShovels;			//the shovel at the end of each route.
	private int[] routeCrushers;		//the crusher at the start of each route.

	/*
	 * Route information.
	 */
	private IntList[] routesFromCrusher;	//lists of routes out of each crusher.
	private IntList[] routesFromShovel;		//lists of routes out of each shovel.
	private IntList[] routesOnRoad;			//list of routes using each road.

	private boolean oneWayRestriction;	//whether to restrict one-lane road access to a single direction.
	private double[] maxExpRoadFlow;	//the maximum expected truck flow for each road based on shovel rates.

	/**
	 * Constructor for LP class.
	 * The one way restriction is enabled by default.
	 *
	 * @param numTrucks				the number of trucks.
	 * @param numCrushers			the number of crushers.
	 * @param numShovels			the number of shovels.
	 * @param numRoads				the number of roads.
	 * @param emptyTimesMean		an array of average emptying times for each crusher.
	 * @param fillTimesMean			an array of average filling times for each shovel.
	 * @param roadTravelTimesMean	an array of average travelling times on each road.
	 * @param fullSlowdown			the travel time increase for travelling full.
	 * @param isOneWay				an array specifying whether each road is one-lane.
	 * @param numRoutes				the number of routes.
	 * @param routeRoads			a 2D array listing the roads comprising each route.
	 * @param routeDirections		a 2D array listing the directions travelled on each road in each route.
	 * @param routeLengths			an array of the number of roads in each route.
	 * @param routeCrushers			an array of the crusher at the start of each route.
	 * @param routeShovels			an array of the shovel at the end of each route.
	 */
	public LPFlowConstructor(int numTrucks, int numCrushers, int numShovels, int numRoads, double[] emptyTimesMean, double[] fillTimesMean, 
		double[] roadTravelTimesMean, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections, 
		int[] routeLengths, int[] routeCrushers, int[] routeShovels) {
		this.numTrucks = numTrucks;
		this.numShovels = numShovels;
		this.numCrushers = numCrushers;
		this.numRoads = numRoads;
		this.emptyTimesMean = emptyTimesMean;
		this.fillTimesMean = fillTimesMean;
		this.roadTravelTimesMean = roadTravelTimesMean;
		this.fullSlowdown = fullSlowdown;
		this.isOneWay = isOneWay;
		this.numRoutes = numRoutes;
		this.routeRoads = routeRoads;
		this.routeDirections = routeDirections;
		this.routeLengths = routeLengths;
		this.routeCrushers = routeCrushers;
		this.routeShovels = routeShovels;

		routesFromCrusher = new IntList[numCrushers];
		routesFromShovel = new IntList[numShovels];
		routesOnRoad = new IntList[numRoads];
		for (int i=0; i<numCrushers; i++) {
			routesFromCrusher[i] = new IntList();
		}
		for (int i=0; i<numShovels; i++) {
			routesFromShovel[i] = new IntList();
		}
		for (int i=0; i<numRoads; i++) {
			routesOnRoad[i] = new IntList();
		}
		for (int i=0; i<numRoutes; i++) {
			routesFromCrusher[routeCrushers[i]].add(i);
			routesFromShovel[routeShovels[i]].add(i);
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				int dir = routeDirections[i][j];
				routesOnRoad[road].add((dir == 0 ? i + 1 : -1 - i));
			}
		}

		oneWayRestriction = true;
		boolean[][] roadSuppliesShovel = new boolean[numRoads][numShovels];
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				roadSuppliesShovel[road][routeShovels[i]] = true;
			}
		}
		maxExpRoadFlow = new double[numRoads];
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<numShovels; j++) {
				if (roadSuppliesShovel[i][j]) {
					maxExpRoadFlow[i] += 1.0 / fillTimesMean[j];
				}
			}
		}
	}

	/**
	 * Set the one way restriction.
	 * If set to true, one-lane road access will be restricted to a single direction,
	 * guaranteeing no waiting times at traffic lights.
	 *
	 * @param	oneWayRestriction	whether to enable the one way restriction.
	 * @return	this object.
	 */
	public LPFlowConstructor setOneWayRestriction(boolean oneWayRestriction) {
		this.oneWayRestriction = oneWayRestriction;
		return this;
	}

	/**
	 * Get the 'optimal' flow based on simulation parameters.
	 * The LP model will be saved to a file if a non-null model name is provided,
	 * or if the model fails to solve.
	 *
	 * @param	modelName	the file prefix to save,
	 *						a null value will output no file by default.
	 * @return	the calculated flow along each route in both directions.
	 */
	public double[][] getFlow(String modelName) {
		String modelFile = (modelName == null ? "fmodel.lp" : String.format("%s.lp",modelName));
		try {
			int[][] routeVariables = new int[numRoutes][2];
			LpSolve lp = getLPModel(routeVariables);
			int result = -1;
			for (int i=0; i<1000; i++) {
				result = lp.solve();
				if (result == 0) {
					break;
				}
			}
			if (result != 0) {
				lp.writeLp(modelFile);
				throw new LpSolveException(String.format("LP model could not be solved: %d",result));
			}
			double[][] flow = new double[numRoutes][2];
			double[] vars = lp.getPtrVariables();
			for (int i=0; i<numRoutes; i++) {
				for (int j=0; j<2; j++) {
					flow[i][j] = vars[routeVariables[i][j]];
				}
			}
			if (modelName != null) {
				lp.writeLp(modelFile);
			}
			return flow;
		}
		catch (LpSolveException lse) {
			throw new RuntimeException(String.format("An unknown LP error occurred: %s",lse.getMessage()));
		}
	}

	/**
	 * Get the LP model to solve for 'optimal' flow.
	 *
	 * @param	routeVariables	a 2D int array,
	 *							(numRoutes by 2).
	 *							the indexes to access the flow variables will be stored in this array.
	 * @return	an LpSolve object from the external LPSolve library.
	 * @throws	LpSolveException if an error occurs when interacting with the external LPSolve library.
	 */
	public LpSolve getLPModel(int[][] routeVariables) throws LpSolveException {
		if (oneWayRestriction) {
			return getLPModelRestrict(routeVariables);
		}
		else {
			return getLPModelScale(routeVariables);
		}
	}

	/**
	 * Get the LP model to solve for 'optimal' flow,
	 * without using the one way restriction.
	 * Expected waiting times on one-lane roads are estimated using an approximation,
	 * assuming the flow is equal to the sum of flows for shovels supplied by that road.
	 *
	 * @param	routeVariables	a 2D int array,
	 *							(numRoutes by 2).
	 *							the indexes to access the flow variables will be stored in this array.
	 * @return	an LpSolve object from the external LPSolve library.
	 * @throws	LpSolveException if an error occurs when interacting with the external LPSolve library.
	 */
	private LpSolve getLPModelScale(int[][] routeVariables) throws LpSolveException {
		int numVars = numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1;
		LpSolve lp = LpSolve.makeLp(0,numVars);
		for (int i=0; i<numCrushers; i++) {
			lp.setColName(i + 1,String.format("C_%d",i));
		}
		for (int i=0; i<numShovels; i++) {
			lp.setColName(numCrushers + 1 + i,String.format("S_%d",i));
		}
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				lp.setColName(numCrushers + numShovels + 1 + 2 * i + j,String.format("Rd_%d_%d",i,j));
			}
		}
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<2; j++) {
				lp.setColName(numCrushers + numShovels + 2 * numRoads + 1 + 2 * i + j,String.format("Rt_%d_%d",i,j));
				routeVariables[i][j] = numCrushers + numShovels + 2 * numRoads + 2 * i + j;
			}
		}
		lp.setColName(numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1,"T");
		lp.setAddRowmode(false);
		double[] objmult = new double[numCrushers + 1];
		int[] objvars = new int[numCrushers + 1];
		for (int i=0; i<numCrushers; i++) {
			objmult[i] = LARGECONSTANT;
			objvars[i] = i + 1;
		}
		objmult[numCrushers] = -1;
		objvars[numCrushers] = numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1;
		lp.setObjFnex(numCrushers + 1,objmult,objvars);
		lp.setMaxim();
		lp.setVerbose(LpSolve.NEUTRAL);
		lp.setAddRowmode(true);
		double[] mult;
		int[] vars;
		for (int i=0; i<numCrushers; i++) {
			mult = new double[]{1};
			vars = new int[]{i + 1};
			lp.addConstraintex(1,mult,vars,LpSolve.LE,1.0 / emptyTimesMean[i]);
			int availableRoutes = routesFromCrusher[i].size();
			HashSet<Integer> roadsOut = new HashSet<>();
			for (int j=0; j<availableRoutes; j++) {
				int route = routesFromCrusher[i].get(j);
				int road = routeRoads[route][0];
				int dir = routeDirections[route][0];
				roadsOut.add(road + dir * numRoads);
			}
			int availableRoads = roadsOut.size();
			for (int j=0; j<2; j++) {
				mult = new double[1 + availableRoads];
				vars = new int[1 + availableRoads];
				mult[0] = 1;
				vars[0] = i + 1;
				int k = 0;
				for (Integer r : roadsOut) {
					int road = r % numRoads;
					int dir = r / numRoads;
					if (j == 1) {
						dir = 1 - dir;
					}
					mult[k + 1] = -1;
					vars[k + 1] = numCrushers + numShovels + 1 + 2 * road + dir;
					k++;
				}
				lp.addConstraintex(1 + availableRoads,mult,vars,LpSolve.EQ,0);
			}
		}
		for (int i=0; i<numShovels; i++) {
			mult = new double[]{1};
			vars = new int[]{numCrushers + i + 1};
			lp.addConstraintex(1,mult,vars,LpSolve.LE,1.0 / fillTimesMean[i]);
			int availableRoutes = routesFromShovel[i].size();
			HashSet<Integer> roadsOut = new HashSet<>();
			for (int j=0; j<availableRoutes; j++) {
				int route = routesFromShovel[i].get(j);
				int last = routeLengths[route] - 1;
				int road = routeRoads[route][last];
				int dir = routeDirections[route][last];
				roadsOut.add(road + dir * numRoads);
			}
			int availableRoads = roadsOut.size();
			for (int j=0; j<2; j++) {
				mult = new double[1 + availableRoads];
				vars = new int[1 + availableRoads];
				mult[0] = 1;
				vars[0] = numCrushers + i + 1;
				int k = 0;
				for (Integer r : roadsOut) {
					int road = r % numRoads;
					int dir = r / numRoads;
					if (j == 1) {
						dir = 1 - dir;
					}
					mult[k + 1] = -1;
					vars[k + 1] = numCrushers + numShovels + 1 + 2 * road + dir;
					k++;
				}
				lp.addConstraintex(1 + availableRoads,mult,vars,LpSolve.EQ,0);
			}
		}
		for (int i=0; i<numRoads; i++) {
			int numUsing = routesOnRoad[i].size();
			for (int j=0; j<2; j++) {
				mult = new double[numUsing + 1];
				vars = new int[numUsing + 1];
				mult[0] = 1;
				vars[0] = numCrushers + numShovels + 1 + 2 * i + j;
				for (int k=0; k<numUsing; k++) {
					int routep = routesOnRoad[i].get(k);
					if (j == 1) {
						routep = -routep;
					}
					int dir = 0;
					if (routep < 0) {
						routep = -routep;
						dir = 1;
					}
					mult[k + 1] = -1;
					vars[k + 1] = numCrushers + numShovels + 2 * numRoads + 1 + 2 * (routep - 1) + dir;
				}
				lp.addConstraintex(1 + numUsing,mult,vars,LpSolve.EQ,0);
			}
		}
		mult = new double[numCrushers + numShovels + 2 * numRoutes + 1];
		vars = new int[numCrushers + numShovels + 2 * numRoutes + 1];
		for (int i=0; i<numCrushers; i++) {
			mult[i] = emptyTimesMean[i];
			vars[i] = i + 1;
		}
		for (int i=0; i<numShovels; i++) {
			mult[i + numCrushers] = fillTimesMean[i];
			vars[i + numCrushers] = numCrushers + i + 1;
		}
		mult[numCrushers + numShovels + 2 * numRoutes] = -1;
		vars[numCrushers + numShovels + 2 * numRoutes] = numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1;
		for (int i=0; i<numRoutes; i++) {
			double routeTime = 0;
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				double scale = 1;
				if (isOneWay[road]) {
					scale = (maxExpRoadFlow[road] > 0.5 / roadTravelTimesMean[road] ? 2 : 1 + 0.25 * maxExpRoadFlow[road] / (0.5 / 
						roadTravelTimesMean[road]));
				}
				routeTime += roadTravelTimesMean[road] * scale;
			}
			for (int j=0; j<2; j++) {
				mult[numCrushers + numShovels + 2 * i + j] = routeTime * (j == 0 ? 1 : fullSlowdown);
				vars[numCrushers + numShovels + 2 * i + j] = numCrushers + numShovels + 2 * numRoads + 1 + 2 * i + j;
			}
		}
		lp.addConstraintex(numCrushers + numShovels + 2 * numRoutes + 1,mult,vars,LpSolve.EQ,0);
		mult = new double[]{1};
		vars = new int[]{numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1};
		lp.addConstraintex(1,mult,vars,LpSolve.LE,numTrucks);
		lp.setAddRowmode(false);
		return lp;
	}

	/**
	 * Get the LP model to solve for 'optimal' flow,
	 * using the one way restriction.
	 * Travel on any one-lane road is restricted to a single direction.
	 *
	 * @param	routeVariables	a 2D int array,
	 *							(numRoutes by 2).
	 *							the indexes to access the flow variables will be stored in this array.
	 * @return	an LpSolve object from the external LPSolve library.
	 * @throws	LpSolveException if an error occurs when interacting with the external LPSolve library.
	 */
	private LpSolve getLPModelRestrict(int[][] routeVariables) throws LpSolveException {
		int numVars = numCrushers + numShovels + 3 * numRoads + 2 * numRoutes + 1;
		LpSolve lp = LpSolve.makeLp(0,numVars);
		for (int i=0; i<numCrushers; i++) {
			lp.setColName(i + 1,String.format("C_%d",i));
		}
		for (int i=0; i<numShovels; i++) {
			lp.setColName(numCrushers + 1 + i,String.format("S_%d",i));
		}
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				lp.setColName(numCrushers + numShovels + 1 + 2 * i + j,String.format("Rd_%d_%d",i,j));
			}
		}
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<2; j++) {
				lp.setColName(numCrushers + numShovels + 2 * numRoads + 1 + 2 * i + j,String.format("Rt_%d_%d",i,j));
				routeVariables[i][j] = numCrushers + numShovels + 2 * numRoads + 2 * i + j;
			}
		}
		lp.setColName(numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1,"T");
		for (int i=0; i<numRoads; i++) {
			lp.setColName(numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 2 + i,String.format("d_%d",i));
		}
		lp.setAddRowmode(false);
		double[] objmult = new double[numCrushers + 1];
		int[] objvars = new int[numCrushers + 1];
		for (int i=0; i<numCrushers; i++) {
			objmult[i] = LARGECONSTANT;
			objvars[i] = i + 1;
		}
		objmult[numCrushers] = -1;
		objvars[numCrushers] = numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1;
		lp.setObjFnex(numCrushers + 1,objmult,objvars);
		lp.setMaxim();
		lp.setVerbose(LpSolve.NEUTRAL);
		lp.setAddRowmode(true);
		double[] mult;
		int[] vars;
		for (int i=0; i<numCrushers; i++) {
			mult = new double[]{1};
			vars = new int[]{i + 1};
			lp.addConstraintex(1,mult,vars,LpSolve.LE,1.0 / emptyTimesMean[i]);
			int availableRoutes = routesFromCrusher[i].size();
			HashSet<Integer> roadsOut = new HashSet<>();
			for (int j=0; j<availableRoutes; j++) {
				int route = routesFromCrusher[i].get(j);
				int road = routeRoads[route][0];
				int dir = routeDirections[route][0];
				roadsOut.add(road + dir * numRoads);
			}
			int availableRoads = roadsOut.size();
			for (int j=0; j<2; j++) {
				mult = new double[1 + availableRoads];
				vars = new int[1 + availableRoads];
				mult[0] = 1;
				vars[0] = i + 1;
				int k = 0;
				for (Integer r : roadsOut) {
					int road = r % numRoads;
					int dir = r / numRoads;
					if (j == 1) {
						dir = 1 - dir;
					}
					mult[k + 1] = -1;
					vars[k + 1] = numCrushers + numShovels + 1 + 2 * road + dir;
					k++;
				}
				lp.addConstraintex(1 + availableRoads,mult,vars,LpSolve.EQ,0);
			}
		}
		for (int i=0; i<numShovels; i++) {
			mult = new double[]{1};
			vars = new int[]{numCrushers + i + 1};
			lp.addConstraintex(1,mult,vars,LpSolve.LE,1.0 / fillTimesMean[i]);
			int availableRoutes = routesFromShovel[i].size();
			HashSet<Integer> roadsOut = new HashSet<>();
			for (int j=0; j<availableRoutes; j++) {
				int route = routesFromShovel[i].get(j);
				int last = routeLengths[route] - 1;
				int road = routeRoads[route][last];
				int dir = routeDirections[route][last];
				roadsOut.add(road + dir * numRoads);
			}
			int availableRoads = roadsOut.size();
			for (int j=0; j<2; j++) {
				mult = new double[1 + availableRoads];
				vars = new int[1 + availableRoads];
				mult[0] = 1;
				vars[0] = numCrushers + i + 1;
				int k = 0;
				for (Integer r : roadsOut) {
					int road = r % numRoads;
					int dir = r / numRoads;
					if (j == 1) {
						dir = 1 - dir;
					}
					mult[k + 1] = -1;
					vars[k + 1] = numCrushers + numShovels + 1 + 2 * road + dir;
					k++;
				}
				lp.addConstraintex(1 + availableRoads,mult,vars,LpSolve.EQ,0);
			}
		}
		for (int i=0; i<numRoads; i++) {
			int numUsing = routesOnRoad[i].size();
			for (int j=0; j<2; j++) {
				mult = new double[numUsing + 1];
				vars = new int[numUsing + 1];
				mult[0] = 1;
				vars[0] = numCrushers + numShovels + 1 + 2 * i + j;
				for (int k=0; k<numUsing; k++) {
					int routep = routesOnRoad[i].get(k);
					if (j == 1) {
						routep = -routep;
					}
					int dir = 0;
					if (routep < 0) {
						routep = -routep;
						dir = 1;
					}
					mult[k + 1] = -1;
					vars[k + 1] = numCrushers + numShovels + 2 * numRoads + 1 + 2 * (routep - 1) + dir;
				}
				lp.addConstraintex(1 + numUsing,mult,vars,LpSolve.EQ,0);
			}
		}
		mult = new double[numCrushers + numShovels + 2 * numRoutes + 1];
		vars = new int[numCrushers + numShovels + 2 * numRoutes + numRoads + 1];
		for (int i=0; i<numCrushers; i++) {
			mult[i] = emptyTimesMean[i];
			vars[i] = i + 1;
		}
		for (int i=0; i<numShovels; i++) {
			mult[i + numCrushers] = fillTimesMean[i];
			vars[i + numCrushers] = numCrushers + i + 1;
		}
		for (int i=0; i<numRoutes; i++) {
			double routeTime = 0;
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				routeTime += roadTravelTimesMean[road];
			}
			for (int j=0; j<2; j++) {
				mult[numCrushers + numShovels + 2 * i + j] = routeTime * (j == 0 ? 1 : fullSlowdown);
				vars[numCrushers + numShovels + 2 * i + j] = numCrushers + numShovels + 2 * numRoads + 1 + 2 * i + j;
			}
		}
		mult[numCrushers + numShovels + 2 * numRoutes] = -1;
		vars[numCrushers + numShovels + 2 * numRoutes] = numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1;
		lp.addConstraintex(numCrushers + numShovels + 2 * numRoutes + 1,mult,vars,LpSolve.EQ,0);
		for (int i=0; i<numRoads; i++) {
			if (isOneWay[i]) {
				mult = new double[]{1,LARGECONSTANT};
				vars = new int[]{numCrushers + numShovels + 1 + 2 * i,numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 2 + i};
				lp.addConstraintex(2,mult,vars,LpSolve.LE,LARGECONSTANT);
				mult = new double[]{1,-LARGECONSTANT};
				vars = new int[]{numCrushers + numShovels + 1 + 2 * i + 1,numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 2 + i};
				lp.addConstraintex(2,mult,vars,LpSolve.LE,0);
			}
		}
		mult = new double[]{1};
		vars = new int[]{numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 1};
		lp.addConstraintex(1,mult,vars,LpSolve.LE,numTrucks);
		lp.setAddRowmode(false);
		for (int i=0; i<numRoads; i++) {
			if (isOneWay[i]) {
				lp.setBinary(numCrushers + numShovels + 2 * numRoads + 2 * numRoutes + 2 + i,true);
			}
		}
		return lp;
	}
	
}