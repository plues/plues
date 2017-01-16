package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.components.detailview.AbstractUnitDetailView;
import de.hhu.stups.plues.ui.components.detailview.CourseDetailView;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class CourseDetailViewRoute implements Route {

  private final Provider<CourseDetailView> courseDetailViewProvider;

  @Inject
  public CourseDetailViewRoute(final Provider<CourseDetailView>
                                         courseDetailViewProvider) {
    this.courseDetailViewProvider = courseDetailViewProvider;
  }

  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    final CourseDetailView courseDetailView = courseDetailViewProvider.get();
    courseDetailView.setCourse((Course) args[0]);

    final Stage stage = new Stage();
    stage.setTitle(courseDetailView.getTitle());
    stage.setScene(SceneFactory.create(courseDetailView));
    stage.show();

  }
}
