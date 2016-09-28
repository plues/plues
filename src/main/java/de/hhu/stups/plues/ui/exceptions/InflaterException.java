package de.hhu.stups.plues.ui.exceptions;

public class InflaterException extends RuntimeException {
  public InflaterException(Exception exception) {
    super(exception.getMessage());
  }
}
