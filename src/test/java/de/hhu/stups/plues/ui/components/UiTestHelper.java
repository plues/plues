package de.hhu.stups.plues.ui.components;

import static org.mockito.Mockito.mock;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.ProBSolver;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.concurrent.Task;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class UiTestHelper {

  /**
   * Create a dummy course.
   *
   * @param degree "bk" is combinable, "ba" is not
   */
  public static Course createCourse(final String shortName, final String degree,
                                    final String kzfa) {
    final Course course = new Course();
    course.setShortName(shortName);
    course.setLongName(shortName);
    course.setDegree(degree);
    course.setKzfa(kzfa);
    return course;
  }

  /**
   * Simple solver task that just waits some time, used for ui tests.
   */
  public static SolverTask<Boolean> getSimpleCheckFeasibilityTask() {
    return new SolverTask<Boolean>("", mock(ProBSolver.class), (() -> true), 2) {
      @Override
      protected Boolean call() throws InterruptedException, ExecutionException {
        TimeUnit.SECONDS.sleep(2);
        return true;
      }
    };
  }

  /**
   * See {@link #getSimpleCheckFeasibilityTask}.
   */
  public static SolverTask<FeasibilityResult> getSimpleComputeFeasibilityTask() {
    final FeasibilityResult feasibilityResult = mock(FeasibilityResult.class);
    return new SolverTask<FeasibilityResult>("", mock(ProBSolver.class),
        (() -> feasibilityResult), 2) {
      @Override
      protected FeasibilityResult call() throws InterruptedException, ExecutionException {
        TimeUnit.SECONDS.sleep(2);
        return feasibilityResult;
      }
    };
  }

  /**
   * See {@link #getSimpleCheckFeasibilityTask}.
   */
  public static SolverTask<Set<String>> getSimpleImpossibleCoursesTask() {
    return new SolverTask<Set<String>>("", mock(ProBSolver.class),
        (HashSet::new), 2) {
      @Override
      protected Set<String> call() throws InterruptedException, ExecutionException {
        TimeUnit.SECONDS.sleep(2);
        return new HashSet<>();
      }
    };
  }

  /**
   * Simple task that just waits some time.
   *
   * @param sleep The time to sleep in seconds.
   */
  public static Task<Boolean> getSimpleTask(final int sleep) {
    return new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        TimeUnit.SECONDS.sleep(sleep);
        return true;
      }
    };
  }
}
