package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.controller.Timetable;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Display the unsat core in a {@link #conflictTreeTableView TreeView} grouped by the day of week
 * and the time of the day. Furthermore provide a button to highlight all conflicting session in the
 * {@link Timetable}. This component is dynamically added to a {@link FeasibilityBox} in case the
 * specific combination of courses is infeasible and we have found an unsat core.
 */
public class ConflictTree extends VBox implements Initializable {

  private final UiDataService uiDataService;
  private final Router router;
  private final Delayed<Store> delayedStore;
  private ResourceBundle resources;

  private ListProperty<Integer> unsatCoreProperty;
  private List<Session> conflictSessions;

  @FXML
  @SuppressWarnings("unused")
  private TreeTableView<Object> conflictTreeTableView;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<Object> conflictTreeRootItem;
  @FXML
  @SuppressWarnings("unused")
  private TreeTableColumn<Object, String> treeColumnTitle;
  @FXML
  @SuppressWarnings("unused")
  private TreeTableColumn<Object, String> treeColumnKey;
  @FXML
  @SuppressWarnings("unused")
  private TreeTableColumn<Object, String> treeColumnSemesters;

  /**
   * Initialize the conflict tree.
   */
  @Inject
  public ConflictTree(final Inflater inflater, final UiDataService uiDataService,
                      final Delayed<Store> delayedStore,
                      final Router router) {
    this.uiDataService = uiDataService;
    this.router = router;
    this.delayedStore = delayedStore;
    inflater.inflate("components/ConflictTree", this, this, "conflictTree", "Column", "Days");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    conflictTreeTableView.setShowRoot(false);
    conflictTreeTableView.setPrefHeight(175.0);

    conflictTreeTableView.rootProperty().bind(new ReadOnlyObjectWrapper<>(conflictTreeRootItem));

    conflictTreeTableView.setOnMouseClicked(event -> {
      final TreeItem<Object> item = conflictTreeTableView.getSelectionModel().getSelectedItem();

      if (event.getClickCount() == 2 && item != null && item.getValue() instanceof Session) {
        router.transitionTo(RouteNames.SESSION_IN_TIMETABLE, item.getValue());

      }
    });
    initTreeTableViewValueFactories();

    bindTableColumnsWidth();
  }

  private void bindTableColumnsWidth() {
    treeColumnTitle.prefWidthProperty().bind(
        conflictTreeTableView.widthProperty().multiply(0.66));
    treeColumnKey.prefWidthProperty().bind(
        conflictTreeTableView.widthProperty().multiply(0.15));
    treeColumnSemesters.prefWidthProperty().bind(
        conflictTreeTableView.widthProperty().multiply(0.15));
  }

  @FXML
  @SuppressWarnings("unused")
  private void highlightConflicts() {
    uiDataService.setConflictMarkedSessions(unsatCoreProperty.get());
  }

  /**
   * Add the sessions from the unsat core to the {@link #conflictTreeRootItem TreeItem} that is
   * bound to the {@link #conflictTreeTableView TreeView's} root property.
   *
   * @param unsatCore The list of sessions that form the unsat core.
   */
  @SuppressWarnings("unused")
  private void showConflictResult(final List<Session> unsatCore) {
    conflictTreeRootItem.getChildren().clear();

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

    // add all conflicting sessions to the TreeView's root grouped by the day of the week and the
    // time of the day
    sortedSessionsByDay.entrySet().forEach(dayOfWeekEntry -> {
      final TreeItem<Object> dayRootItem = new TreeItem<>(resources.getString(
          Helpers.shortDayOfWeekMap.get(dayOfWeekEntry.getKey())));
      dayRootItem.setExpanded(true);
      groupSessionsByTime(sortedSessionsByDay.get(dayOfWeekEntry.getKey())).entrySet()
          .forEach(timeAtDayEntry -> {
            final TreeItem<Object> timeRootItem = new TreeItem<>(timeAtDayEntry.getKey());
            timeRootItem.setExpanded(true);
            timeRootItem.getChildren().addAll(timeAtDayEntry.getValue().stream()
                .map(session -> new TreeItem<>((Object) session))
                .collect(Collectors.toList()));
            dayRootItem.getChildren().add(timeRootItem);
          });
      conflictTreeRootItem.getChildren().add(dayRootItem);
    });
  }

  private Map<String, ArrayList<Session>> groupSessionsByTime(final ArrayList<Session> sessions) {
    final Map<String, ArrayList<Session>> sortedSessionsByTime = new HashMap<>(sessions.size());
    sessions.forEach(session -> {
      final String timeString = Helpers.timeMap.get(session.getTime());
      if (!sortedSessionsByTime.containsKey(timeString)) {
        sortedSessionsByTime.put(timeString, new ArrayList<>());
      }
      sortedSessionsByTime.get(timeString).add(session);
    });
    return sortedSessionsByTime;
  }

  @SuppressWarnings("unused")
  private void setConflictSessions(final List<Session> conflictSessions) {
    this.conflictSessions = conflictSessions;
    showConflictResult(conflictSessions);
  }

  void setUnsatCoreProperty(final ListProperty<Integer> unsatCoreProperty) {
    this.unsatCoreProperty = unsatCoreProperty;
    this.delayedStore.whenAvailable(store ->
        setConflictSessions(unsatCoreProperty.get()
            .stream().map(store::getSessionById)
            .collect(Collectors.toList())));
  }

  private void initTreeTableViewValueFactories() {
    treeColumnTitle.setCellValueFactory(param -> {
      if (param.getValue().getValue() instanceof String) {
        return new ReadOnlyStringWrapper((String) param.getValue().getValue());
      } else if (param.getValue().getValue() instanceof Session) {
        return new ReadOnlyStringWrapper(
            ((Session) param.getValue().getValue()).getGroup().getUnit().getTitle());
      } else {
        return new ReadOnlyStringWrapper("");
      }
    });
    treeColumnKey.setCellValueFactory(param ->
        new ReadOnlyStringWrapper(
            (param.getValue().getValue() instanceof Session)
                ?
                ((Session) param.getValue().getValue()).getGroup().getUnit().getKey() : ""));

    treeColumnSemesters.setCellValueFactory(param -> {
      final String semesters;
      if (param.getValue().getValue() instanceof Session) {
        semesters = ((Session) param.getValue().getValue()).getGroup().getUnit().getSemesters()
          .stream()
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
      } else {
        semesters = "";
      }
      return new ReadOnlyStringWrapper(semesters);
    });
  }
}
