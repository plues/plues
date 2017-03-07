package de.hhu.stups.plues.tasks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;

import javax.annotation.Nullable;

@FunctionalInterface
public interface PdfRenderingTaskFactory {
  @Inject
  PdfRenderingTask create(@Assisted("major") Course major,
                          @Assisted("minor") @Nullable Course minor,
                          @Assisted final SolverTask<FeasibilityResult> solverTask);
}
