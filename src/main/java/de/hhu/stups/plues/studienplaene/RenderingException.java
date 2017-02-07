package de.hhu.stups.plues.studienplaene;

import java.io.IOException;

public class RenderingException extends Exception {

  public RenderingException(Throwable exc) {
    super(exc);
  }

  public RenderingException(String message, Throwable exc) {
    super(message, exc);
  }
}
