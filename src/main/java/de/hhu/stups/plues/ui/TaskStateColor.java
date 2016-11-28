package de.hhu.stups.plues.ui;

public enum TaskStateColor {
  WARNING("#FEEFB3"), FAILURE("#FFBABA"), SUCCESS("#DFF2BF"),
  WORKING("#BDE5F8"), READY("#FFFCE6"), SCHEDULED("#FFFCE6");

  private final String color;

  TaskStateColor(final String color) {
    this.color = color;
  }

  public String getColor() {
    return this.color;
  }
}
