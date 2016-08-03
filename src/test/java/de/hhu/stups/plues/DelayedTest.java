package de.hhu.stups.plues;

import org.junit.Test;

import static org.junit.Assert.*;

public class DelayedTest {
    @Test
    public void whenAvailableIsNotCalledWhenObjectIsNotSet() throws Exception {
        Delayed<Integer> d = new Delayed<>();
        d.whenAvailable(t -> fail("Handler was called, although object was never set"));
    }

    @Test
    public void set() throws Exception {
        final boolean[] cond = {false};
        Delayed<Integer> d = new Delayed<>();

        d.whenAvailable(t -> cond[0] = true);
        assertFalse(cond[0]);

        d.set(0);
        assertTrue(cond[0]);
    }

    @Test
    public void handlerIsCalledImmediatelyIfObjectIsSet() {
        final boolean[] cond = {false};
        Delayed<Integer> d = new Delayed<>();
        d.set(0);
        assertFalse(cond[0]);

        d.whenAvailable(t -> cond[0] = true);
        assertTrue(cond[0]);
    }

}