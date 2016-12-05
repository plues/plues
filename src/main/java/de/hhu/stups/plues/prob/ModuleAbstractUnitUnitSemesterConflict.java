package de.hhu.stups.plues.prob;

import java.io.Serializable;
import java.util.Set;

public class ModuleAbstractUnitUnitSemesterConflict implements Serializable {
  private static final long serialVersionUID = 6459045002667850077L;

  private final Integer moduleId;
  private final Integer abstractUnitId;
  private final Set<Integer> abstractUnitSemesters;
  private final Integer unitId;

  ModuleAbstractUnitUnitSemesterConflict(final Integer module, final Integer abstractUnit,
      final Set<Integer> semesters, final Integer unit) {
    this.moduleId = module;
    this.abstractUnitId = abstractUnit;
    this.abstractUnitSemesters = semesters;
    this.unitId = unit;
  }

  public Integer getModuleId() {
    return moduleId;
  }

  public Integer getAbstractUnitId() {
    return abstractUnitId;
  }

  public Set<Integer> getAbstractUnitSemesters() {
    return abstractUnitSemesters;
  }

  public Integer getUnitId() {
    return unitId;
  }
}
