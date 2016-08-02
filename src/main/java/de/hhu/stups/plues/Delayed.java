package de.hhu.stups.plues;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class Delayed<T> {
    private T instance;
    private List<Consumer<T>> listeners;

    public Delayed() {
        this.listeners = new LinkedList<>();
    }

    public void whenAvailable(Consumer<T> r) {
        if (this.instance != null) {
            r.accept(this.instance);
        } else {
            this.listeners.add(r);
        }
    }

    public void set(T instance) {
        this.instance = instance;
        for (Consumer<T> r : this.listeners) {
            r.accept(this.instance);
        }
        this.listeners.clear();
    }
}
