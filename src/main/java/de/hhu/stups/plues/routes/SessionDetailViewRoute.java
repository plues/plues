package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.ui.components.detailview.SessionDetailView;
import de.hhu.stups.plues.ui.components.timetable.SessionFacade;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import javafx.stage.Stage;

public class SessionDetailViewRoute implements Route {

  private final Provider<SessionDetailView> sessionDetailViewProvider;

  @Inject
  public SessionDetailViewRoute(final Provider<SessionDetailView> sessionDetailViewProvider) {
    this.sessionDetailViewProvider = sessionDetailViewProvider;
  }

  @Override
  public void transition(final RouteNames routeName, Object... args) {
    final SessionDetailView sessionDetailView = sessionDetailViewProvider.get();
    sessionDetailView.setSession(new SessionFacade((Session) args[0]));

    final Stage stage = new Stage();
    stage.setTitle(sessionDetailView.getTitle());
    stage.setScene(SceneFactory.create(sessionDetailView));
    stage.show();

  }
}
