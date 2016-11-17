package de.hhu.stups.plues.tasks;

import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.services.SolverService;

public interface SolverServiceFactory {
  SolverService create(Solver solver);
}
