package de.hhu.stups.plues.studienplaene;

import java.awt.Color;

/**
 * Not explicitly grayscales but we use simple colors that can be easily printed in grayscale.
 */
class Grayscale implements ColorPalette {

  private Color[] colors = new Color[8];
  private int pointer;

  Grayscale() {
    pointer = 0;

    colors[0] = new Color(224, 176, 255); // Mauve
    colors[1] = new Color(240, 230, 140); // Khaki
    colors[2] = new Color(0, 255, 0);     // Lime
    colors[3] = new Color(0, 255, 255);   // Cyan
    colors[4] = new Color(85, 107, 47);   // Darkolivegreen
    colors[5] = new Color(127, 255, 212); // Aquamarine
    colors[6] = new Color(178, 34, 34);   // Firebrick
    colors[7] = new Color(221, 160, 221); // Plum
  }

  @Override
  public Color nextColor() {
    Color color;
    color = colors[pointer];
    pointer++;
    pointer %= colors.length;
    return color;
  }
}
