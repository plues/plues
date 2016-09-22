package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class ConflictMatrix extends VBox implements Initializable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private List<Course> majorCourses;
  private List<Course> minorCourses;
  private List<Course> standaloneCourses;

  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneCombinable;

  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneStandalone;

  /**
   * This view presents a matrix of all possible combinations and if known their feasibility.
   *
   * @param loader               TaskLoader to load fxml file and to set controller
   * @param delayedStore         Store containing relevant data
   * @param delayedSolverService SolverService for usage of ProB solver
   */
  @Inject
  public ConflictMatrix(final FXMLLoader loader, final Delayed<Store> delayedStore,
                        final Delayed<SolverService> delayedSolverService) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;

    this.delayedStore.whenAvailable(store -> {
      final List<Course> courses = store.getCourses();
      standaloneCourses = courses.stream()
          .filter(course -> !course.isCombinable()).collect(Collectors.toList());
      majorCourses = courses.stream()
          .filter(course ->
            course.isMajor() && course.isCombinable()).collect(Collectors.toList());
      minorCourses = courses.stream()
          .filter(course ->
              course.isMinor() && course.isCombinable()).collect(Collectors.toList());
    });

    loader.setLocation(getClass().getResource("/fxml/ConflictMatrix.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {

  }
}
