package de.hhu.stups.plues.prob;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;
import de.prob.translator.types.Record;
import de.prob.translator.types.Set;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProBSolver implements Solver {
  private static final String CHECK = "check";
  private static final String CHECK_PARTIAL = "checkPartial";
  private static final String MOVE = "move";
  private static final String IMPOSSIBLE_COURSES = "getImpossibleCourses";
  private static final String UNSAT_CORE = "unsatCore";
  private static final String LOCAL_ALTERNATIVES = "localAlternatives";

  private static final String DEFAULT_PREDICATE = "1=1";
  private final StateSpace stateSpace;
  private final SolverCache solverResultCache;
  private final SolverCache operationExecutionCache;
  private Trace trace;

  @Inject
  ProBSolver(final Api api, @Assisted final String modelPath)
      throws IOException, BException {
    this.stateSpace = api.b_load(modelPath);
    this.stateSpace.getSubscribedFormulas()
      .forEach(it -> stateSpace.unsubscribe(this.stateSpace, it));
    this.solverResultCache = new SolverCache(100);
    this.operationExecutionCache = new SolverCache(100);
    this.trace = traceFrom(stateSpace);
  }

  private static Trace traceFrom(final StateSpace space) {
    return ((Trace) space.asType(Trace.class))
      .execute("$setup_constants")
      .execute("$initialise_machine");
  }

  private static String getFeasibilityPredicate(final String[] courses) {
    final Iterator<String> iterator = Arrays.stream(courses)
        .filter(it -> it != null && !it.equals(""))
        .map(it -> "\"" + it + "\"").iterator();
    return "ccss={" + Joiner.on(", ").join(iterator) + "}";
  }

  /**
   * Checks if the version of the loaded model is compatible with the version
   * provided as parameter.
   * Currently strings must be an exact match.
   */
  public final void checkModelVersion(final String expectedVersion) /* or read properties here? */
      throws SolverException {
    final String modelVersion = this.getModelVersion();
    if (modelVersion.equals(expectedVersion)) {
      return;
    }
    throw new SolverException(
      "Incompatible model version numbers, expected "
        + expectedVersion
        + " but was " + modelVersion);

  }

  private Boolean executeOperation(final String op, final String predicate) {

    final String key = op + predicate;
    synchronized (operationExecutionCache) {
      final Boolean cacheObject = (Boolean) operationExecutionCache.get(key);
      if (cacheObject != null) {
        return cacheObject;
      }
    }

    final boolean result;
    if (this.trace.canExecuteEvent(op, predicate)) {
      this.trace = this.trace.execute(op, predicate);
      result = true;
    } else {
      result = false;
    }

    synchronized (operationExecutionCache) {
      operationExecutionCache.put(key,result);
    }

    return result;
  }

  private <T extends BObject> T executeOperationWithOneResult(final String op,
      final String predicate, final Class<T> type) throws SolverException {

    final List<T> modelResult = executeOperationWithResult(op, predicate, type);

    assert modelResult.size() == 1;
    return modelResult.get(0);
  }

  private <T extends BObject> T executeOperationWithOneResult(final String op, final Class<T> type)
      throws SolverException {

    return executeOperationWithOneResult(op, DEFAULT_PREDICATE, type);
  }

  @SuppressWarnings("unchecked")
  private <T extends BObject> List<T> executeOperationWithResult(final String op,
      final String predicate, final Class<T> type) throws SolverException {

    final String key = op + predicate;
    synchronized (solverResultCache) {
      final List<T> cacheObject = (List<T>) solverResultCache.get(key);
      if (cacheObject != null) {
        return cacheObject;
      }
    }

    if (!executeOperation(op, predicate)) {
      throw new SolverException("Could not execute operation " + op + " - " + predicate);
    }

    final Transition trans = trace.getCurrentTransition();
    final List<String> returnValues = trans.evaluate(FormulaExpand.expand).getReturnValues();

    List<T> result = returnValues.stream().map(i -> {
      try {
        return type.cast(Translator.translate(i));
      } catch (BException bexception) {
        bexception.printStackTrace();
      }
      return null;
    }).collect(Collectors.toList());

    synchronized (solverResultCache) {
      solverResultCache.put(key,result);
    }

    return result;
  }

  public final void interrupt() {
    System.out.println("Sending interrupt to state space");
    this.stateSpace.sendInterrupt();
  }

  // OPERATIONS

  /**
   * Check if a combination of major and minor courses is feasible.
   *
   * @param courses The combination of major and minor courses.
   * @return Return true if the combination is feasible otherwise false.
   */
  public final Boolean checkFeasibility(final String... courses) {

    final String predicate = getFeasibilityPredicate(courses);
    return executeOperation(CHECK, predicate);
  }

  /**
   * Compute the {@link FeasibilityResult feasibility result} for a given combination of major and
   * minor courses.
   *
   * @param courses The combination of major and minor courses.
   * @return Return the computed {@link FeasibilityResult FeasibilityResult}.
   */
  public final FeasibilityResult computeFeasibility(final String... courses)
      throws SolverException {

    final String predicate = getFeasibilityPredicate(courses);
    /* Check returns values in the following order:
     *  0: Semester choice - map from abstract unit to a semester
     *  1: Group choice - map from unit to the group chosen for each
     *  2: Module choice - set of modules
     *  3: Unit choice - map from abstract units to units
     */
    final List<Set> modelResult = executeOperationWithResult(CHECK, predicate, Set.class);
    //
    final Map<Integer, Integer> semesterChoice = Mappers.mapSemesterChoice(modelResult.get(0));

    final Map<Integer, Integer> groupChoice = Mappers.mapGroupChoice(modelResult.get(1));

    final Map<String, java.util.Set<Integer>> moduleChoice
        = Mappers.mapModuleChoice(modelResult.get(2));

    final Map<Integer, Integer> unitChoice = Mappers.mapUnitChoice(modelResult.get(3));
    //
    return new FeasibilityResult(moduleChoice, unitChoice,
        semesterChoice, groupChoice);
  }

  public final FeasibilityResult computePartialFeasibility(final List<String> courses,
      final Map<String, List<Integer>> moduleChoice, final List<Integer> abstractUnitChoice)
      throws SolverException {

    final String mc = Mappers.mapToModuleChoice(moduleChoice);
    final String ac = Joiner.on(',').join(
        abstractUnitChoice.stream().map(i -> "au" + i).iterator());

    final String predicate = getFeasibilityPredicate(courses.toArray(new String[0]))
        + " & partialModuleChoice=" + mc
        + " & partialAbstractUnitChoice={" + ac + "}";

    /* Check returns values in the following order:
     *  0: Semester choice - map from abstract unit to a semester
     *  1: Group choice - map from unit to the group chosen for each
     *  2: Module choice - set of modules
     *  3: Unit choice - map from abstract units to units
     */
    final List<Set> modelResult = executeOperationWithResult(CHECK_PARTIAL, predicate, Set.class);
    //
    final Map<Integer, Integer> computedSemesterChoice
        = Mappers.mapSemesterChoice(modelResult.get(0));

    final Map<Integer, Integer> computedGroupChoice
        = Mappers.mapGroupChoice(modelResult.get(1));

    final Map<String, java.util.Set<Integer>> computedModuleChoice
        = Mappers.mapModuleChoice(modelResult.get(2));

    final Map<Integer, Integer> computedUnitChoice
        = Mappers.mapUnitChoice(modelResult.get(3));
    //
    return new FeasibilityResult(computedModuleChoice, computedUnitChoice,
      computedSemesterChoice, computedGroupChoice);
  }

  public final List<Integer> unsatCore(final String... courses) throws SolverException {

    final String predicate = getFeasibilityPredicate(courses);
    //
    final Set uc = executeOperationWithOneResult(UNSAT_CORE, predicate, Set.class);
    //
    return Mappers.mapSessions(uc);
  }


  public final void move(final String sessionId,
                         final String day, final String slot) {
    final String predicate
        = "session=session" + sessionId + " & dow=" + day + " & slot=slot" + slot;
    executeOperation(MOVE, predicate);
    solverResultCache.clear();
  }

  /**
   * A course is impossible if it is statically known to be infeasible.
   *
   * @return Return the set of all impossible courses.
   */
  public final java.util.Set<String> getImpossibleCourses() throws SolverException {

    final Record result = this.executeOperationWithOneResult(IMPOSSIBLE_COURSES, Record.class);

    return Mappers.mapCourseSet((Set) result.get("courses"));
  }

  public final List<Alternative> getLocalAlternatives(final int session, final String... courses)
      throws SolverException {

    final String coursePredicate = getFeasibilityPredicate(courses);
    final String predicate = coursePredicate + " & session=" + Mappers.mapSession(session);
    final Set modelResult = this.executeOperationWithOneResult(
        LOCAL_ALTERNATIVES, predicate, Set.class);

    return Mappers.mapAlternatives(modelResult);
  }


  /**
   * Get the model's version.
   *
   * @return String the version string of the model
   */
  @SuppressWarnings("WeakerAccess")
  public final String getModelVersion() throws SolverException {
    final BObject result = this.executeOperationWithOneResult("getVersion", BObject.class);
    return Mappers.mapString(result.toString());
  }

  /**
   * Get the solver cache for testing.
   *
   * @return Return the solver cache containing computed results by the solver.
   */
  public final SolverCache getSolverResultCache() {
    return this.solverResultCache;
  }

}
