package de.hhu.stups.plues.studienplaene;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.prob.FeasibilityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

class DataPreparatory {

  private static final Logger logger = LoggerFactory.getLogger(DataPreparatory.class);

  private Map<AbstractUnit, Module> unitModule;
  private Map<AbstractUnit, Integer> unitSemester;
  private Map<AbstractUnit, Group> unitGroup;

  DataPreparatory(final Store store,
                  final FeasibilityResult feasibilityResult,
                  final Course major,
                  @Nullable final Course minor) {
    readData(store, feasibilityResult, major, minor);
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
   * Collect the pais of abstract unit and module for all selected.
   * @param store Store
   * @param result Object containing maps to collect different choices of data
   * @param major Course
   * @param minor Course
   * @return Map associating Abstract Units to the Module they were chosen in
   */
  private static Map<AbstractUnit, Module> filterModules(final Store store,
      final FeasibilityResult result,
      final Course major, @Nullable final Course minor) {
    final HashMap<AbstractUnit, Module> modules = new HashMap<>();
    modules.putAll(collectChosenCourseModules(store, result, major));
    if (minor != null) {
      modules.putAll(collectChosenCourseModules(store, result, minor));

    }
    return modules;
  }

  /**
   * Collect the abstract unit -> module pairs for a given course, identified by its key.
   *
   * @param store  Store
   * @param result Object containing maps to collect different choices of data
   * @param course Course
   * @return Map of Abstract Unit to corresponding Module
   */
  private static Map<AbstractUnit, Module> collectChosenCourseModules(final Store store,
      final FeasibilityResult result, final Course course) {

    final Map<Integer, Integer> semesterChoice = result.getSemesterChoice();
    final Map<String, Set<Integer>> moduleChoice = result.getModuleChoice();

    final java.util.Set<Integer> courseModules = moduleChoice.get(course.getKey());
    if (courseModules.isEmpty()) {
      throw new AssertionError("courseModules is empty");
    }

    return courseModules.stream()
      .map(store::getModuleById)
      .peek(module -> {
        if (!course.getModules().contains(module)) {
          throw new AssertionError("Expected course to contain module " + module.getTitle());
        }
      })
      .flatMap(module -> module.getModuleAbstractUnitSemesters().stream())
      .filter(maus -> {
        // find if the pair of abstract unit and semester exists for this module
        final AbstractUnit au = maus.getAbstractUnit();
        final Integer s = maus.getSemester();
        return semesterChoice.containsKey(au.getId())
            && semesterChoice.get(au.getId()).equals(s);
      })
      .collect(
          Collectors.toMap(
            ModuleAbstractUnitSemester::getAbstractUnit,
            ModuleAbstractUnitSemester::getModule));
  }

  private void readData(final Store store, final FeasibilityResult result,
                        final Course major, @Nullable final Course minor) {
    unitModule = filterModules(store, result, major, minor);
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
