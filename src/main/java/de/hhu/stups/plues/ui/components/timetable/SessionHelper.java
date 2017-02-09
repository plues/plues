package de.hhu.stups.plues.ui.components.timetable;

import de.hhu.stups.plues.data.sessions.SessionFacade;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SessionHelper {

  private SessionHelper() {
    throw new IllegalAccessError("Utility class");
  }

  /**
   * Compute the textual representation for a session facade based on a chosen display format.
   *
   * @param displayFormat DisplayFormat
   * @param sessionFacade SessionFacade
   * @return String text representation of SessionFacade
   */
  @SuppressWarnings("WeakerAccess")
  public static String displayText(final SessionDisplayFormat displayFormat,
                                   final SessionFacade sessionFacade) {
    final String representation;
    switch (displayFormat) {
      case TITLE:
        representation = sessionFacade.getTitle();
        break;
      case ABSTRACT_UNIT_KEYS:
        final String unitKeys = sessionFacade.getAbstractUnitKeys().stream()
            .map(SessionHelper::trimUnitKey).collect(Collectors.joining(", "));
        // display session title if there are no abstract units
        representation = unitKeys.isEmpty() ? sessionFacade.toString() : unitKeys;
        break;
      case UNIT_KEY:
      default:
        representation = String.format("%s/%d", sessionFacade.getUnitKey(),
          sessionFacade.getGroupId());
        break;
    }
    return representation;
  }

  /**
   * Adapt a unit key to be displayed within the timetable view, i.e. remove the key's prefix for
   * WiWi data like 'W-WiWi' or 'W-Wichem' and for all other data remove the first letter in the
   * key, e.g. 'P-..'.
   */
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
   * Build a comparator to compare sessionFacade objects based on a given SessionDisplayFormat.
   * @param sessionDisplayFormat SessionDisplayFormat
   * @return Comparator for SessionFacade objects
   */
  public static Comparator<SessionFacade> comparator(
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
}
