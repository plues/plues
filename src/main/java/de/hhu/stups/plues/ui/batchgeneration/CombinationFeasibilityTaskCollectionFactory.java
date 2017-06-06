package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.batchgeneration.CollectCombinationFeasibilityTasksTask;

public interface CombinationFeasibilityTaskCollectionFactory {
  CollectCombinationFeasibilityTasksTask create(Course course);
}
