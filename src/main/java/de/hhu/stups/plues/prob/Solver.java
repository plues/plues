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

  /**
   * Compute if and how a list of courses might be feasible based on a partial setup of modules
   * and abstract units.
   *
   * @param courses            List of course keys as String
   * @param moduleChoice       map of course key to a set of module IDs already completed in that
   *                           course.
   * @param abstractUnitChoice List of abstract unit IDs already compleated
   * @return FeasiblityResult
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  FeasibilityResult computePartialFeasibility(final List<String> courses,
                                              final Map<String, List<Integer>> moduleChoice,
                                              final List<Integer> abstractUnitChoice)
      throws SolverException;

  /**
   * For a given list of course keys computes the session IDs in one of the unsat-cores
   *
   * @param courses String[] of course keys
   * @return a list of sessions IDs
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  List<Integer> unsatCore(final String... courses) throws SolverException;

  /**
   * Move a session identified by its ID to a new day and time slot.
   *
   * @param sessionId the ID of the Session
   * @param day       String day, valid values are "1".."7"
   * @param slot      Sting representing the selected time slot, valid values are "1".."8".
   */
  void move(final String sessionId, final String day, final String slot);

  /**
   * A course is impossible if it is statically known to be infeasible.
   *
   * @return Return the set of all impossible courses.
   */
  java.util.Set<String> getImpossibleCourses() throws SolverException;

  /**
   * Compute alternative slots for a given session ID, in the context of a specific
   * course combination.
   *
   * @param session ID of the session for which alternatives should be computed
   * @param courses List of courses
   * @return List of alternatives
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  List<Alternative> getLocalAlternatives(final int session, final String... courses)
      throws SolverException;

  /**
   * Get the model's version.
   *
   * @return String the version string of the model
   */
  String getModelVersion() throws SolverException;
}
