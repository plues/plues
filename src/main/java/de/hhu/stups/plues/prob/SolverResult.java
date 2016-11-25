package de.hhu.stups.plues.prob;

import de.prob.translator.types.BObject;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class SolverResult {
  private ResultState state;
  private List<BObject> value;

  @SuppressWarnings("unused")
  ResultState getState() {
    return state;
  }

  void setState(final ResultState state) {
    this.state = state;
  }

  void setValue(final List<BObject> value) {
    this.value = value;
  }

  public boolean succeeded() {
    return ResultState.SUCCEEDED.equals(state);
  }

  public boolean timeout() {
    return ResultState.TIMEOUT.equals(state);
  }

  List<BObject> getValue() {
    return value;
  }
}
