package de.hhu.stups.plues.services;

import static javafx.concurrent.WorkerStateEvent.WORKER_STATE_CANCELLED;
import static javafx.concurrent.WorkerStateEvent.WORKER_STATE_FAILED;
import static javafx.concurrent.WorkerStateEvent.WORKER_STATE_SUCCEEDED;

import com.google.common.base.Joiner;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.Alternative;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.ReportData;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.tasks.SolverTask;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;


public class SolverService {
  private final Solver solver;
  private final ResourceBundle resources = ResourceBundle.getBundle("lang.solverTask");
  private final ReadOnlyMapProperty<CourseSelection, ResultState> courseSelectionResults;
  private final String langTimeout = ResourceBundle.getBundle("lang.tasks").getString("timeout");
  private int timeout = 60;

  /**
   * Create an ew SolverService instance. Using executorService to run tasks executed by solver.
   *
   * @param solver          Solver object to execute operations on ProB instance.
   */
  public SolverService(final Solver solver) {
    this.solver = solver;
    courseSelectionResults = new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());
  }

  /**
   * Create a {@link SolverTask} for some courses in order to check the feasibility.
   *
   * @param courses Courses to be checked
   * @return SolverTasks to check
   */
  @SuppressWarnings("unused")
  public SolverTask<Boolean> checkFeasibilityTask(final Course... courses) {
    assert this.solver != null;

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    final SolverTask<Boolean> checkFeasibilityTask =
        new SolverTask<>(resources.getString("check"), msg, this.solver,
            () -> solver.checkFeasibility(names), timeout);
    addOnCancelListener(courses, checkFeasibilityTask);

    checkFeasibilityTask.addEventHandler(WORKER_STATE_SUCCEEDED, event -> {
      if (langTimeout.equals(checkFeasibilityTask.getReason())) {
        addCourseResult(courses, ResultState.TIMEOUT);
      } else {
        addCourseResult(courses, checkFeasibilityTask.getValue() ? ResultState.SUCCEEDED
            : ResultState.FAILED);
      }
    });
    checkFeasibilityTask.addEventHandler(WORKER_STATE_FAILED, event -> {
      if (langTimeout.equals(checkFeasibilityTask.getReason())) {
        addCourseResult(courses, ResultState.TIMEOUT);
      } else {
        addCourseResult(courses, ResultState.FAILED);
      }
    });
    return checkFeasibilityTask;
  }

  /**
   * Compute feasibility for given courses.
   *
   * @param courses Given courses
   * @return SolverTask to compute if a course is feasible or not
   */
  @SuppressWarnings("unused")
  public SolverTask<FeasibilityResult> computeFeasibilityTask(
      final Course... courses) {

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    final SolverTask<FeasibilityResult> computeFeasibilityTask =
        new SolverTask<>(resources.getString("compute"), msg, solver,
            () -> {
              final FeasibilityResult result = solver.computeFeasibility(names);
              this.addCourseResult(courses, ResultState.SUCCEEDED);
              return result;
            }, timeout);
    addOnCancelListener(courses, computeFeasibilityTask);

    computeFeasibilityTask.addEventHandler(WORKER_STATE_FAILED, event -> {
      if (langTimeout.equals(computeFeasibilityTask.getReason())) {
        addCourseResult(courses, ResultState.TIMEOUT);
      } else {
        addCourseResult(courses, ResultState.FAILED);
      }
    });
    return computeFeasibilityTask;
  }

  /**
   * Compute partial feasibility for given courses, a choice for the modules and a choice for
   * abstract units.
   *
   * @param courses            List of courses to check
   * @param moduleChoice       module choice
   * @param abstractUnitChoice abstract unit choice
   * @return Instance of FeasibleResult to represent the result
   */
  @SuppressWarnings("unused")
  public SolverTask<FeasibilityResult> computePartialFeasibility(
      final List<Course> courses,
      final Map<Course, List<Module>> moduleChoice,
      final List<AbstractUnit> abstractUnitChoice) {

    final List<String> names = courses.stream()
        .map(Course::getName)
        .collect(Collectors.toList());
    final Course[] coursesArray = courses.toArray(new Course[]{});

    final Map<String, List<Integer>> mc = moduleChoice.entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey().getName(),
            e -> e.getValue().stream()
                .map(Module::getId)
                .collect(Collectors.toList())));

    final List<Integer> auc = abstractUnitChoice.stream()
        .map(AbstractUnit::getId)
        .collect(Collectors.toList());

    final String msg = getMessage(names);
    //
    final SolverTask<FeasibilityResult> computeFeasibilityTask =
        new SolverTask<>(resources.getString("compute"), msg, solver,
            () -> {
              final FeasibilityResult result = solver.computePartialFeasibility(names, mc, auc);
              addCourseResult(coursesArray, ResultState.SUCCEEDED);
              return result;
            }, timeout);
    addOnCancelListener(coursesArray, computeFeasibilityTask);

    computeFeasibilityTask.addEventHandler(WORKER_STATE_FAILED, event -> {
      if (langTimeout.equals(computeFeasibilityTask.getReason())) {
        addCourseResult(coursesArray, ResultState.TIMEOUT);
      } else {
        addCourseResult(coursesArray, ResultState.FAILED);
      }
    });

    return computeFeasibilityTask;
  }

  /**
   * Compute unsat core given some courses.
   *
   * @param courses Courses to build core
   * @return SolverTasks containing a list of integers representing the unsat core
   */
  @SuppressWarnings("unused")
  public SolverTask<Set<Integer>> unsatCore(final Course... courses) {

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    return new SolverTask<>(resources.getString("unsat"), msg, solver,
        () -> solver.unsatCore(names), timeout);
  }

  /**
   * Compute a set of modules in conflict for a given set of courses.
   *
   * @param courses Courses to compute modules in conflict
   * @return SolverTask to compute the unsat core of modules
   */
  public SolverTask<Set<Integer>> unsatCoreModules(final Course... courses) {
    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    return new SolverTask<>(resources.getString("unsatCoreModules"), msg, solver,
        () -> solver.unsatCoreModules(names), timeout);
  }

  /**
   * For a given list of modules, compute a set of abstract unit IDs that are in conflict.
   *
   * @param modules List of Modules
   * @return SolverTask to compute unsat core of abstract units
   */
  public SolverTask<Set<Integer>> unsatCoreAbstractUnits(final List<Module> modules) {
    final String msg = "";
    final List<Integer> moduleIds = modules.stream()
        .map(Module::getId).collect(Collectors.toList());
    //
    return new SolverTask<>(resources.getString("unsatCoreAbstractUnits"), msg, solver,
        () -> solver.unsatCoreAbstractUnits(moduleIds), timeout);
  }

  /**
   * For a given list of abstract units and modules, compute the associated groups that are in
   * conflict.
   *
   * @param abstractUnits List of abstract untis
   * @param modules       List of modules
   * @return SolverTask to compute unsat core of groups
   */
  public SolverTask<Set<Integer>> unsatCoreGroups(final List<AbstractUnit> abstractUnits,
                                                  final List<Module> modules) {
    final String msg = "";
    final List<Integer> abstractUnitIds = abstractUnits.stream().map(AbstractUnit::getId)
        .collect(Collectors.toList());
    final List<Integer> moduleIds = modules.stream()
        .map(Module::getId).collect(Collectors.toList());
    return new SolverTask<>(resources.getString("unsatCoreGroups"), msg, solver,
        () -> solver.unsatCoreGroups(abstractUnitIds, moduleIds), timeout);
  }

  /**
   * For a given list of group IDs compute the set of sessions in those groups that are in
   * conflict.
   *
   * @param groups List of groups
   * @return SolverTask to compute unsat core of sessions
   */
  public SolverTask<Set<Integer>> unsatCoreSessions(final List<Group> groups) {
    final String msg = "";
    final List<Integer> groupIds = groups.stream().map(Group::getId).collect(Collectors.toList());
    //
    return new SolverTask<>(resources.getString("unsatCoreSessions"), msg, solver,
        () -> solver.unsatCoreSessions(groupIds), timeout);
  }

  /**
   * Find a list of alternatives so a session and some courses.
   *
   * @param session Session to find alternatives to
   * @param courses Courses for the given sessions
   * @return List of alternatives
   */
  @SuppressWarnings("unused")
  public SolverTask<List<Alternative>> localAlternativesTask(final Session session,
                                                             final Course... courses) {
    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    return new SolverTask<>(resources.getString("alternatives"), msg, solver,
        () -> solver.getLocalAlternatives(session.getId(), names), timeout);
  }


  /**
   * Create solver task to handle impossible singleton courses.
   *
   * @return SolverTask
   */
  SolverTask<Set<String>> impossibleCoursesTask() {
    return new SolverTask<>(resources.getString("impossible"),
        resources.getString("message.impossible"), solver, solver::getImpossibleCourses,
        timeout);
  }


  public SolverTask<ReportData> collectReportDataTask() {
    return new SolverTask<>(resources.getString("report"), resources.getString("message.report"),
        solver, solver::getReportingData, timeout);
  }

  private String getMessage(final String[] names) {
    return Joiner.on(", ").join(names);
  }

  private String getMessage(final List<String> names) {
    return Joiner.on(", ").join(names);
  }


  @SuppressWarnings("unused")
  private String[] getNames(final Course[] courses) {
    final String[] names = new String[courses.length];
    for (int i = 0; i < courses.length; i++) {
      names[i] = courses[i].getName();
    }
    return names;
  }

  /**
   * Add a {@link ResultState result} to the cache {@link #courseSelectionResults}. A result is
   * replaced if the existing one is {@link ResultState#FAILED failed}.
   *  @param courses The list of courses or a single standalone course.
   * @param result  The {@link ResultState result} to be stored.
   */
  private synchronized void addCourseResult(final Course[] courses, final ResultState result) {
    final CourseSelection key = new CourseSelection(courses);
    //
    if (!courseSelectionResults.containsKey(key)
        || !ResultState.SUCCEEDED.equals(courseSelectionResults.get(key))) {
      courseSelectionResults.put(key, result);
    }
    // if we checked a pair of courses and the result is SUCCEEDED we know each course is feasible
    // and can add that information to the cache
    if (courses.length == 2 && ResultState.SUCCEEDED.equals(result)) {
      for (final Course course : courses) {
        courseSelectionResults.put(new CourseSelection(course), result);
      }
    }
  }

  /**
   * Set the onCancelled() method of a task to catch and distinguish between timeouts and failed
   * computations.
   */
  private void addOnCancelListener(final Course[] names, final SolverTask<?> solverTask) {
    solverTask.addEventHandler(WORKER_STATE_CANCELLED, event -> {
      if (langTimeout.equals(solverTask.getReason())) {
        addCourseResult(names, ResultState.TIMEOUT);
      }
    });
  }

  public final ReadOnlyMapProperty<CourseSelection, ResultState> courseSelectionResultsProperty() {
    return courseSelectionResults;
  }

  /**
   * Create a solver task to move a session to a new day/time and thus modifying the model's state.
   * Clears all caches as a side-effect.
   *
   * @param sessionId The id of the session to be moved
   * @param slot      the target slot (tay time)
   * @return SolverTask object for moving a session
   */
  public SolverTask<Void> moveSessionTask(final int sessionId, final SessionFacade.Slot slot) {
    return new SolverTask<>(resources.getString("moving"), resources.getString("message.moving"),
      solver, () -> {
      solver.move(
            String.valueOf(sessionId),
            slot.getDayString(),
            slot.getTime().toString());
      Platform.runLater(courseSelectionResults::clear);
      return null;
    }, timeout);
  }

  public void setTimeout(final int timeout) {
    this.timeout = timeout;
  }
}
