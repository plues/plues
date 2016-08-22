package de.hhu.stups.plues.prob;

import java.util.LinkedHashMap;
import java.util.Map;

class SolverCache extends LinkedHashMap<String,Object> {

  private int cacheSize;

  /**
   * Cache the results computed by the solver to increase performance. Since the solver gets
   * injected and is single threaded by default we probably would have no problems without
   * synchronization but never the less accessing the cache should be done in a synchronized block.
   */
  SolverCache(int cacheSize) {
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
  protected boolean removeEldestEntry(Map.Entry<String, Object> eldestEntry) {
    return this.size() > cacheSize;
  }
}
