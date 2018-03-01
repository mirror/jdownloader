package org.jdownloader.captcha.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;

import org.appwork.timetracker.TimeTracker;
import org.appwork.timetracker.TimeTrackerController;
import org.appwork.timetracker.TrackerRule;
import org.appwork.utils.Application;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.api.captcha.CaptchaAPISolver;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.event.ChallengeResponseEvent;
import org.jdownloader.captcha.event.ChallengeResponseEventSender;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaDialogSolver;
import org.jdownloader.captcha.v2.challenge.keycaptcha.jac.KeyCaptchaJACSolver;
import org.jdownloader.captcha.v2.challenge.oauth.AccountOAuthSolver;
import org.jdownloader.captcha.v2.challenge.oauth.OAuthDialogSolver;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.solver.antiCaptchaCom.AntiCaptchaComSolver;
import org.jdownloader.captcha.v2.solver.browser.BrowserSolver;
import org.jdownloader.captcha.v2.solver.captchasolutions.CaptchaSolutionsSolver;
import org.jdownloader.captcha.v2.solver.cheapcaptcha.CheapCaptchaSolver;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolver;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogClickCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogMultiClickCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.RecaptchaChooseFrom3x3Solver;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.myjd.CaptchaMyJDSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverPuzzle;
import org.jdownloader.captcha.v2.solver.twocaptcha.TwoCaptchaSolver;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

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

    private ChallengeResponseEventSender eventSender;

    public ChallengeResponseEventSender getEventSender() {
        return eventSender;
    }

    private LogSource             logger;
    private TimeTrackerController trackerCache;

    /**
     * Create a new instance of ChallengeResponseController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ChallengeResponseController() {
        logger = LogController.getInstance().getLogger(getClass().getName());
        eventSender = new ChallengeResponseEventSender(logger);
        trackerCache = new TimeTrackerController();
        HashMap<String, ArrayList<CaptchaQualityEnsuranceRule>> rules = CFG_CAPTCHA.CFG.getQualityEnsuranceRules();
        if (rules == null) {
            rules = new HashMap<String, ArrayList<CaptchaQualityEnsuranceRule>>();
        }
        boolean save = false;
        save = addDefaultRules(rules, "recaptcha", new CaptchaQualityEnsuranceRule(20, 10 * 60 * 1000), new CaptchaQualityEnsuranceRule(4, 60 * 1000), new CaptchaQualityEnsuranceRule(3, 30 * 1000), new CaptchaQualityEnsuranceRule(2, 10 * 1000)) || save;
        save = addDefaultRules(rules, RecaptchaV2Challenge.RECAPTCHAV2, new CaptchaQualityEnsuranceRule(60, 10 * 60 * 1000), new CaptchaQualityEnsuranceRule(6, 60 * 1000), new CaptchaQualityEnsuranceRule(3, 30 * 1000), new CaptchaQualityEnsuranceRule(2, 10 * 1000)) || save;
        if (save) {
            CFG_CAPTCHA.CFG.setQualityEnsuranceRules(rules);
        }
        for (Entry<String, ArrayList<CaptchaQualityEnsuranceRule>> es : rules.entrySet()) {
            ArrayList<CaptchaQualityEnsuranceRule> rc = es.getValue();
            final TimeTracker tracker = trackerCache.getTracker(es.getKey());
            for (CaptchaQualityEnsuranceRule r : rc) {
                logger.info("Add Captcha Limit Rule for " + es.getKey() + " " + r.getLimit() + " Reqs in " + TimeFormatter.formatMilliSeconds(r.getInterval(), 0));
                tracker.addRule(new TrackerRule(r.getLimit(), r.getInterval()));
            }
        }
    }

    private boolean addDefaultRules(HashMap<String, ArrayList<CaptchaQualityEnsuranceRule>> rules, String key, CaptchaQualityEnsuranceRule... defList) {
        ArrayList<CaptchaQualityEnsuranceRule> rc = rules.get(key);
        if (rc == null || rc.size() == 0) {
            rc = new ArrayList<CaptchaQualityEnsuranceRule>();
            for (CaptchaQualityEnsuranceRule r : defList) {
                rc.add(r);
            }
            rules.put(key, rc);
            return true;
        }
        return false;
    }

    private final AtomicBoolean init = new AtomicBoolean(false);

    public void init() {
        if (init.compareAndSet(false, true)) {
            addSolver(JACSolver.getInstance());
            if (CFG_GENERAL.CFG.isMyJDownloaderCaptchaSolverEnabled()) {
                addSolver(CaptchaMyJDSolver.getInstance());
            }
            addSolver(DeathByCaptchaSolver.getInstance());
            addSolver(ImageTyperzCaptchaSolver.getInstance());
            addSolver(CheapCaptchaSolver.getInstance());
            addSolver(CaptchaSolutionsSolver.getInstance());
            addSolver(TwoCaptchaSolver.getInstance());
            addSolver(AntiCaptchaComSolver.getInstance());
            addSolver(EndCaptchaSolver.getInstance());
            addSolver(Captcha9kwSolver.getInstance());
            addSolver(Captcha9kwSolverClick.getInstance());
            addSolver(Captcha9kwSolverPuzzle.getInstance());
            if (!Application.isHeadless()) {
                addSolver(DialogBasicCaptchaSolver.getInstance());
            }
            if (!Application.isHeadless()) {
                addSolver(DialogClickCaptchaSolver.getInstance());
                addSolver(DialogMultiClickCaptchaSolver.getInstance());
            }
            if (!Application.isHeadless()) {
                addSolver(BrowserSolver.getInstance());
                addSolver(OAuthDialogSolver.getInstance());
            }
            if (!Application.isHeadless()) {
                addSolver(RecaptchaChooseFrom3x3Solver.getInstance());
            }
            addSolver(AccountOAuthSolver.getInstance());
            addSolver(KeyCaptchaJACSolver.getInstance());
            if (!Application.isHeadless()) {
                addSolver(KeyCaptchaDialogSolver.getInstance());
            }
            addSolver(CaptchaAPISolver.getInstance());
        }
    }

    public List<ChallengeSolver<?>> listSolvers() {
        return new ArrayList<ChallengeSolver<?>>(solverList);
    }

    private final HashMap<String, SolverService> solverMap   = new HashMap<String, SolverService>();
    private final List<SolverService>            serviceList = new CopyOnWriteArrayList<SolverService>();

    private synchronized boolean addSolver(ChallengeSolver<?> solver) {
        if (solverMap.put(solver.getService().getID(), solver.getService()) == null) {
            serviceList.add(solver.getService());
        }
        return solverList.add(solver);
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

    private final List<ChallengeSolver<?>>               solverList          = new CopyOnWriteArrayList<ChallengeSolver<?>>();
    private final List<SolverJob<?>>                     activeJobs          = new ArrayList<SolverJob<?>>();
    private final HashMap<UniqueAlltimeID, SolverJob<?>> challengeIDToJobMap = new HashMap<UniqueAlltimeID, SolverJob<?>>();

    /**
     * When one job gets a skiprequest, we have to check all pending jobs if this skiprequest affects them as well. if so, we have to skip
     * them as well.
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

    public void keepAlivePendingChallenges() {
        for (final SolverJob<?> job : activeJobs) {
            job.getChallenge().keepAlive();
        }
    }

    public <T> SolverJob<T> handle(final Challenge<T> c) throws InterruptedException, SkipException {
        LogSource logger = LogController.getInstance().getPreviousThreadLogSource();
        if (logger == null) {
            logger = this.logger;
        }
        logger.info("Log to " + logger.getName());
        logger.info("Handle Challenge: " + c);
        final ArrayList<ChallengeSolver<T>> solver = createList(c);
        logger.info("Solver: " + solver);
        if (solver.size() == 0) {
            logger.info("No solver available!");
            throw new SkipException(c, SkipRequest.BLOCK_HOSTER);
        }
        final SolverJob<T> job = new SolverJob<T>(this, c, solver);
        job.setLogger(logger);
        c.initController(job);
        final UniqueAlltimeID challengeID = c.getId();
        synchronized (activeJobs) {
            activeJobs.add(job);
            challengeIDToJobMap.put(challengeID, job);
        }
        try {
            for (final ChallengeSolver<T> cs : solver) {
                logger.info("Send to solver: " + cs + " " + job);
                cs.enqueue(job);
            }
            logger.info("Fire New Job Event");
            fireNewJobEvent(job);
            logger.info("Wait");
            boolean timeout = false;
            while (!job.isSolved() && !job.isDone()) {
                final BlacklistEntry<?> blackListEntry = CaptchaBlackList.getInstance().matches(c);
                synchronized (job) {
                    final Challenge<T> challenge = job.getChallenge();
                    challenge.poll(job);
                    if (!job.isSolved() && !job.isDone()) {
                        final long validUntil = challenge.getValidUntil();
                        if (validUntil != -1 && System.currentTimeMillis() > validUntil) {
                            timeout = true;
                            break;
                        }
                        job.wait(1000);
                    } else {
                        break;
                    }
                    if (blackListEntry != null && job.setSkipRequest(SkipRequest.SINGLE)) {
                        break;
                    }
                }
            }
            if (timeout && job.setSkipRequest(SkipRequest.TIMEOUT)) {
                final Challenge<T> challenge = job.getChallenge();
                final long expired = System.currentTimeMillis() - challenge.getCreated();
                final int jobTimeout = challenge.getTimeout();
                logger.info("Challenge Timeout detected|Job:" + job + "|Expired:" + expired + "|Timeout:" + jobTimeout);
            }
            if (job.getSkipRequest() != null) {
                throw new SkipException(c, job.getSkipRequest());
            }
            final ResponseList<T> response = job.getResponseAndKill();
            logger.info("All Responses: " + job.getResponses());
            logger.info("Solving Done. Result: " + response);
            return job;
        } catch (InterruptedException e) { // for example downloads have been stopped
            job.kill();
            throw e;
        } finally {
            try {
                synchronized (activeJobs) {
                    activeJobs.remove(job);
                    challengeIDToJobMap.remove(challengeID);
                }
            } finally {
                fireJobDone(job);
            }
            c.onHandled();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ArrayList<ChallengeSolver<T>> createList(Challenge<T> c) {
        final ArrayList<ChallengeSolver<T>> ret = new ArrayList<ChallengeSolver<T>>();
        for (final ChallengeSolver<?> s : solverList) {
            try {
                if (s.isEnabled() && s.validateBlackWhite(c) && s.canHandle(c)) {
                    ret.add((ChallengeSolver<T>) s);
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        return ret;
    }

    public SolverJob<?> getJobByChallengeId(long id) {
        synchronized (challengeIDToJobMap) {
            return challengeIDToJobMap.get(new UniqueAlltimeID(id));
        }
    }

    public List<SolverService> listServices() {
        return new ArrayList<SolverService>(serviceList);
    }

    public SolverService getServiceByID(String key) {
        for (final SolverService service : serviceList) {
            if (service.getID().equals(key)) {
                return service;
            }
        }
        return null;
    }

    public void resetTiming() {
        final HashSet<Object> dupe = new HashSet<Object>();
        for (final ChallengeSolver<?> s : solverList) {
            if (dupe.add(s.getService())) {
                s.getService().getConfig().setWaitForMap(null);
            }
        }
    }

    public TimeTracker getTracker(String method) {
        return trackerCache.getTracker(method);
    }
}
