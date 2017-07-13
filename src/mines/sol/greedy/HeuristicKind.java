package mines.sol.greedy;

/**
 * Enum for heuristic types.
 * Possible types are:
 * Minimise truck cycle time (MTCT),
 * Minimise truck service time (MTST),
 * Minimise truck waiting time (MTWT),
 * Minimise shovel waiting time (MSWT).
 *
 * For more information on these greedy heuristics see:
 *
 * Tan, S., & Ramani, R. V. (1992, February). 
 * Evaluation of computer truck dispatching criteria. 
 * In Proceedings of the SME/AIME annual meeting and exhibition, Arizona (pp. 192-215).
 */
public enum HeuristicKind {
	MTCT, MTST, MTWT, MSWT
}