package de.hhu.stups.plues.ui.components.conflictmatrix;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
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

  /**
   * The contex menu for a {@link ResultGridCell} in the {@link de.hhu.stups.plues.ui.controller
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

    resultState.addListener((observable, oldValue, newValue) -> updateMenu(newValue));

    itemGeneratePdf.setOnAction(event ->
        router.transitionTo("pdfTimetables", (Object[]) courses));
    itemComputeConflict.setOnAction(event ->
        router.transitionTo("unsatCore", (Object[]) courses));
    itemGeneratePartialTimetable.setOnAction(event ->
        router.transitionTo("partialTimetables", (Object[]) courses));
  }

  private void updateMenu(final ResultState resultState) {
    getItems().clear();
    switch (resultState) {
      case SUCCEEDED:
        getItems().addAll(itemShowInTimetable, itemGeneratePartialTimetable, itemGeneratePdf);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo("timetableView", courses, ResultState.SUCCEEDED));
        break;
      case IMPOSSIBLE:
        getItems().addAll(itemShowInTimetable);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo("timetableView", courses, ResultState.IMPOSSIBLE));
        break;
      case FAILED:
        getItems().addAll(itemShowInTimetable, itemComputeConflict);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo("timetableView", courses, ResultState.FAILED));
        break;
      default:
        break;
    }
  }
}
