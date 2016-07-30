package de.hhu.stups.plues.prob;

import java.util.Map;
import java.util.Set;

public class FeasibilityResult {
    private final Map<Integer, Integer> groupChoice;
    private final Map<String, Set<Integer>> moduleChoice;
    private final Map<Integer, Integer> semesterChoice;
    private final Map<Integer, Integer> unitChoice;

    public FeasibilityResult(Map<String, Set<Integer>> moduleChoice, Map<Integer, Integer> unitChoice,
                             Map<Integer, Integer> semesterChoice, Map<Integer, Integer> groupChoice) {
        this.moduleChoice = moduleChoice;
        this.semesterChoice = semesterChoice;
        this.unitChoice = unitChoice;
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

    public Map<Integer, Integer> getUnitChoice() {
        return unitChoice;
    }
}
