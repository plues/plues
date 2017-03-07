package de.hhu.stups.plues.studienplaene;

public class RenderingException extends Exception {

  public RenderingException(Throwable exc) {
    super(exc);
  }

  public RenderingException(String message, Throwable exc) {
    super(message, exc);
  }
}
