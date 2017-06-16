package de.hhu.stups.plues.ui.components.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.components.timetable.TimetableMisc;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupUnsatCore extends VBox implements Initializable {

  private final ListProperty<Group> groups;
  private final ListProperty<AbstractUnit> abstractUnits;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Group> groupsTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Group, String> tableColumnGroupUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Group, String> tableColumnGroupUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Group, String> tableColumnGroupUnitSemesters;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Group, Set<Session>> tableColumnGroupSessions;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Group, Set<AbstractUnit>> tableColumnGroupAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar unsatCoreButtonBar;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   */
  @Inject
  public GroupUnsatCore(final Inflater inflater,
                        final Router router) {
    groups = new SimpleListProperty<>(FXCollections.emptyObservableList());
    abstractUnits = new SimpleListProperty<>(FXCollections.emptyObservableList());
    this.router = router;

    inflater.inflate("components/unsatcore/GroupUnsatCore", this, this, "unsatCore", "Column",
        "Days");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    txtExplanation.wrappingWidthProperty().bind(widthProperty().subtract(150));

    groupsTable.itemsProperty().bind(groups);
    groupsTable.setOnMouseClicked(DetailViewHelper.getGroupMouseHandler(
        groupsTable, router));
    tableColumnGroupUnitKey.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "unit", "key"));
    tableColumnGroupUnitTitle.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "unit", "title"));
    tableColumnGroupUnitSemesters.setCellValueFactory(param
        -> new SimpleStringProperty(param.getValue().getUnit().getSemesters().stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","))));

    // display a bullet-list of sessions to represent the group
    tableColumnGroupSessions.setCellFactory(param -> new TableCell<Group, Set<Session>>() {
      @Override
      protected void updateItem(final Set<Session> item, final boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText(null);
          return;
        }
        final String prefix = getPrefix(item);
        setText(item.stream()
            .map(s -> {
              String dayString = resources.getString(
                  TimetableMisc.shortDayOfWeekMap.get(s.getDayOfWeekMap().get(s.getDay())));
              String timeString = String.valueOf(6 + s.getTime() * 2) + ":30";

              return String.format("%s%s - %s%n", prefix, dayString, timeString);
            })
            .reduce(String::concat).orElse("??"));
      }
    });

    unsatCoreButtonBar.setSubmitText(resources.getString("button.unsatCoreSession"));

    // extract abstract units associated to group (through unit) in the current abstract unit core
    tableColumnGroupAbstractUnits.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(
        param.getValue().getUnit().getAbstractUnits().stream()
            .filter(getAbstractUnits()::contains)
            .collect(Collectors.toSet())));

    tableColumnGroupAbstractUnits.setCellFactory(param ->
        new TableCell<Group, Set<AbstractUnit>>() {
          @Override
          protected void updateItem(final Set<AbstractUnit> item, final boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
              setText(null);
              return;
            }
            final String prefix = getPrefix(item);
            setText(item.stream()
                .map(e -> String.format("%s%s", prefix, e.getKey())).collect(Collectors.joining(
                    String.format("%n"))));
          }
        });
  }

  private String getPrefix(final Collection<?> item) {
    if (item.size() > 1) {
      return "• ";
    }
    return "";
  }

  public void resetTaskState() {
    unsatCoreButtonBar.taskProperty().set(null);
  }

  public void setGroups(final ObservableList<Group> groups) {
    this.groups.set(groups);
  }

  public ObservableList<Group> getGroups() {
    return this.groups.get();
  }

  private ObservableList<AbstractUnit> getAbstractUnits() {
    return abstractUnits.get();
  }

  public ListProperty<AbstractUnit> abstractUnitsProperty() {
    return abstractUnits;
  }

  public void setAbstractUnits(final ObservableList<AbstractUnit> abstractUnits) {
    this.abstractUnits.set(abstractUnits);
  }


  public ListProperty<Group> groupProperty() {
    return groups;
  }

  public UnsatCoreButtonBar getUnsatCoreButtonBar() {
    return unsatCoreButtonBar;
  }
}
