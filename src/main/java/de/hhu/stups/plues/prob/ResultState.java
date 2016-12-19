package de.hhu.stups.plues.prob;

public enum ResultState {
  FAILED, SUCCEEDED, INTERRUPTED, TIMEOUT, IMPOSSIBLE, IMPOSSIBLE_COMBINATION;

  public Boolean failed() {
    return this.equals(FAILED);
  }

  public Boolean succeeded() {
    return this.equals(SUCCEEDED);
  }

  public boolean timedOut() {
    return this.equals(TIMEOUT);
  }

  public boolean isImpossible() {
    return this.equals(IMPOSSIBLE);
  }
}
