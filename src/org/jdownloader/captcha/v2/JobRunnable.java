package org.jdownloader.captcha.v2;

import java.util.concurrent.ScheduledExecutorService;

import jd.controlling.captcha.SkipException;

import org.appwork.scheduler.DelayedRunnable;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class JobRunnable<T> implements Runnable {

    private SolverJob<T>                         job;
    private ChallengeSolver<T>                   solver;
    private boolean                              canceled;

    public final static ScheduledExecutorService TIMINGQUEUE = DelayedRunnable.getNewScheduledExecutorService();

    public JobRunnable(ChallengeSolver<T> challengeSolver, SolverJob<T> job) {
        this.job = job;
        this.solver = challengeSolver;

    }

    public SolverJob<T> getJob() {
        return job;
    }

    public void fireTimeoutEvent() {
        if (!job.isDone(solver)) {
            solver.kill(job);
            job.fireTimeoutEvent(solver);

        }
    }

    @Override
    public void run() {

        try {
            synchronized (this) {
                if (canceled) return;

                getJob().getLogger().info(solver + " RUN!");
                thread = Thread.currentThread();
                thread.setName(solver + "-Thread");
            }
            fireBeforeSolveEvent();

            DelayedRunnable timeout = null;
            // final Thread thread = Thread.currentThread();
            if (solver.getTimeout() > 0) {
                timeout = new DelayedRunnable(TIMINGQUEUE, solver.getTimeout()) {
                    @Override
                    public void delayedrun() {
                        System.out.println("Timeout!");
                        fireTimeoutEvent();
                    }

                };

            }

            try {

                if (timeout != null) timeout.resetAndStart();

                solver.solve(job);

            } catch (SkipException e) {

                ChallengeResponseController.getInstance().setSkipRequest(e.getSkipRequest(), solver, job.getChallenge());

            } catch (Throwable e) {
                getJob().getLogger().log(e);

            } finally {
                solver.setStatus(job, null);
                fireDoneAndAfterSolveEvents();

            }
        } finally {
            thread = null;
        }
    }

    public void fireDoneAndAfterSolveEvents() {
        // order is important. listeners should have a chance to validate which solvers are done
        job.setSolverDone(solver);

        job.fireAfterSolveEvent(solver);
    }

    public void fireBeforeSolveEvent() {
        job.fireBeforeSolveEvent(solver);
    }

    private Thread thread;

    public Thread getThread() {
        return thread;
    }

    public void cancel() {
        synchronized (this) {
            this.canceled = true;
            Thread locThread = thread;
            if (locThread != null && locThread != Thread.currentThread()) {
                getJob().getLogger().warning("Interrupt: " + solver + " : " + locThread);
                locThread.interrupt();
            } else if (locThread != Thread.currentThread()) {
                getJob().getLogger().warning("Could Not Interrupt: " + solver + " : " + locThread);
            }
        }
    }
}
