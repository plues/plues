package de.hhu.stups.plues;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class DelayedTest {
  @Test
  public void whenAvailableIsNotCalledWhenObjectIsNotSet() throws Exception {
    final Delayed<Integer> delayed = new Delayed<>();
    delayed.whenAvailable(t -> fail("Handler was called, although object was never set"));
  }

  @Test
  public void set() throws Exception {
    final boolean[] cond = {false};
    final Delayed<Integer> delayed = new Delayed<>();

    delayed.whenAvailable(t -> cond[0] = true);
    assertFalse(cond[0]);

    delayed.set(0);
    assertTrue(cond[0]);
  }

  @Test
  public void handlerIsCalledImmediatelyIfObjectIsSet() {
    final boolean[] cond = {false};
    final Delayed<Integer> delayed = new Delayed<>();
    delayed.set(0);
    assertFalse(cond[0]);

    delayed.whenAvailable(t -> cond[0] = true);
    assertTrue(cond[0]);
  }

}
