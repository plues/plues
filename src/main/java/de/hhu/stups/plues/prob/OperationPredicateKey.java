package de.hhu.stups.plues.prob;

import java.util.Objects;

final class OperationPredicateKey {
  private final String operation;
  private final String predicate;

  OperationPredicateKey(final String operation, final String predicate) {
    this.operation = operation;
    this.predicate = predicate;
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final OperationPredicateKey that = (OperationPredicateKey) obj;
    return Objects.equals(operation, that.operation)
        && Objects.equals(predicate, that.predicate);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(operation, predicate);
  }

  @Override
  public final String toString() {
    return String.format("Key: operation='%s' predicate='%s'", operation, predicate);
  }
}
