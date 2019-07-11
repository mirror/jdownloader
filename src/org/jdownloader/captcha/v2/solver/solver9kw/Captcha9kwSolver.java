package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;
import java.util.Map;

import jd.http.Browser;

import org.appwork.utils.IO;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;

public class Captcha9kwSolver extends AbstractCaptcha9kwSolver<String> {
    private static final Captcha9kwSolver INSTANCE = new Captcha9kwSolver();

    public static Captcha9kwSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private Captcha9kwSolver() {
        super();
        NineKwSolverService.getInstance().setTextSolver(this);
    }

    @Override
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof RecaptchaV2Challenge || c instanceof BasicCaptchaChallenge;
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (c instanceof RecaptchaV2Challenge) {
            try {
                checkForEnoughCredits();
            } catch (SolverException e) {
                return false;
            }
        } else if (c instanceof BasicCaptchaChallenge) {
            try {
                checkForEnoughCredits();
            } catch (SolverException e) {
                return false;
            }
        }
        return super.canHandle(c);
    }

    @Override
    protected void solveCES(CESSolverJob<String> solverJob) throws InterruptedException, SolverException {
        Challenge<String> captchaChallenge = getChallenge(solverJob);
        if (captchaChallenge instanceof RecaptchaV2Challenge) {
            handleRecaptchaV2(solverJob);
            return;
        }
        checkInterruption();
        RequestOptions options = prepare(solverJob);
        if (options.getMoreoptions().containsKey("userconfirm")) {
            options.getMoreoptions().remove("userconfirm");
        } else {
            options.setConfirm(config.isconfirm());
        }
        try {
            final byte[] data = IO.readFile(((ImageCaptchaChallenge) captchaChallenge).getImageFile());
            UrlQuery qi = createQueryForUpload(solverJob, options, data);
            UrlQuery queryPoll = createQueryForPolling();
            Browser br = new Browser();
            br.setAllowedResponseCodes(new int[] { 500 });
            String captchaID = upload(br, solverJob, qi);
            poll(br, options, solverJob, captchaID, queryPoll);
        } catch (IOException e) {
            solverJob.getChallenge().sendStatsError(this, e);
            setdebug(solverJob, "Interrupted: " + e);
            counterInterrupted.incrementAndGet();
            solverJob.getLogger().log(e);
        } finally {
            System.out.println(1);
        }
    }

    private void handleRecaptchaV2(CESSolverJob<String> solverJob) throws InterruptedException, SolverException {
        checkInterruption();
        RequestOptions options = prepare(solverJob);
        if (options.getMoreoptions().containsKey("userconfirm")) {
            options.getMoreoptions().remove("userconfirm");
        } else {
            options.setConfirm(config.isconfirm());
        }
        try {
            RecaptchaV2Challenge rcChallenge = (RecaptchaV2Challenge) getChallenge(solverJob);
            UrlQuery qi = new UrlQuery();
            qi.appendEncoded("action", "usercaptchaupload");
            qi.appendEncoded("jd", "2");
            qi.appendEncoded("source", "jd2");
            qi.appendEncoded("captchaperhour", options.getCph() + "");
            qi.appendEncoded("captchapermin", options.getCpm() + "");
            qi.appendEncoded("prio", options.getPriothing() + "");
            qi.appendEncoded("selfsolve", options.isSelfsolve() + "");
            qi.appendEncoded("proxy", options.getproxyhostport() + "");
            qi.appendEncoded("proxytype", options.getproxytype() + "");
            qi.appendEncoded("confirm", "false");
            qi.appendEncoded("maxtimeout", options.getTimeoutthing() + "");
            qi.addAll(options.getMoreoptions().list());
            qi.appendEncoded("apikey", config.getApiKey() + "");
            qi.appendEncoded("captchaSource", "jdPlugin");
            qi.appendEncoded("version", "1.2");
            qi.appendEncoded("data-sitekey", rcChallenge.getSiteKey());
            qi.appendEncoded("oldsource", rcChallenge.getTypeID() + "");
            final Map<String, Object> v3action = rcChallenge.getV3Action();
            if (v3action != null) {
                qi.appendEncoded("pageurl", rcChallenge.getSiteUrl());
                qi.appendEncoded("captchachoice", "recaptchav3");
                qi.appendEncoded("actionname", (String) v3action.get("action"));
                qi.appendEncoded("min_score", "0.3");// minimal score
            } else {
                if (options.isSiteDomain()) {
                    qi.appendEncoded("pageurl", rcChallenge.getSiteDomain());
                } else {
                    qi.appendEncoded("pageurl", rcChallenge.getSiteUrl());
                }
                qi.appendEncoded("captchachoice", "recaptchav2");
            }
            qi.appendEncoded("securetoken", rcChallenge.getSecureToken());
            qi.appendEncoded("interactive", 1 + "");
            UrlQuery queryPoll = createQueryForPolling();
            Browser br = new Browser();
            br.setAllowedResponseCodes(new int[] { 500 });
            String captchaID = upload(br, solverJob, qi);
            poll(br, options, solverJob, captchaID, queryPoll);
        } catch (IOException e) {
            solverJob.getChallenge().sendStatsError(this, e);
            setdebug(solverJob, "Interrupted: " + e);
            counterInterrupted.incrementAndGet();
            solverJob.getLogger().log(e);
        } finally {
            System.out.println(1);
        }
    }

    @Override
    protected void parseResponse(CESSolverJob<String> solverJob, Challenge<String> captchaChallenge, String captchaID, String ret) {
        final AbstractResponse<String> answer = captchaChallenge.parseAPIAnswer(ret, captchaChallenge instanceof RecaptchaV2Challenge ? "rawtoken" : null, this);
        solverJob.setAnswer(new Captcha9kwResponse(captchaChallenge, this, answer.getValue(), answer.getPriority(), captchaID));
    }
}
