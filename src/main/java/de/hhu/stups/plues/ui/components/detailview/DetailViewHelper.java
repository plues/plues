package de.hhu.stups.plues.ui.components.detailview;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import javafx.event.EventHandler;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

public class DetailViewHelper {

  private DetailViewHelper() {
    // static helper class
    throw new IllegalAccessError("Utility Class");
  }

  /**
   * Create handler for onClick event for abstract units.
   */
  public static EventHandler<MouseEvent> getAbstractUnitMouseHandler(
      final TableView<AbstractUnit> abstractUnitTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final AbstractUnit abstractUnit = abstractUnitTableView.getSelectionModel().getSelectedItem();
      router.transitionTo(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW, abstractUnit);
    };
  }

  /**
   * Create handler for onClick event for abstract units.
   */
  public static EventHandler<MouseEvent> getCourseMouseHandler(
      final TableView<Course> courseTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final Course course = courseTableView.getSelectionModel().getSelectedItem();
      router.transitionTo(RouteNames.COURSE_DETAIL_VIEW, course);
    };
  }

  /**
   * Create handler for onClick event for units in group table.
   */
  public static EventHandler<MouseEvent> getGroupMouseHandler(
      final TableView<Group> groupTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      Unit unit = groupTableView.getSelectionModel().getSelectedItem().getUnit();
      router.transitionTo(RouteNames.UNIT_DETAIL_VIEW, unit);
    };
  }

  /**
   * Create handler for onClick event for units.
   */
  public static EventHandler<MouseEvent> getUnitMouseHandler(
      final TableView<Unit> unitTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final Unit unit = unitTableView.getSelectionModel().getSelectedItem();
      router.transitionTo(RouteNames.UNIT_DETAIL_VIEW, unit);
    };
  }

  /**
   * Create handler for onClick event for modules.
   */
  public static EventHandler<MouseEvent> getModuleMouseHandler(
      final TableView<Module> moduleTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final Module module = moduleTableView.getSelectionModel().getSelectedItem();
      router.transitionTo(RouteNames.MODULE_DETAIL_VIEW, module);
    };
  }

  /**
   * Create handler for onClick event for session.
   */
  public static EventHandler<MouseEvent> getSessionMouseHandler(
      final TableView<Session> sessionTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final Session session = sessionTableView.getSelectionModel().getSelectedItem();
      router.transitionTo(RouteNames.SESSION_DETAIL_VIEW, session);
    };
  }

  /**
   * Create table cell.
   */
  public static TableCell<Module, String> createTableCell() {
    return new TableCell<Module, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText(null);
          return;
        }

        setText(item);
      }
    };
  }
}
