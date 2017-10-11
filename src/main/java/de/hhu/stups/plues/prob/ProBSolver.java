package de.hhu.stups.plues.prob;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.types.BObject;
import de.prob.translator.types.Record;
import de.prob.translator.types.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ProBSolver implements Solver {
  private static final String CHECK = "check";
  private static final String CHECK_PARTIAL = "checkPartial";
  private static final String MOVE = "move";
  private static final String IMPOSSIBLE_COURSES = "getImpossibleCourses";
  private static final String UNSAT_CORE = "unsatCore";
  private static final String UNSAT_CORE_MODULES = "unsatCoreModules";
  private static final String UNSAT_CORE_ABSTRACT_UNITS = "unsatCoreAbstractUnits";
  private static final String UNSAT_CORE_GROUPS = "unsatCoreGroups";
  private static final String UNSAT_CORE_SESSIONS = "unsatCoreSessions";
  private static final String LOCAL_ALTERNATIVES = "localAlternatives";

  private static final String DEFAULT_PREDICATE = "1=1";
  private final StateSpace stateSpace;
  private final SolverCache<SolverResult> operationExecutionCache;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final CommandFactory commandFactory;
  private Trace trace;

  @Inject
  ProBSolver(final Api api, final CommandFactory commandFactory, @Assisted final String modelPath)
      throws SolverException {

    this.operationExecutionCache = new SolverCache<>(100);
    this.commandFactory = commandFactory;

    final long t1 = System.nanoTime();
    try {
      this.stateSpace = api.b_load(modelPath);
    } catch (final IOException | ModelTranslationError exception) {
      logger.error("Exception while loading model", exception);
      throw new SolverException(exception);
    }
    final long t2 = System.nanoTime();
    logger.info("Loaded machine in {} ms.", TimeUnit.NANOSECONDS.toMillis(t2 - t1));
    //
    new HashSet<>(this.stateSpace.getSubscribedFormulas())
        .forEach(it -> stateSpace.unsubscribe(this.stateSpace, it));


    final long t3 = System.nanoTime();
    this.trace = traceFrom(stateSpace);
    final long t4 = System.nanoTime();
    logger.info("Loaded trace in {} ms.", TimeUnit.NANOSECONDS.toMillis(t4 - t3));
  }

  private static String getFeasibilityPredicate(final String[] courses) {
    final String ccss = Arrays.stream(courses)
        .filter(it -> it != null && !"".equals(it))
        .map(it -> "\"" + it + "\"")
        .collect(Collectors.joining(", "));
    return String.format("ccss={%s}", ccss);
  }

  private Trace traceFrom(final StateSpace space) {
    Trace traceFromSpace = (Trace) space.asType(Trace.class);

    final long start = System.nanoTime();
    traceFromSpace = traceFromSpace.execute("$setup_constants");

    final long t = System.nanoTime();

    traceFromSpace = traceFromSpace.execute("$initialise_machine");
    final long end = System.nanoTime();

    logger.info("$setup_constants took {} ms.", TimeUnit.NANOSECONDS.toMillis(t - start));
    logger.info("$initialise_machine took {} ms.", TimeUnit.NANOSECONDS.toMillis(end - t));
    return traceFromSpace;
  }

  private SolverResult executeOperation(final String op, final String predicate)
      throws SolverException {

    final OperationPredicateKey key = new OperationPredicateKey(op, predicate);
    synchronized (operationExecutionCache) {
      final SolverResult cacheObject = operationExecutionCache.get(key);
      if (cacheObject != null) {
        logger.info("Solver cache hit for key {}", key);
        return cacheObject;
      }
    }

    final IEvalElement evalElement = stateSpace.getModel().parseFormula(predicate);
    final String stateId = trace.getCurrentState().getId();
    final GetOperationByPredicateCommandDelegate cmd
        = commandFactory.create(this.stateSpace, stateId, op, evalElement, 1);
    //
    final SolverResult solverResult = new SolverResult();

    try {
      stateSpace.execute(cmd);
      if (cmd.isInterrupted() || !cmd.isCompleted()) {
        solverResult.setState(ResultState.INTERRUPTED);

        logger.debug("RESULT {} {} = TIMEOUT/CANCEL // interrupted {} completed {}",
            op, predicate, cmd.isInterrupted(), cmd.isCompleted());
        return solverResult;
      } else if (cmd.hasErrors()) {
        solverResult.setState(ResultState.FAILED);
        cmd.getErrors().forEach(logger::error);
      } else {
        solverResult.setState(ResultState.SUCCEEDED);

        trace = trace.addTransitions(cmd.getNewTransitions());

        final List<BObject> res = getOperationReturnValue();
        solverResult.setValue(res);
      }

      synchronized (operationExecutionCache) {
        operationExecutionCache.put(key, solverResult);
      }
    } catch (final ProBError error) {
      logger.error("Error in ProB", error);
      throw new SolverException(error.getMessage());
    }


    logger.debug("RESULT {} {} = {}", op, predicate, solverResult);

    return solverResult;
  }

  private List<BObject> getOperationReturnValue() {
    final Transition trans = trace.getCurrentTransition();
    try {
      return trans.getTranslatedReturnValues();
    } catch (final BCompoundException exception) {
      logger.error("Compound Exception", exception.getCause());
      return Collections.emptyList();
    }
  }

  private BObject executeOperationWithOneResult(final String op, final String predicate)
      throws SolverException {

    final List<BObject> modelResult = executeOperationWithResult(op, predicate);

    if (modelResult.size() != 1) {
      throw new SolverException("Expected one result got " + modelResult.size());
    }
    return modelResult.get(0);
  }

  private BObject executeOperationWithOneResult(final String op) throws SolverException {
    return executeOperationWithOneResult(op, DEFAULT_PREDICATE);
  }

  @SuppressWarnings("unchecked")
  private List<BObject> executeOperationWithResult(
      final String op, final String predicate) throws SolverException {

    final SolverResult result = executeOperation(op, predicate);

    if (!result.succeeded()) {
      throw new SolverException("Could not execute operation " + op + " - " + predicate);
    }

    return result.getValue();
  }

  /**
   * Checks if the version of the loaded model is compatible with the version provided as parameter.
   * Currently strings must be an exact match.
   */
  @Override
  public final synchronized void checkModelVersion(final String expectedVersion)
      throws SolverException { /* or read properties here? */

    final String modelVersion = this.getModelVersion();
    if (modelVersion.equals(expectedVersion)) {
      return;
    }
    throw new SolverException("Incompatible model version numbers, expected " + expectedVersion
        + " but was " + modelVersion);
  }

  @Override
  public final void interrupt() {
    logger.info("Sending interrupt to state space");
    this.stateSpace.sendInterrupt();
  }

  /**
   * Undo the last move operation by going back on the {@link #trace}.
   */
  @Override
  public final synchronized void undoLastMoveOperation() {
    this.trace = trace.back();
  }

  /**
   * Redo the last move operation by going forward on the {@link #trace}.
   */
  @Override
  public final synchronized void redoLastMoveOperation() {
    this.trace = trace.forward();
  }

  // OPERATIONS

  /**
   * Check if a combination of major and minor courses is feasible.
   *
   * @param courses The combination of major and minor courses.
   * @return Return true if the combination is feasible otherwise false.
   */
  @Override
  public final synchronized Boolean checkFeasibility(final String... courses)
      throws SolverException {

    // if we have more than one course we check each separately before checking the combination
    // of courses
    boolean error = false;
    //
    if (courses.length > 1) {
      for (final String course : courses) {
        if (!checkOperation(course)) {
          error = true;
          break;
        }
      }
    }
    //
    error = error || !checkOperation(courses);
    if (error) {
      throw new SolverException("Could not execute operation " + CHECK + " - "
        + Arrays.toString(courses));
    }
    //
    return true;
  }

  private Boolean checkOperation(final String... courses) throws SolverException {
    final String predicate = getFeasibilityPredicate(courses);
    final SolverResult result = executeOperation(CHECK, predicate);

    return result.succeeded();
  }

  /**
   * Compute the {@link FeasibilityResult feasibility result} for a given combination of major and
   * minor courses.
   *
   * @param courses The combination of major and minor courses.
   * @return Return the computed {@link FeasibilityResult FeasibilityResult}.
   */
  @Override
  public final synchronized FeasibilityResult computeFeasibility(final String... courses)
      throws SolverException {

    final String predicate = getFeasibilityPredicate(courses);
    /* Check returns values in the following order:
     *  0: Semester choice - map from abstract unit to a semester
     *  1: Group choice - map from unit to the group chosen for each
     *  2: Module choice - set of modules
     *  3: Unit choice - map from abstract units to units
     */
    final List<BObject> modelResult = executeOperationWithResult(CHECK, predicate);

    //
    final Map<Integer, Integer> semesterChoice = Mappers.mapSemesterChoice(
        (Set) modelResult.get(0));

    final Map<Integer, Integer> groupChoice = Mappers.mapGroupChoice(
        (Set) modelResult.get(1));

    final Map<Integer, java.util.Set<Integer>> abstractUnitChoice
        = Mappers.mapAbstractUnitChoice((Set) modelResult.get(2));

    final Map<String, java.util.Set<Integer>> moduleChoice
        = Mappers.mapModuleChoice((Set) modelResult.get(3));
    //
    return new FeasibilityResult(moduleChoice, abstractUnitChoice, semesterChoice, groupChoice);
  }

  /**
   * Compute if and how a list of courses might be feasible based on a partial setup of modules and
   * abstract units.
   *
   * @param courses            List of course keys as String
   * @param moduleChoice       map of course key to a set of module IDs already completed in that
   *                           course.
   * @param abstractUnitChoice List of abstract unit IDs already completed
   * @return FeasibilityResult
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  @Override
  public final synchronized FeasibilityResult computePartialFeasibility(
      final List<String> courses, final Map<String, List<Integer>> moduleChoice,
      final Map<Integer, List<Integer>> abstractUnitChoice) throws SolverException {

    final String mc = Mappers.mapToModuleChoice(moduleChoice);
    final String ac = abstractUnitChoice.entrySet().stream()
        .flatMap(auc -> auc.getValue().stream()
            .map(value -> String.format("(mod%s, au%s)", auc.getKey(), value)))
        .collect(Collectors.joining(", "));

    final String predicate = getFeasibilityPredicate(courses.toArray(new String[0]))
        + " & partialModuleChoice=" + mc
        + " & partialAbstractUnitChoice={" + ac + "}";

    /* Check returns values in the following order:
     *  0: Semester choice - map from abstract unit to a semester
     *  1: Group choice - map from unit to the group chosen for each
     *  2: Module choice - set of modules
     *  3: Unit choice - map from abstract units to units
     */
    final List<BObject> modelResult = executeOperationWithResult(CHECK_PARTIAL, predicate);
    //
    final Map<Integer, Integer> computedSemesterChoice
        = Mappers.mapSemesterChoice((Set) modelResult.get(0));

    final Map<Integer, Integer> computedGroupChoice
        = Mappers.mapGroupChoice((Set) modelResult.get(1));

    final Map<Integer, java.util.Set<Integer>> computedAbstractUnitChoice
        = Mappers.mapAbstractUnitChoice((Set) modelResult.get(2));

    final Map<String, java.util.Set<Integer>> computedModuleChoice
        = Mappers.mapModuleChoice((Set) modelResult.get(3));
    //
    return new FeasibilityResult(computedModuleChoice, computedAbstractUnitChoice,
        computedSemesterChoice, computedGroupChoice);
  }

  /**
   * For a given list of course keys computes the session IDs in one of the unsat-cores
   *
   * @param courses String[] of course keys
   * @return a list of sessions IDs
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  @Override
  public final synchronized java.util.Set<Integer> unsatCore(final String... courses)
      throws SolverException {

    final String predicate = getFeasibilityPredicate(courses);
    //
    final Set uc = (Set) executeOperationWithOneResult(UNSAT_CORE, predicate);
    //
    return Mappers.mapSessions(uc);
  }

  @Override
  public final synchronized java.util.Set<Integer> unsatCoreModules(final String... courses)
      throws SolverException {

    final String predicate = getFeasibilityPredicate(courses);
    logger.info(predicate);
    //
    final Set uc = (Set) executeOperationWithOneResult(UNSAT_CORE_MODULES, predicate);
    //
    return Mappers.mapModules(uc);
  }

  @Override
  public final synchronized java.util.Set<Integer> unsatCoreAbstractUnits(
      final List<Integer> modules) throws SolverException {

    final String predicate
        = String.format("uc_modules={%s}",
        Mappers.mapToModules(modules).stream().collect(Collectors.joining(", ")));
    //
    final Set uc = (Set) executeOperationWithOneResult(UNSAT_CORE_ABSTRACT_UNITS, predicate);
    //
    return Mappers.mapAbstractUnits(uc);
  }

  @Override
  public final synchronized java.util.Set<Integer> unsatCoreGroups(
      final List<Integer> abstractUnits, final List<Integer> modules) throws SolverException {

    final String ucModules
        = Mappers.mapToModules(modules).stream().collect(Collectors.joining(", "));
    final String ucAbstractUnits
        = Mappers.mapToAbstractUnits(abstractUnits).stream().collect(Collectors.joining(", "));

    final String predicate
        = String.format("uc_modules={%s} & uc_abstract_units={%s}", ucModules, ucAbstractUnits);
    //
    final Set uc = (Set) executeOperationWithOneResult(UNSAT_CORE_GROUPS, predicate);
    //
    return Mappers.mapGroups(uc);
  }

  @Override
  public final synchronized java.util.Set<Integer> unsatCoreSessions(final List<Integer> groups)
      throws SolverException {

    final String predicate
        = String.format("uc_groups={%s}",
        Mappers.mapToGroups(groups).stream().collect(Collectors.joining(", ")));
    //
    final Set uc = (Set) executeOperationWithOneResult(UNSAT_CORE_SESSIONS, predicate);
    //
    return Mappers.mapSessions(uc);
  }

  /**
   * Move a session identified by its ID to a new day and time slot.
   *
   * @param sessionId the ID of the Session
   * @param day       String day, valid values are "1".."7"
   * @param slot      String representing the selected time slot, valid values are "1".."7".
   */
  @Override
  public final synchronized void move(final String sessionId,
                                      final String day, final String slot) throws SolverException {
    final String predicate
        = "session=session" + sessionId + " & dow=" + day + " & slot=slot" + slot;
    executeOperation(MOVE, predicate);
    operationExecutionCache.clear();
  }

  /**
   * A course is impossible if it is statically known to be infeasible.
   *
   * @return Return the set of all impossible courses.
   */
  @Override
  public final synchronized java.util.Set<String> getImpossibleCourses() throws SolverException {

    final Record result = (Record) this.executeOperationWithOneResult(IMPOSSIBLE_COURSES);

    if (logger.isDebugEnabled()) {
      logger.debug(result.toString());
    }
    return Mappers.mapCourseSet((Set) result.get("courses"));
  }

  /**
   * Compute alternative slots for a given session ID, in the context of a specific course
   * combination.
   *
   * @param session ID of the session for which alternatives should be computed
   * @param courses List of courses
   * @return List of alternatives
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  @Override
  public final synchronized List<Alternative> getLocalAlternatives(
      final int session, final String... courses) throws SolverException {

    final String coursePredicate = getFeasibilityPredicate(courses);
    final String predicate = coursePredicate + " & session=" + Mappers.mapSession(session);
    final Set modelResult = (Set) this.executeOperationWithOneResult(LOCAL_ALTERNATIVES, predicate);

    return Mappers.mapAlternatives(modelResult);
  }


  /**
   * Extract all computed data that is useful for generating reports from the model.
   *
   * @return ReportData
   * @throws SolverException if there is an error fetching the report data.
   */
  @Override
  public final synchronized ReportData getReportingData() throws SolverException {
    final Record data = (Record) this.executeOperationWithOneResult("getReportingData");

    final ReportData report = new ReportData();

    report.setImpossibleCourseModuleAbstractUnits(
        Mappers.mapCourseModuleAbstractUnits((Set) data.get("impossible_course_abstract_units")));

    report.setImpossibleCourses(Mappers.mapCourseSet((Set) data.get("impossible_courses")));
    report.setImpossibleCoursesBecauseOfImpossibleModules(Mappers.mapCourseSet(
        (Set) data.get("impossible_courses_because_of_impossible_modules")));

    report.setImpossibleCourseModuleAbstractUnitPairs(
        Mappers.mapCourseModuleAbstractUnitPairs(
            (Set) data.get("impossible_courses_module_combinations")));

    report.setImpossibleAbstractUnitsInModule(
        Mappers.mapModuleAbstractUnitPairs((Set) data.get("impossible_module_abstract_unit")));

    report.setIncompleteModules(
        Mappers.extractModules((Set) data.get("incomplete_modules")));

    report.setMandatoryModules(Mappers.mapModuleChoice((Set) data.get("mandatory_modules")));

    report.setQuasiMandatoryModuleAbstractUnits(
        Mappers.mapQuasiMandatoryModuleAbstractUnits(
            (Set) data.get("quasi_mandatory_module_abstract_units")));

    report.setRedundantUnitGroups(Mappers.mapUnitGroups((Set) data.get("redundant_unit_groups")));

    report.setImpossibleModulesBecauseOfMissingElectiveAbstractUnits(
        Mappers.mapModules(
            (Set) data.get("impossible_modules_because_of_missing_elective_abstract_units")));

    report.setImpossibleCoursesBecauseOfImpossibleModuleCombinations(
        Mappers.mapCourseSet(
            (Set) data.get("impossible_courses_because_of_impossible_module_combinations")));

    report.setModuleAbstractUnitUnitSemesterConflicts(
        Mappers.mapModuleAbstractUnitUnitSemesterMismatch(
            (Set) data.get("module_abstract_unit_unit_semester_mismatch")));

    report.setImpossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits(
        Mappers.mapQuasiMandatoryModuleAbstractUnits((Set) data.get(
            "impossible_modules_because_of_incomplte_quasi_mandatory_abstract_units")));

    report.setGroupsWithInnerConflicts(
        Mappers.mapGroups((Set) data.get("groups_with_inner_conflicts")));

    return report;
  }


  /**
   * Get the model's version.
   *
   * @return String the version string of the model
   */
  @SuppressWarnings("WeakerAccess")
  @Override
  public final synchronized String getModelVersion() throws SolverException {
    final BObject result = this.executeOperationWithOneResult("getVersion");
    return Mappers.mapString(result.toString());
  }

  /**
   * Get the solver's operation execution cache for testing.
   *
   * @return Return the solver cache containing boolean values for executed operations.
   */
  final SolverCache getOperationExecutionCache() {
    return this.operationExecutionCache;
  }
}
