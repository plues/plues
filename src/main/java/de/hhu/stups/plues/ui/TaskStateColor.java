package de.hhu.stups.plues.ui;

// NOTE: Keep in Sync with index.css
public enum TaskStateColor {
  WARNING("#FEEFB3"),
  FAILURE("#FFBABA"), // IMPOSSIBLE("EA2B1F")
  SUCCESS("#DFF2BF"),
  WORKING("#5386E4"),
  READY("#FCEFEF"), SCHEDULED("#FCEFEF");

  private final String color;

  TaskStateColor(final String color) {
    this.color = color;
  }

  public String getColor() {
    return this.color;
  }
}
