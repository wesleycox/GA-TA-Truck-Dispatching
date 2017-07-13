package mines.ga;

import mines.ga.chrom.*;
import mines.ga.op.*;
import mines.system.Debugger;
import java.util.*;

/**
 * A genetic algorithm for stochastic fitness functions.
 * Fitness is maintained as a rolling average,
 * by continuously adding fitness evaluations to a fitness bucket,
 * and removing old values from the bucket.
 */
public class RollingGeneticAlgorithm<E extends RollingChromosome> implements GeneticAlgorithm<E> {

	private Random rng;						//RNG.
	private FitnessFunction<E> ff;			//fitness function.
	private ChromosomeBuilder<E> cBuilder;	//random chromosome generator.
	private Comparator<E> comp;				//objective measure of 'goodness' of chromosomes.
	private GeneticOperator<E> operator;	//mutation and crossover operator.
	private SelectionOperator<E> selector;	//selection operator for survival.
	private int popSize;					//surviving population size
	private int selectionSize;				//number of offspring per generation
	private double elitism;					//portion of best chromosomes guaranteed to survive.
	private int maxGen;						//maximum number of generations.
	private int bucketSize;					//size of fitness bucket.
	private int resampleRate;				//number of generations between reevaluations.
	private int resampleSize;				//number of evaluations per reevaluation period.
	private boolean allowSurvivors;			//whether to allow non-elite chromosomes to survive between generations.
	private int conCutoff;					//number of generations allowed without improvement.

	private boolean initialised;	//whether this algorithm has been initialised or not.
	private int gen;				//the current generation number.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	ff			the fitness function.
	 * @param	cBuilder	the random chromosome generator.
	 * @param	comp		an objective comparator of chromosomes.
	 * @param	operator	the mutation and crossover operator.
	 * @param	selector	the selection operator for survival.
	 */
	public RollingGeneticAlgorithm(FitnessFunction<E> ff, ChromosomeBuilder<E> cBuilder, Comparator<E> comp, GeneticOperator<E> operator,
		SelectionOperator<E> selector) {
		rng = new Random();
		this.ff = ff;
		this.cBuilder = cBuilder;
		this.comp = comp;
		this.operator = operator;
		this.selector = selector;
		this.popSize = 100;
		this.selectionSize = popSize * 2;
		this.elitism = 0;
		this.maxGen = 999;
		this.conCutoff = maxGen + 1;
		this.bucketSize = 20;
		this.resampleRate = 1;
		this.resampleSize = 1;
		allowSurvivors = true;
		initialised = false;
	}

	/**
	 * Initialise this object for use.
	 * Can only be used once.
	 *
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public RollingGeneticAlgorithm<E> initialise() {
		if (!initialised) {
			RollingChromosome.setBucketSize(bucketSize);
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Set the population and offspring sizes.
	 * Can only be used before initialisation.
	 *
	 * @param	popSize			the population size.
	 * @param	selectionSize	the number of offspring per generation.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public RollingGeneticAlgorithm<E> setPopulationParams(int popSize, int selectionSize) {
		if (!initialised) {
			this.popSize = popSize;
			this.selectionSize = selectionSize;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Set the elitism value.
	 * Can only be used before initialisation.
	 *
	 * @param	elitism	the portion of best chromosomes to survive by elitism.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public RollingGeneticAlgorithm<E> setElitism(double elitism) {
		if (!initialised) {
			this.elitism = elitism;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Set the number of generations to run the GA.
	 * Early termination is disabled.
	 * Can only be used before initialisation.
	 *
	 * @param	maxGen		the maximum number of generations.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see		setMaximumGeneration(int,int)
	 */
	public RollingGeneticAlgorithm<E> setMaximumGeneration(int maxGen) {
		return setMaximumGeneration(maxGen,maxGen + 1);
	}

	/**
	 * Set the termination parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	maxGen		the maximum number of generations.
	 * @param	conCutoff	the number of generations allowed without improvement.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public RollingGeneticAlgorithm<E> setMaximumGeneration(int maxGen, int conCutoff) {
		if (!initialised) {
			this.maxGen = maxGen;
			this.conCutoff = conCutoff;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Set the resampling parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	bucketSize		the fitness bucket size.
	 * @param	resampleRate	the period between fitness resampling.
	 * @param	resampleSize	the number of reevaluations per resampling period.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public RollingGeneticAlgorithm<E> setSamplingParams(int bucketSize, int resampleRate, int resampleSize) {
		if (!initialised) {
			this.bucketSize = bucketSize;
			this.resampleRate = resampleRate;
			this.resampleSize = resampleSize;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Set whether to allow chromosomes to survive between generations.
	 * Can only be used before initialisation.
	 *
	 * @param	allowSurvivors	whether to allow non-elite chromosomes to survive between generations.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public RollingGeneticAlgorithm<E> setAllowSurvivors(boolean allowSurvivors) {
		if (!initialised) {
			this.allowSurvivors = allowSurvivors;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Run the genetic algorithm and return the best chromosome.
	 * The operation is as follows:
	 * The population is initialised randomly,
	 * and each initial chromosome has its fitness bucket filled.
	 * For each generation,
	 * mark the best chromosomes for survival by elitism,
	 * produce offspring by the genetic operator,
	 * and create a new population by selection and elitism.
	 * New chromosomes have their fitness buckets filled,
	 * and surviving chromosomes receive new evaluations every resampling period.
	 * The algorithm terminates if the maximum generation is reached,
	 * or no improvement is seen for several generations.
	 *
	 * @return	the best chromosome as determined by a comparator.
	 * @throws	IllegalStateException if not initialised.
	 */
	public E run() {
		if (initialised) {
			ArrayList<E> population = new ArrayList<>(popSize);
			for (int i=0; i<popSize; i++) {
				E rand = cBuilder.getRandomChromosome(rng);
				for (int j=0; j<bucketSize; j++) {
					rand.giveFitness(ff.getFitness(rand));
				}
				rand.incrementAge();
				population.add(rand);
			}
			Collections.sort(population,comp);
			E best = population.get(0);
			gen = 0;
			double bestFitness = best.getFitness();
			boolean maximising = ff.isMaximising();
			int conCount = 0;
			Debugger.print(String.format("%d-%s-%f\n",gen,best,ff.getDefaultFitness(best)));
			for (gen=1; gen<=maxGen; gen++) {
				ArrayList<E> nextPopulation = new ArrayList<>(popSize);
				int currentPopSize = population.size();
				int survive = Math.max(1,(int) (elitism * currentPopSize));
				for (int i=0; i<survive; i++) {
					E add = population.get(i);
					if (resampleRate > 0 && add.getAge() % resampleRate == 0) {
						for (int j=0; j<resampleSize; j++) {
							add.giveFitness(ff.getFitness(add));
						}
					}
					add.incrementAge();
					nextPopulation.add(add);
				}
				ArrayList<E> offspring = operator.performOperation(population,selectionSize);
				for (E e : offspring) {
					for (int i=0; i<bucketSize; i++) {
						e.giveFitness(ff.getFitness(e));
					}
					e.incrementAge();
				}
				if (allowSurvivors) {
					for (int i=survive; i<currentPopSize; i++) {
						E survivor = population.get(i);
						if (resampleRate > 0 && survivor.getAge() % resampleRate == 0) {
							for (int j=0; j<resampleSize; j++) {
								survivor.giveFitness(ff.getFitness(survivor));
							}
						}
						survivor.incrementAge();
						offspring.add(survivor);
					}
				}
				selector.loadPool(offspring);
				nextPopulation.addAll(selector.performSelection(popSize - survive));
				population = nextPopulation;
				Collections.sort(population,comp);
				best = population.get(0);
				if ((best.getFitness() > bestFitness) == maximising) {
					bestFitness = best.getFitness();
					conCount = 0;
				}
				else {
					conCount++;
					if (conCount >= conCutoff) {
						break;
					}
				}
				if (gen % 10 == 0) {
					Debugger.print(String.format("%d-%s-%f\n",gen,best,ff.getDefaultFitness(best)));
				}
			}
			Debugger.print(String.format("%d-%s-%f\n",gen,best,ff.getDefaultFitness(best)));
			return best;
		}
		else {
			throw new IllegalStateException("Algorithm not initialised");
		}
	}

	/**
	 * Get the number of generations of the last run.
	 *
	 * @return	the number of generations.
	 */
	public int getGenerations() {
		return gen;
	}

}