package de.hhu.stups.plues.tasks;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.Solver;
import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SolverService {
    private final ExecutorService executor;
    private Solver solver;

    @Inject
    public SolverService(Solver s) {
        this.executor = Executors.newSingleThreadExecutor();
        this.solver = s;
    }

    public Task<Boolean> checkFeasibilityTask(Course... courses) {
        assert this.solver != null;
        String[] names = getNames(courses);
        String msg = Joiner.on(", ").join(names);
        return new SolverTask<>("Checking Feasibility", msg, this.solver,
                () -> this.solver.checkFeasibility(names));
    }

    public Task<FeasibilityResult> computeFeasibilityTask(Course... courses) {
        String[] names = getNames(courses);
        String msg = Joiner.on(", ").join(names);
        return new SolverTask<>("Computing Feasibility",
                msg, solver,
                () -> solver.computeFeasibility(names));
    }

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
