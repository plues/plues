package de.hhu.stups.plues.studienplaene;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A color scheme definition yielding a set of hexadecimal colors and a visualization of the
 * colors.
 */
public final class ColorScheme {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String name;
  private final ColorChoice colorChoice;
  private final Set<String> colors;
  private final Pattern hexColorPattern = Pattern.compile("^#(?:[0-9a-fA-F]{3}){1,2}$");

  /**
   * Create a color scheme defined by its name and a set of hex color codes.
   */
  public ColorScheme(final String name, final ColorChoice colorChoice, final Set<String> colors) {
    this.name = name;
    this.colorChoice = colorChoice;
    this.colors = colors;
    colors.forEach(color -> {
      if (!hexColorPattern.matcher(color).matches()) {
        logger.error("Wrong color code definition for " + color + " in class ColorScheme.");
        throw new InstantiationError();
      }
    });
  }

  /**
   * Add a given amount of preview colors to a hbox.
   */
  public void addColorPreviews(final HBox imageBox,
                               final long previewAmount,
                               final double previewRectangleSize) {
    final Iterator<String> colorIterator = getColors().iterator();
    int previewCounter = 0;
    while (colorIterator.hasNext()) {
      imageBox.getChildren().add(getColorPreview(previewRectangleSize, colorIterator.next()));
      previewCounter++;
      if (previewCounter == previewAmount) {
        break;
      }
    }
    final Label label = new Label(" - " + getName());
    label.setStyle("-fx-text-fill: #000000;");
    imageBox.getChildren().add(imageBox.getChildren().size(), label);
  }

  private Rectangle getColorPreview(final double rectangleSize, final String hexColorCode) {
    final Rectangle rectangle = new Rectangle(rectangleSize, rectangleSize);
    rectangle.setFill(Color.web(hexColorCode));
    rectangle.setStroke(Color.BLACK);
    rectangle.setStrokeWidth(1.0);
    return rectangle;
  }

  public String getName() {
    return name;
  }

  public Set<String> getColors() {
    return colors;
  }

  public boolean isGrayscale() {
    return ColorChoice.GRAYSCALE.equals(colorChoice);
  }
}