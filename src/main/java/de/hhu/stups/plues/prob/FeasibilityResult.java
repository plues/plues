package de.hhu.stups.plues.prob;

import java.util.Map;
import java.util.Set;

public class FeasibilityResult {
    private final Map<Integer, Integer> groupChoice;
    private final Map<String, Set<Integer>> moduleChoice;
    private final Map<Integer, Integer> semesterChoice;
    private final Map<Integer, Integer> unitChoice;

    FeasibilityResult(final Map<String, Set<Integer>> moduleChoice,
                      final Map<Integer, Integer> unitChoice,
                      final Map<Integer, Integer> semesterChoice,
                      final Map<Integer, Integer> groupChoice) {

        this.moduleChoice = moduleChoice;
        this.semesterChoice = semesterChoice;
        this.unitChoice = unitChoice;
        this.groupChoice = groupChoice;
    }

    public final Map<Integer, Integer> getGroupChoice() {
        return groupChoice;
    }

    public final Map<String, Set<Integer>> getModuleChoice() {
        return moduleChoice;
    }

    public final Map<Integer, Integer> getSemesterChoice() {
        return semesterChoice;
    }

    public final Map<Integer, Integer> getUnitChoice() {
        return unitChoice;
    }
}
