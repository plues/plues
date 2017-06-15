package de.hhu.stups.plues;

public enum HistoryChangeType {
  BACK, FORWARD;

  public boolean isBack() {
    return this.equals(BACK);
  }

  public boolean isForward() {
    return this.equals(FORWARD);
  }
}