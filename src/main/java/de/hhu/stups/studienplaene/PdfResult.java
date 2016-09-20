package de.hhu.stups.studienplaene;

import java.util.Map;
import java.util.Set;

public class PdfResult {

  private final Map<Integer, Integer> groupChoice;
  private final Map<String, Set<Integer>> moduleChoice;
  private final Map<Integer, Integer> semesterChoice;
  private final Map<Integer, Integer> unitChoice;

  public PdfResult(Map<String, Set<Integer>> moduleChoice,
                   Map<Integer, Integer> groupChoice,
                   Map<Integer, Integer> semesterChoice,
                   Map<Integer, Integer> unitChoice) {
    this.groupChoice = groupChoice;
    this.moduleChoice = moduleChoice;
    this.semesterChoice = semesterChoice;
    this.unitChoice = unitChoice;
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

  public Map<Integer, Integer> getUnitChoice() {
    return unitChoice;
  }


}
