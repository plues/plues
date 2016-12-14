package de.hhu.stups.plues.prob;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

class SolverCache<T> extends LinkedHashMap<OperationPredicateKey,T> {

  private final int cacheSize;

  /**
   * Cache the results computed by the solver to increase performance. Since the solver gets
   * injected and is single threaded by default we probably would have no problems without
   * synchronization but never the less accessing the cache should be done in a synchronized block.
   */
  SolverCache(final int cacheSize) {
    super(cacheSize, 0.75f, true);
    this.cacheSize = cacheSize;
  }

  /**
   * Remove the eldest entry if the cache size is exhausted.
   *
   * @param eldestEntry The eldest entry in the linked hash map, i.e. the least recently used one.
   * @return Return true if the cache size is exhausted otherwise false.
   */
  @Override
  protected boolean removeEldestEntry(final Map.Entry<OperationPredicateKey, T> eldestEntry) {
    return this.size() > cacheSize;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    if (!super.equals(other)) {
      return false;
    }
    final SolverCache<?> that = (SolverCache<?>) other;
    return cacheSize == that.cacheSize;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), cacheSize);
  }
}
