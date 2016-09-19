package de.hhu.stups.plues.tasks;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.keys.MajorMinorKey;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.prob.Alternative;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.ReportData;
import de.hhu.stups.plues.prob.Solver;

import de.hhu.stups.plues.prob.SolverException;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

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
  private final ReadOnlyMapProperty<MajorMinorKey, Boolean> courseCombinationResults;

  /**
   * Create an ew SolverService instance. Using executorService to run tasks executed by solver.
   * @param executorService ExecutorService to run tasks
   * @param solver Solver object to execute operations on ProB instance.
   */
  @Inject
  public SolverService(@Named("prob") final ExecutorService executorService,
      @Assisted final Solver solver) {
    this.executor = executorService;
    this.solver = solver;
    this.courseCombinationResults = new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());
  }

  /**
   * Create SolverTaks for some courses in order to check the feasiblity.
   * @param courses Courses to be checked
   * @return SolverTasks to check
   */
  @SuppressWarnings("unused")
  public SolverTask<Boolean> checkFeasibilityTask(final Course... courses) {
    assert this.solver != null;

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    return new SolverTask<>(resources.getString("check"), msg, this.solver,
        () -> {
          final Boolean result = this.solver.checkFeasibility(names);
          this.addCourseCombinationResult(names, result);
          return result;
        });
  }

  /**
   * Compute feasibility for given courses.
   * @param courses Given courses
   * @return SolverTask to compute if a course is feasible or not
   */
  @SuppressWarnings("unused")
  public SolverTask<FeasibilityResult> computeFeasibilityTask(
      final Course... courses) {

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    return new SolverTask<>(resources.getString("compute"),
        msg, solver,
        () -> {
          try {
            final FeasibilityResult result = solver.computeFeasibility(names);
            this.addCourseCombinationResult(names, true);
            return result;
          } catch (final SolverException exception) {
            this.addCourseCombinationResult(names, false);
            throw exception;
          }
        });
  }

  /**
   * Compute partial feasibility for given courses, a choice for the modules
   * and a choice for abstract units.
   * @param courses List of courses to check
   * @param moduleChoice module choice
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
    final String[] combination = names.toArray(new String[]{});

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
    return new SolverTask<>(resources.getString("compute"),
      msg, solver,
        () -> {
          try {
            final FeasibilityResult result = solver.computePartialFeasibility(names, mc, auc);
            this.addCourseCombinationResult(combination, true);
            return result;
          } catch (final SolverException exception) {
            this.addCourseCombinationResult(combination, false);
            throw exception;
          }
        });
  }

  /**
   * Compute unsat core given some courses.
   * @param courses Courses to build core
   * @return SolverTasks containing a list of integers representing the unsat core
   */
  @SuppressWarnings("unused")
  public SolverTask<List<Integer>> unsatCore(final Course... courses) {

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    return new SolverTask<>(resources.getString("unsat"), msg, solver,
        () -> solver.unsatCore(names));
  }

  /**
   * Find a list of alternatives so a session and some courses.
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
        () -> solver.getLocalAlternatives(session.getId(), names));
  }


  /**
   * Create solver task to handle impossible singleton courses.
   * @return SolverTask
   */
  public SolverTask<Set<String>> impossibleCoursesTask() {
    return new SolverTask<>(resources.getString("impossible"),
      resources.getString("impossibleMessage"), solver, solver::getImpossibleCourses);
  }


  public SolverTask<ReportData> collectReportDataTask() {
    return new SolverTask<>(resources.getString("report"), resources.getString("reportMessage"),
      solver, solver::getReportingData);
  }

  /**
   * Create a solver task to move a session to a new day/time and thus modifying the model's state.
   * Clears all caches as a side-effect.
   * TODO: Adapt signature to requirements of consumers (once merged)
   * @param session Session to be moved
   * @param day String target day
   * @param time String target time slot
   * @return SolverTask
   */
  @SuppressWarnings("unused")
  public SolverTask<Void> moveTask(final Session session, final String day, final String time) {
    final String sessionId = String.valueOf(session.getId());
    return new SolverTask<>(resources.getString("moving"), resources.getString("movingMessage"),
      solver, () -> {
      solver.move(sessionId, day, time);
      courseCombinationResults.clear();
      return null;
    });
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
  public <T> ListenableFuture<T>  submit(final SolverTask<T> command) {
    return (ListenableFuture<T>) this.executor.submit(command);
  }

  /**
   * Add a boolean valued result to the cache {@link SolverService#courseCombinationResults}.
   *  @param courses The list of courses or a single standalone course.
   * @param result  The boolean valued feasibility result.
   */
  private synchronized void addCourseCombinationResult(final String[] courses,
      final boolean result) {

    final MajorMinorKey key;
    if (courses.length == 1) {
      key = new MajorMinorKey(courses[0], null);
    } else {
      key = new MajorMinorKey(courses[0], courses[1]);
    }
    // only replace if cache not contains key or the existing result is false
    if (!courseCombinationResults.containsKey(key) || !courseCombinationResults.get(key)) {
      courseCombinationResults.put(key, result);
    }
  }

  public final ObservableMap<MajorMinorKey, Boolean> getCourseCombinationResults() {
    return this.courseCombinationResults;
  }

  public SolverTask<Void> moveSession2(int sessionId, SessionFacade.Slot slot) {
    return new SolverTask<Void>("Verschiebe a nach b", "Verschiebe es!!!", solver, () -> {
      solver.move(
        String.valueOf(sessionId),
        slot.getDayString(),
        slot.getTime().toString()
      );

      return null;
    });
  }

  public SolverTask<Void> moveSession(int sessionId, SessionFacade.Slot slot) {
    return new SolverTask<Void>("Verschiebe a nach b", "Verschiebe es!!!", solver, () -> {
      solver.move(
        String.valueOf(sessionId),
        slot.getDayString(),
        slot.getTime().toString());
      return null;
    });
  }
}
