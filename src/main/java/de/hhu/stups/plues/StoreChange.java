package de.hhu.stups.plues;

import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Session;

public class StoreChange {

  private final HistoryChangeType historyChangeType;
  private final Log log;

  StoreChange(final Log log) {
    this.log = log;
    historyChangeType = HistoryChangeType.FORWARD;
  }

  StoreChange(final Log log,
              final HistoryChangeType historyChangeType) {
    this.log = log;
    this.historyChangeType = historyChangeType;
  }

  public Session getSession() {
    return log.getSession();
  }

  public Log getLog() {
    return log;
  }

  public HistoryChangeType historyChangeType() {
    return historyChangeType;
  }
}
