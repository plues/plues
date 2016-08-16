package de.hhu.stups.plues.prob;

import com.google.inject.name.Named;

public interface SolverFactory {
  @Named("prob")
  ProBSolver createProbSolver(String modelPath);

  @Named("mock")
  MockSolver createMockSolver(String modelPath);
}
