package de.hhu.stups.plues.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.ProBSolver;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.studienplaene.ColorChoice;
import de.hhu.stups.plues.studienplaene.ColorScheme;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public interface UiTestDataCreator {

  /**
   * Create a dummy course.
   *
   * @param degree "bk" is combinable, "ba" is not
   */
  static Course createCourse(final String shortName, final String degree,
                             final String kzfa) {
    return createCourse(shortName, degree, kzfa, Collections.emptySet());
  }

  /**
   * Create a dummy course with a given set of minor courses.
   */
  static Course createCourse(final String shortName, final String degree,
                             final String kzfa, final Set<Course> minorCourses) {
    final Course course = new Course();
    course.setShortName(shortName);
    course.setLongName(shortName);
    course.setDegree(degree);
    course.setKzfa(kzfa);
    course.setMinorCourses(minorCourses);
    return course;
  }

  /**
   * Simple solver task that just waits some time, used for ui tests.
   */
  static SolverTask<Boolean> getSimpleCheckFeasibilityTask() {
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
  static SolverTask<FeasibilityResult> getSimpleComputeFeasibilityTask() {
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
  static SolverTask<Set<String>> getSimpleImpossibleCoursesTask() {
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
  static Task<Boolean> getSimpleTask(final int sleep) {
    return new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        TimeUnit.SECONDS.sleep(sleep);
        return true;
      }
    };
  }

  /**
   * Create a list of courses to use in the tests.
   */
  static ObservableList<Course> createCourseList() {
    final List<Course> courseList = new ArrayList<>(10);
    courseList.add(createCourse("shortName1", "bk", "H"));
    courseList.add(createCourse("shortName2", "ba", "H"));
    courseList.add(createCourse("shortName3", "bk", "N"));
    courseList.add(createCourse("shortName4", "bk", "N"));
    courseList.add(createCourse("shortName5", "bk", "H"));
    courseList.add(createCourse("shortName6", "bk", "N"));
    courseList.add(createCourse("shortName7", "ma", "N"));
    courseList.add(createCourse("shortName8", "ma", "N"));
    courseList.add(createCourse("shortName9", "bk", "H"));
    courseList.add(createCourse("shortName10", "ma", "H"));
    courseList.forEach(course -> {
      if (course.isMajor()) {
        course.setMinorCourses(
            courseList.stream().filter(Course::isMinor).collect(Collectors.toSet()));
      }
    });
    return FXCollections.observableArrayList(courseList);
  }

  /**
   * Return a mocked {@link SolverService}.
   */
  static SolverService getMockedSolverService() {
    final SolverService solverService = mock(SolverService.class);
    when(solverService.computeFeasibilityTask(any()))
        .thenReturn(UiTestDataCreator.getSimpleComputeFeasibilityTask());
    when(solverService.checkFeasibilityTask(any()))
        .thenReturn(UiTestDataCreator.getSimpleCheckFeasibilityTask());
    when(solverService.checkFeasibilityTask(any(), any()))
        .thenReturn(UiTestDataCreator.getSimpleCheckFeasibilityTask());
    when(solverService.impossibleCoursesTask())
        .thenReturn(UiTestDataCreator.getSimpleImpossibleCoursesTask());
    return solverService;
  }

  /**
   * Return a mocked major course.
   */
  static Course getMockedMajorCourse(final Set<Module> modules) {
    final Course majorCourse = getMockedCourse(modules);
    when(majorCourse.getKzfa()).thenReturn("H");
    when(majorCourse.isMajor()).thenReturn(true);
    when(majorCourse.isMinor()).thenReturn(false);
    return majorCourse;
  }

  /**
   * Return a mocked minor course.
   */
  static Course getMockedMinorCourse(final Set<Module> modules) {
    final Course minorCourse = getMockedCourse(modules);
    when(minorCourse.getKzfa()).thenReturn("N");
    when(minorCourse.isMajor()).thenReturn(false);
    when(minorCourse.isMinor()).thenReturn(true);
    return minorCourse;
  }

  /**
   * Return a mocked {@link Course} whereat the kzfa is not set.
   */
  static Course getMockedCourse(final Set<Module> modules) {
    final Course course = mock(Course.class);
    when(course.getFullName()).thenReturn("Major Course");
    when(course.getLongName()).thenReturn("Major Course");
    when(course.getDegree()).thenReturn("bk");
    when(course.getPo()).thenReturn(2016);
    when(course.getModules()).thenReturn(modules);
    when(course.getMinorCourses()).thenReturn(Collections.emptySet());
    return course;
  }

  /**
   * Return a {@link PdfRenderingTask} that just waits some time.
   */
  static PdfRenderingTask getWaitingPdfRenderingTask() {
    return new TestPdfTask();
  }

  /**
   * Create and return a color scheme for colored pdf generation.
   */
  static ColorScheme getColorScheme() {
    return new ColorScheme("Test color", ColorChoice.COLOR,
        new LinkedHashSet<>(Arrays.asList("#DCBFBE", "#DCD6BE", "#C1DCBE", "#F1EAB4", "#C5CBF1",
            "#EFF1CB", "#E5CBF1", "#DCF1E9", "#EFB9B9", "#FFA6A6", "#FCFE80", "#C7FF72",
            "#9AFFA4", "#9AFFD6", "#9AFFF9", "#94E5FF", "#A4C1FF", "#CFA4FF", "#F2A4FF",
            "#F6CCFF", "#FFB5F0", "#F7D9E4", "#E78FFB", "#DAFFB4", "#B4FFFD", "#69BCFF",
            "#FFA361")));
  }

  final class TestPdfTask extends PdfRenderingTask {
    TestPdfTask() {
      super(null, null, null, null, null);
    }

    public Path call() {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (final InterruptedException exception) {
        exception.printStackTrace();
      }
      return Paths.get(".");
    }
  }

}
