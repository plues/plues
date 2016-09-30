package de.hhu.stups.plues;

import com.google.inject.Provider;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Delayed represents an instance of T that becomes available with an
 * arbitrary delay.
 * Consumers can register Event handlers to be executed once the T object
 * is available.
 * If the T object is already set, new event handlers are executed immediately.
 */
public class Delayed<T> implements Provider<T> {
  private final List<Consumer<T>> listeners;
  private T instance;

  public Delayed() {
    this.listeners = new LinkedList<>();
  }

  /**
   * Register a consumer executed once the deleyaed object has been set, or immediately if it was
   * aleready set.
   *
   * @param consumer Consumer
   */
  public final synchronized void whenAvailable(final Consumer<T> consumer) {
    if (this.instance != null) {
      consumer.accept(this.instance);
    } else {
      this.listeners.add(consumer);
    }
  }

  /**
   * Set the delayed object to the passed value and trigger registered.
   *
   * @param instance of type T
   */
  public final synchronized void set(final T instance) {
    this.instance = instance;
    for (final Consumer<T> r : this.listeners) {
      r.accept(this.instance);
    }
    this.listeners.clear();
  }

  /**
   * Read the delayed values, assuming it has been set.
   *
   * @return T the value stored in the instance.
   * @throws NullPointerException if the object has not been set.
   */
  @Override
  public final synchronized T get() {
    if (this.instance == null) {
      throw new NullPointerException("Delayed object has not been initialized yet");
    }
    return this.instance;
  }
}
