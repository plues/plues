package de.hhu.stups.plues.studienplaene;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.FeasibilityResult;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class DataPreparatory {

  private Map<AbstractUnit, Module> unitModule;
  private Map<AbstractUnit, Integer> unitSemester;
  private Map<AbstractUnit, Group> unitGroup;

  DataPreparatory(final Store store,
                  final FeasibilityResult feasibilityResult) {
    readData(store, feasibilityResult);
  }

  private static Map<AbstractUnit, Integer> filterSemester(final Store store,
                                                           final FeasibilityResult result) {
    return result.getSemesterChoice().entrySet().stream()
      .collect(Collectors.toMap(
        e -> store.getAbstractUnitById(e.getKey()),
        Map.Entry::getValue));
  }

  /**
   * @param store Store
   * @param result Object containing maps to collect different choices of data
   * @return map of abstract unit.id to chosen group object
   */
  private static Map<AbstractUnit, Group> filterUnitGroup(final Store store,
                                                          final FeasibilityResult result) {
    return result.getGroupChoice().entrySet().stream().collect(Collectors.toMap(
      e -> store.getAbstractUnitById(e.getKey()), e -> store.getGroupById(e.getValue())));
  }

  /**
   * Collect the pairs of abstract unit and module for all selected.
   * @param store Store
   * @param result Object containing maps to collect different choices of data
   * @return Map associating Abstract Units to the Module they were chosen in
   */
  private static Map<AbstractUnit, Module> filterModules(final Store store,
      final FeasibilityResult result) {
    return collectChosenCourseModules(store, result);
  }

  /**
   * Collect the abstract unit -> module pairs.
   *
   * @param store  Store
   * @param result Object containing maps to collect different choices of data
   * @return Map of Abstract Unit to corresponding Module
   */
  private static Map<AbstractUnit, Module> collectChosenCourseModules(final Store store,
      final FeasibilityResult result) {

    final Map<Integer, Set<Integer>> abstractUnitChoice = result.getAbstractUnitChoice();

    if (abstractUnitChoice.isEmpty()) {
      throw new AssertionError("abstractUnitChoice is empty");
    }

    final Map<AbstractUnit, Module> chosenModules
        = abstractUnitChoice.entrySet().stream().flatMap(entry -> {
          final Module module = store.getModuleById(entry.getKey());
          return entry.getValue().stream()
            .map(store::getAbstractUnitById)
            .collect(Collectors.toMap(o -> o, o -> module)).entrySet().stream();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (new HashSet<>(chosenModules.values()).size() != abstractUnitChoice.keySet().size()) {
      throw new AssertionError("Collection sizes differ");
    }

    return chosenModules;
  }

  private void readData(final Store store, final FeasibilityResult result) {
    unitModule = filterModules(store, result);
    unitGroup = filterUnitGroup(store, result);
    unitSemester = filterSemester(store, result);

    if (unitModule.size() != unitGroup.size() || unitGroup.size() != unitSemester.size()) {
      throw new AssertionError("Collection sizes differ");
    }
  }

  final Map<AbstractUnit, Integer> getUnitSemester() {
    return unitSemester;
  }

  final Map<AbstractUnit, Module> getUnitModule() {
    return unitModule;
  }

  final Map<AbstractUnit, Group> getUnitGroup() {
    return unitGroup;
  }
}
