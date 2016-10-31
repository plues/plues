package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.controlsfx.control.PopOver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class SessionCell extends ListCell<SessionFacade> {

  private final Inflater inflater;

  @Inject
  SessionCell(final Inflater inflater) {
    super();

    this.inflater = inflater;

    setOnDragDetected(this::dragItem);
    setOnMouseClicked(this::clickItem);
  }

  private void dragItem(MouseEvent event) {
    if (getItem() == null) {
      return;
    }

    Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
    ClipboardContent content = new ClipboardContent();
    content.putString(String.valueOf(getItem().getSession().getId()));
    dragboard.setContent(content);
    event.consume();
  }

  private void clickItem(MouseEvent event) {
    Session session = getItem().getSession();
    Unit unit = session.getGroup().getUnit();
    Set<AbstractUnit> abstractUnits = unit.getAbstractUnits();

    final Map<Course, Set<Module>> courseToModuleMap = new HashMap<>();
    abstractUnits.forEach(abstractUnit ->
        abstractUnit.getModules().forEach(module ->
            module.getCourses().forEach(course -> {
              Set<Module> modules;
              if (courseToModuleMap.containsKey(course)) {
                modules = courseToModuleMap.get(course);
                modules.add(module);
              } else {
                modules = new HashSet<>();
              }
              courseToModuleMap.put(course, modules);
            })));

    DetailView view = new DetailView(inflater);
    view.setContent(abstractUnits, unit, courseToModuleMap);

    PopOver pop = new PopOver(view);
    pop.setPrefHeight(400);
    pop.setPrefWidth(400);
    pop.setTitle("Session Detail");
    pop.show(this);
  }

  @Override
  protected void updateItem(SessionFacade session, boolean empty) {
    super.updateItem(session, empty);

    setText(empty || session == null ? null : session.toString());
  }
}
