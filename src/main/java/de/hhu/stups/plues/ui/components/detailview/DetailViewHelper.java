package de.hhu.stups.plues.ui.components.detailview;

import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.data.sessions.SessionFacade;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class DetailViewHelper {

  private static void openAbstractUnitDetailView(final AbstractUnit abstractUnit,
                                                 final Provider<AbstractUnitDetailView> provider) {
    final AbstractUnitDetailView abstractUnitDetailView = provider.get();
    abstractUnitDetailView.setAbstractUnit(abstractUnit);

    final Stage stage = new Stage();
    stage.setTitle(abstractUnitDetailView.getTitle());
    stage.setScene(new Scene(abstractUnitDetailView));
    stage.show();
  }

  /**
   * Open detail view for unit.
   * @param unit Unit to display
   */
  private static void openUnitDetailView(final Unit unit,
                                         final Provider<UnitDetailView> provider) {
    final UnitDetailView unitDetailView = provider.get();
    unitDetailView.setUnit(unit);

    final Stage stage = new Stage();
    stage.setTitle(unitDetailView.getTitle());
    stage.setScene(new Scene(unitDetailView));
    stage.show();
  }

  /**
   * Open detail view for module.
   * @param module Module to display
   */
  private static void openModuleDetailView(final Module module,
                                           final Provider<ModuleDetailView> provider) {
    final ModuleDetailView moduleDetailView = provider.get();
    moduleDetailView.setModule(module);

    final Stage stage = new Stage();
    stage.setTitle(moduleDetailView.getTitle());
    stage.setScene(new Scene(moduleDetailView));
    stage.show();
  }

  /**
   * Open detail view for session.
   * @param sessionFacade SessionFacade to display
   */
  private static void openSessionDetailView(final SessionFacade sessionFacade,
                                           final Provider<SessionDetailView> provider) {
    final SessionDetailView sessionDetailView = provider.get();
    sessionDetailView.setSession(sessionFacade);

    final Stage stage = new Stage();
    stage.setTitle(sessionDetailView.getTitle());
    stage.setScene(new Scene(sessionDetailView));
    stage.show();
  }


  /**
   * Create handler for onClick event for abstract units.
   * @param abstractUnitTableView TableView to get abstract unit from
   * @return Handler
   */
  public static EventHandler<MouseEvent> getAbstractUnitMouseHandler(
      final TableView<AbstractUnit> abstractUnitTableView,
      final Provider<AbstractUnitDetailView> provider) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      AbstractUnit abstractUnit = abstractUnitTableView.getSelectionModel().getSelectedItem();
      openAbstractUnitDetailView(abstractUnit, provider);
    };
  }

  /**
   * Create handler for onClick event for units.
   * @param unitTableView TableView to get unit from
   * @return Handler
   */
  public static EventHandler<MouseEvent> getUnitMouseHandler(
      final TableView<Unit> unitTableView,
      final Provider<UnitDetailView> provider) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      Unit unit = unitTableView.getSelectionModel().getSelectedItem();
      openUnitDetailView(unit, provider);
    };
  }

  /**
   * Create handler for onClick event for modules.
   * @param moduleTableView TableView to get module from
   * @return Handler
   */
  public static EventHandler<MouseEvent> getModuleMouseHandler(
      final TableView<Module> moduleTableView,
      final Provider<ModuleDetailView> provider) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      Module module = moduleTableView.getSelectionModel().getSelectedItem();
      openModuleDetailView(module, provider);
    };
  }

  /**
   * Create handler for onClick event for session.
   * @param sessionTableView TableView to get module from
   * @return Handler
   */
  public static EventHandler<MouseEvent> getSessionMouseHandler(
      final TableView<Session> sessionTableView,
      final Provider<SessionDetailView> provider) {
    return event -> {
      if (event.getClickCount() < 2) {
        return;
      }
      Session session = sessionTableView.getSelectionModel().getSelectedItem();
      openSessionDetailView(new SessionFacade(session), provider);
    };
  }
}
