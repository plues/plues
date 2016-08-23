package de.hhu.stups.plues.prob;

import java.util.List;
import java.util.Map;

public interface Solver {

  void checkModelVersion(final String expectedVersion) throws SolverException;

  void interrupt();

  /**
   * Check if a combination of major and minor courses is feasible.
   *
   * @param courses The combination of major and minor courses.
   * @return Return true if the combination is feasible otherwise false.
   */
  Boolean checkFeasibility(final String... courses);

  /**
   * Compute the {@link FeasibilityResult feasibility result} for a given combination of major and
   * minor courses.
   *
   * @param courses The combination of major and minor courses.
   * @return Return the computed {@link FeasibilityResult FeasibilityResult}.
   */
  FeasibilityResult computeFeasibility(final String... courses) throws SolverException;

  FeasibilityResult computePartialFeasibility(final List<String> courses,
                                              final Map<String, List<Integer>> moduleChoice,
                                              final List<Integer> abstractUnitChoice)
      throws SolverException;

  List<Integer> unsatCore(final String... courses) throws SolverException;

  void move(final String sessionId, final String day, final String slot);

  /**
   * A course is impossible if it is statically known to be infeasible.
   *
   * @return Return the set of all impossible courses.
   */
  java.util.Set<String> getImpossibleCourses() throws SolverException;

  List<Alternative> getLocalAlternatives(final int session, final String... courses)
      throws SolverException;

  /**
   * Get the model's version.
   *
   * @return String the version string of the model
   */
  String getModelVersion() throws SolverException;

  /**
   * Get the solver cache for testing.
   *
   * @return Return the solver cache containing computed results by the solver.
   */
  SolverCache getSolverResultCache();
}
