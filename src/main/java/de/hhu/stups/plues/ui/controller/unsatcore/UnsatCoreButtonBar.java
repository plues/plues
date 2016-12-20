package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class UnsatCoreButtonBar extends HBox implements Initializable {

  private final ObjectProperty<SolverService> solverService;
  private final ObjectProperty<Store> store;
  private final ExecutorService executorService;
  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private Button button;
  @FXML
  @SuppressWarnings("unused")
  private Label taskStateLabel;
  @FXML
  @SuppressWarnings("unused")
  private Label taskStateIcon;

  /**
   * Default constructor.
   */
  @Inject
  public UnsatCoreButtonBar(final Inflater inflater,
                            final Delayed<SolverService> delayedSolverService,
                            final Delayed<Store> delayedStore,
                            final ExecutorService executorService) {
    solverService = new SimpleObjectProperty<>();
    delayedSolverService.whenAvailable(this.solverService::set);

    store = new SimpleObjectProperty<>();
    delayedStore.whenAvailable(this.store::set);

    this.executorService = executorService;

    inflater.inflate("components/unsatcore/UnsatCoreButtonBar", this, this, "unsatCore");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.resources = resources;
  }

  /**
   * Configure button for modules unsat core.
   */
  void configureButton(final String text,
                       final ListProperty<Course> courses,
                       final ModuleUnsatCore moduleUnsatCore) {
    button.setText(text);
    button.disableProperty().bind(solverService.isNull()
        .or(courses.emptyProperty())
        .or(moduleUnsatCore.getModuleProperty().emptyProperty().not()));
    button.setOnMouseClicked(event -> {
      final ObservableList<Course> courseList = courses.get();
      final Course[] selectedCourses = new Course[courseList.size()];
      final SolverTask<Set<Integer>> task =
          getSolverService().unsatCoreModules(courseList.toArray(selectedCourses));

      task.setOnSucceeded(succeeded -> {
        final Set<Integer> moduleIds = task.getValue();
        moduleUnsatCore.setModules(moduleIds.stream().map(getStore()::getModuleById)
            .collect(Collectors.collectingAndThen(Collectors.toList(),
              FXCollections::observableArrayList)));
      });

      showTaskState(taskStateIcon, taskStateLabel, task, resources);
      executorService.submit(task);
    });
  }

  /**
   * Configure button for abstract units unsat core.
   */
  void configureButton(final String text,
                       final ListProperty<Module> modules,
                       final AbstractUnitUnsatCore abstractUnitUnsatCore) {
    button.setText(text);
    button.disableProperty().bind(modules.emptyProperty()
        .or(abstractUnitUnsatCore.getAbstractUnitProperty().emptyProperty().not()));
    button.setOnMouseClicked(event -> {
      final SolverTask<Set<Integer>> task =
          getSolverService().unsatCoreAbstractUnits(modules.get());

      task.setOnSucceeded(succeeded -> {
        final Set<Integer> abstractUnitIds = task.getValue();
        abstractUnitUnsatCore.setAbstractUnits(abstractUnitIds.stream()
            .map(getStore()::getAbstractUnitById).collect(Collectors
              .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));

      });
      showTaskState(taskStateIcon, taskStateLabel, task, resources);
      executorService.submit(task);
    });
  }

  /**
   * Configure button for group unsat core.
   */
  void configureButton(final String text,
                       final ListProperty<Module> modules,
                       final ListProperty<AbstractUnit> abstractUnits,
                       final GroupUnsatCore groupUnsatCore) {
    button.setText(text);
    button.disableProperty().bind(abstractUnits.emptyProperty()
        .or(groupUnsatCore.getGroupProperty().emptyProperty().not()));
    button.setOnMouseClicked(event -> {
      final SolverTask<Set<Integer>> task =
          getSolverService().unsatCoreGroups(abstractUnits.get(), modules.get());

      task.setOnSucceeded(succeeded -> {
        final Set<Integer> groupIds = task.getValue();
        groupUnsatCore.setGroups(groupIds.stream().map(getStore()::getGroupById).collect(Collectors
            .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
      });
      showTaskState(taskStateIcon, taskStateLabel, task, resources);
      executorService.submit(task);
    });
  }

  /**
   * Configure button for session unsat core.
   */
  void configureButton(final String text,
                       final ListProperty<Group> groups,
                       final SessionUnsatCore sessionUnsatCore) {
    button.setText(text);
    button.disableProperty().bind(groups.emptyProperty()
        .or(sessionUnsatCore.getSessionProperty().emptyProperty().not()));
    button.setOnMouseClicked(event -> {
      final SolverTask<Set<Integer>> task = getSolverService().unsatCoreSessions(groups.get());

      task.setOnSucceeded(succeeded -> {
        final Set<Integer> sessionIds = task.getValue();
        sessionUnsatCore.setSessions(sessionIds.stream().map(getStore()::getSessionById).collect(
            Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
      });
      showTaskState(taskStateIcon, taskStateLabel, task, resources);
      executorService.submit(task);
    });
  }

  /**
   * Show and set current task state.
   */
  private void showTaskState(final Label icon, final Label message, final Task<?> task,
                             final ResourceBundle resources) {
    icon.graphicProperty().unbind();
    icon.styleProperty().unbind();
    message.textProperty().unbind();

    icon.graphicProperty().bind(TaskBindings.getIconBinding("25", task));
    icon.styleProperty().bind(TaskBindings.getStyleBinding(task));
    message.textProperty().bind(Bindings.createStringBinding(() -> {
      final String msg;
      switch (task.getState()) {
        case SUCCEEDED:
          msg = "";
          break;
        case CANCELLED:
          msg = resources.getString("task.Cancelled");
          break;
        case FAILED:
          msg = resources.getString("task.Failed");
          break;
        case READY:
        case SCHEDULED:
        case RUNNING:
        default:
          msg = resources.getString("task.Running");
          break;
      }
      return msg;
    }, task.stateProperty()));
  }


  void resetTaskState() {
    taskStateIcon.styleProperty().unbind();
    taskStateIcon.setStyle("");
    //
    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.setGraphic(null);
    //
    taskStateLabel.textProperty().unbind();
    taskStateLabel.setText("");
  }

  public SolverService getSolverService() {
    return solverService.get();
  }

  public Store getStore() {
    return store.get();
  }
}
