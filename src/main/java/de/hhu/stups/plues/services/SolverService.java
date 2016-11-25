package de.hhu.stups.plues.services;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.keys.CourseKey;
import de.hhu.stups.plues.keys.MajorMinorKey;
import de.hhu.stups.plues.prob.Alternative;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.ReportData;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


public class SolverService {
  private final ExecutorService executor;
  private final Solver solver;
  private final ResourceBundle resources = ResourceBundle.getBundle("lang.solverTask");
  private final ReadOnlyMapProperty<MajorMinorKey, ResultState> courseCombinationResults;
  private final ReadOnlyMapProperty<CourseKey, ResultState> singleCourseResults;
  private final String langTimeout = ResourceBundle.getBundle("lang.tasks").getString("timeout");
  private int timeout = 60;

  /**
   * Create an ew SolverService instance. Using executorService to run tasks executed by solver.
   *
   * @param executorService ExecutorService to run tasks
   * @param solver          Solver object to execute operations on ProB instance.
   */
  @Inject

  public SolverService(@Named("prob") final ExecutorService executorService,
                       @Assisted final Solver solver) {
    this.executor = executorService;
    this.solver = solver;
    courseCombinationResults = new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());
    singleCourseResults = new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());
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
    addOnCancelListener(names, checkFeasibilityTask);
    checkFeasibilityTask.setOnSucceeded(event -> {
      if (langTimeout.equals(checkFeasibilityTask.getReason())) {
        addCourseResult(names, ResultState.TIMEOUT);
      } else {
        addCourseResult(names, checkFeasibilityTask.getValue() ? ResultState.SUCCEEDED
            : ResultState.FAILED);
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
              this.addCourseResult(names, ResultState.SUCCEEDED);
              return result;
            }, timeout);
    addOnCancelListener(names, computeFeasibilityTask);
    computeFeasibilityTask.setOnFailed(event -> {
      if (langTimeout.equals(computeFeasibilityTask.getReason())) {
        addCourseResult(names, ResultState.TIMEOUT);
      } else {
        addCourseResult(names, ResultState.FAILED);
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
    final String[] combination = names.toArray(new String[] {});

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
              addCourseResult(combination, ResultState.SUCCEEDED);
              return result;
            }, timeout);
    addOnCancelListener(combination, computeFeasibilityTask);
    computeFeasibilityTask.setOnFailed(event -> {
      if (langTimeout.equals(computeFeasibilityTask.getReason())) {
        addCourseResult(combination, ResultState.TIMEOUT);
      } else {
        addCourseResult(combination, ResultState.FAILED);
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

  /**
   * Create a solver task to move a session to a new day/time and thus modifying the model's state.
   * Clears all caches as a side-effect.
   *
   * @param session Session to be moved
   * @param day     String target day
   * @param time    String target time slot
   * @return SolverTask
   */
  @SuppressWarnings("unused")
  public SolverTask<Void> moveTask(final Session session, final String day, final String time) {
    final String sessionId = String.valueOf(session.getId());

    return new SolverTask<>(resources.getString("moving"), resources.getString("message.moving"),
        solver, () -> {
      solver.move(sessionId, day, time);
      courseCombinationResults.clear();
      singleCourseResults.clear();
      return null;
    }, timeout);
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

  @SuppressWarnings("unchecked")
  public <T> ListenableFuture<T> submit(final SolverTask<T> command) {
    return (ListenableFuture<T>) this.executor.submit(command);
  }

  /**
   * Add a {@link ResultState result} to the cache {@link #courseCombinationResults}. A result is
   * replaced if the existing one is {@link ResultState#FAILED failed}.
   *
   * @param courses The list of courses or a single standalone course.
   * @param result  The {@link ResultState result} to be stored.
   */
  private synchronized void addCourseResult(final String[] courses, final ResultState result) {
    final MajorMinorKey key;
    if (courses.length == 1) {
      key = new MajorMinorKey(courses[0], null);
      addSingleCourseResult(courses[0], result);
    } else {
      key = new MajorMinorKey(courses[0], courses[1]);
      if (ResultState.SUCCEEDED.equals(result)) {
        addSingleCourseResult(courses[0], result);
        addSingleCourseResult(courses[1], result);
      }
    }
    if (!courseCombinationResults.containsKey(key)
        || !ResultState.SUCCEEDED.equals(courseCombinationResults.get(key))) {
      courseCombinationResults.put(key, result);
    }
  }

  /**
   * Add a {@link ResultState result} to the cache {@link #singleCourseResults}. A result is
   * replaced if the existing one is {@link ResultState#FAILED failed}.
   *
   * @param courseName The name of the single course.
   * @param result     The {@link ResultState result} to be stored.
   */
  private synchronized void addSingleCourseResult(final String courseName,
                                                  final ResultState result) {
    final CourseKey courseKey = new CourseKey(courseName);
    if (!singleCourseResults.containsKey(courseKey)
        || !ResultState.SUCCEEDED.equals(singleCourseResults.get(courseKey))) {
      singleCourseResults.put(courseKey, result);
    }
  }

  /**
   * Set the onCancelled() method of a task to catch and distinguish between timeouts and failed
   * computations.
   */
  private void addOnCancelListener(final String[] names, final SolverTask<?> solverTask) {
    solverTask.setOnCancelled(event -> {
      if (langTimeout.equals(solverTask.getReason())) {
        addCourseResult(names, ResultState.TIMEOUT);
      }
    });
  }

  public final ReadOnlyMapProperty<MajorMinorKey, ResultState> getCourseCombinationResults() {
    return courseCombinationResults;
  }

  public final ReadOnlyMapProperty<CourseKey, ResultState> getSingleCourseResults() {
    return singleCourseResults;
  }


  /**
   * Move a session to a new day/time slot.
   *
   * @param sessionId The id of the session to be moved
   * @param slot      the target slot (tay time)
   * @return SolverTask object for moving a session
   */
  public SolverTask<Void> moveSession(final int sessionId, final SessionFacade.Slot slot) {
    return new SolverTask<>("Verschiebe a nach b", "Verschiebe es!!!", solver, () -> {
      solver.move(
          String.valueOf(sessionId),
          slot.getDayString(),
          slot.getTime().toString());
      courseCombinationResults.clear();
      singleCourseResults.clear();
      return null;
    }, timeout);
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}
