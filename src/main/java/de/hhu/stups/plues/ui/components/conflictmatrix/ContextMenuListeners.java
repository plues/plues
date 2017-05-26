package de.hhu.stups.plues.ui.components.conflictmatrix;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public interface ContextMenuListeners {

  /**
   * Set all necessary listeners for context menus used in the {@link
   * de.hhu.stups.plues.ui.controller.ConflictMatrix}. In detail: show the context menu on click of
   * the parent node, hide the context menu when leaving the parent node.
   *
   * @param node                       The context menu's parent node. Thought to be one of {@link
   *                                   ResultGridCell} or {@link CourseGridCell}.
   * @param contextMenu                The context menu to set the listeners on.
   * @param contextMenuFocusedProperty A boolean property stating whether the mouse is currently
   *                                   inside the context menu.
   * @param showContextMenuProperty    An object property referring to a {@link MouseEvent} to get
   *                                   the position to display the context menu.
   */
  static void setContextMenuListeners(final Node node,
                                      final ContextMenu contextMenu,
                                      final BooleanProperty contextMenuFocusedProperty,
                                      final ObjectProperty<MouseEvent> showContextMenuProperty) {
    node.setOnMouseExited(event -> {
      if (contextMenu != null && !contextMenuFocusedProperty.get()) {
        contextMenu.hide();
      }
    });
    node.setOnMouseClicked(event -> {
      if (event.getButton().equals(MouseButton.PRIMARY)) {
        showContextMenuProperty.set(event);
      }
    });
    contextMenu.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, event -> {
      if (!contextMenuFocusedProperty.get()) {
        contextMenuFocusedProperty.set(true);
      }
    });
    contextMenu.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, event -> {
      if (contextMenuFocusedProperty.get()) {
        contextMenuFocusedProperty.set(false);
      }
    });
  }
}
