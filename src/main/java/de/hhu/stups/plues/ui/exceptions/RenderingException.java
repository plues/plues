package de.hhu.stups.plues.ui.exceptions;

public class RenderingException extends Exception {
  public RenderingException(final Exception exc) {
    super(exc);
  }

  public RenderingException(final String message, final Throwable exc) {
    super(message, exc);
  }
}
