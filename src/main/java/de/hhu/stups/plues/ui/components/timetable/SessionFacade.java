package de.hhu.stups.plues.ui.components.timetable;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.Session;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SessionFacade {
  private final Session session;

  private final ObjectProperty<Slot> slotObjectProperty = new SimpleObjectProperty<>();
  private final Set<Course> courses;
  private final List<String> abstractUnitKeys;
  private HashSet<AbstractUnit> intendedAbstractUnits;

  /**
   * A class that facades a session for ui representation.
   *
   * @param session the session to facade
   */
  public SessionFacade(final Session session) {
    this.session = session;
    final Set<AbstractUnit> abstractUnits = this.getIntendedAbstractUnits();

    courses = abstractUnits.stream()
        .map(AbstractUnit::getModules)
        .flatMap(Set::stream)
        .map(Module::getCourses)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());

    abstractUnitKeys = abstractUnits.stream()
        .map(AbstractUnit::getKey)
        .sorted(String::compareTo)
        .collect(Collectors.toList());

    slotObjectProperty.set(new Slot(getDayOfWeek(), session.getTime()));
  }

  private DayOfWeek getDayOfWeek() {
    return session.getDayOfWeek();
  }

  public ObjectProperty<Slot> slotProperty() {
    return slotObjectProperty;
  }

  public Slot getSlot() {
    return slotObjectProperty.get();
  }

  private List<String> getAbstractUnitKeys() {
    return this.abstractUnitKeys;
  }


  /**
   * Set slot of SessionFacade and session.
   *
   * @param slot SessionFacade.Slot
   */
  public void setSlot(final Slot slot) {
    session.setDay(slot.getDayString());
    session.setTime(slot.getTime());
    Platform.runLater(() -> slotObjectProperty.set(slot));
  }


  public Session getSession() {
    return session;
  }

  public int getId() {
    return session.getId();
  }

  private String getUnitKey() {
    return session.getGroup().getUnit().getKey();
  }

  private Integer getGroupId() {
    return getGroup().getId();
  }

  public Group getGroup() {
    return session.getGroup();
  }

  public String getTitle() {
    return session.getGroup().getUnit().getTitle();
  }

  /**
   * Compute a text representation for the current session based on a given
   * {@link de.hhu.stups.plues.ui.components.timetable.SessionDisplayFormat}.
   *
   * @param displayFormat SessionDisplayFormat
   * @return String representation of the session
   */
  @SuppressWarnings("WeakerAccess")
  public String displayText(final SessionDisplayFormat displayFormat) {
    final String representation;
    switch (displayFormat) {
      case TITLE:
        representation = this.getTitle();
        break;
      case ABSTRACT_UNIT_KEYS:
        final String unitKeys = this.getAbstractUnitKeys().stream()
            .map(SessionFacade::trimUnitKey).collect(Collectors.joining(", "));
        // display session title if there are no abstract units
        representation = unitKeys.isEmpty() ? this.toString() : unitKeys;
        break;
      case UNIT_KEY:
      default:
        representation = String.format("%s/%d", this.getUnitKey(), this.getGroupId());
        break;
    }
    return representation;
  }

  /**
   * Adapt a unit key to be displayed within the timetable view, i.e. remove the key's prefix for
   * WiWi data like 'W-WiWi' or 'W-Wichem' and for all other data remove the first letter in the
   * key, e.g. 'P-..'.
   */
  @SuppressWarnings("unused")
  private static String trimUnitKey(final String unitKey) {
    final List<String> splittedKey = Arrays.asList(unitKey.split("-"));
    if ("w".equalsIgnoreCase(splittedKey.get(0))) {
      return splittedKey.subList(2, splittedKey.size()).stream()
          .collect(Collectors.joining("-"));
    } else {
      return splittedKey.subList(1, splittedKey.size()).stream()
          .collect(Collectors.joining("-"));
    }
  }

  /**
   * Build a displayTextComparator to compare sessionFacade objects based on a given
   * SessionDisplayFormat.
   *
   * @param sessionDisplayFormat SessionDisplayFormat
   * @return Comparator for SessionFacade objects
   */
  public static Comparator<SessionFacade> displayTextComparator(
      final SessionDisplayFormat sessionDisplayFormat) {

    switch (sessionDisplayFormat) {
      case TITLE:
        return Comparator.comparing(SessionFacade::getTitle);
      case ABSTRACT_UNIT_KEYS:
        return Comparator.comparing(facade -> facade.getAbstractUnitKeys().toString());
      case UNIT_KEY:
      default:
        return Comparator.comparing(SessionFacade::getUnitKey);
    }
  }

  public static class Slot {
    private final DayOfWeek day;
    private final Integer time;

    /**
     * Create a new Slot object.
     *
     * @param day  DayOfWeek for the slot
     * @param time integer representing the time slot
     */
    public Slot(final DayOfWeek day, final Integer time) {
      this.day = day;
      this.time = time;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null || !(obj instanceof Slot)) {
        return false;
      }
      if (obj == this) {
        return true;
      }

      final Slot slot = (Slot) obj;

      return slot.day.equals(day) && slot.time.equals(time);
    }

    @Override
    public int hashCode() {
      return Objects.hash(day, time);
    }

    /**
     * Returns the string representation of the day of this slot.
     *
     * @return String representation of the day
     */
    @SuppressWarnings("WeakerAccess")
    public String getDayString() {
      final String dayString = TimetableMisc.shortDayOfWeekMap.get(day);

      if (dayString == null) {
        return "sun";
      }

      return dayString;
    }

    @Override
    public String toString() {
      final String timeString = String.valueOf(6 + time * 2) + ":30";
      return String.format("%s, %s", getDayString(), timeString);
    }

    public Integer getTime() {
      return time;
    }
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isTentative() {
    return session.isTentative();
  }

  @Override
  public String toString() {
    return session.toString();
  }

  public Set<Integer> getUnitSemesters() {
    return session.getGroup().getUnit().getSemesters();
  }

  /**
   * <p>Calculates all semesters that this session is intended to be heard in. This is not
   * necessarily the same set of semesters as this session is provided in.</p>
   *
   * <p>Usually this is a subset of the provided semesters but it is in fact possible that those
   * sets are disjunct. This can happen due to bad planning.</p>
   *
   * @return A set of semesters where this session should be heard in
   */
  @SuppressWarnings("unused")
  public Set<Integer> getIntendedSemesters() {
    return getIntendedAbstractUnits().stream()
        .flatMap(abstractUnit -> abstractUnit.getModuleAbstractUnitSemesters().stream()
            .map(ModuleAbstractUnitSemester::getSemester))
        .collect(Collectors.toSet());
  }

  /**
   * Calculates all courses that this session is part of.
   *
   * @return A set of courses that this session is part of
   */
  public Set<Course> getIntendedCourses() {
    return courses;
  }

  /**
   * Compute all abstract Units this session is associated to.
   *
   * @return Set of abstract units
   */
  public Set<AbstractUnit> getIntendedAbstractUnits() {
    if (intendedAbstractUnits == null) {
      intendedAbstractUnits = new HashSet<>(session.getGroup().getUnit().getAbstractUnits());
    }
    return intendedAbstractUnits;
  }
}
