package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.SolverService;

import javafx.collections.ObservableMap;

import java.util.Set;

public interface CheckCourseCombination {

  /**
   * Check if the feasibility of a combination of courses or a standalone course has already been
   * computed or contains an impossible course. The results are stored in {@link
   * SolverService#courseSelectionResults}. Furthermore, check that the computed result in the
   * cache is true, because the user could have cancelled a task that is feasible normally.
   *
   * @param courses The key of the courses.
   * @return Return false if {@link SolverService#courseSelectionResults} contains the key and the
   *         stored result is true or the key contains an impossible course, otherwise return true.
   */
  static boolean shouldBeChecked(final ObservableMap<CourseSelection, ResultState> results,
                                 final Set<Course> impossibleCourses,
                                 final Course... courses) {
    final CourseSelection courseSelection = new CourseSelection(courses);
    // if course has been successfully checked we do not want to check it again
    return !results.getOrDefault(courseSelection, ResultState.FAILED).succeeded()
        && canBeChecked(courseSelection, impossibleCourses);

  }

  /**
   * Check that no course is statically impossible.
   */
  static boolean canBeChecked(final CourseSelection courseSelection,
                              final Set<Course> impossibleCourses) {
    // if the given selection contains impossible courses we do not bother to check it.
    for (final Course course : courseSelection.getCourses()) {
      if (!impossibleCourses.contains(course)) {
        continue;
      }
      return false;
    }
    return true;
  }
}
