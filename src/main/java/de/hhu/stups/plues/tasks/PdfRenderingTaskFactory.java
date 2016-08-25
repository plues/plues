package de.hhu.stups.plues.tasks;

import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;

public interface PdfRenderingTaskFactory {
  PdfRenderingTask create(@Assisted("major") Course major, @Assisted("minor") Course minor);
}
