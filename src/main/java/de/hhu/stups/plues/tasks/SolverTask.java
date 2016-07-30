package de.hhu.stups.plues.tasks;

import de.hhu.stups.plues.prob.Solver;
import javafx.concurrent.Task;

import java.util.concurrent.*;

public class SolverTask<T> extends Task<T> {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Callable<T> function;
    private final Solver solver;
    Future<T> r;

    public SolverTask(String title, String message, Solver solver, Callable<T> func) {
        this.function = func;
        this.solver = solver;
        updateTitle(title);
        updateMessage(message);
    }

    @Override
    protected T call() throws Exception {
        updateTitle("Starting Task");
        updateProgress(10, 100);
        r = executor.submit(function);
        int p = 10;
        while (!r.isDone()) {
            p = (p + 5) % 95;
            updateProgress(p, 100);
            if (this.isCancelled()) {
                updateMessage("Task canceled");
                return null;
            }
            if (r.isCancelled()) {
                updateMessage("ProB exited");
                return null;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        updateProgress(100, 100);
        return r.get();
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        r.cancel(true);
        System.out.println("Task cancelled.");
        solver.interrupt();
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        updateMessage("Done!");
        T i = this.getValue();
        System.out.println("Result: " + i.toString());
    }
}
