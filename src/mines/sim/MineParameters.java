package mines.sim;

import java.util.*;
import java.io.*;

/**
 * Structure for simulation parameters.
 */
public class MineParameters {

	/*
	 * Simulation parameters.
	 */
	private int numTrucks;						//number of trucks.
	private final int numShovels;				//number of shovels.
	private final int numCrushers;				//number of crushers.
	private final double[] emptyTimesMean;		//average emptying times for each crusher.
	private final double[] emptyTimesSD;		//standard deviations of emptying times for each crusher.
	private final double[] fillTimesMean;		//average filling times for each shovel.
	private final double[] fillTimesSD;			//standard deviations of filling times for each shovel.
	private final double[][] travelTimesMean;	//average travelling times to each shovel from each crusher.
	private final double[][] travelTimesSD;		//standard deviations of travelling time to each shovel from each crusher.

	/**
	 * Initialise the parameters from an input file.
	 *
	 * Input files are of the format:
	 *	First line:
	 *		T NT
	 *		where NT is the number of trucks.
	 *	Second line:
	 *		C NC
	 *		where NC is the number of crushers.
	 *	NC lines of:
	 *		EM ESD
	 *		where for the nth line, 
	 *		EM is mean emptying time for the nth crusher,
	 *		and ESD is the standard deviation.
	 *	One line of:
	 *		S NS
	 *		where NS is the number of shovels.
	 *	NS lines with NC + 1 pairs of values:
	 *		where for the mth pair of the nth line,
	 *		the first value is the mean travel time to the nth shovel from the mth crusher,
	 *		and the second value is the standard deviation.
	 *		the final pair is the mean and standard deviation of the filling time for the nth shovel.
	 * Any deviation from this format will result in error.
	 *
	 * @param	input	the input filename
	 * @throws	IllegalArgumentException 	if the input file is incorrectly formatted,
	 *										or could not be opened.
	 */
	public MineParameters(String input) {
		try {
			Scanner in = new Scanner(new File(input));
			String[] line = in.nextLine().split(" ");
			if (line.length == 2 && line[0].equals("T")) {
				numTrucks = Integer.parseInt(line[1]);
			}
			else {
				throw new IllegalArgumentException("The input file format is invalid");
			}
			line = in.nextLine().split(" ");
			if (line.length == 2 && line[0].equals("C")) {
				numCrushers = Integer.parseInt(line[1]);
			}
			else {
				throw new IllegalArgumentException("The input file format is invalid");
			}
			emptyTimesMean = new double[numCrushers];
			emptyTimesSD = new double[numCrushers];
			for (int i=0; i<numCrushers; i++) {
				line = in.nextLine().split(" ");
				if (line.length == 2) {
					emptyTimesMean[i] = Double.parseDouble(line[0]);
					emptyTimesSD[i] = Double.parseDouble(line[1]);
				}
				else {
					throw new IllegalArgumentException("The input file format is invalid");
				}
			}
			line = in.nextLine().split(" ");
			if (line.length == 2 && line[0].equals("S")) {
				numShovels = Integer.parseInt(line[1]);
			}
			else {
				throw new IllegalArgumentException("The input file format is invalid");
			}
			fillTimesMean = new double[numShovels];
			fillTimesSD = new double[numShovels];
			travelTimesMean = new double[numCrushers][numShovels];
			travelTimesSD = new double[numCrushers][numShovels];
			for (int i=0; i<numShovels; i++) {
				line = in.nextLine().split(" ");
				if (line.length == 2 * numCrushers + 2) {
					for (int j=0; j<numCrushers; j++) {
						travelTimesMean[j][i] = Double.parseDouble(line[2 * j]);
						travelTimesSD[j][i] = Double.parseDouble(line[2 * j + 1]);
					}
					fillTimesMean[i] = Double.parseDouble(line[2 * numCrushers]);
					fillTimesSD[i] = Double.parseDouble(line[2 * numCrushers + 1]);
				}
				else {
					throw new IllegalArgumentException("The input file format is invalid");
				}
			}
		}
		catch (FileNotFoundException fnfe) {
			throw new IllegalArgumentException("The input file could not be found");
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("The input file format is invalid");
		}
		catch (IndexOutOfBoundsException ioobe) {
			throw new IllegalArgumentException("The input file format is invalid");
		}
		catch (NoSuchElementException nsee) {
			throw new IllegalArgumentException("The input file format is invalid");
		}
	}

	/**
	 * Get the number of trucks.
	 *
	 * @return	the number of trucks.
	 */
	public int getNumTrucks() {
		return numTrucks;
	}

	/**
	 * Set the number of trucks.
	 *
	 * @param	numTrucks	the new number of trucks.
	 */
	public void setNumTrucks(int numTrucks) {
		this.numTrucks = numTrucks;
	}

	/**
	 * Get the number of shovels.
	 *
	 * @return	the number of shovels.
	 */
	public int getNumShovels() {
		return numShovels;
	}

	/**
	 * Get the number of crushers.
	 *
	 * @return	the number of crushers.
	 */
	public int getNumCrushers() {
		return numCrushers;
	}

	/**
	 * Get the mean emptying time of one crusher.
	 *
	 * @param	the index of the crusher.
	 * @return	the mean emptying time.
	 */
	public double getMeanEmptyTime(int cid) {
		return emptyTimesMean[cid];
	}

	/**
	 * Get the mean emptying time of all crushers.
	 *
	 * @return	an array of mean emptying times.
	 */
	public double[] getMeanEmptyTimes() {
		return Arrays.copyOf(emptyTimesMean,numCrushers);
	}

	/**
	 * Get the emptying time standard deviation of one crusher.
	 *
	 * @param	the index of the crusher.
	 * @return	the emptying time SD.
	 */
	public double getEmptyTimeSD(int cid) {
		return emptyTimesSD[cid];
	}

	/**
	 * Get the emptying time standard deviation of all crushers.
	 *
	 * @return	an array of emptying time SDs.
	 */
	public double[] getEmptyTimesSD() {
		return Arrays.copyOf(emptyTimesSD,numCrushers);
	}

	/**
	 * Get the mean filling time of one shovel.
	 *
	 * @param	the index of the shovel.
	 * @return	the mean filling time.
	 */
	public double getMeanFillTime(int sid) {
		return fillTimesMean[sid];
	}

	/**
	 * Get the mean filling time of all shovels.
	 *
	 * @return	an array of mean filling times.
	 */
	public double[] getMeanFillTimes() {
		return Arrays.copyOf(fillTimesMean,numShovels);
	}

	/**
	 * Get the filling time standard deviation of one shovel.
	 *
	 * @param	the index of the shovel.
	 * @return	the filling time standard deviation.
	 */
	public double getFillTimeSD(int sid) {
		return fillTimesSD[sid];
	}

	/**
	 * Get the filling time standard deviation of all shovels.
	 *
	 * @return	an array of filling time SDs.
	 */
	public double[] getFillTimesSD() {
		return Arrays.copyOf(fillTimesSD,numShovels);
	}

	/**
	 * Get the mean travelling time between a crusher shovel pair.
	 *
	 * @param	cid	the crusher index.
	 * @param	sid	the shovel index.
	 * @return	the mean travel time between the crusher and shovel.
	 */
	public double getMeanTravelTime(int cid, int sid) {
		return travelTimesMean[cid][sid];
	}

	/**
	 * Get the mean travelling times to all shovels from a crusher.
	 *
	 * @param	cid	the crusher index.
	 * @return	an array of mean travel times from the crusher.
	 */
	public double[] getMeanTravelTimes(int cid) {
		return Arrays.copyOf(travelTimesMean[cid],numShovels);
	}

	/**
	 * Get the travelling time standard deviation between a crusher shovel pair.
	 *
	 * @param	cid	the crusher index.
	 * @param	sid	the shovel index.
	 * @return	the travel time SD between the crusher and shovel.
	 */
	public double getTravelTimeSD(int cid, int sid) {
		return travelTimesSD[cid][sid];
	}

	/**
	 * Get the travelling time standard deviations to all shovels from a crusher.
	 *
	 * @param	cid	the crusher index.
	 * @return	an array of travel time SDs from the crusher.
	 */
	public double[] getTravelTimesSD(int cid) {
		return Arrays.copyOf(travelTimesSD[cid],numShovels);
	}
}