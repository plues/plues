package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleLevel;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.components.detailview.AbstractUnitDetailView;
import de.hhu.stups.plues.ui.components.detailview.CourseDetailView;
import de.hhu.stups.plues.ui.components.detailview.DetailView;
import de.hhu.stups.plues.ui.components.detailview.ModuleDetailView;
import de.hhu.stups.plues.ui.components.detailview.SessionDetailView;
import de.hhu.stups.plues.ui.components.detailview.UnitDetailView;
import de.hhu.stups.plues.ui.components.timetable.SessionFacade;
import de.hhu.stups.plues.ui.layout.SceneFactory;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class DetailViewRoute implements Route {

  private final Provider<AbstractUnitDetailView> abstractUnitDetailViewProvider;
  private final Provider<CourseDetailView> courseDetailViewProvider;
  private final Provider<ModuleDetailView> moduleDetailViewProvider;
  private final Provider<SessionDetailView> sessionDetailViewProvider;
  private final Provider<UnitDetailView> unitDetailViewProvider;

  @Inject
  private DetailViewRoute(final Provider<CourseDetailView> courseDetailViewProvider,
                          final Provider<ModuleDetailView> moduleDetailViewProvider,
                          final Provider<AbstractUnitDetailView> abstractUnitDetailViewProvider,
                          final Provider<UnitDetailView> unitDetailViewProvider,
                          final Provider<SessionDetailView> sessionDetailViewProvider) {
    this.abstractUnitDetailViewProvider = abstractUnitDetailViewProvider;
    this.courseDetailViewProvider = courseDetailViewProvider;
    this.moduleDetailViewProvider = moduleDetailViewProvider;
    this.sessionDetailViewProvider = sessionDetailViewProvider;
    this.unitDetailViewProvider = unitDetailViewProvider;
  }

  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    final DetailView detailView = getDetailView(routeName, args);

    final Stage stage = new Stage();
    stage.setTitle(detailView.getTitle());
    stage.setScene(SceneFactory.create((Parent) detailView));
    stage.show();

  }

  private DetailView getDetailView(final RouteNames routeName, final Object[] args) {
    switch (routeName) {
      case MODULE_DETAIL_VIEW:
        return getDetailView((Module) args[0]);
      case ABSTRACT_UNIT_DETAIL_VIEW:
        return getDetailView((AbstractUnit) args[0]);
      case COURSE_DETAIL_VIEW:
        return getDetailView((ModuleLevel) args[0]);
      case SESSION_DETAIL_VIEW:
        final SessionFacade facade;
        if (args[0] instanceof SessionFacade) {
          facade = (SessionFacade) args[0];
        } else {
          facade = new SessionFacade((Session) args[0]);
        }
        return getDetailView(facade);
      case UNIT_DETAIL_VIEW:
        return getDetailView((Unit) args[0]);
      default:
        throw new IllegalArgumentException();
    }
  }

  private DetailView getDetailView(final Unit unit) {
    final UnitDetailView unitDetailView = unitDetailViewProvider.get();
    unitDetailView.setUnit(unit);
    return unitDetailView;
  }

  private DetailView getDetailView(final SessionFacade sessionFacade) {
    final SessionDetailView sessionDetailView = sessionDetailViewProvider.get();
    sessionDetailView.setSession(sessionFacade);
    return sessionDetailView;
  }

  private DetailView getDetailView(final ModuleLevel moduleLevel) {
    final CourseDetailView courseDetailView = courseDetailViewProvider.get();
    courseDetailView.setCourse(moduleLevel.getCourse());
    return courseDetailView;
  }

  private DetailView getDetailView(final AbstractUnit abstractUnit) {
    final AbstractUnitDetailView abstractUnitDetailView = abstractUnitDetailViewProvider.get();
    abstractUnitDetailView.setAbstractUnit(abstractUnit);
    return abstractUnitDetailView;
  }

  private DetailView getDetailView(final Module module) {
    final ModuleDetailView moduleDetailView = moduleDetailViewProvider.get();
    moduleDetailView.setModule(module);
    return moduleDetailView;
  }
}
