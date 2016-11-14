package de.hhu.stups.plues.prob;

import de.hhu.stups.plues.prob.report.Pair;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class ReportData {

  private Map<String, Map<Integer, Set<Integer>>> impossibleCourseModuleAbstractUnits;
  private Set<String> impossibleCourses;
  private Set<String> impossibleCoursesBecauseOfImpossibleModules;
  private Map<String, Map<Integer, Set<Pair<Integer>>>> impossibleCourseModuleAbstractUnitPairs;
  private Map<Integer, Set<Integer>> impossibleAbstractUnitsInModule;
  private Set<Integer> incompleteModules;
  private Map<String, Set<Integer>> mandatoryModules;
  private Map<Integer, Set<Integer>> quasiMandatoryModuleAbstractUnits;
  private Map<Integer, Set<Pair<Integer>>> redundantUnitGroups;
  private Set<Integer> impossibleModulesBecauseOfMissingElectiveAbstractUnits;
  private Set<String> impossibleCoursesBecauseOfImpossibleModuleCombinations;
  private Set<ModuleAbstractUnitUnitSemesterConflict> moduleAbstractUnitUnitSemesterConflicts;

  ReportData() {
    // package private constructor
  }

  /**
   * Get the list of course keys "bk-phi-h-2013" of all courses that are impossible to complete for
   * any reason.
   *
   * @return Set of Strings representing the course keys
   */
  public Set<String> getImpossibleCourses() {
    return impossibleCourses;
  }

  void setImpossibleCourses(final Set<String> impossibleCourses) {
    this.impossibleCourses = impossibleCourses;
  }

  /**
   * B: impossible_courses_because_of_impossible_modules
   * Set of course keys for those course that are impossible to complete due to modules that are
   * impossible to complete. E.g. module has less elective abstract units than required.
   *
   * @return Set of Strings representing the course keys
   */
  public Set<String> getImpossibleCoursesBecauseofImpossibleModules() {
    return impossibleCoursesBecauseOfImpossibleModules;
  }

  void setImpossibleCoursesBecauseOfImpossibleModules(
      final Set<String> impossibleCoursesBecauseOfImpossibleModules) {
    this.impossibleCoursesBecauseOfImpossibleModules = impossibleCoursesBecauseOfImpossibleModules;
  }

  /**
   * B: impossible_courses_module_combinations
   * are those courses with (implicitly)
   * mandatory modules that contain pairs of mandatory abstract units which
   * each have one unit and which is the same in both abstract units, hence it
   * is impossible to choose a different unit for each abstract unit in
   * question.
   *
   * @return Map from course key to a map from module ID to pairs of abstract unit IDs
   */
  public Map<String, Map<Integer, Set<Pair<Integer>>>>
      getImpossibleCourseModuleAbstractUnitPairs() {
    return impossibleCourseModuleAbstractUnitPairs;
  }

  void setImpossibleCourseModuleAbstractUnitPairs(
      final Map<String, Map<Integer, Set<Pair<Integer>>>> impossibleCourseModuleAbstractUnitPairs) {
    this.impossibleCourseModuleAbstractUnitPairs = impossibleCourseModuleAbstractUnitPairs;
  }

  /**
   * B: impossible_module_abstract_unit.
   * module and corresponding abstract unit pairs for which it is impossible to choose a unit
   * that satisfies the semester requirements
   *
   * @return Map from module ID to a set of abstract unit IDs
   */
  public Map<Integer, Set<Integer>> getImpossibleAbstractUnitsInModule() {
    return impossibleAbstractUnitsInModule;
  }

  void setImpossibleAbstractUnitsInModule(
      final Map<Integer, Set<Integer>> impossibleModuleAbstractUnit) {
    this.impossibleAbstractUnitsInModule = impossibleModuleAbstractUnit;
  }

  /**
   * B: incomplete_modules
   * Modules that contain less elective abstract units than required according
   * to the module definition.  This does not affect the validation, in the
   * sense that we only consider the minimum of required and available modules
   * (needed among other things in UC computation)
   *
   * @return Set of module IDs
   */
  public Set<Integer> getIncompleteModules() {
    return incompleteModules;
  }

  void setIncompleteModules(final Set<Integer> incompleteModules) {
    this.incompleteModules = incompleteModules;
  }

  /**
   * B: mandatory_modules.
   * mandatory and quasi_mandatory modules
   *
   * @return Map from course key to a set of module IDs
   */
  public Map<String, Set<Integer>> getMandatoryModules() {
    return mandatoryModules;
  }

  void setMandatoryModules(final Map<String, Set<Integer>> mandatoryModules) {
    this.mandatoryModules = mandatoryModules;
  }

  /**
   * B: quasi_mandatory_module_abstract_units.
   * modules and set of abstract units that are elective but quasi-mandatory,
   * because the module contains exactly as much elective abstract units as it
   * requires for its completion
   *
   * @return Map from module ID to a set of abstract unit IDs
   */
  public Map<Integer, Set<Integer>> getQuasiMandatoryModuleAbstractUnits() {
    return quasiMandatoryModuleAbstractUnits;
  }

  void setQuasiMandatoryModuleAbstractUnits(
      final Map<Integer, Set<Integer>> quasiMandatoryModuleAbstractUnits) {
    this.quasiMandatoryModuleAbstractUnits = quasiMandatoryModuleAbstractUnits;
  }

  /**
   * B: redundant_unit_groups.
   * Pairs of groups for each unit that have the same half_semester and contain
   * the same sessions (same day and time) hence one could be removed
   *
   * @return Map from unit id to a set of pairs of group IDs
   */
  public Map<Integer, Set<Pair<Integer>>> getRedundantUnitGroups() {
    return redundantUnitGroups;
  }

  void setRedundantUnitGroups(final Map<Integer, Set<Pair<Integer>>> redundantUnitGroups) {
    this.redundantUnitGroups = redundantUnitGroups;
  }

  /**
   * B: impossible_course_abstract_units.
   * Are those courses that contain mandatory
   * abstract units without units (due to missing links or invalid import data)
   * in (implicitly) mandatory modules such that it is impossible to choose a
   * unit in any of these courses.
   *
   * @return Map from course to a map of module to sets of abstract units
   */
  public Map<String, Map<Integer, Set<Integer>>> getImpossibleCourseModuleAbstractUnits() {
    return impossibleCourseModuleAbstractUnits;
  }

  void setImpossibleCourseModuleAbstractUnits(
      final Map<String, Map<Integer, Set<Integer>>> impossibleCourseModuleAbstractUnits) {
    this.impossibleCourseModuleAbstractUnits = impossibleCourseModuleAbstractUnits;
  }

  /**
   * B: impossible_modules_because_of_missing_elective_abstract_units.
   * modules which are impossible, because they contain less valid (with a proper unit
   * associated to it) elective abstract units than required by the module definition.
   */
  public Set<Integer> getImpossibleModulesBecauseOfMissingElectiveAbstractUnits() {
    return this.impossibleModulesBecauseOfMissingElectiveAbstractUnits;
  }

  void setImpossibleModulesBecauseOfMissingElectiveAbstractUnits(
      final Set<Integer> impossibleModulesBecauseOfMissingElectiveAbstractUnits) {
    this.impossibleModulesBecauseOfMissingElectiveAbstractUnits
      = impossibleModulesBecauseOfMissingElectiveAbstractUnits;
  }

  /**
   * B: impossible_courses_because_of_impossible_module_combinations.
   * Impossible courses, because all module combinations contain impossible modules
   */
  void setImpossibleCoursesBecauseOfImpossibleModuleCombinations(final Set<String>
          impossibleCoursesBecauseOfImpossibleModuleCombinations) {
    this.impossibleCoursesBecauseOfImpossibleModuleCombinations
      = impossibleCoursesBecauseOfImpossibleModuleCombinations;
  }

  public Set<String> getImpossibleCoursesBecauseOfImpossibleModuleCombinations() {
    return impossibleCoursesBecauseOfImpossibleModuleCombinations;
  }


  void setModuleAbstractUnitUnitSemesterMismatch(
      final Set<ModuleAbstractUnitUnitSemesterConflict> moduleAbstractUnitUnitSemesterConflicts) {
    this.moduleAbstractUnitUnitSemesterConflicts = moduleAbstractUnitUnitSemesterConflicts;

  }

  /**
   * B: module_abstract_unit_unit_semester_mismatch.
   * pairs of abstract units and units that have no semesters in common in a given module
   */
  public Set<ModuleAbstractUnitUnitSemesterConflict> getModuleAbstractUnitUnitSemesterConflicts() {
    return moduleAbstractUnitUnitSemesterConflicts;
  }

}
