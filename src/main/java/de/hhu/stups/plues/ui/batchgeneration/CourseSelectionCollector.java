package de.hhu.stups.plues.ui.batchgeneration;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleSetProperty;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builder class to create and configure a collector object that creates {@link CourseSelection}
 * objects for different courses and combinations of courses based on configuration.
 */
public class CourseSelectionCollector {

  private final MapProperty<CourseSelection, ResultState> results = new SimpleMapProperty<>();

  private boolean collectCombinations = false;
  private boolean collectStandalones = false;
  private boolean withoutKnownResults = false;
  private boolean collectSingleCourses = false;

  private List<Course> courses = Collections.emptyList();

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final SetProperty<Course> impossibleCourses = new SimpleSetProperty<>();

  /**
   * Create a new Collector instance.
   * @param uiDataService UiDataService instance, needed for the list of impossible courses
   * @param delayedSolverService DelayedSolverService needed for the list of known results
   */
  @Inject
  public CourseSelectionCollector(final UiDataService uiDataService,
                                  final Delayed<SolverService> delayedSolverService) {

    delayedSolverService.whenAvailable(solverService
        -> this.results.bind(solverService.courseSelectionResultsProperty()));

    this.impossibleCourses.bind(uiDataService.impossibleCoursesProperty());
  }

  CourseSelectionCollector withCombinations() {
    this.collectCombinations = true;
    return this;
  }

  CourseSelectionCollector withStandaloneCourses() {
    this.collectStandalones = true;
    return this;
  }

  CourseSelectionCollector usingCourses(final List<Course> courses) {
    this.courses = courses;
    return this;
  }

  CourseSelectionCollector withoutKnownResults() {
    this.withoutKnownResults = true;
    return this;
  }

  Stream<CourseSelection> stream() {
    final Stream<CourseSelection> combinations = this.collectCombinations();
    final Stream<CourseSelection> standaloneCourses = this.collectStandaloneCourses();
    final Stream<CourseSelection> singleCourses = this.collectSingleCourses();

    final Stream<CourseSelection> courseSelectionStream
        = Stream.concat(Stream.concat(combinations, standaloneCourses), singleCourses);

    return courseSelectionStream
        .filter(this::canBeChecked)
        .filter(this::shouldBeChecked)
        .distinct();
  }

  private boolean canBeChecked(final CourseSelection courseSelection) {
    // if the given selection contains impossible courses we do not bother to check it.
    for (final Course course : courseSelection.getCourses()) {
      if (!impossibleCourses.contains(course)) {
        continue;
      }
      return false;
    }
    return true;
  }

  private boolean shouldBeChecked(final CourseSelection courseSelection) {
    return !this.withoutKnownResults
        || !results.getOrDefault(courseSelection, ResultState.FAILED).succeeded();
  }

  private Stream<CourseSelection> collectSingleCourses() {
    if (!collectSingleCourses) {
      return Stream.empty();
    }
    return courses.stream()
      .map(CourseSelection::new);
  }

  private Stream<CourseSelection> collectStandaloneCourses() {
    if (!collectStandalones) {
      return Stream.empty();
    }
    return courses.stream()
      .filter(course -> !course.isCombinable())
      .map(CourseSelection::new);
  }

  private Stream<CourseSelection> collectCombinations() {
    if (!collectCombinations) {
      return Stream.empty();
    }
    return courses.stream()
        .flatMap(course -> this.getCombinableCourses(course).stream()
            .map(other -> new CourseSelection(course, other))
            .collect(Collectors.toList()).stream());
  }

  // Note: consider improving the implementation of this method to avoid creating repeated
  // CourseSelection objects.
  // Repetition can occur if the list of courses being evaluated contains a major and one of its
  // minors.
  // In that case it could happen, that we build the list of minors for the major and the list of
  // majors for the minor course.
  // Nevertheless, repeated entries are currently discarded at a later point in the stream method.
  private Set<Course> getCombinableCourses(final Course course) {
    final Set<Course> others;
    if (course.isMajor()) {
      others = course.getMinorCourses();
    } else {
      others = course.getMajorCourses();
    }
    return others;
  }

  CourseSelectionCollector withSingleCourses() {
    this.collectSingleCourses = true;
    return this;
  }
}
