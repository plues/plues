package de.hhu.stups.plues.prob.report;

import java.util.Objects;

@SuppressWarnings("unused")
public class Triple<T> {
  private final T first;
  private final T second;
  private final T third;

  /**
   * An object to obtain three values of the same type, e.g. to use within the table view to
   * represent report data.
   */
  public Triple(final T first, final T second, final T third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    Triple<?> triple = (Triple<?>) other;
    return Objects.equals(first, triple.first)
        && Objects.equals(second, triple.second)
        && Objects.equals(third, triple.third);
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second, third);
  }

  public T getFirst() {
    return this.first;
  }

  public T getSecond() {
    return this.second;
  }

  public T getThird() {
    return this.third;
  }

}
