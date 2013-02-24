package org.jdownloader.captcha.v2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.JsonConfig;

public class ChallengeResponseController {
    private static final ChallengeResponseController INSTANCE = new ChallengeResponseController();

    /**
     * get the only existing instance of ChallengeResponseController. This is a singleton
     * 
     * @return
     */
    public static ChallengeResponseController getInstance() {
        return ChallengeResponseController.INSTANCE;
    }

    private CaptchaSettings config;

    /**
     * Create a new instance of ChallengeResponseController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ChallengeResponseController() {
        config = JsonConfig.create(CaptchaSettings.class);
    }

    public boolean addSolver(ChallengeSolver<?> solver) {
        synchronized (solverList) {
            return solverList.add(solver);
        }

    }

    public List<SolverJob<?>> listJobs() {
        synchronized (activeJobs) {
            return new ArrayList<SolverJob<?>>(activeJobs);

        }
    }

    private HashSet<ChallengeSolver<?>> solverList = new HashSet<ChallengeSolver<?>>();
    private List<SolverJob<?>>          activeJobs = new ArrayList<SolverJob<?>>();

    public void handle(final Challenge<?> c) {

        ArrayList<ChallengeSolver<?>> solver = new ArrayList<ChallengeSolver<?>>(solverList);

        synchronized (solverList) {
            solver = createList(c.getResultType());
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final SolverJob job = new SolverJob(c, solver);
        synchronized (activeJobs) {
            activeJobs.add(job);
        }
        try {
            ArrayList<Thread> threads = new ArrayList<Thread>();
            for (final ChallengeSolver<?> cs : solver) {
                // TODO:use threadpool here
                Thread th = new Thread("ChallengeSolver<?>: " + cs) {
                    @SuppressWarnings("unchecked")
                    public void run() {
                        try {
                            cs.solve(job);
                        } finally {
                            job.setDone(cs);
                        }

                    }
                };
                th.start();
                threads.add(th);

            }
            try {
                while (!job.isSolved() && !job.isDone()) {
                    Thread.sleep(200);
                }
                for (Thread t : threads) {
                    t.interrupt();
                    t.join();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            synchronized (activeJobs) {
                activeJobs.remove(job);
            }
        }
    }

    private ArrayList<ChallengeSolver<?>> createList(Class<?> resultType) {
        ArrayList<ChallengeSolver<?>> ret = new ArrayList<ChallengeSolver<?>>();
        synchronized (solverList) {
            for (ChallengeSolver<?> s : solverList) {
                if (s.getResultType().isAssignableFrom(resultType)) {
                    ret.add(s);
                }

            }
        }

        return ret;
    }
}
