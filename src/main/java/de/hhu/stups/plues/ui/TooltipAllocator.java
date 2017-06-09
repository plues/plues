package de.hhu.stups.plues.ui;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;

public interface TooltipAllocator {

  /**
   * Show the tooltip immediately when the mouse enters the node and accept a boolean property that
   * can handle individual exceptions when to hide the tooltip or respectively not even show it. If
   * there is no hide exception just pass a simple boolean property set to false. We set the tooltip
   * of nodes that inherit from {@link Control} to null since we show the tooltip manually.
   */
  static void showTooltipOnEnter(final Node node,
                                 final Tooltip tooltip,
                                 final ReadOnlyBooleanProperty hide) {
    if (node instanceof Control) {
      ((Control) node).setTooltip(null);
    }
    node.setOnMouseEntered(event -> {
      if (tooltip.getText().isEmpty() || hide.get()) {
        tooltip.hide();
        return;
      }
      final Point2D pos = node.localToScreen(
          node.getLayoutBounds().getMaxX(),
          node.getLayoutBounds().getMaxY());
      tooltip.show(node, pos.getX(), pos.getY());
    });
    node.setOnMouseExited(event -> tooltip.hide());
  }
}
