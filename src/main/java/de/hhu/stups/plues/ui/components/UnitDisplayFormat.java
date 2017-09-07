package de.hhu.stups.plues.ui.components;

/**
 * The display format of units in a generated PDF timetable which is either the title or the
 * id/key.
 */
public enum UnitDisplayFormat {
  TITLE, ID;

  public boolean isTitle() {
    return TITLE.equals(this);
  }

  @Override
  public String toString() {
    if (TITLE.equals(this)) {
      return "title";
    } else {
      return "id";
    }
  }
}
