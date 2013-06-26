package org.jdownloader.captcha.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.event.ChallengeResponseEvent;
import org.jdownloader.captcha.event.ChallengeResponseEventSender;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.logging.LogController;

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

    private CaptchaSettings              config;
    private ChallengeResponseEventSender eventSender;

    public ChallengeResponseEventSender getEventSender() {
        return eventSender;
    }

    private LogSource logger;

    /**
     * Create a new instance of ChallengeResponseController. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private ChallengeResponseController() {
        config = JsonConfig.create(CaptchaSettings.class);
        logger = LogController.getInstance().getLogger(getClass().getName());
        eventSender = new ChallengeResponseEventSender(logger);
    }

    public boolean addSolver(ChallengeSolver<?> solver) {
        synchronized (solverList) {
            return solverList.add(solver);
        }

    }

    public <E> void fireNewAnswerEvent(SolverJob<E> job, AbstractResponse<E> abstractResponse) {
        eventSender.fireEvent(new ChallengeResponseEvent(this, ChallengeResponseEvent.Type.JOB_ANSWER, abstractResponse, job));
    }

    public List<SolverJob<?>> listJobs() {
        synchronized (activeJobs) {
            return new ArrayList<SolverJob<?>>(activeJobs);
        }
    }

    public boolean hasPendingJobs() {
        synchronized (activeJobs) {
            return activeJobs.size() > 0;
        }
    }

    public void fireBeforeSolveEvent(SolverJob<?> job, ChallengeSolver<?> solver) {
        eventSender.fireEvent(new ChallengeResponseEvent(this, ChallengeResponseEvent.Type.SOLVER_START, solver, job));
    }

    public void fireAfterSolveEvent(SolverJob<?> job, ChallengeSolver<?> solver) {
        synchronized (job) {
            job.getLogger().info("Solver " + solver + " finished job " + job);
            job.notifyAll();
        }
        eventSender.fireEvent(new ChallengeResponseEvent(this, ChallengeResponseEvent.Type.SOLVER_END, solver, job));
    }

    private void fireNewJobEvent(SolverJob<?> job) {
        eventSender.fireEvent(new ChallengeResponseEvent(this, ChallengeResponseEvent.Type.NEW_JOB, job));
    }

    private void fireJobDone(SolverJob<?> job) {
        eventSender.fireEvent(new ChallengeResponseEvent(this, ChallengeResponseEvent.Type.JOB_DONE, job));

    }

    private HashSet<ChallengeSolver<?>> solverList = new HashSet<ChallengeSolver<?>>();
    private List<SolverJob<?>>          activeJobs = new ArrayList<SolverJob<?>>();
    private HashMap<Long, SolverJob<?>> idToJobMap = new HashMap<Long, SolverJob<?>>();

    /**
     * When one job gets a skiprequest, we have to check all pending jobs if this skiprequest affects them as well. if so, we have to skip them as well.
     * 
     * @param skipRequest
     * @param solver
     * @param challenge
     */
    public <T> void setSkipRequest(SkipRequest skipRequest, ChallengeSolver<T> solver, Challenge<T> sourceChallenge) {
        synchronized (activeJobs) {
            for (SolverJob<?> job : activeJobs) {
                if (job.getChallenge() == sourceChallenge) {
                    job.setSkipRequest(skipRequest);
                } else if (job.getChallenge().canBeSkippedBy(skipRequest, solver, sourceChallenge)) {
                    job.setSkipRequest(skipRequest);
                }
            }

        }
    }

    public <T> void handle(final Challenge<T> c) throws InterruptedException, SkipException {
        ArrayList<ChallengeSolver<T>> solver = null;
        LogSource logger = LogController.getInstance().getPreviousThreadLogSource();
        if (logger == null) logger = this.logger;
        logger.info("Log to " + logger.getName());
        synchronized (solverList) {
            solver = createList(c);
        }
        logger.info("Handle Challenge: " + c);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final SolverJob<T> job = new SolverJob<T>(this, c, solver);
        job.setLogger(logger);
        synchronized (activeJobs) {
            activeJobs.add(job);
            idToJobMap.put(c.getId().getID(), job);
        }
        try {
            for (final ChallengeSolver<T> cs : solver) {
                logger.info("Send to solver: " + cs + " " + job);
                cs.enqueue(job);
            }
            logger.info("Fire New Job Event");
            fireNewJobEvent(job);
            logger.info("Wait");
            while (!job.isSolved() && !job.isDone()) {
                synchronized (job) {
                    if (!job.isSolved() && !job.isDone()) {
                        job.wait();
                        logger.info("Notified");
                    }
                }
            }
            if (job.getSkipRequest() != null) { throw new SkipException(job.getSkipRequest()); }
            logger.info("All Responses: " + job.getResponses());
            logger.info("Solving Done. Result: " + job.getResponse());
        } catch (InterruptedException e) { // for example downloads have been stopped
            job.kill();
            throw e;
        } finally {
            try {
                synchronized (activeJobs) {
                    activeJobs.remove(job);
                    idToJobMap.remove(job.getChallenge().getId().getID());
                }
            } finally {
                fireJobDone(job);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ArrayList<ChallengeSolver<T>> createList(Challenge<T> c) {
        ArrayList<ChallengeSolver<T>> ret = new ArrayList<ChallengeSolver<T>>();
        synchronized (solverList) {
            for (ChallengeSolver<?> s : solverList) {
                try {
                    if (s.canHandle(c)) {
                        ret.add((ChallengeSolver<T>) s);
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        }

        return ret;
    }

    public SolverJob<?> getJobById(long id) {
        return idToJobMap.get(id);
    }

}
