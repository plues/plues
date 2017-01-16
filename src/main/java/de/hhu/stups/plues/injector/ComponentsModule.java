package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;

import de.hhu.stups.plues.ui.components.AboutWindow;
import de.hhu.stups.plues.ui.components.AbstractUnitFilter;
import de.hhu.stups.plues.ui.components.ChangeLog;
import de.hhu.stups.plues.ui.components.CheckCourseFeasibility;
import de.hhu.stups.plues.ui.components.CombinationOrSingleCourseSelection;
import de.hhu.stups.plues.ui.components.ConflictTree;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.SetOfCourseSelection;
import de.hhu.stups.plues.ui.controller.Timetable;
import de.hhu.stups.plues.ui.components.detailview.AbstractUnitDetailView;
import de.hhu.stups.plues.ui.components.detailview.CourseDetailView;
import de.hhu.stups.plues.ui.components.detailview.ModuleDetailView;
import de.hhu.stups.plues.ui.components.detailview.SessionDetailView;
import de.hhu.stups.plues.ui.components.detailview.UnitDetailView;
import de.hhu.stups.plues.ui.components.reports.AbstractUnitsWithoutUnits;
import de.hhu.stups.plues.ui.components.reports.ImpossibleAbstractUnitsInModule;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourseModuleAbstractUnitPairs;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourseModuleAbstractUnits;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourses;
import de.hhu.stups.plues.ui.components.reports.ImpossibleModules;
import de.hhu.stups.plues.ui.components.reports.MandatoryModules;
import de.hhu.stups.plues.ui.components.reports.ModuleAbstractUnitUnitSemesterConflicts;
import de.hhu.stups.plues.ui.components.reports.QuasiMandatoryModuleAbstractUnits;
import de.hhu.stups.plues.ui.components.reports.RedundantUnitGroups;
import de.hhu.stups.plues.ui.components.reports.UnitsWithoutAbstractUnits;
import de.hhu.stups.plues.ui.components.timetable.TimetableSideBar;
import de.hhu.stups.plues.ui.controller.BatchTimetableGeneration;
import de.hhu.stups.plues.ui.controller.ConflictMatrix;
import de.hhu.stups.plues.ui.controller.Musterstudienplaene;
import de.hhu.stups.plues.ui.controller.PartialTimeTables;
import de.hhu.stups.plues.ui.components.unsatcore.AbstractUnitUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.CourseUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.GroupUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.ModuleUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.SessionUnsatCore;
import de.hhu.stups.plues.ui.controller.UnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.UnsatCoreButtonBar;

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
    bind(SessionDetailView.class);
    bind(AboutWindow.class);
    bind(ImpossibleModules.class);
    bind(ImpossibleAbstractUnitsInModule.class);
    bind(ImpossibleCourses.class);
    bind(MandatoryModules.class);
    bind(QuasiMandatoryModuleAbstractUnits.class);
    bind(RedundantUnitGroups.class);
    bind(ImpossibleCourseModuleAbstractUnits.class);
    bind(ImpossibleCourseModuleAbstractUnitPairs.class);
    bind(ModuleAbstractUnitUnitSemesterConflicts.class);
    bind(AbstractUnitsWithoutUnits.class);
    bind(UnitsWithoutAbstractUnits.class);
    bind(ConflictTree.class);
    bind(CourseUnsatCore.class);
    bind(TimetableSideBar.class);
    bind(AbstractUnitDetailView.class);
    bind(ModuleDetailView.class);
    bind(UnitDetailView.class);
    bind(CourseDetailView.class);
    bind(ModuleUnsatCore.class);
    bind(AbstractUnitUnsatCore.class);
    bind(GroupUnsatCore.class);
    bind(SessionUnsatCore.class);
    bind(UnsatCoreButtonBar.class);
    bind(UnsatCore.class);
  }
}
