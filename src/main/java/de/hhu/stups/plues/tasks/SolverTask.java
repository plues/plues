package de.hhu.stups.plues.tasks;

import de.hhu.stups.plues.prob.Solver;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SolverTask<T> extends Task<T> {
    private static final ExecutorService EXECUTOR
            = Executors.newSingleThreadExecutor();

    private final Callable<T> function;
    private final Solver solver;
    private Future<T> r;

    SolverTask(final String title, final String message, final Solver s,
               final Callable<T> func) {
        this(title, s, func);

        updateMessage(message);
    }

    SolverTask(final String title, final Solver s, final Callable<T> func) {
        this.function = func;
        this.solver = s;

        updateTitle(title);
    }

    @Override
    protected T call() throws Exception {
        updateTitle("Starting Task");
        updateProgress(10, 100);
        r = EXECUTOR.submit(function);
        int p = 10;
        while(!r.isDone()) {
            p = (p + 5) % 95;
            updateProgress(p, 100);
            if(this.isCancelled()) {
                updateMessage("Task canceled");
                return null;
            }
            if(r.isCancelled()) {
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
        final T i = this.getValue();
        System.out.println("Result: " + i.toString());
    }
}
