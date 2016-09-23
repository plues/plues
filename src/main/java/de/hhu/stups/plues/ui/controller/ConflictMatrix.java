package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConflictMatrix extends GridPane implements Initializable {

  private ObservableMap<String, Boolean> courseCombinationResults;

  private final BooleanProperty solverProperty;
  private List<Course> courses;
  private List<Course> majorCourses;
  private List<Course> minorCourses;
  private List<Course> standaloneCourses;

  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneCombinable;
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneStandalone;
  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPaneCombinable;
  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPaneStandalone;
  @FXML
  @SuppressWarnings("unused")
  private Label lbCombinableCourses;
  @FXML
  @SuppressWarnings("unused")
  private Label lbStandaloneCourses;

  /**
   * This view presents a matrix of all possible combinations of combinable major and minor courses
   * and if known their feasibility. Furthermore a list of all standalone courses and if known their
   * feasibility is displayed.
   *
   * @param loader               TaskLoader to load fxml file and to set controller
   * @param delayedSolverService SolverService for usage of ProB solver
   */
  @Inject
  public ConflictMatrix(final FXMLLoader loader, final Delayed<Store> delayedStore,
                        final Delayed<SolverService> delayedSolverService) {
    solverProperty = new SimpleBooleanProperty(false);

    delayedStore.whenAvailable(store -> {
      courses = store.getCourses();
      majorCourses = courses.stream()
          .filter(c -> c.isMajor() && c.isCombinable()).collect(Collectors.toList());
      minorCourses = courses.stream()
          .filter(c -> c.isMinor() && c.isCombinable()).collect(Collectors.toList());
      standaloneCourses = courses.stream()
          .filter(c -> !c.isCombinable()).collect(Collectors.toList());
      initializeGridPaneCombinable();
      initializeGridPaneStandalone();
    });

    delayedSolverService.whenAvailable(solverService -> {
      courseCombinationResults = delayedSolverService.get().getCourseCombinationResults();
      courseCombinationResults.addListener(new MapChangeListener<String, Boolean>() {
        @Override
        public void onChanged(Change<? extends String, ? extends Boolean> change) {
          if (change.wasAdded()) {
            if (change.getKey().split(";").length == 2) {
              gridPaneCombinableAddElm(change.getKey(), change.getValueAdded());
            } else {
              gridPaneStandaloneAddElm(change.getKey(), change.getValueAdded());
            }
          } else {
            // discard all if a session has been moved
            // Todo: only discard the specific session's results ?
            gridPaneCombinable.getChildren().clear();
          }
        }
      });
      solverProperty.set(true);
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

  private void initializeGridPaneStandalone() {
    IntStream.range(0, standaloneCourses.size())
        .forEach(index -> gridPaneStandalone.add(
            getDefaultGridCellPane(standaloneCourses.get(index).getName()), index, 0));
    IntStream.range(0, standaloneCourses.size())
        .forEach(index -> gridPaneStandalone.add(
            getDefaultGridCellPane(""), index, 1));
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    gridPaneCombinable.visibleProperty().bind(solverProperty);
    scrollPaneCombinable.visibleProperty().bind(solverProperty);
    scrollPaneStandalone.visibleProperty().bind(solverProperty);
    gridPaneStandalone.visibleProperty().bind(solverProperty);
    lbCombinableCourses.visibleProperty().bind(solverProperty);
    lbStandaloneCourses.visibleProperty().bind(solverProperty);
  }

  /**
   * Initialize the grid pane for the combinable courses, i.e. fill the first column/row with the
   * major/minor course names and set the default cells.
   */
  private void initializeGridPaneCombinable() {
    IntStream.range(0, majorCourses.size())
        .forEach(index -> gridPaneCombinable.add(
            getDefaultGridCellPane(majorCourses.get(index).getName()), index + 1, 0));
    IntStream.range(0, minorCourses.size())
        .forEach(index -> gridPaneCombinable.add(
            getDefaultGridCellPane(minorCourses.get(index).getName()), 0, index + 1));
    IntStream.range(0, minorCourses.size())
        .forEach(row -> IntStream.range(0, majorCourses.size())
            .forEach(col -> {
              if (majorCourses.get(col).getShortName()
                  .equals(minorCourses.get(row).getShortName())) {
                gridPaneCombinable.add(getImpossibleGridCellPane(), col + 1, row + 1);
              } else {
                gridPaneCombinable.add(getDefaultGridCellPane(""), col + 1, row + 1);
              }
            }));
    gridPaneCombinable.add(getDefaultGridCellPane(""), 0, 0);
  }

  /**
   * Create a default grid pane cell.
   *
   * @param courseName The course's name to display or empty for default empty cells.
   * @return Return a pane.
   */
  private Pane getDefaultGridCellPane(String courseName) {
    Pane pane = new Pane();
    pane.setId("conflictMatrixCellDefault");
    pane.setPrefHeight(25.0);

    Label label;
    if (!courseName.isEmpty()) {
      label = new Label("  " + courseName + "  ");
      String fullName = courses.stream()
          .filter(c -> c.getName().equals(courseName))
          .collect(Collectors.toList()).get(0).getFullName();
      Tooltip tooltip = new Tooltip(fullName);
      label.setTooltip(tooltip);
    } else {
      label = new Label();
    }
    pane.getChildren().add(label);

    return pane;
  }

  /**
   * Create a grid pane cell for impossible combinations where major and minor courses are "equal".
   *
   * @return Return a pane.
   */
  private Pane getImpossibleGridCellPane() {
    Pane pane = new Pane();
    pane.setId("conflictMatrixCellImpossible");
    pane.setPrefHeight(25.0);

    Label label = new Label();
    label.prefWidthProperty().bind(pane.widthProperty());
    label.prefHeightProperty().bind(pane.heightProperty());
    Tooltip tooltip = new Tooltip("Impossible combination: Same major and minor course.");
    label.setTooltip(tooltip);
    pane.getChildren().add(label);

    return pane;
  }

  /**
   * Create an active grid pane cell, i.e. the feasibility is known and the pane's background color
   * is set according to the result. Furthermore a tooltip with major and minor course name is
   * added.
   *
   * @param result      True if the combination of major and minor course is feasible.
   * @param courseNames An optional array of the major and minor course names.
   */
  private Pane getActiveGridCellPane(Boolean result, String... courseNames) {
    Pane pane = new Pane();
    final String paneId = result ? "conflictMatrixCellSuccess" : "conflictMatrixCellFailed";
    pane.setId(paneId);
    if (courseNames.length != 0) {
      Label label = new Label();
      label.prefWidthProperty().bind(pane.widthProperty());
      label.prefHeightProperty().bind(pane.heightProperty());
      Tooltip tooltip = new Tooltip("Major: " + courseNames[0] + "\nMinor: " + courseNames[1]);
      label.setTooltip(tooltip);
      pane.getChildren().add(label);
    }
    pane.setPrefHeight(25.0);

    return pane;
  }

  /**
   * Add a result to the conflict matrix of combinable courses for given key and result. Define the
   * column and row according to the major and minor name by searching their position in the
   * specific list of courses.
   *
   * @param key    The string of major and minor course name split by a semicolon.
   * @param result True if the combination is feasible otherwise false.
   */
  private void gridPaneCombinableAddElm(String key, Boolean result) {
    final String majorName = key.split(";")[0];
    final String minorName = key.split(";")[1];

    final int row = minorCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(minorName) + 1;
    final int col = majorCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(majorName) + 1;

    Platform.runLater(() -> gridPaneCombinable.add(
        getActiveGridCellPane(result, majorName, minorName), col, row));
  }

  /**
   * Add a result to the list of standalone courses for given key and result.
   *
   * @param key    The major course's name.
   * @param result True if the course is feasible otherwise false.
   */
  private void gridPaneStandaloneAddElm(String key, Boolean result) {
    final String majorName = key.split(";")[0];
    final int col = standaloneCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(majorName);

    Platform.runLater(() -> gridPaneStandalone.add(getActiveGridCellPane(result), col, 1));
  }
}
