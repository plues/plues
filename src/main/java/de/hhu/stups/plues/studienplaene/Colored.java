package de.hhu.stups.plues.studienplaene;

import java.awt.Color;
import java.util.Set;

class Colored implements ColorPalette {

  private final Color[] colors;
  private int pointer = 0;

  Colored(final Set<String> initialColors) {
    colors = new Color[initialColors.size()];
    initialColors.forEach(color -> {
      colors[pointer] = Color.decode(color);
      pointer++;
    });
    pointer = 0;
  }

  @Override
  public Color nextColor() {
    final Color c;
    c = colors[pointer];
    pointer++;
    pointer %= colors.length;
    return c;
  }
}
