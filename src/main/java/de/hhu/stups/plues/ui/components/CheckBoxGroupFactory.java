package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;

import java.util.List;

public interface CheckBoxGroupFactory {
  CheckBoxGroup create(Module module, List<AbstractUnit> units);
}
