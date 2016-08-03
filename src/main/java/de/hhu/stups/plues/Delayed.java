package de.hhu.stups.plues;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Delayed<T> represents an instance of T that becomes available with an arbitrary delay.
 * Consumers can register Event handlers to be executed once the T object is available.
 * If the T object is already set, new event handlers are executed immediately.
 *
 * @param <T>
 */
public class Delayed<T> {
    private T instance;
    private List<Consumer<T>> listeners;

    public Delayed() {
        this.listeners = new LinkedList<>();
    }

    public synchronized void whenAvailable(Consumer<T> r) {
        if (this.instance != null) {
            r.accept(this.instance);
        } else {
            this.listeners.add(r);
        }
    }

    public synchronized void set(T instance) {
        this.instance = instance;
        for (Consumer<T> r : this.listeners) {
            r.accept(this.instance);
        }
        this.listeners.clear();
    }
}
