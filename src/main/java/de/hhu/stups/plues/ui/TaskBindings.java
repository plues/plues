package de.hhu.stups.plues.ui;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.concurrent.Task;
import javafx.scene.text.Text;

public class TaskBindings {

  private static final String ICON_SIZE = "50";

  TaskBindings() {
    throw new IllegalAccessError("Utility class");
  }

  /**
   * Wrapper for collecting the icon binding for a given task that uses the default icon size.
   *
   * @param task Given task
   * @return Object binding depending on the tasks state
   */
  public static ObjectBinding<Text> getIconBinding(final Task<?> task) {
    return getIconBinding(ICON_SIZE, task);
  }

  /**
   * Collect icon binding for a given task and given icon size. Depends on how the task behaves.
   *
   * @param task     Given task
   * @param iconSize The given icon size.
   * @return Object binding depending on the tasks state
   */
  public static ObjectBinding<Text> getIconBinding(final String iconSize,
                                                   final Task<?> task) {
    return Bindings.createObjectBinding(() -> {
      final FontAwesomeIcon symbol = getIcon(task);
      if (symbol == null) {
        return null;
      }

      final FontAwesomeIconFactory iconFactory = FontAwesomeIconFactory.get();
      return iconFactory.createIcon(symbol, iconSize);

    }, task.stateProperty());
  }

  private static FontAwesomeIcon getIcon(final Task<?> task) {
    final FontAwesomeIcon symbol;

    switch (task.getState()) {
      case SUCCEEDED:
        symbol = FontAwesomeIcon.CHECK;
        break;
      case CANCELLED:
        symbol = FontAwesomeIcon.QUESTION;
        break;
      case FAILED:
        symbol = FontAwesomeIcon.REMOVE;
        break;
      case READY:
      case SCHEDULED:
        symbol = FontAwesomeIcon.CLOCK_ALT;
        break;
      case RUNNING:
      default:
        symbol = FontAwesomeIcon.SPINNER;
        break;
    }
    return symbol;
  }

  /**
   * Collect string binding for given task.
   *
   * @param task Given task
   * @return String binding depending on the tasks state
   */
  public static StringBinding getStyleBinding(final Task<?> task) {
    return Bindings.createStringBinding(() -> {
      final String color = getColor(task);
      final String centerIcon = "-fx-alignment: center;";
      if (color == null) {
        return centerIcon;
      }
      return "-fx-background-color: " + color + "; " + centerIcon;
    }, task.stateProperty());
  }

  private static String getColor(final Task<?> task) {
    final TaskStateColor color;

    switch (task.getState()) {
      case SUCCEEDED:
        color = TaskStateColor.SUCCESS;
        break;
      case CANCELLED:
        color = TaskStateColor.WARNING;
        break;
      case FAILED:
        color = TaskStateColor.FAILURE;
        break;
      case READY:
        color = TaskStateColor.READY;
        break;
      case SCHEDULED:
        color = TaskStateColor.SCHEDULED;
        break;
      case RUNNING:
      default:
        color = TaskStateColor.WORKING;
    }
    return color.getColor();
  }
}
