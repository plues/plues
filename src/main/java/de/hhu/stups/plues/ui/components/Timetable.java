package de.hhu.stups.plues.ui.components;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.ui.components.timetable.SessionListView;
import de.hhu.stups.plues.ui.components.timetable.SessionListViewFactory;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.DayOfWeek;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Timetable extends BorderPane implements Initializable {

  private final Delayed<Store> delayedStore;
  private final SessionListViewFactory factory;

  @FXML
  private AbstractUnitFilter abstractUnitFilter;

  @FXML
  private GridPane timeTable;

  @FXML
  @SuppressWarnings("unused")
  private SetOfCourseSelection setOfCourseSelection;
  @FXML
  private ToggleGroup semesterToggle;

  private final ListProperty<SessionFacade> sessions = new SimpleListProperty<>();

  /**
   * Timetable component.
   */
  @Inject
  public Timetable(final Inflater inflater, final Delayed<Store> delayedStore,
                   final SessionListViewFactory factory) {
    this.delayedStore = delayedStore;
    this.factory = factory;

    // TODO: remove controller param if possible
    // TODO: currently not possible because of dependency circle
    inflater.inflate("components/Timetable", this, this, "timetable");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.delayedStore.whenAvailable(store -> {
      this.abstractUnitFilter.setAbstractUnits(store.getAbstractUnits());
      setOfCourseSelection.setCourses(store.getCourses());

      setSessions(store.getSessions()
          .parallelStream()
          .map(SessionFacade::new)
          .collect(Collectors.toList()));
    });

    initSessionBoxes();
  }

  private void initSessionBoxes() {
    final int offX = 1;
    final int offY = 1;
    final int widthX = 5;

    IntStream.range(0, 35).forEach(i -> {
      final SessionFacade.Slot slot = getSlot(i, widthX);

      final ListView<SessionFacade> view = getSessionFacadeListView(slot);

      timeTable.add(view, i % widthX + offX, (i / widthX) + offY);
    });
  }

  private ListView<SessionFacade> getSessionFacadeListView(final SessionFacade.Slot slot) {
    final ListView<SessionFacade> view = factory.create(slot);

    ((SessionListView) view).setSessions(sessions);
    view.itemsProperty().bind(new SessionFacadeListBinding(slot));
    return view;
  }

  private SessionFacade.Slot getSlot(final int index, final int widthX) {
    final DayOfWeek[] days = { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY };
    final Integer[] times = { 1, 2, 3, 4, 5, 6, 7 };

    return new SessionFacade.Slot(days[index % widthX], times[index / widthX]);
  }

  private void setSessions(final List<SessionFacade> sessions) {
    sessions.forEach(SessionFacade::initSlotProperty);
    this.sessions.set(FXCollections.observableArrayList(sessions));
  }

  private class SessionFacadeListBinding extends ListBinding<SessionFacade> {

    private final SessionFacade.Slot slot;

    SessionFacadeListBinding(final SessionFacade.Slot slot) {
      this.slot = slot;
      bind(sessions, semesterToggle.selectedToggleProperty());

      // http://stackoverflow.com/questions/32536096/javafx-bindings-not-working-as-expected
      sessions.addListener((Change<? extends SessionFacade> change) -> {
        while (change.next()) {
          if (change.wasAdded()) {
            change.getAddedSubList()
              .forEach(sessionFacade -> bind(sessionFacade.slotProperty()));
          }

          if (change.wasRemoved()) {
            change.getRemoved().forEach(sessionFacade -> unbind(sessionFacade.slotProperty()));
          }
        }
      });
    }

    @Override
    protected ObservableList<SessionFacade> computeValue() {
      final ToggleButton semesterButton = (ToggleButton) semesterToggle.getSelectedToggle();

      return sessions.filtered(session -> {
        final Set<Integer> semesters = session.getSemesters();

        Integer semester = null;
        if (null != semesterButton) {
          semester = Integer.valueOf(semesterButton.getText());
        }

        return session.getSlot().equals(slot)
          && (semester == null || semesters.contains(semester));
      });
    }
  }
}
