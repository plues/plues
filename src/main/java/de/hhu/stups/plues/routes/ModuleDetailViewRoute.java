package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.components.detailview.ModuleDetailView;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import javafx.stage.Stage;

public class ModuleDetailViewRoute implements Route {

  private final Provider<ModuleDetailView> moduleDetailViewProvider;

  @Inject
  public ModuleDetailViewRoute(final Provider<ModuleDetailView> moduleDetailViewProvider) {
    this.moduleDetailViewProvider = moduleDetailViewProvider;
  }

  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    final ModuleDetailView moduleDetailView = moduleDetailViewProvider.get();
    moduleDetailView.setModule((Module) args[0]);

    final Stage stage = new Stage();
    stage.setTitle(moduleDetailView.getTitle());
    stage.setScene(SceneFactory.create(moduleDetailView));
    stage.show();

  }
}
