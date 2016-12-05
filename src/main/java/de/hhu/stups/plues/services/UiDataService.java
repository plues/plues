package de.hhu.stups.plues.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.tasks.SolverTask;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Stupid data container that can be injected and used by all UI components to store global data.
 */

@Singleton
public class UiDataService {
  private final ListProperty<Integer> conflictMarkedSessionsProperty =
      new SimpleListProperty<>(FXCollections.emptyObservableList());
  private final StringProperty sessionDisplayFormatProperty = new SimpleStringProperty();
  private final SetProperty<String> impossibleCoursesProperty
      = new SimpleSetProperty<>(FXCollections.observableSet());
  private final ObjectProperty<Date> lastSavedDate
      = new SimpleObjectProperty<>(new Date(ManagementFactory.getRuntimeMXBean().getStartTime()));

  private final ExecutorService executorService;

  @Inject
  public UiDataService(final Delayed<SolverService> solverServiceDelayed,
      final ExecutorService executorService) {
    this.executorService = executorService;
    solverServiceDelayed.whenAvailable(this::loadImpossibleCourses);
  }

  public Date getLastSavedDate() {
    return lastSavedDate.get();
  }

  public void setLastSavedDate(final Date lastSavedDate) {
    this.lastSavedDate.set(lastSavedDate);
  }

  public ObjectProperty<Date> lastSavedDateProperty() {
    return lastSavedDate;
  }

  public ObservableSet<String> getImpossibleCourses() {
    return impossibleCoursesProperty.get();
  }

  private void setImpossibleCourses(final Set<String> value) {
    this.impossibleCoursesProperty.set(FXCollections.observableSet(value));
  }

  public SetProperty<String> impossibleCoursesProperty() {
    return impossibleCoursesProperty;
  }

  private void loadImpossibleCourses(final SolverService solverService) {
    final SolverTask<Set<String>> t = solverService.impossibleCoursesTask();
    executorService.submit(t);
    t.setOnSucceeded(event -> this.setImpossibleCourses(t.getValue()));
  }

  public String getSessionDisplayFormatProperty() {
    return sessionDisplayFormatProperty.get();
  }

  public void setSessionDisplayFormatProperty(final String sessionDisplayFormatProperty) {
    this.sessionDisplayFormatProperty.set(sessionDisplayFormatProperty);
  }

  public StringProperty sessionDisplayFormatProperty() {
    return sessionDisplayFormatProperty;
  }

  public ObservableList<Integer> getConflictMarkedSessions() {
    return conflictMarkedSessionsProperty.get();
  }

  public void setConflictMarkedSessions(final ObservableList<Integer> conflictMarkedSessions) {
    this.conflictMarkedSessionsProperty.set(conflictMarkedSessions);
  }

  public ListProperty<Integer> conflictMarkedSessionsProperty() {
    return conflictMarkedSessionsProperty;
  }
}
