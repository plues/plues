package de.hhu.stups.plues.studienplaene;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

class DataPreparatory {

  private Map<AbstractUnit, Module> unitModule;
  private Map<AbstractUnit, Integer> unitSemester;
  private Map<AbstractUnit, Group> unitGroup;

  // constructors
  DataPreparatory(final Store store, final Map<Integer, Integer> groupChoice,
                  final Map<Integer, Integer> semesterChoice,
                  final Map<String, Set<Integer>> moduleChoice,
                  final Course major, @Nullable final Course minor) {
    readData(store, groupChoice, semesterChoice, moduleChoice, major, minor);
  }

  private static Map<AbstractUnit, Integer> filterSemester(final Store store,
                                                           final Map<Integer, Integer> sc) {
    return sc.entrySet().stream()
      .collect(Collectors.toMap(
        e -> store.getAbstractUnitById(e.getKey()),
        Map.Entry::getValue));
  }

  /**
   * @param store Store
   * @param gc    Map of abstract unit.id to group.id
   * @return map of abstract unit.id to chosen group object
   */
  private static Map<AbstractUnit, Group> filterUnitGroup(final Store store,
                                                          final Map<Integer, Integer> gc) {
    return gc.entrySet().stream().collect(Collectors.toMap(
      e -> store.getAbstractUnitById(e.getKey()), e -> store.getGroupById(e.getValue())));
  }

  /**
   * Collect the pais of abstract unit and module for all selected.
   * @param store Store
   * @param sc    Map from each abstract unit to the semester it should be attended
   * @param mc    Map from course key to the set of chosen modules in that course
   * @param major Course
   * @param minor Course
   * @return Map associating Abstract Units to the Module they were chosen in
   */
  private static Map<AbstractUnit, Module> filterModules(final Store store,
      final Map<Integer, Integer> sc, final Map<String, Set<Integer>> mc,
      final Course major, @Nullable final Course minor) {
    final HashMap<AbstractUnit, Module> modules = new HashMap<>();
    modules.putAll(collectChosenCourseModules(store, sc, mc, major));
    if (minor != null) {
      modules.putAll(collectChosenCourseModules(store, sc, mc, minor));

    }
    return modules;
  }

  /**
   * Collect the abstract unit -> module pairs for a given course, identified by its key.
   *
   * @param store  Store
   * @param sc     Map from each abstract unit to the semester it should be attended
   * @param mc     Map from course key to the set of chosen modules in that course
   * @param course Course
   * @return Map of Abstract Unit to corresponding Module
   */
  private static Map<AbstractUnit, Module> collectChosenCourseModules(final Store store,
      final Map<Integer, Integer> sc, final Map<String, java.util.Set<Integer>> mc,
      final Course course) {

    final HashMap<AbstractUnit, Module> modules = new HashMap<>();

    final java.util.Set<Integer> courseModules = mc.get(course.getKey());
    assert courseModules.size() > 0;
    for (final Integer mid : courseModules) {
      final Module m = store.getModuleById(mid);
      assert course.getModules().contains(m);
      // find if the pair of abstract unit and semester
      // exists for this module
      for (final ModuleAbstractUnitSemester maus : m.getModuleAbstractUnitSemesters()) {
        final AbstractUnit au = maus.getAbstractUnit();
        final Integer s = maus.getSemester();
        if (!sc.containsKey(au.getId())) {
          continue; // this abstract unit was not chosen at all;
        }
        if (!sc.get(au.getId()).equals(s)) {
          continue; // this abstract unit was not chosen in semester s
        }
        modules.put(au, m);
      }
    }
    return modules;
  }

  private void readData(final Store store, final Map<Integer, Integer> gc,
                        final Map<Integer, Integer> sc,
                        final Map<String, Set<Integer>> mc,
                        final Course major, @Nullable final Course minor) {
    unitModule = filterModules(store, sc, mc, major, minor);
    unitGroup = filterUnitGroup(store, gc);
    unitSemester = filterSemester(store, sc);

    assert unitModule.size() == unitGroup.size()
      && unitGroup.size() == unitSemester.size();
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
