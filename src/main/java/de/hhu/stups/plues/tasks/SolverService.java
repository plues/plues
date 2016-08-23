package de.hhu.stups.plues.tasks;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.prob.Alternative;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.Solver;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class SolverService {
  private final ExecutorService executor;
  private final Solver solver;

  @Inject
  public SolverService(@Named("prob") ExecutorService executorService, @Assisted Solver solver) {
    this.executor = executorService;
    this.solver = solver;
  }

  /**
   * Create SolverTaks for some courses in order to check the feasiblity.
   * @param courses Courses to be checked
   * @return SolverTasks to check
   */
  @SuppressWarnings("unused")
  public final SolverTask<Boolean> checkFeasibilityTask(final Course... courses) {
    assert this.solver != null;

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    return new SolverTask<>("Checking Feasibility", msg, this.solver,
        () -> this.solver.checkFeasibility(names));
  }

  /**
   * Compute feasibility for given courses.
   * @param courses Given courses
   * @return SolverTask to compute if a course is feasible or not
   */
  @SuppressWarnings("unused")
  public final SolverTask<FeasibilityResult> computeFeasibilityTask(
      final Course... courses) {

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    return new SolverTask<>("Computing Feasibility",
      msg, solver,
        () -> solver.computeFeasibility(names));
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
  public final SolverTask<FeasibilityResult> computePartialFeasibility(
      final List<Course> courses,
      final Map<Course, List<Module>> moduleChoice,
      final List<AbstractUnit> abstractUnitChoice) {

    final List<String> names = courses.stream()
        .map(Course::getName)
        .collect(Collectors.toList());

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
    return new SolverTask<>("Computing Feasibility",
      msg, solver,
        () -> solver.computePartialFeasibility(names, mc, auc));
  }

  /**
   * Compute unsat core given some courses.
   * @param courses Courses to build core
   * @return SolverTasks containing a list of integers representing the unsat core
   */
  @SuppressWarnings("unused")
  public final SolverTask<List<Integer>> unsatCore(final Course... courses) {

    final String[] names = getNames(courses);
    final String msg = getMessage(names);
    //
    return new SolverTask<>("Computing UNSAT Core", msg, solver,
        () -> solver.unsatCore(names));
  }

  /**
   * Find a list of alternatives so a session and some courses.
   * @param session Session to find alternatives to
   * @param courses Courses for the given sessions
   * @return List of alternatives
   */
  public SolverTask<List<Alternative>> localAlternativesTask(final Session session,
                                                             final Course... courses) {
    String[] names = getNames(courses);
    String msg = getMessage(names);
    return new SolverTask<>("Computing alternatives", msg, solver,
        () -> solver.getLocalAlternatives(session.getId(), names));
  }

  public SolverTask<Set<String>> impossibleCoursesTask() {
    return new SolverTask<>("Collecting impossible courses", "Impossible",
      solver, () -> solver.getImpossibleCourses());
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
//  return Arrays.asList(courses).stream()
//  .map(c -> c.getName())
//  .collect(Collectors.toList())
//  .toArray(new String[courses.length]);
  }

  public final void submit(final SolverTask<?> command) {
    this.executor.submit(command);
  }

}
