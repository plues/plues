package de.hhu.stups.plues.ui.components.conflictmatrix;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.util.ResourceBundle;

class ResultContextMenu extends ContextMenu {

  private final Router router;
  private final Course[] courses;
  private final MenuItem itemShowInTimetable;
  private final MenuItem itemGeneratePartialTimetable;
  private final MenuItem itemComputeConflict;
  private final MenuItem itemGeneratePdf;
  private final MenuItem itemRecomputeFeasibility;
  private final MenuItem itemCheckFeasibility;

  /**
   * The context menu for a {@link ResultGridCell} in the {@link de.hhu.stups.plues.ui.controller
   * .ConflictMatrix}.
   */

  ResultContextMenu(final Router router, final ObjectProperty<ResultState> resultState,
                    final Course... courses) {
    this.router = router;
    this.courses = courses;
    final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrixContextMenu");

    itemShowInTimetable = new MenuItem(resources.getString("showInTimetable"));
    itemGeneratePartialTimetable = new MenuItem(resources.getString("generatePartialTimetable"));
    itemComputeConflict = new MenuItem(resources.getString("computeConflict"));
    itemGeneratePdf = new MenuItem(resources.getString("generatePdf"));
    itemRecomputeFeasibility = new MenuItem(resources.getString("recomputeFeasibility"));
    itemCheckFeasibility = new MenuItem(resources.getString("checkFeasibility"));

    resultState.addListener((observable, oldValue, newValue) -> updateMenu(newValue));
    updateMenu(resultState.get());

    itemGeneratePdf.setOnAction(event ->
        router.transitionTo(RouteNames.PDF_TIMETABLES, (Object[]) courses));
    itemComputeConflict.setOnAction(event ->
        router.transitionTo(RouteNames.UNSAT_CORE, (Object[]) courses));
    itemGeneratePartialTimetable.setOnAction(event ->
        router.transitionTo(RouteNames.PARTIAL_TIMETABLES, (Object[]) courses));
    itemRecomputeFeasibility.setOnAction(event ->
        router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.TIMEOUT));
    itemCheckFeasibility.setOnAction(event ->
        // do not open timetable view but run check feasibility task in background
        router.transitionTo(RouteNames.CHECK_FEASIBILITY_TIMETABLE, courses,
            ResultState.UNKNOWN, false));
  }

  private void updateMenu(final ResultState resultState) {
    getItems().clear();
    if (resultState == null) {
      return;
    }
    switch (resultState) {
      case SUCCEEDED:
        updateToSucceedingState();
        break;
      case IMPOSSIBLE:
        getItems().addAll(itemShowInTimetable);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.IMPOSSIBLE));
        break;
      case FAILED:
        getItems().addAll(itemShowInTimetable, itemComputeConflict);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.FAILED));
        break;
      case TIMEOUT:
        getItems().add(itemRecomputeFeasibility);
        break;
      case UNKNOWN:
        getItems().addAll(itemCheckFeasibility, itemShowInTimetable);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.UNKNOWN));
        break;
      default:
        break;
    }
  }

  private void updateToSucceedingState() {
    if (courses.length == 1 && courses[0].isCombinable()) {
      getItems().add(itemShowInTimetable);
    } else {
      getItems().addAll(itemShowInTimetable, itemGeneratePartialTimetable, itemGeneratePdf);
    }
    itemShowInTimetable.setOnAction(event ->
        router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.SUCCEEDED));
  }
}
