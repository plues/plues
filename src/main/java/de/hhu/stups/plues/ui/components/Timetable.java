package de.hhu.stups.plues.ui.components;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.DayOfWeek;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class Timetable extends BorderPane implements Initializable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;
  private ListProperty<Session> sessions = new SimpleListProperty<>();

  private final BooleanProperty
      solverProperty = new SimpleBooleanProperty(false);
  @FXML
  private AbstractUnitFilter abstractUnitFilter;
  @FXML
  private GridPane timeTable;

  @FXML
  @SuppressWarnings("unused")
  private SetOfCourseSelection setOfCourseSelection;

  /**
   * Timetable component.
   */
  @Inject
  public Timetable(final Inflater inflater, final Delayed<Store> delayedStore,
                          final Delayed<SolverService> delayedSolverService) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;

    // TODO: remove controller param if possible
    // TODO: currently not possible because of dependency circle
    inflater.inflate("components/Timetable", this, this, "timetable");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.delayedStore.whenAvailable(s -> {
      Runtime.getRuntime().addShutdownHook(new Thread(s::close));
      this.abstractUnitFilter.setAbstractUnits(s.getAbstractUnits());
      setOfCourseSelection.setCourses(s.getCourses());

      setSessions(s.getSessions());
    });

    this.delayedSolverService.whenAvailable(s -> {
      this.solverProperty.set(true);
    });

    initSessionBoxes();
  }

  private void initSessionBoxes() {
    final int offX = 1;
    final int offY = 1;
    final int widthX = 5;

    IntStream.range(0, 35).forEach(i -> {

      ListView<Session> view = new ListView<>();
      view.itemsProperty().bind(new ListBinding<Session>() {
        {
          // TODO: bind to all slot properties and to new slot properties
          // TODO: http://stackoverflow.com/questions/32536096/javafx-bindings-not-working-as-expected
          bind(sessions);
        }

        @Override
        protected ObservableList<Session> computeValue() {
          return sessions.filtered(session -> session.getSlot().equals(getSlot(i, widthX)));
        }
      });

      timeTable.add(view, i % widthX + offX, (i / widthX) + offY);
    });
  }

  private Session.Slot getSlot(int index, int widthX) {
    DayOfWeek[] days = { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY };
    Integer[] times = { 1, 2, 3, 4, 5, 6, 7 };

    return new Session.Slot(days[index % widthX], times[index / widthX]);
  }

  private void setSessions(List<Session> sessions) {
    sessions.forEach(Session::initSlotProperty);
    this.sessions.set(FXCollections.observableArrayList(sessions));
  }
}
