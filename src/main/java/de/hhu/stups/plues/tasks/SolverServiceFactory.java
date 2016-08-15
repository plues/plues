package de.hhu.stups.plues.tasks;

import de.hhu.stups.plues.prob.Solver;

public interface SolverServiceFactory {
  SolverService create(Solver solver);
}
