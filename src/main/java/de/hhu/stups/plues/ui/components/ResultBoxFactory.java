package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import javafx.scene.control.ListView;

@FunctionalInterface
public interface ResultBoxFactory {
  @Inject
  ResultBox create(@Assisted("major") Course major,
                   @Assisted("minor") Course minor,
                   @Assisted("parent") ListView<ResultBox> parent,
                   @Assisted PdfGenerationSettings pdfGenerationSettings);
}
