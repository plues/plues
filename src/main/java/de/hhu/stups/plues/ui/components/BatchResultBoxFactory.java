package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.tasks.PdfRenderingTask;

public interface BatchResultBoxFactory {
  BatchResultBox create(PdfRenderingTask task);
}
