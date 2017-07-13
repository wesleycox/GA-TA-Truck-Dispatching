import mines.sim.*;
import mines.util.*;
import mines.util.times.*;
import mines.system.*;
import mines.sol.*;
import mines.sol.flow.dispatch.*;
import mines.sol.ga.cycle.*;
import mines.sol.greedy.*;
import mines.sol.greedy.network.*;
import mines.sol.string.*;

public class Main {

	public static void main(String[] args) {
		if (args.length < 4) {
			throw new IllegalArgumentException(String.format("\nusage: ... Main filename numSamples runtime solIndex...\n" +
				"\tfilename the input file name\n" +
				"\tnumSamples the integer number of to run simulations per solution\n" +
				"\truntime the real-valued shift length per simulation\n" +
				"\tsolIndex a solution index between 0 and 5 (inclusive)\n"));
		}
		try {
			Main main = new Main();
			String file = args[0];
			int numSamples = Integer.parseInt(args[1]);
			double runtime = Double.parseDouble(args[2]);
			int[] solIndexes = new int[args.length - 3];
			for (int i=3; i<args.length; i++) {
				solIndexes[i - 3] = Integer.parseInt(args[i]);
			}
			main.run(file,solIndexes,numSamples,runtime);
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(String.format("\nusage: ... Main filename numSamples runtime solIndex...\n" +
				"\tfilename the input file name\n" +
				"\tnumSamples the integer number of to run simulations per solution\n" +
				"\truntime the real-valued shift length per simulation\n" +
				"\tsolIndex a solution index between 0 and 5 (inclusive)\n"));
		}
	}

	public void run(String file, int[] solIndexes, int numSamples, double runtime) {
		MineParameters params = new MineParameters(file);
		TimeDistribution tgen = new UniformTimes();
		MineSimulatorNarrow sim = new MineSimulatorNarrow(params,tgen);
		Debugger.setDebug(false);
		for (int solIndex : solIndexes) {
			Solution sol;
			System.out.printf("Preparing solution index %d...\n",solIndex);
			switch (solIndex) {
				case 0: {
					Debugger.setDebug(true);
					double spcProb = 0.9;
					double vmProb = 0.05;
					double inversionProb = 0.05;
					double swapProb = 0.0;
					double moveProb = 0.0;
					double insertProb = 0.05;
					double deleteProb = 0.05;
					int popSize = 100;
					int selectionSize = 200;
					double elitism = 0.1;
					int maxGen = 500;
					int bucketSize = 20;
					int resampleRate = 1;
					int resampleSize = 1;
					sol = new GACycleSolution(params,tgen,runtime)
						.setSinglePointXOProb(spcProb)
						.setValueMutationProb(vmProb)
						.setInversionProb(inversionProb)
						.setSwapMutationProb(swapProb)
						.setMoveMutationProb(moveProb)
						.setInsertionProb(insertProb)
						.setDeletionProb(deleteProb)
						.setPopulationParams(popSize,selectionSize)
						.setElitism(elitism)
						.setMaximumGeneration(maxGen)
						.setSamplingParams(bucketSize,resampleRate,resampleSize)
						.initialise();
					break;
				}
				case 1:
				case 2:
				case 3:
				case 4: {
					HeuristicKind[] hKinds = new HeuristicKind[]{HeuristicKind.MTCT,HeuristicKind.MTWT,HeuristicKind.MTST,HeuristicKind.MSWT};
					sol = new HeuristicSolution(params,hKinds[solIndex - 1],tgen,20);
					break;
				}
				case 5: {
					sol = new DISPATCHSolution(params);
					break;
				}
				default: {
					throw new IllegalArgumentException(String.format("Illegal solution index provided: %d",solIndex));
				}
			}
			System.out.printf("Preparing controller...\n");
			sim.loadController(sol.getController());
			double[] samples = new double[numSamples];
			double total = 0;
			Debugger.setDebug(false);
			System.out.printf("Beginning simulations...\n");
			for (int i=0; i<numSamples; i++) {
				sim.initialise();
				sim.simulate(runtime);
				samples[i] = sim.getEmpties();
				total += samples[i];
			}
			System.out.printf("%d simulations complete...\n",numSamples);
			double average = total / numSamples;
			double stdev = 0;
			for (int i=0; i<numSamples; i++) {
				stdev += (samples[i] - average) * (samples[i] - average);
			}
			stdev = Math.sqrt(stdev / numSamples);
			System.out.printf("%s : mean-%f sd-%f\n\n",sol.getSolutionName(),average,stdev);
		}
	}
	
}