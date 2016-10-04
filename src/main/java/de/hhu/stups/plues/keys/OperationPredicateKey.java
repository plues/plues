package de.hhu.stups.plues.keys;

import java.util.Objects;

public final class OperationPredicateKey {
  private final String operation;
  private final String predicate;

  public OperationPredicateKey(String operation, String predicate) {
    this.operation = operation;
    this.predicate = predicate;
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    OperationPredicateKey that = (OperationPredicateKey) obj;
    return Objects.equals(operation, that.operation)
        && Objects.equals(predicate, that.predicate);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(operation, predicate);
  }

  public final String getOperation() {
    return this.operation;
  }

  public final String getPredicate() {
    return this.predicate;
  }
}
