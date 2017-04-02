package de.hhu.stups.plues.prob;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeasibilityResult {
  private final Map<Integer, Integer> groupChoice;
  private final Map<String, Set<Integer>> moduleChoice;
  private final Map<Integer, Integer> semesterChoice;
  private final Map<Integer, Set<Integer>> abstractUnitChoice;

  /**
   * Constructor for feasibility result.
   * @param moduleChoice Maps course id to set of module ids
   * @param semesterChoice Maps abstract unit id to semester
   * @param groupChoice Maps unit id to  group id
   */
  public FeasibilityResult(final Map<String, Set<Integer>> moduleChoice,
                    final Map<Integer, Set<Integer>> abstractUnitChoice,
                    final Map<Integer, Integer> semesterChoice,
                    final Map<Integer, Integer> groupChoice) {

    this.moduleChoice = moduleChoice;
    this.abstractUnitChoice = abstractUnitChoice;
    this.semesterChoice = semesterChoice;
    this.groupChoice = groupChoice;
  }

  public Map<Integer, Integer> getGroupChoice() {
    return groupChoice;
  }

  public Map<String, Set<Integer>> getModuleChoice() {
    return moduleChoice;
  }

  public Map<Integer, Integer> getSemesterChoice() {
    return semesterChoice;
  }

  public Map<Integer, Set<Integer>> getAbstractUnitChoice() {
    return abstractUnitChoice;
  }
}
