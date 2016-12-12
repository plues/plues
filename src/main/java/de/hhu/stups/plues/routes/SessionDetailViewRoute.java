package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.ui.components.detailview.SessionDetailView;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class SessionDetailViewRoute implements Route {

  private final Provider<SessionDetailView> sessionDetailViewProvider;

  @Inject
  public SessionDetailViewRoute(final Provider<SessionDetailView> sessionDetailViewProvider) {
    this.sessionDetailViewProvider = sessionDetailViewProvider;
  }

  @Override
  public void transition(Object... args) {
    final SessionDetailView sessionDetailView = sessionDetailViewProvider.get();
    sessionDetailView.setSession(new SessionFacade((Session) args[0]));

    Scene scene = new Scene(sessionDetailView, 500, 500);

    final Stage stage = new Stage();
    scene.getStylesheets().add("/styles/index.css");
    stage.setTitle(sessionDetailView.getTitle());
    stage.show();

    stage.setScene(scene);
  }
}
