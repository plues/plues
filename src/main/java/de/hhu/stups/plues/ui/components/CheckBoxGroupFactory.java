package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;

import java.util.List;

public interface CheckBoxGroupFactory {
  CheckBoxGroup create(Module module, List<Unit> units);
}
