package de.hhu.stups.plues.prob;

public class SolverException extends Exception {
  SolverException(final String message) {
    super(message);
  }

  public SolverException(Exception exception) {
    super(exception);
  }
}
