package de.hhu.stups.plues.ui.batchgeneration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CourseSelectionCollectorTest {
  private CourseSelectionCollector courseSelectionCollector;
  private Course course1;
  private Course course2;
  private CourseSelectionCollector courseSelectionCollector1;

  /**
   * Test setup method.
   */
  @Before
  public void setUp() {
    final UiDataService uiDataService = mock(UiDataService.class);
    when(uiDataService.impossibleCoursesProperty())
        .thenReturn(new SimpleSetProperty<>(FXCollections.observableSet()));

    this.course1 = mock(Course.class);
    this.course2 = mock(Course.class);

    final Delayed<SolverService> delayed = new Delayed<>();

    this.courseSelectionCollector = new CourseSelectionCollector(uiDataService, delayed)
        .usingCourses(Arrays.asList(course1, course2));
    this.courseSelectionCollector1 = new CourseSelectionCollector(uiDataService, delayed)
        .usingCourses(Arrays.asList(course1, course2));
  }

  @Test
  public void withCombinations() throws Exception {
    when(course1.isCombinable()).thenReturn(true);
    when(course2.isCombinable()).thenReturn(true);

    when(course1.isMajor()).thenReturn(true);
    when(course2.isMinor()).thenReturn(true);

    when(course1.getMinorCourses()).thenReturn(new HashSet<>(Collections.singletonList(course2)));

    final Course course3 = mock(Course.class);
    when(course3.isCombinable()).thenReturn(true);
    when(course3.isMajor()).thenReturn(true);

    final List<Course> courses = Arrays.asList(course1, course2, course3);
    final List<CourseSelection> results = courseSelectionCollector
        .withCombinations()
        .usingCourses(courses)
        .stream()
        .collect(Collectors.toList());
    assertEquals(1, results.size());
    assertTrue(results.contains(new CourseSelection(course1, course2)));

    // negative test
    final List<CourseSelection> selections = courseSelectionCollector1
        .usingCourses(courses)
        .stream()
        .collect(Collectors.toList());
    assertEquals(0, selections.size());

  }

  @Test
  public void withStandaloneCourses() throws Exception {
    when(course1.isCombinable()).thenReturn(false);
    when(course2.isCombinable()).thenReturn(true);

    final List<CourseSelection> keys = this.courseSelectionCollector
        .withStandaloneCourses().stream().collect(Collectors.toList());

    assertEquals(1, keys.size());
    assertEquals(new CourseSelection(course1), keys.get(0));

  }

  @Test
  public void testNoStandaloneCourses() throws Exception {
    when(course1.isCombinable()).thenReturn(true);
    when(course2.isCombinable()).thenReturn(true);

    final List<CourseSelection> courseSelections
        = this.courseSelectionCollector1
          .withStandaloneCourses().stream().collect(Collectors.toList());
    assertEquals(0, courseSelections.size());
  }

  @Test
  public void withSingleCourses() throws Exception {
    final List<CourseSelection> keys
        = this.courseSelectionCollector.usingCourses(Arrays.asList(course1, course2))
          .withSingleCourses().stream().collect(Collectors.toList());
    assertEquals(2, keys.size());
    assertTrue(
        keys.containsAll(
            Stream.of(course1, course2).map(CourseSelection::new).collect(Collectors.toList())));

    // negative test
    final List<CourseSelection> courseSelections
        = this.courseSelectionCollector1.usingCourses(Arrays.asList(course1, course2))
            .stream().collect(Collectors.toList());
    assertEquals(0, courseSelections.size());
  }

}
