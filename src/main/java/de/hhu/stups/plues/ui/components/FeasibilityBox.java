package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class FeasibilityBox extends VBox implements Initializable {

  private static final String WORKING_COLOR = "#BDE5F8";

  private String removeString;
  private String unsatCoreString;
  private String cancelString;
  private String impossibleCourseString;
  private String noConflictString;

  private SolverTask<List<Integer>> unsatCoreTask;
  private SolverTask<Boolean> feasibilityTask;
  private ResourceBundle resources;
  private final EnumMap<DayOfWeek, String> dayOfWeekStrings;
  private final Map<Integer, String> timeStrings;
  private final ObjectProperty<Course> majorCourseProperty;
  private final ObjectProperty<Course> minorCourseProperty;
  private final ExecutorService executorService;
  private final Delayed<SolverService> delayedSolverService;
  private final Delayed<Store> delayedStore;
  private final Set<String> impossibleCourses;
  private final VBox parent;
  private final UiDataService uiDataService;

  private final ListProperty<Integer> unsatCoreProperty = new SimpleListProperty<>();

  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneResults;
  @FXML
  @SuppressWarnings("unused")
  private StackPane statePane;
  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private Label lbIcon;
  @FXML
  @SuppressWarnings("unused")
  private Label lbMajor;
  @FXML
  @SuppressWarnings("unused")
  private Label lbMinor;
  @FXML
  @SuppressWarnings("unused")
  private Label lbErrorMsg;
  @FXML
  @SuppressWarnings("unused")
  private ComboBox<String> cbAction;
  @FXML
  @SuppressWarnings("unused")
  private Button btSubmit;

  @FXML
  private Button btHighlightAllConflicts;

  @FXML
  private VBox conflictResults;

  private TreeView<String> conflictTreeView;
  private TreeItem<String> conflictTreeRootItem;

  /**
   * A container to display the feasibility of a combination of courses or a single one. For
   * infeasible courses it is possible to compute the unsat core which is presented in a {@link
   * #conflictTreeView} grouped by the day of week.
   */
  @Inject
  public FeasibilityBox(final Inflater inflater,
                        final Delayed<Store> delayedStore,
                        final Delayed<SolverService> delayedSolverService,
                        final ExecutorService executorService,
                        final UiDataService uiDataService,
                        @Assisted("major") final Course majorCourse,
                        @Nullable @Assisted("minor") final Course minorCourse,
                        @Assisted("impossibleCourses") final Set<String> impossibleCourses,
                        @Assisted("parent") final VBox parent) {
    super();
    this.delayedSolverService = delayedSolverService;
    this.delayedStore = delayedStore;
    this.executorService = executorService;
    this.impossibleCourses = impossibleCourses;
    this.parent = parent;
    this.uiDataService = uiDataService;

    majorCourseProperty = new SimpleObjectProperty<>(majorCourse);
    minorCourseProperty = new SimpleObjectProperty<>(minorCourse);
    dayOfWeekStrings = new EnumMap<>(DayOfWeek.class);
    timeStrings = new HashMap<>();

    inflater.inflate("components/FeasibilityBox", this, this, "feasibilityBox");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    initDayOfWeekString();
    initTimeStrings();

    removeString = resources.getString("remove");
    unsatCoreString = resources.getString("unsatCore");
    cancelString = resources.getString("cancel");
    impossibleCourseString = resources.getString("impossibleCourse");
    noConflictString = resources.getString("noConflict");

    gridPaneResults.setHgap(5.0);

    btHighlightAllConflicts.visibleProperty().bind(unsatCoreProperty.emptyProperty().not());

    conflictTreeView = new TreeView<>();
    conflictTreeRootItem = new TreeItem<>();

    lbMajor.textProperty()
        .bind(Bindings.selectString(majorCourseProperty, "fullName"));
    lbMinor.textProperty()
        .bind(Bindings.selectString(minorCourseProperty, "fullName"));

    conflictTreeView.rootProperty().bind(new ReadOnlyObjectWrapper<>(conflictTreeRootItem));

    conflictTreeRootItem.setValue(resources.getString("conflictPaneTitle"));

    conflictTreeRootItem.expandedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        conflictTreeView.setPrefHeight(26.0);
      } else {
        conflictTreeView.setPrefHeight(175.0);
      }
    });
    conflictTreeRootItem.setExpanded(true);

    btHighlightAllConflicts.visibleProperty().bind(unsatCoreProperty.emptyProperty().not());

    delayedSolverService.whenAvailable(solver -> {
      final Course cMajor = majorCourseProperty.get();
      final Course cMinor = minorCourseProperty.get();

      if (cMinor != null) {
        feasibilityTask = solver.checkFeasibilityTask(cMajor, cMinor);
      } else {
        feasibilityTask = solver.checkFeasibilityTask(cMajor);
      }

      progressIndicator.setStyle("-fx-progress-color: " + WORKING_COLOR);
      progressIndicator.visibleProperty().bind(feasibilityTask.runningProperty());

      executorService.submit(feasibilityTask);
    });

    final String bgColorCommand = "-fx-background-color:";
    feasibilityTask.setOnSucceeded(event -> Platform.runLater(() -> {
      cbAction.setItems(feasibilityTask.getValue()
          ? FXCollections.observableList(Collections.singletonList(removeString))
          : getActionsForInfeasibleCourse());
      cbAction.getSelectionModel().selectFirst();
      lbIcon.setGraphic(FontAwesomeIconFactory.get().createIcon(feasibilityTask.getValue()
          ? FontAwesomeIcon.CHECK : FontAwesomeIcon.REMOVE, "50"));
      lbIcon.setStyle(bgColorCommand + (feasibilityTask.getValue()
          ? PdfRenderingHelper.SUCCESS_COLOR : PdfRenderingHelper.FAILURE_COLOR));
    }));

    feasibilityTask.setOnFailed(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
      cbAction.getSelectionModel().selectFirst();
      lbIcon.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.REMOVE, "50"));
      lbIcon.setStyle(bgColorCommand + PdfRenderingHelper.FAILURE_COLOR);
    });

    feasibilityTask.setOnCancelled(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
      cbAction.getSelectionModel().selectFirst();
      lbIcon.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.QUESTION, "50"));
      lbIcon.setStyle(bgColorCommand + PdfRenderingHelper.WARNING_COLOR);
    });

    progressIndicator.setStyle("-fx-progress-color: " + WORKING_COLOR);
    progressIndicator.visibleProperty().bind(feasibilityTask.runningProperty());

    cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancelString)));
    cbAction.getSelectionModel().selectFirst();
  }

  @FXML
  @SuppressWarnings("unused")
  private void highlightConflicts() {
    uiDataService.setConflictMarkedSessions(unsatCoreProperty.get());
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final String selectedItem = cbAction.getSelectionModel().getSelectedItem();

    if (selectedItem.equals(unsatCoreString)) {
      initUnsatCoreTask();
    }
    if (selectedItem.equals(removeString)) {
      parent.getChildren().remove(this);
    }
    if (selectedItem.equals(cancelString)) {
      if (feasibilityTask.isRunning()) {
        interrupt();
        cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
        cbAction.getSelectionModel().selectFirst();
      } else if (unsatCoreTask.isRunning()) {
        unsatCoreTask.cancel(true);
      }
    }
  }

  /**
   * Initialize and submit the {@link #unsatCoreTask task} to compute the unsat core and set all its
   * necessary listeners.
   */
  private void initUnsatCoreTask() {
    final Course majorCourse = majorCourseProperty.get();
    final Course minorCourse = minorCourseProperty.get();

    if (minorCourse != null) {
      unsatCoreTask = delayedSolverService.get().unsatCore(majorCourse, minorCourse);
    } else {
      unsatCoreTask = delayedSolverService.get().unsatCore(majorCourse);
    }

    unsatCoreTask.setOnSucceeded(unsatCore -> {
      unsatCoreProperty.set(FXCollections.observableList(unsatCoreTask.getValue()));
      showConflictResult(unsatCoreProperty.get());
      cbAction.setItems(FXCollections.singletonObservableList(removeString));
      cbAction.getSelectionModel().selectFirst();
    });

    unsatCoreTask.setOnCancelled(unsatCore -> {
      if (ResourceBundle.getBundle("lang.tasks").getString("timeout")
          .equals(unsatCoreTask.getReason())) {
        lbErrorMsg.setText(noConflictString);
        cbAction.setItems(FXCollections.singletonObservableList(removeString));
        cbAction.getSelectionModel().selectFirst();
      } else {
        cbAction.setItems(FXCollections.observableList(
            Arrays.asList(unsatCoreString, removeString)));
        cbAction.getSelectionModel().selectFirst();
      }
    });

    unsatCoreTask.setOnFailed(unsatCore -> {
      lbErrorMsg.setText(noConflictString);
      cbAction.setItems(FXCollections.singletonObservableList(removeString));
      cbAction.getSelectionModel().selectFirst();
    });

    unsatCoreTask.setOnScheduled(unsatCore -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancelString)));
      cbAction.getSelectionModel().selectFirst();
    });

    executorService.submit(unsatCoreTask);
  }

  /**
   * Display the unsat core in a TreeView grouped by the day of week and the time of the day. The
   * TreeView's root property is bound to {@link #conflictTreeRootItem}.
   *
   * @param unsatCore The list of session Ids that form the unsat core.
   */
  @SuppressWarnings("unused")
  private void showConflictResult(final List<Integer> unsatCore) {
    conflictTreeRootItem.getChildren().clear();

    final List<Session> conflictSessions = delayedStore.get().getSessions()
        .stream().filter(session -> unsatCore.contains(session.getId()))
        .collect(Collectors.toList());
    final EnumMap<DayOfWeek, ArrayList<Session>> sortedSessionsByDay =
        new EnumMap<>(DayOfWeek.class);

    // group conflicting sessions by the day of week
    conflictSessions.forEach(session -> {
      if (session != null) {
        final DayOfWeek dayOfWeek = session.getDayOfWeekMap().get(session.getDay());
        if (!sortedSessionsByDay.containsKey(dayOfWeek)) {
          sortedSessionsByDay.put(dayOfWeek, new ArrayList<>());
        }
        sortedSessionsByDay.get(dayOfWeek).add(session);
      }
    });

    // add all conflicting sessions to the tree view grouped by the day of week and time of the day
    sortedSessionsByDay.entrySet().forEach(dayOfWeekEntry -> {
      final TreeItem<String> dayRootItem = new TreeItem<>(dayOfWeekStrings
          .get(dayOfWeekEntry.getKey()));
      dayRootItem.setExpanded(true);
      groupSessionsByTime(sortedSessionsByDay.get(dayOfWeekEntry.getKey())).entrySet()
          .forEach(timeAtDayEntry -> {
            final TreeItem<String> timeRootItem = new TreeItem<>(timeAtDayEntry.getKey());
            timeRootItem.setExpanded(true);
            timeRootItem.getChildren().addAll(timeAtDayEntry.getValue().stream()
                .map(session -> new TreeItem<>(session.toString()))
                .collect(Collectors.toList()));
            dayRootItem.getChildren().add(timeRootItem);
          });
      conflictTreeRootItem.getChildren().add(dayRootItem);
    });
    conflictResults.getChildren().add(conflictTreeView);
  }

  private Map<String, ArrayList<Session>> groupSessionsByTime(ArrayList<Session> sessions) {
    final Map<String, ArrayList<Session>> sortedSessionsByTime = new HashMap<>(sessions.size());
    sessions.forEach(session -> {
      final String timeString = timeStrings.get(session.getTime());
      if (!sortedSessionsByTime.containsKey(timeString)) {
        sortedSessionsByTime.put(timeString, new ArrayList<>());
      }
      sortedSessionsByTime.get(timeString).add(session);
    });
    return sortedSessionsByTime;
  }

  private void initTimeStrings() {
    timeStrings.put(1, "08:30");
    timeStrings.put(2, "10:30");
    timeStrings.put(3, "12:30");
    timeStrings.put(4, "14:30");
    timeStrings.put(5, "16:30");
    timeStrings.put(6, "18:30");
    timeStrings.put(7, "20:30");
  }

  private void initDayOfWeekString() {
    dayOfWeekStrings.put(DayOfWeek.MONDAY, resources.getString("monday"));
    dayOfWeekStrings.put(DayOfWeek.TUESDAY, resources.getString("tuesday"));
    dayOfWeekStrings.put(DayOfWeek.WEDNESDAY, resources.getString("wednesday"));
    dayOfWeekStrings.put(DayOfWeek.THURSDAY, resources.getString("thursday"));
    dayOfWeekStrings.put(DayOfWeek.FRIDAY, resources.getString("friday"));
  }

  /**
   * Get the strings of the actions in {@link #cbAction} for infeasible courses, i.e. compute the
   * unsat core if the course is not impossible or the combination does not contain an impossible
   * course. Otherwise just offer the possibility to remove the feasibility box.
   */
  private ObservableList<String> getActionsForInfeasibleCourse() {
    if (impossibleCourses.contains(majorCourseProperty.get().getName())
        || (minorCourseProperty.get() != null
        && impossibleCourses.contains(minorCourseProperty.get().getName()))) {
      lbErrorMsg.setText(impossibleCourseString);
      return FXCollections.observableList(Collections.singletonList(removeString));
    } else {
      return FXCollections.observableList(Arrays.asList(unsatCoreString, removeString));
    }
  }

  @FXML
  private void interrupt() {
    feasibilityTask.cancel();
  }
}