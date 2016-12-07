package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.components.detailview.AbstractUnitDetailView;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class AbstractUnitDetailViewRoute implements Route {

  private final Provider<AbstractUnitDetailView> abstractUnitDetailViewProvider;

  @Inject
  public AbstractUnitDetailViewRoute(final Provider<AbstractUnitDetailView>
                                         abstractUnitDetailViewProvider) {
    this.abstractUnitDetailViewProvider = abstractUnitDetailViewProvider;
  }

  @Override
  public void transition(Object... args) {
    final AbstractUnitDetailView moduleDetailView = abstractUnitDetailViewProvider.get();
    moduleDetailView.setAbstractUnit((AbstractUnit) args[0]);

    Scene scene = new Scene(moduleDetailView, 500, 500);

    final Stage stage = new Stage();
    stage.setTitle(moduleDetailView.getTitle());
    stage.show();

    stage.setScene(scene);
  }
}
