package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
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


public class ConflictTree extends VBox implements Initializable {

  private final EnumMap<DayOfWeek, String> dayOfWeekStrings;
  private final Map<Integer, String> timeStrings;
  private final UiDataService uiDataService;

  private ListProperty<Integer> unsatCoreProperty;
  private ResourceBundle resources;
  private List<Session> conflictSessions;

  @FXML
  @SuppressWarnings("unused")
  private TreeView<String> conflictTreeView;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<String> conflictTreeRootItem;

  /**
   * Display the unsat core in a {@link #conflictTreeView TreeView} grouped by the day of week and
   * the time of the day. Furthermore provide a button to highlight all conflicting session in the
   * {@link Timetable}.
   */
  @Inject
  public ConflictTree(final Inflater inflater, final UiDataService uiDataService) {
    this.uiDataService = uiDataService;
    dayOfWeekStrings = new EnumMap<>(DayOfWeek.class);
    timeStrings = new HashMap<>();

    inflater.inflate("components/ConflictTree", this, this, "conflictTree");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;

    initDayOfWeekString();
    initTimeStrings();

    conflictTreeView.rootProperty().bind(new ReadOnlyObjectWrapper<>(conflictTreeRootItem));

    conflictTreeRootItem.expandedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        conflictTreeView.setPrefHeight(26.0);
      } else {
        conflictTreeView.setPrefHeight(175.0);
      }
    });
    conflictTreeRootItem.setExpanded(true);
  }

  @FXML
  @SuppressWarnings("unused")
  private void highlightConflicts() {
    uiDataService.setConflictMarkedSessions(unsatCoreProperty.get());
  }

  /**
   * Display the unsat core in a TreeView grouped by the day of week and the time of the day. The
   * TreeView's root property is bound to {@link #conflictTreeRootItem}.
   *
   * @param unsatCore The list of session Ids that form the unsat core.
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
