package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.ConflictMatrixService;
import de.hhu.stups.plues.ui.components.ControllerHeader;
import de.hhu.stups.plues.ui.components.conflictmatrix.CourseGridCell;
import de.hhu.stups.plues.ui.components.conflictmatrix.ResultGridCell;
import de.hhu.stups.plues.ui.components.conflictmatrix.ResultGridCellFactory;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import org.fxmisc.easybind.EasyBind;
import org.reactfx.EventSource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConflictMatrix extends GridPane implements Initializable {

  private static final String VERTICAL = "vertical";
  private static final String HORIZONTAL = "";

  private final ResultGridCellFactory resultGridCellFactory;
  private final ConflictMatrixService conflictMatrixService;
  private final LongProperty impossibleCoursesAmount = new SimpleLongProperty(0L);
  private final MapProperty<CourseSelection, ResultState> results
      = new SimpleMapProperty<>(FXCollections.emptyObservableMap());
  private final Map<CourseSelection, ResultGridCell> cellMap = new HashMap<>();
  private final EventSource<Course> checkCourseCombinationsEventSource = new EventSource<>();

  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private ControllerHeader controllerHeader;
  @FXML
  @SuppressWarnings("unused")
  private Accordion accordionConflictMatrices;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneCombinableCourses;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneStandaloneCourses;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneSingleCourses;
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneCombinable;
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneStandalone;
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneSingleCourses;
  @FXML
  @SuppressWarnings("unused")
  private Label lbFeasibleCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbInfeasibleCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lblImpossibleCoursesAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbTimeoutCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Button btCheckAll;
  @FXML
  @SuppressWarnings("unused")
  private Button btCancelCheckAll;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendSuccess;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendFailure;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendTimeout;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendImpossible;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendInfeasible;

  /**
   * This view presents a matrix of all possible combinations of combinable major and minor courses
   * and if known their feasibility. Furthermore a list of all standalone courses as well as a list
   * of all single courses and if known their feasibility is displayed. Each cell is represented by
   * a {@link ResultGridCell}.
   */
  @Inject
  public ConflictMatrix(final Inflater inflater,
                        final Delayed<Store> delayedStore,
                        final ResultGridCellFactory resultGridCellFactory,
                        final ConflictMatrixService conflictMatrixService) {
    this.resultGridCellFactory = resultGridCellFactory;
    this.conflictMatrixService = conflictMatrixService;

    /*
      Check all combinations with a course. Use {@link Course#getMinorCourses()} to create
      combinations for major courses. Filter all courses to get the combinations with
      a minor course.
     */
    this.checkCourseCombinationsEventSource
      .filter(Objects::nonNull)
      .subscribe(this.conflictMatrixService::checkAllCombinations);

    delayedStore.whenAvailable(this::setInitialGridPaneVisibility);

    EasyBind.subscribe(conflictMatrixService.impossibleCoursesProperty(),
        this::highlightImpossibleCourses);

    conflictMatrixService.resultsProperty().addListener(getCourseResultChangeListener());

    inflater.inflate("ConflictMatrix", this, this, "conflictMatrix");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    initializeStats();

    controllerHeader.setInfoText(resources.getString("explanationStats"));
    controllerHeader.setTitle(resources.getString("header"));

    btCheckAll.disableProperty().bind(conflictMatrixService.availableProperty().not()
        .or(conflictMatrixService.isCheckRunningProperty()));
    btCancelCheckAll.disableProperty().bind(conflictMatrixService.isCheckRunningProperty().not());

    // draw small circles to distinguish between each cell independent from its color
    paneLegendSuccess.getChildren().add(new Circle(5, 5, 2));
    paneLegendFailure.getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2));
    paneLegendTimeout.getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2),
        new Circle(5, 10, 2));
    paneLegendInfeasible.getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2),
        new Circle(5, 10, 2), new Circle(10, 10, 2));
    paneLegendImpossible.getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2),
        new Circle(5, 10, 2), new Circle(10, 10, 2), new Circle(5, 15, 2));

  }

  private void initializeStats() {
    // for counting we only consider results for course combinations and standalone courses
    // single courses are ignored.
    lbTimeoutCourseAmount.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf(
        results.entrySet().parallelStream()
          .filter(entry -> entry.getKey().isCurriculum()
            && entry.getValue().timedOut()).count()), results));

    lbFeasibleCourseAmount.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf(
        results.entrySet().parallelStream()
          .filter(entry -> entry.getKey().isCurriculum()
            && entry.getValue().succeeded()).count()), results));

    lbInfeasibleCourseAmount.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf(
        results.entrySet().parallelStream()
          .filter(entry -> {
            CourseSelection cs = entry.getKey();
            ResultState result = entry.getValue();

            return cs.isCurriculum()
              && cs.getCourses().stream().noneMatch(course
                  -> conflictMatrixService.impossibleCoursesProperty().contains(course))
              && result.failed();
          }).count()), results));

    lblImpossibleCoursesAmount.textProperty().bind(Bindings.convert(impossibleCoursesAmount));
  }

  /**
   * Initialize and set the visibility of the grid panes according to the current data.
   *
   * @param store Store
   */
  private void setInitialGridPaneVisibility(final Store store) {
    final List<Course> courses = store.getCourses().stream()
        .sorted(Comparator.comparing(Course::getPo).thenComparing(Course::getShortName))
        .collect(Collectors.toList());
    final List<Course> standaloneCourses
        = courses.stream().filter(c -> !c.isCombinable()).collect(Collectors.toList());
    final List<Course> combinableCourses
        = courses.stream().filter(Course::isCombinable).collect(Collectors.toList());

    final List<Course> combinableMajorCourses
        = combinableCourses.stream().filter(Course::isMajor).collect(Collectors.toList());
    final List<Course> combinableMinorCourses
        = combinableCourses.stream().filter(Course::isMinor).collect(Collectors.toList());

    if (!standaloneCourses.isEmpty()) {
      initializeGridPaneStandalone(standaloneCourses);
      accordionConflictMatrices.setExpandedPane(titledPaneStandaloneCourses);
    } else {
      accordionConflictMatrices.getPanes().remove(titledPaneStandaloneCourses);
    }

    if (!combinableMajorCourses.isEmpty() && !combinableMinorCourses.isEmpty()) {
      initializeGridPaneCombinable(combinableMajorCourses, combinableMinorCourses);
      accordionConflictMatrices.setExpandedPane(titledPaneCombinableCourses);
    } else {
      accordionConflictMatrices.getPanes().remove(titledPaneCombinableCourses);
      accordionConflictMatrices.getPanes().remove(titledPaneSingleCourses);
    }

    initializeGridPaneSingleCourse(combinableMajorCourses, combinableMinorCourses);
    highlightImpossibleCombinations(combinableMajorCourses, combinableMinorCourses);
  }

  /**
   * Highlight the impossible courses, i.e. courses that are statically known to be infeasible.
   *
   * @param courses Set of Course objects
   */
  @SuppressWarnings("unused")
  private void highlightImpossibleCourses(final ObservableSet<Course> courses) {
    final List<CourseSelection> courseSelections
        = courses.stream().map(CourseSelection::new).collect(Collectors.toList());

    courses.stream().filter(Course::isCombinable).flatMap(course -> {
      final Set<Course> courseSet;
      if (course.isMajor()) {
        courseSet = course.getMinorCourses();
      } else {
        courseSet = course.getMajorCourses();
      }
      return courseSet.stream().map(other -> new CourseSelection(course, other));
    }).collect(Collectors.toCollection(() -> courseSelections))
        .forEach(courseSelection
            -> cellMap.get(courseSelection).setResultState(ResultState.IMPOSSIBLE));

    impossibleCoursesAmount.set(cellMap.entrySet().stream().filter(entry ->
        entry.getKey().isCurriculum()
        && entry.getValue().getResultState() != null
        && entry.getValue().getResultState().isImpossible()).count());
  }

  /**
   * Highlight impossible combinations using each major courses' given list of minor courses.
   */
  private void highlightImpossibleCombinations(final List<Course> combinableMajorCourses,
                                               final List<Course> combinableMinorCourses) {

    IntStream.range(0, combinableMinorCourses.size()).forEach(row -> {
      final Course minorCourse = combinableMinorCourses.get(row);
      highlightImpossibleCombinationsForGivenMinor(minorCourse, combinableMajorCourses);
    });
  }

  private void highlightImpossibleCombinationsForGivenMinor(
      final Course minorCourse,
      final List<Course> combinableMajorCourses) {

    IntStream.range(0, combinableMajorCourses.size()).forEach(col -> {
      final Course majorCourse = combinableMajorCourses.get(col);
      if (!majorCourse.getMinorCourses().contains(minorCourse)) {
        cellMap.get(new CourseSelection(majorCourse, minorCourse))
          .setResultState(ResultState.IMPOSSIBLE_COMBINATION);
      }
    });
  }

  private void initializeGridPaneCombinable(final List<Course> combinableMajorCourses,
                                            final List<Course> combinableMinorCourses) {

    final DoubleProperty heightProperty = new SimpleDoubleProperty();
    final DoubleProperty widthProperty = new SimpleDoubleProperty();
    initializeMinorCourseNames(heightProperty, combinableMinorCourses);
    initializeMajorCourseNames(widthProperty, combinableMajorCourses);
    // add legend like cell at position (0,0)
    gridPaneCombinable.add(getLegendGridCell(heightProperty, widthProperty), 0, 0);
    initializeResultGridCells(combinableMajorCourses, combinableMinorCourses);
  }

  private void initializeMinorCourseNames(final DoubleProperty heightProperty,
                                          final List<Course> combinableMinorCourses) {

    final BooleanBinding binding = getContextMenuBooleanBinding();

    IntStream.range(0, combinableMinorCourses.size()).forEach(index -> {
      final Course course = combinableMinorCourses.get(index);
      final CourseGridCell courseGridCell =
            new CourseGridCell(course, VERTICAL, checkCourseCombinationsEventSource);
      courseGridCell.enabledProperty().bind(binding);
      gridPaneCombinable.add(courseGridCell, index + 1, 0);
      // get the height property of a minor names row..
      if (index == combinableMinorCourses.size() - 1) {
        heightProperty.bind(courseGridCell.heightProperty());
      }
    });
  }

  private void initializeMajorCourseNames(final DoubleProperty widthProperty,
                                          final List<Course> combinableMajorCourses) {

    final BooleanBinding binding = getContextMenuBooleanBinding();
    IntStream.range(0, combinableMajorCourses.size()).forEach(index -> {
      final Course course = combinableMajorCourses.get(index);
      final CourseGridCell courseGridCell =
          new CourseGridCell(course, HORIZONTAL, checkCourseCombinationsEventSource);
      courseGridCell.enabledProperty().bind(binding);
      gridPaneCombinable.add(courseGridCell, 0, index + 1);
      // ..and the width property of a major names column
      if (index == combinableMajorCourses.size() - 1) {
        widthProperty.bind(courseGridCell.widthProperty());
      }
    });
  }

  private void initializeResultGridCells(final List<Course> combinableMajorCourses,
                                         final List<Course> combinableMinorCourses) {

    IntStream.range(0, combinableMinorCourses.size()).forEach(col ->
        IntStream.range(0, combinableMajorCourses.size()).forEach(row -> {
          final Course majorCourse = combinableMajorCourses.get(row);
          final Course minorCourse = combinableMinorCourses.get(col);
          final ResultGridCell gridCell
              = resultGridCellFactory.create(majorCourse, minorCourse);

          gridCell.enabledProperty().bind(conflictMatrixService.availableProperty());

          cellMap.put(new CourseSelection(majorCourse, minorCourse), gridCell);
          gridPaneCombinable.add(gridCell, col + 1, row + 1);
        }));
  }

  /**
   * Create a grid cell for position (0,0) in the conflict matrix yielding a short description on
   * the column and row content like major/minor.
   */
  private Pane getLegendGridCell(final ReadOnlyDoubleProperty heightProperty,
                                 final ReadOnlyDoubleProperty widthProperty) {
    final HBox hBox = new HBox();
    hBox.prefHeightProperty().bind(heightProperty);
    hBox.prefWidthProperty().bind(widthProperty);
    hBox.getStyleClass().addAll("matrix-cell", "windowPaddingTiny");
    final Label minorLabel = new Label(resources.getString("minor"));
    minorLabel.setRotate(270.0);
    minorLabel.prefHeightProperty().bind(widthProperty.multiply(0.15));
    minorLabel.prefWidthProperty().bind(heightProperty.subtract(25.0));
    final Group group = new Group(minorLabel);
    final Label majorLabel = new Label(resources.getString("major"));
    majorLabel.setAlignment(Pos.BOTTOM_CENTER);
    majorLabel.prefHeightProperty().bind(heightProperty);
    majorLabel.prefWidthProperty().bind(widthProperty.multiply(0.85));
    hBox.getChildren().addAll(majorLabel, group);
    return hBox;
  }

  private void initializeGridPaneStandalone(final List<Course> standaloneCourses) {
    initGridPane(standaloneCourses, gridPaneStandalone, cellMap);
  }

  private void initGridPane(final List<Course> courses,
                            final GridPane gridPane,
                            final Map<CourseSelection, ResultGridCell> cellMap) {
    final BooleanBinding binding = getContextMenuBooleanBinding();

    gridPane.addColumn(0, courses.stream()
        .map(course -> {
          final CourseGridCell cell
              = new CourseGridCell(course, HORIZONTAL, checkCourseCombinationsEventSource);
          cell.enabledProperty().bind(binding);
          return cell;
        }).collect(Collectors.toList()).toArray(new Node[] {}));

    IntStream.range(0, courses.size()).forEach(index -> {
      final Course course = courses.get(index);
      final ResultGridCell gridCell = resultGridCellFactory.create(course);
      gridCell.enabledProperty().bind(conflictMatrixService.availableProperty());
      cellMap.put(new CourseSelection(course), gridCell);
      gridPane.add(gridCell, 1, index);
    });
  }

  private BooleanBinding getContextMenuBooleanBinding() {
    return conflictMatrixService.availableProperty()
      .and(conflictMatrixService.isCheckRunningProperty().not());
  }

  private void initializeGridPaneSingleCourse(final List<Course> combinableMajorCourses,
                                              final List<Course> combinableMinorCourses) {

    final ArrayList<Course> singleCourses = new ArrayList<>();

    singleCourses.addAll(combinableMajorCourses);
    singleCourses.addAll(combinableMinorCourses);

    initGridPane(singleCourses, gridPaneSingleCourses, cellMap);
  }

  /**
   * Action of button {@link ConflictMatrix#btCheckAll} to check the feasibility of all
   * combinations.
   */
  @FXML
  @SuppressWarnings("unused")
  public void checkAll() {
    conflictMatrixService.checkAll();
  }

  /**
   * Action of button {@link ConflictMatrix#btCancelCheckAll} to cancel the batch feasibility
   * check.
   */
  @FXML
  @SuppressWarnings("unused")
  public void cancelCheckAll() {
    conflictMatrixService.cancelRunningTasks();
  }

  private MapChangeListener<CourseSelection, ResultState> getCourseResultChangeListener() {
    return change -> {
      final ResultGridCell cell = cellMap.get(change.getKey());

      if (cell == null || cell.getResultState() == ResultState.IMPOSSIBLE) {
        return;
      }

      if (change.wasAdded()) {
        cell.setResultState(change.getValueAdded());
      } else {
        cell.setResultState(ResultState.UNKNOWN);
      }
    };
  }
}
