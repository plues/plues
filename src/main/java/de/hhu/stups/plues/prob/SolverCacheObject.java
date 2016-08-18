package de.hhu.stups.plues.prob;

/**
 * The object to be stored in the solver's cache. The class provides the last access time to manage
 * the cache and the specific object which needs to be casted to the expected type during readout.
 */
public class SolverCacheObject {
  private long lastAccess;
  private Object value;

  public SolverCacheObject(Object value) {
    this.lastAccess = System.currentTimeMillis();
    this.value = value;
  }

  public Object getObject() {
    return this.value;
  }

  public void setLastAccess(long lastAccess) {
    this.lastAccess = lastAccess;
  }

  public long getLastAccess() {
    return this.lastAccess;
  }
}
