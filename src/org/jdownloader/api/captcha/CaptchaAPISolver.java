package org.jdownloader.api.captcha;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.plugins.DownloadLink;

import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderRequestInterface;
import org.jdownloader.captcha.event.ChallengeResponseListener;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.JobRunnable;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaCategoryChallenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogClickCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solver.service.DialogSolverService;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CaptchaAPISolver extends ChallengeSolver<Object> implements CaptchaAPI, ChallengeResponseListener {

    private static final CaptchaAPISolver INSTANCE = new CaptchaAPISolver();

    public static CaptchaAPISolver getInstance() {
        return INSTANCE;
    }

    protected int getDefaultWaitForOthersTimeout() {
        return 120000;
    }

    private CaptchaAPIEventPublisher                 eventPublisher;
    private CaptchaMyJDownloaderRemoteSolverSettings config;
    private CaptchaAPISolverEventSender              eventSender;

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (c instanceof KeyCaptchaPuzzleChallenge && super.canHandle(c)) {
            return true;
        }
        if (c instanceof KeyCaptchaCategoryChallenge && super.canHandle(c)) {
            return true;
        }
        return c instanceof ImageCaptchaChallenge && super.canHandle(c);
    }

    @Override
    public void solve(final SolverJob<Object> job) throws InterruptedException, SolverException, SkipException {
        // JobRunnable<Object> jr;
        // jr = new JobRunnable<Object>(this, job);
        //
        // synchronized (map) {
        // map.put(job, jr);
        //
        //
        // }
        job.getLogger().info("Fire MyJDownloader Captcha Event");
        eventSender.fireEvent(new CaptchaAPISolverEvent(this) {

            @Override
            public void sendTo(CaptchaAPISolverListener listener) {
                listener.onAPIJobStarted(job);
            }
        });
        MyJDownloaderController.getInstance().pushCaptchaFlag(true);
        eventPublisher.fireNewJobEvent(job);
        if (Application.isHeadless() || !DialogSolverService.getInstance().isEnabled()) {
            // in headless mode, we should wait, because we have no gui dialog
            job.getLogger().info("Wait for Answer");
            try {
                while (!isJobDone(job)) {
                    Thread.sleep(250);
                }
            } finally {
                job.getLogger().info("Wait Done");
            }
        }
    }

    public CaptchaAPISolver() {
        // 0: no threadpool
        super(new CaptchaAPIManualRemoteSolverService(), 0);
        eventSender = new CaptchaAPISolverEventSender();
        config = JsonConfig.create(CaptchaMyJDownloaderRemoteSolverSettings.class);
        eventPublisher = new CaptchaAPIEventPublisher();
        ChallengeResponseController.getInstance().getEventSender().addListener(this);
    }

    public CaptchaAPISolverEventSender getEventSender() {
        return eventSender;
    }

    public CaptchaAPIEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public List<CaptchaJob> list() {
        final List<CaptchaJob> ret = new ArrayList<CaptchaJob>();
        if (!isEnabled()) {
            return ret;
        }
        for (final SolverJob<?> entry : listJobs()) {
            if (entry.isDone()) {
                continue;
            }
            if (entry.getChallenge() instanceof ImageCaptchaChallenge) {
                final CaptchaJob job = new CaptchaJob();
                final Challenge<?> challenge = entry.getChallenge();
                Class<?> cls = challenge.getClass();
                while (cls != null && StringUtils.isEmpty(job.getType())) {
                    job.setType(cls.getSimpleName());
                    cls = cls.getSuperclass();
                }
                job.setID(entry.getChallenge().getId().getID());
                job.setHoster(((ImageCaptchaChallenge) entry.getChallenge()).getPlugin().getHost());
                job.setCaptchaCategory(entry.getChallenge().getTypeID());
                job.setTimeout(entry.getChallenge().getTimeout());
                job.setCreated(entry.getChallenge().getCreated());
                ret.add(job);
            }
        }
        return ret;
    }

    public void get(RemoteAPIRequest request, RemoteAPIResponse response, long id) throws InternalApiException, InvalidCaptchaIDException {
        final SolverJob<?> job = ChallengeResponseController.getInstance().getJobById(id);
        if (job == null || job.isDone()) {
            throw new InvalidCaptchaIDException();
        }
        try {
            final Challenge<?> challenge = job.getChallenge();
            final OutputStream out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), true);
            try {
                final HashMap<String, Object> captchaResponseData = new HashMap<String, Object>();
                captchaResponseData.put("data", challenge.getAPIStorable());
                if (request.getHttpRequest() instanceof MyJDownloaderRequestInterface) {
                    captchaResponseData.put("rid", ((MyJDownloaderRequestInterface) request.getHttpRequest()).getRid());
                }
                out.write(JSonStorage.serializeToJson(captchaResponseData).getBytes("UTF-8"));
            } finally {
                try {
                    out.close();
                } catch (final Throwable e) {
                }

            }

        } catch (Exception e) {
            Log.exception(e);
            throw new InternalApiException(e);
        }
    }

    public boolean isJobDone(final SolverJob<?> job) {
        if (isMyJDownloaderActive()) {
            synchronized (map) {
                return !map.containsKey(job);
            }
        }
        return false;
    }

    @Override
    public void enqueue(SolverJob<Object> job) {
        if (!isMyJDownloaderActive()) {
            job.setSolverDone(this);
        } else {
            super.enqueue(job);
        }
    }

    private boolean isMyJDownloaderActive() {
        return MyJDownloaderController.getInstance().isActive();
    }

    @SuppressWarnings("unchecked")
    public boolean solve(long id, String result) throws InvalidCaptchaIDException, InvalidChallengeTypeException {
        // the current webinterface sends an empty result when the user clicks on refresh
        if (StringUtils.isEmpty(result)) { //
            return skip(id, SkipRequest.REFRESH);
        }
        final SolverJob<?> job = ChallengeResponseController.getInstance().getJobById(id);
        if (job == null || job.isDone()) {
            throw new InvalidCaptchaIDException();
        }
        final Challenge<?> challenge = job.getChallenge();
        final AbstractResponse<?> ret = challenge.parseAPIAnswer(result, this);
        if (ret != null) {
            ((SolverJob<Object>) job).addAnswer((AbstractResponse<Object>) ret);
        } else {
            throw new InvalidChallengeTypeException(challenge.getClass().getName());
        }
        return true;
    }

    @Deprecated
    public boolean skip(long id) throws InvalidCaptchaIDException {
        return skip(id, SkipRequest.SINGLE);
    }

    @SuppressWarnings("static-access")
    public boolean skip(long id, SkipRequest type) throws InvalidCaptchaIDException {
        final SolverJob<Object> job = (SolverJob<Object>) ChallengeResponseController.getInstance().getJobById(id);
        if (job == null) {
            throw new InvalidCaptchaIDException();
        }
        ChallengeResponseController.getInstance().setSkipRequest(type, this, job.getChallenge());
        return true;
    }

    public void kill(SolverJob<Object> job) {
        super.kill(job);
        MyJDownloaderController.getInstance().pushCaptchaFlag(true);
    }

    @Override
    public CaptchaJob getCaptchaJob(long id) {
        final SolverJob<?> entry = ChallengeResponseController.getInstance().getJobById(id);
        if (entry == null) {
            return null;
        }
        final CaptchaJob ret = new CaptchaJob();
        final Challenge<?> challenge = entry.getChallenge();
        Class<?> cls = challenge.getClass();
        while (cls != null && StringUtils.isEmpty(ret.getType())) {
            ret.setType(cls.getSimpleName());
            cls = cls.getSuperclass();
        }
        ret.setID(entry.getChallenge().getId().getID());
        ret.setHoster(entry.getChallenge().getHost());
        ret.setCaptchaCategory(entry.getChallenge().getTypeID());
        ret.setExplain(entry.getChallenge().getExplain());
        final DownloadLink link = entry.getChallenge().getDownloadLink();
        if (link != null) {
            ret.setLink(link.getUniqueID().getID());
        }
        return ret;
    }

    @Override
    public void onNewJobAnswer(SolverJob<?> job, AbstractResponse<?> response) {
    }

    @Override
    public void onJobDone(final SolverJob<?> job) {
        eventSender.fireEvent(new CaptchaAPISolverEvent(this) {

            @Override
            public void sendTo(CaptchaAPISolverListener listener) {
                listener.onAPIJobDone(job);
            }
        });
        eventPublisher.fireJobDoneEvent(job);
        synchronized (map) {
            dispose(job);
            if (map.size() > 0) {
                MyJDownloaderController.getInstance().pushCaptchaFlag(true);
            } else {
                MyJDownloaderController.getInstance().pushCaptchaFlag(false);
            }
        }
    }

    protected void dispose(SolverJob<?> job) {
        final JobRunnable<Object> suc;
        synchronized (map) {
            suc = map.remove(job);
        }
        if (suc != null) {
            suc.fireDoneAndAfterSolveEvents();
        }
    }

    @Override
    public void onNewJob(SolverJob<?> job) {
        // we fire our event in #enqueue(..)
    }

    @Override
    public void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job) {
        if (solver == this) {
            return;
        }
        if (Application.isHeadless()) {
            // dispose(job);
        } else {
            if (job.areDone(DialogBasicCaptchaSolver.getInstance(), DialogClickCaptchaSolver.getInstance(), BrowserSolver.getInstance())) {
                // dialogs and jac is done. let's kill this one,
                dispose(job);
            }
        }
    }

    @Override
    public void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job) {
    }

}
