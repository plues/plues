package de.hhu.stups.plues.ui.components.detailview;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.routes.Router;
import javafx.event.EventHandler;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

public class DetailViewHelper {

  private DetailViewHelper() {
    // static helper class
    throw new IllegalAccessError("Utility Class");
  }

  /**
   * Create handler for onClick event for abstract units.
   * @param abstractUnitTableView TableView to get abstract unit from
   * @return Handler
   */
  public static EventHandler<MouseEvent> getAbstractUnitMouseHandler(
      final TableView<AbstractUnit> abstractUnitTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final AbstractUnit abstractUnit = abstractUnitTableView.getSelectionModel().getSelectedItem();
      router.transitionTo("abstractUnitDetailView", abstractUnit);
    };
  }

  /**
   * Create handler for onClick event for units.
   * @param unitTableView TableView to get unit from
   * @return Handler
   */
  public static EventHandler<MouseEvent> getUnitMouseHandler(
      final TableView<Unit> unitTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final Unit unit = unitTableView.getSelectionModel().getSelectedItem();
      router.transitionTo("unitDetailView", unit);
    };
  }

  /**
   * Create handler for onClick event for modules.
   * @param moduleTableView TableView to get module from
   * @return Handler
   */
  public static EventHandler<MouseEvent> getModuleMouseHandler(
      final TableView<Module> moduleTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final Module module = moduleTableView.getSelectionModel().getSelectedItem();
      router.transitionTo("moduleDetailView", module);
    };
  }

  /**
   * Create handler for onClick event for session.
   * @param sessionTableView TableView to get module from
   * @return Handler
   */
  public static EventHandler<MouseEvent> getSessionMouseHandler(
      final TableView<Session> sessionTableView,
      final Router router) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      final Session session = sessionTableView.getSelectionModel().getSelectedItem();
      router.transitionTo("sessionDetailView", session);
    };
  }
}
