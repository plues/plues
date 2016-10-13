package de.hhu.stups.plues.prob;

import de.prob.translator.types.BObject;

import java.util.List;

public class SolverResult {
  private ResultState state;
  private List<BObject> value;

  void setState(final ResultState state) {
    this.state = state;
  }

  @SuppressWarnings("unused")
  ResultState getState() {
    return state;
  }

  void setValue(final List<BObject> value) {
    this.value = value;
  }

  boolean succeeded() {
    return this.state == ResultState.SUCCEEDED;
  }

  List<BObject> getValue() {
    return value;
  }
}
