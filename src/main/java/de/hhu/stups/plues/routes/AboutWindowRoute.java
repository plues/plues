package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.ui.components.AboutWindow;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import javafx.geometry.Insets;
import javafx.stage.Stage;

public class AboutWindowRoute implements Route {

  private final Provider<AboutWindow> aboutWindowProvider;

  @Inject
  public AboutWindowRoute(final Provider<AboutWindow> aboutWindowProvider) {
    this.aboutWindowProvider = aboutWindowProvider;
  }

  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    final AboutWindow aboutWindow = aboutWindowProvider.get();
    final String title = (String) args[0];

    final Stage aboutStage = new Stage();
    aboutWindow.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
    aboutStage.setTitle(title);
    aboutStage.setScene(SceneFactory.create(aboutWindow));
    aboutStage.setResizable(false);
    aboutStage.show();
  }
}
