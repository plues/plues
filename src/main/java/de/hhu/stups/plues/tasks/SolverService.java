package de.hhu.stups.plues.tasks;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.Solver;
import javafx.concurrent.Task;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SolverService {
    private final ExecutorService executor;
    private Solver solver;

    @Inject
    public SolverService(Solver s) {
        this.executor = Executors.newSingleThreadExecutor();
        this.solver = s;
    }

    @SuppressWarnings("unused")
    public Task<Boolean> checkFeasibilityTask(Course... courses) {
        assert this.solver != null;
        String[] names = getNames(courses);
        String msg = Joiner.on(", ").join(names);
        return new SolverTask<>("Checking Feasibility", msg, this.solver,
                () -> this.solver.checkFeasibility(names));
    }

    @SuppressWarnings("unused")
    public Task<FeasibilityResult> computeFeasibilityTask(Course... courses) {
        String[] names = getNames(courses);
        String msg = Joiner.on(", ").join(names);
        return new SolverTask<>("Computing Feasibility",
                msg, solver,
                () -> solver.computeFeasibility(names));
    }

    @SuppressWarnings("unused")
    public Task<FeasibilityResult> computePartialFeasibility(List<Course> courses, Map<Course, List<Module>> moduleChoice, List<AbstractUnit> abstractUnitChoice) {
        List<String> names = courses.stream().map(Course::getName).collect(Collectors.toList());
        Map<String, List<Integer>> mc = moduleChoice.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().getName(),
                e -> e.getValue().stream().map(Module::getId).collect(Collectors.toList())));
        List<Integer> auc = abstractUnitChoice.stream().map(AbstractUnit::getId).collect(Collectors.toList());

        String msg = Joiner.on(", ").join(names);
        return new SolverTask<>("Computing Feasibility",
                msg, solver,
                () -> solver.computePartialFeasibility(names, mc, auc));
    }

    @SuppressWarnings("unused")
    public Task<List<Integer>> unsatCore(Course... courses) {
        String[] names = getNames(courses);
        String msg = Joiner.on(", ").join(names);
        return new SolverTask<>("Computing UNSAT Core", msg, solver, () -> solver.unsatCore(names));
    }

    @SuppressWarnings("unused")
    private String[] getNames(Course[] courses) {
        String[] names = new String[courses.length];
        for (int i = 0; i < courses.length; i++) {
            names[i] = courses[i].getName();
        }
        return names;
//        return Arrays.asList(courses).stream()
//                    .map(c -> c.getName())
//                    .collect(Collectors.toList())
//                    .toArray(new String[courses.length]);
    }

    public void submit(Task<?> command) {
        this.executor.submit(command);
    }
}
