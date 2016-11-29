package de.hhu.stups.plues.prob.report;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("unused")
public class Pair<T> implements Serializable {
  private static final long serialVersionUID = 6459045002667850077L;

  private final T second;
  private final T first;

  public Pair(final T first, final T second) {
    this.first = first;
    this.second = second;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    final Pair<?> pair = (Pair<?>) other;
    return Objects.equals(second, pair.second)
      && Objects.equals(first, pair.first);
  }

  @Override
  public int hashCode() {
    return Objects.hash(second, first);
  }

  public T getSecond() {
    return second;
  }

  public T getFirst() {
    return first;
  }
}
