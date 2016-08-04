package de.hhu.stups.plues.prob;

public interface SolverFactory {
    Solver create(String modelPath);
}
