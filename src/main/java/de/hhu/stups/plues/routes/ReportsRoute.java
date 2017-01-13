package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.ui.controller.Reports;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import javafx.stage.Stage;

public class ReportsRoute implements Route {

  private final Provider<Reports> reportsProvider;

  @Inject
  public ReportsRoute(final Provider<Reports> reportsProvider) {
    this.reportsProvider = reportsProvider;
  }

  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    final Reports reports = reportsProvider.get();
    final String title = (String) args[0];

    final Stage reportStage = new Stage();
    reportStage.setTitle(title);
    reportStage.setScene(SceneFactory.create(reports));
    reportStage.show();
  }
}
