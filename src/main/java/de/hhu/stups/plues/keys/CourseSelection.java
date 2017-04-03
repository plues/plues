package de.hhu.stups.plues.keys;

import de.hhu.stups.plues.data.entities.Course;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CourseSelection represents encapsulates choices and combinations of courses.
 */
// NOTE: consider splitting class into two or three subclasses for each different case.
// I.e. combinations, standalone and any single course
public class CourseSelection {

  private final List<Course> courses;
  private final boolean isStandalone;
  private final boolean isSingle;

  public CourseSelection(final Course course) {
    this(new Course[] {course});
  }

  /**
   * create a course selection from a list of 1 or 2 courses.
   * Some basic validation is done on the arguments
   * If two courses are passed, these must be a major and a minor.
   *
   * @param courses Array of Course objects
   */
  public CourseSelection(final Course... courses) {
    this.courses = Arrays.asList(courses);
    this.courses.sort(Comparator.comparingInt(Course::getId));
    isSingle = courses.length == 1;

    if (isSingle()) {
      isStandalone = !courses[0].isCombinable();
    } else {
      isStandalone = false;
      if (courses.length != 2) {
        throw new IllegalArgumentException(
          "Invalid list of courses, a combination can not contain more than 2.");
      }
      // one must be a major
      if (!(courses[0].isMajor() || courses[1].isMajor())) {
        throw new IllegalArgumentException("Invalid list of courses, a major is required.");
      }

      // one must be a minor
      if (!(courses[0].isMinor() || courses[1].isMinor())) {
        throw new IllegalArgumentException("Invalid list of courses, a minor is required.");
      }
    }
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    final CourseSelection that = (CourseSelection) other;
    return isStandalone == that.isStandalone
      && isSingle == that.isSingle
      && Objects.equals(courses, that.courses);
  }

  @Override
  public int hashCode() {
    if (isSingle()) {
      return Objects.hash(courses.get(0), isStandalone, isSingle);
    }
    return Objects.hash(courses.get(0), courses.get(1), isStandalone, isSingle);
  }

  public List<Course> getCourses() {
    return courses;
  }

  public boolean isStandalone() {
    return isStandalone;
  }

  public boolean isSingle() {
    return isSingle;
  }

  public boolean isCombination() {
    return courses.size() > 1;
  }

  /**
   * Check if the current key represents a valid curriculum. I.e. a combinations of major and minor
   * or a standalone course. Single course keys aren't considered valid curricula.
   * @return boolean
   */
  public boolean isCurriculum() {
    return isCombination() || isStandalone();
  }


  @Override
  public String toString() {
    return String.format("CourseSelection[%s]", courses.stream()
      .map(Course::toString)
      .collect(Collectors.joining(", ")));
  }
}
