package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.ui.components.ChangeLog;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import javafx.stage.Stage;

public class ChangelogRoute implements Route {

  private final Provider<ChangeLog> changeLogProvider;

  @Inject
  public ChangelogRoute(final Provider<ChangeLog> changeLogProvider) {
    this.changeLogProvider = changeLogProvider;
  }

  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    final ChangeLog changeLog = changeLogProvider.get();
    final String title = (String) args[0];

    final Stage logStage = new Stage();
    logStage.setTitle(title);
    logStage.setScene(SceneFactory.create(changeLog));
    logStage.setOnCloseRequest(event -> changeLog.dispose());
    logStage.show();
  }
}
