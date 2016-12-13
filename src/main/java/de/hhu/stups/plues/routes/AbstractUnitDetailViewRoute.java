package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.components.detailview.AbstractUnitDetailView;
import de.hhu.stups.plues.ui.layout.SceneFactory;

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
    final AbstractUnitDetailView abstractUnitDetailView = abstractUnitDetailViewProvider.get();
    abstractUnitDetailView.setAbstractUnit((AbstractUnit) args[0]);

    Scene scene = SceneFactory.create(abstractUnitDetailView);

    final Stage stage = new Stage();
    stage.setTitle(abstractUnitDetailView.getTitle());
    stage.show();

    stage.setScene(scene);
  }
}
