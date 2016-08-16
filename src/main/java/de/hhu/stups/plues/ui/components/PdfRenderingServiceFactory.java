package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.SolverTask;

public interface PdfRenderingServiceFactory {
  PdfRenderingService create(SolverTask<FeasibilityResult> solverTask);
}
