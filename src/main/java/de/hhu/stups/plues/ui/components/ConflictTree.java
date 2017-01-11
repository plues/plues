package de.hhu.stups.plues.ui.components;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.services.UiDataService;
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

  private final EnumMap<DayOfWeek, String> dayOfWeekStrings;
  private final Map<Integer, String> timeStrings;
  private final UiDataService uiDataService;

  private ListProperty<Integer> unsatCoreProperty;
  private ResourceBundle resources;
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
  public ConflictTree(final Inflater inflater, final UiDataService uiDataService) {
    this.uiDataService = uiDataService;
    dayOfWeekStrings = new EnumMap<>(DayOfWeek.class);
    timeStrings = new HashMap<>();

    inflater.inflate("components/ConflictTree", this, this, "conflictTree", "Days", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;

    initDayOfWeekString();
    initTimeStrings();

    conflictTreeTableView.rootProperty().bind(new ReadOnlyObjectWrapper<>(conflictTreeRootItem));

    conflictTreeRootItem.expandedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        conflictTreeTableView.setPrefHeight(
            conflictTreeTableView.lookup(".column-header-background").getBoundsInLocal().getHeight()
                + 50.0);
      } else {
        conflictTreeTableView.setPrefHeight(175.0);
      }
    });
    initTreeTableViewValueFactories();
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
      final TreeItem<Object> dayRootItem = new TreeItem<>(dayOfWeekStrings
          .get(dayOfWeekEntry.getKey()));
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
      final String timeString = timeStrings.get(session.getTime());
      if (!sortedSessionsByTime.containsKey(timeString)) {
        sortedSessionsByTime.put(timeString, new ArrayList<>());
      }
      sortedSessionsByTime.get(timeString).add(session);
    });
    return sortedSessionsByTime;
  }

  void setConflictSessions(final List<Session> conflictSessions) {
    this.conflictSessions = conflictSessions;
    showConflictResult(conflictSessions);
  }

  void setUnsatCoreProperty(final ListProperty<Integer> unsatCoreProperty) {
    this.unsatCoreProperty = unsatCoreProperty;
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

    treeColumnSemesters.setCellValueFactory(param ->
        new ReadOnlyStringWrapper(
          (param.getValue().getValue() instanceof Session)
          ?
            Joiner.on(", ").join(
              ((Session) param.getValue().getValue()).getGroup().getUnit().getSemesters())
            : ""));
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
}
