package de.hhu.stups.plues.ui.components.conflictmatrix;

import de.hhu.stups.plues.data.entities.Course;

public interface ResultGridCellFactory {
  ResultGridCell create(final Course ... courses);
}
