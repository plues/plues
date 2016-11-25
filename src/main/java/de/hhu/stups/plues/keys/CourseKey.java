package de.hhu.stups.plues.keys;

import java.util.Objects;

public final class CourseKey {
  private final String courseName;

  public CourseKey(final String courseName) {
    this.courseName = courseName;
  }

  public String getCourseName() {
    return courseName;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    CourseKey courseKey = (CourseKey) obj;
    return Objects.equals(courseName, courseKey.courseName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseName);
  }
}
