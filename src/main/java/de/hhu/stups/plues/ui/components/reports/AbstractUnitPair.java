package de.hhu.stups.plues.ui.components.reports;

import de.hhu.stups.plues.data.entities.AbstractUnit;

public class AbstractUnitPair {
  private final AbstractUnit second;
  private final AbstractUnit first;

  public AbstractUnitPair(final AbstractUnit first, final AbstractUnit second) {
    this.first = first;
    this.second = second;
  }

  public AbstractUnit getSecond() {
    return second;
  }

  public AbstractUnit getFirst() {
    return first;
  }

}
