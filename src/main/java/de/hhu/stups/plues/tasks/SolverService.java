package de.hhu.stups.plues.tasks;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.prob.Alternative;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.Solver;
import javafx.concurrent.Task;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SolverService {
    private final ExecutorService executor;
    private final Solver solver;

    @Inject
    public SolverService(final Solver s) {
        this.executor = Executors.newSingleThreadExecutor();
        this.solver = s;
    }

    @SuppressWarnings("unused")
    public final Task<Boolean> checkFeasibilityTask(final Course... courses) {
        assert this.solver != null;

        final String[] names = getNames(courses);
        final String msg = getMessage(names);
        //
        return new SolverTask<>("Checking Feasibility", msg, this.solver,
                () -> this.solver.checkFeasibility(names));
    }

    @SuppressWarnings("unused")
    public final Task<FeasibilityResult> computeFeasibilityTask(
            final Course... courses) {

        final String[] names = getNames(courses);
        final String msg = getMessage(names);
        //
        return new SolverTask<>("Computing Feasibility",
                msg, solver,
                () -> solver.computeFeasibility(names));
    }

    @SuppressWarnings("unused")
    public final Task<FeasibilityResult> computePartialFeasibility(
            final List<Course> courses,
            final Map<Course, List<Module>> moduleChoice,
            final List<AbstractUnit> abstractUnitChoice) {

        final List<String> names = courses.stream()
                .map(Course::getName)
                .collect(Collectors.toList());

        final Map<String, List<Integer>> mc = moduleChoice.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getName(),
                        e -> e.getValue().stream()
                                .map(Module::getId)
                                .collect(Collectors.toList())));

        final List<Integer> auc = abstractUnitChoice.stream()
                .map(AbstractUnit::getId)
                .collect(Collectors.toList());

        final String msg = getMessage(names);
        //
        return new SolverTask<>("Computing Feasibility",
                msg, solver,
                () -> solver.computePartialFeasibility(names, mc, auc));
    }

    @SuppressWarnings("unused")
    public final Task<List<Integer>> unsatCore(final Course... courses) {

        final String[] names = getNames(courses);
        final String msg = getMessage(names);
        //
        return new SolverTask<>("Computing UNSAT Core", msg, solver,
                () -> solver.unsatCore(names));
    }

    public Task<List<Alternative>> localAlternativesTask(final Session session,
                                                         final Course... courses) {
        String[] names = getNames(courses);
        String msg = getMessage(names);
        return new SolverTask<>("Computing alternatives", msg, solver,
                () -> solver.getLocalAlternatives(session.getId(), names));
    }

    public Task<Set<String>> impossibleCoursesTask() {
        return new SolverTask<>("Collecting impossible courses", "Impossible",
                solver, () -> solver.getImpossibleCourses());
    }

    private String getMessage(final String[] names) {
        return Joiner.on(", ").join(names);
    }

    private String getMessage(final List<String> names) {
        return Joiner.on(", ").join(names);
    }


    @SuppressWarnings("unused")
    private String[] getNames(final Course[] courses) {
        final String[] names = new String[courses.length];
        for(int i = 0; i < courses.length; i++) {
            names[i] = courses[i].getName();
        }
        return names;
//        return Arrays.asList(courses).stream()
//                    .map(c -> c.getName())
//                    .collect(Collectors.toList())
//                    .toArray(new String[courses.length]);
    }

    public final void submit(final Task<?> command) {
        this.executor.submit(command);
    }

}
