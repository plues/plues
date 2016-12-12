package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.components.detailview.UnitDetailView;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class UnitDetailViewRoute implements Route {

  private final Provider<UnitDetailView> unitDetailViewProvider;

  @Inject
  public UnitDetailViewRoute(final Provider<UnitDetailView> unitDetailViewProvider) {
    this.unitDetailViewProvider = unitDetailViewProvider;
  }

  @Override
  public void transition(Object... args) {
    final UnitDetailView unitDetailView = unitDetailViewProvider.get();
    unitDetailView.setUnit((Unit) args[0]);

    Scene scene = new Scene(unitDetailView, 500, 500);

    final Stage stage = new Stage();
    scene.getStylesheets().add("/styles/index.css");
    stage.setTitle(unitDetailView.getTitle());
    stage.show();

    stage.setScene(scene);
  }
}
