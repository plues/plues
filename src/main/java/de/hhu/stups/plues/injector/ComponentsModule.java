package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;

import de.hhu.stups.plues.ui.components.AboutWindow;
import de.hhu.stups.plues.ui.components.AbstractUnitFilter;
import de.hhu.stups.plues.ui.components.ChangeLog;
import de.hhu.stups.plues.ui.components.CheckCourseFeasibility;
import de.hhu.stups.plues.ui.components.CombinationOrSingleCourseSelection;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.SetOfCourseSelection;
import de.hhu.stups.plues.ui.components.Timetable;
import de.hhu.stups.plues.ui.components.reports.ImpossibleAbstractUnitsInModule;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourses;
import de.hhu.stups.plues.ui.components.reports.IncompleteModules;
import de.hhu.stups.plues.ui.components.timetable.DetailView;
import de.hhu.stups.plues.ui.controller.BatchTimetableGeneration;
import de.hhu.stups.plues.ui.controller.ConflictMatrix;
import de.hhu.stups.plues.ui.controller.Musterstudienplaene;
import de.hhu.stups.plues.ui.controller.PartialTimeTables;

class ComponentsModule extends AbstractModule {
  @Override
  public void configure() {
    bind(Musterstudienplaene.class);
    bind(BatchTimetableGeneration.class);
    bind(PartialTimeTables.class);
    bind(MajorMinorCourseSelection.class);
    bind(Timetable.class);
    bind(ChangeLog.class);
    bind(ConflictMatrix.class);
    bind(AbstractUnitFilter.class);
    bind(SetOfCourseSelection.class);
    bind(CombinationOrSingleCourseSelection.class);
    bind(CheckCourseFeasibility.class);
    bind(DetailView.class);
    bind(AboutWindow.class);
    bind(IncompleteModules.class);
    bind(ImpossibleAbstractUnitsInModule.class);
    bind(ImpossibleCourses.class);
  }
}
