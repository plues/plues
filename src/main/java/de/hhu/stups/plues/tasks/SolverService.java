package de.hhu.stups.plues.tasks;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.Solver;
import javafx.concurrent.Task;

public class SolverService {
    private final Solver solver;

    public SolverService(Solver solver) {
        this.solver = solver;
    }

    public Task<Boolean> checkFeasibilityTask(Course course) {
        return new SolverTask<>("Checking Feasibility", course.getName(), solver,
                                () -> solver.checkFeasibility(course.getName()));
    }
}
