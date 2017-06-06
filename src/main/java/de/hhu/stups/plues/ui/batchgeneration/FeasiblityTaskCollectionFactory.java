package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.batchgeneration.CollectFeasibilityTasksTask;

import java.util.List;

public interface FeasiblityTaskCollectionFactory {
  CollectFeasibilityTasksTask create(List<Course> courses);
}
