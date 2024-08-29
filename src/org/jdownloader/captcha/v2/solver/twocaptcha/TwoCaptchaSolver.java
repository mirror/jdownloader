package org.jdownloader.captcha.v2.solver.twocaptcha;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.cutcaptcha.CutCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.hcaptcha.AbstractHCaptcha;
import org.jdownloader.captcha.v2.challenge.hcaptcha.HCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_TWO_CAPTCHA;

import jd.http.Browser;
import jd.http.requests.PostRequest;

public class TwoCaptchaSolver extends AbstractTwoCaptchaSolver<String> {
    private static final TwoCaptchaSolver INSTANCE = new TwoCaptchaSolver();

    public static TwoCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    public TwoCaptchaSolverService getService() {
        return (TwoCaptchaSolverService) super.getService();
    }

    private TwoCaptchaSolver() {
        super();
        getService().setSolver(this);
    }

    @Override
    protected boolean isChallengeSupported(final Challenge<?> c) {
        if (c instanceof RecaptchaV2Challenge || c instanceof HCaptchaChallenge || c instanceof BasicCaptchaChallenge) {
            return true;
        } else if (c instanceof CutCaptchaChallenge && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {
        final Challenge<String> captchaChallenge = job.getChallenge();
        if (captchaChallenge instanceof RecaptchaV2Challenge) {
            handleRecaptchaV2(job);
            return;
        } else if (captchaChallenge instanceof HCaptchaChallenge) {
            handleHCaptcha(job);
            return;
        } else if (captchaChallenge instanceof CutCaptchaChallenge) {
            handleCutCaptcha(job);
            return;
        }
        job.showBubble(this);
        checkInterruption();
        RequestOptions options = prepare(job);
        try {
            job.getChallenge().sendStatsSolving(this);
            Browser br = new Browser();
            br.setReadTimeout(5 * 60000);
            // Put your CAPTCHA image file, file object, input stream,
            // or vector of bytes here:
            job.setStatus(SolverStatus.SOLVING);
            final byte[] data = IO.readFile(((ImageCaptchaChallenge) captchaChallenge).getImageFile());
            UrlQuery qi = createQueryForUpload(job, options, data);
            String json = br.postPage("https://2captcha.com/in.php", qi);
            BalanceResponse response = JSonStorage.restoreFromString(json, new TypeRef<BalanceResponse>() {
            });
            if (1 == response.getStatus()) {
                String id = response.getRequest();
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 10)));
                while (job.getJob().isAlive() && !job.getJob().isSolved()) {
                    final UrlQuery queryPoll = createQueryForPolling();
                    queryPoll.appendEncoded("id", id);
                    String ret = br.getPage("https://2captcha.com/res.php?" + queryPoll.toString());
                    logger.info(ret);
                    if ("CAPCHA_NOT_READY".equals(ret)) {
                        Thread.sleep(5000);
                        continue;
                    } else if (ret.startsWith("OK|")) {
                        job.setAnswer(new TwoCaptchaResponse(captchaChallenge, this, id, ret.substring(3)));
                    }
                    return;
                }
            }
        } catch (IOException e) {
            job.getChallenge().sendStatsError(this, e);
            job.getLogger().log(e);
        } finally {
            System.out.println(1);
        }
    }

    /**
     * https://2captcha.com/2captcha-api#error_handling
     *
     * @param job
     * @param challenge
     * @param response
     * @return
     * @throws InterruptedException
     * @throws Exception
     */
    private boolean handleResponse(CESSolverJob<String> job, final String challengeID, final String challengeResponse) throws InterruptedException, Exception {
        if ("CAPCHA_NOT_READY".equals(challengeResponse)) {
            Thread.sleep(5000);
            return false;
        } else if ("ERROR_CAPTCHA_UNSOLVABLE".equals(challengeResponse)) {
            return true;
        } else if ("IP_BANNED".equals(challengeResponse)) {
            throw new Exception(challengeResponse);
        } else if (challengeResponse.startsWith("ERROR")) {
            throw new Exception(challengeResponse);
        } else if (challengeResponse.startsWith("OK|")) {
            job.setAnswer(new TwoCaptchaResponse(job.getChallenge(), this, challengeID, challengeResponse.substring(3)));
            return true;
        } else {
            return false;
        }
    }

    /* https://2captcha.com/2captcha-api#solving_hcaptcha */
    private void handleHCaptcha(CESSolverJob<String> job) throws InterruptedException {
        final HCaptchaChallenge challenge = (HCaptchaChallenge) job.getChallenge();
        job.showBubble(this);
        checkInterruption();
        try {
            challenge.sendStatsSolving(this);
            final Browser br = new Browser();
            br.setReadTimeout(5 * 60000);
            job.setStatus(SolverStatus.SOLVING);
            UrlQuery q = new UrlQuery();
            final String apiKey = config.getApiKey();
            q.appendEncoded("key", apiKey);
            q.appendEncoded("method", "hcaptcha");
            q.appendEncoded("json", "1");
            q.appendEncoded("soft_id", getSoftID());
            q.appendEncoded("sitekey", challenge.getSiteKey());
            q.appendEncoded("pageurl", challenge.getSiteUrl());
            final AbstractHCaptcha<?> hCaptcha = challenge.getAbstractCaptchaHelperHCaptcha();
            if (hCaptcha != null && AbstractHCaptcha.TYPE.INVISIBLE.equals(hCaptcha.getType())) {
                q.appendEncoded("invisible", "1");
            }
            final String json = br.getPage("https://2captcha.com/in.php?" + q.toString());
            final BalanceResponse response = JSonStorage.restoreFromString(json, new TypeRef<BalanceResponse>() {
            });
            if (1 == response.getStatus()) {
                final String id = response.getRequest();
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                while (job.getJob().isAlive() && !job.getJob().isSolved()) {
                    q = new UrlQuery();
                    q.appendEncoded("key", apiKey);
                    q.appendEncoded("action", "get");
                    q.appendEncoded("id", id);
                    final String challengeResponse = br.getPage("https://2captcha.com/res.php?" + q.toString());
                    logger.info(challengeResponse);
                    if (handleResponse(job, id, challengeResponse)) {
                        return;
                    }
                }
            } else {
                job.getLogger().warning(json);
            }
        } catch (Exception e) {
            job.getChallenge().sendStatsError(this, e);
            job.getLogger().log(e);
        }
    }

    /**
     * https://2captcha.com/2captcha-api#solving_recaptchav2_new </br>
     * https://2captcha.com/2captcha-api#solving_recaptchav3
     */
    private void handleRecaptchaV2(CESSolverJob<String> job) throws InterruptedException {
        final RecaptchaV2Challenge challenge = (RecaptchaV2Challenge) job.getChallenge();
        job.showBubble(this);
        checkInterruption();
        try {
            challenge.sendStatsSolving(this);
            final Browser br = new Browser();
            br.setReadTimeout(5 * 60000);
            job.setStatus(SolverStatus.SOLVING);
            UrlQuery q = new UrlQuery();
            q.appendEncoded("key", config.getApiKey());
            q.appendEncoded("method", "userrecaptcha");
            q.appendEncoded("json", "1");
            q.appendEncoded("soft_id", getSoftID());
            q.appendEncoded("googlekey", challenge.getSiteKey());
            q.appendEncoded("pageurl", challenge.getSiteUrl());
            final AbstractRecaptchaV2<?> recaptchaChallenge = challenge.getAbstractCaptchaHelperRecaptchaV2();
            if (recaptchaChallenge != null) {
                if (challenge.isEnterprise()) {
                    q.appendEncoded("enterprise", "1");
                }
                final Map<String, Object> action = challenge.getV3Action();
                if (action != null && action.containsKey("action")) {
                    q.appendEncoded("version", "v3");
                    q.appendEncoded("action", String.valueOf(action.get("action")));
                } else if (TYPE.INVISIBLE.equals(recaptchaChallenge.getType())) {
                    q.appendEncoded("invisible", "1");
                }
            }
            final String json = br.getPage("https://2captcha.com/in.php?" + q.toString());
            final BalanceResponse response = JSonStorage.restoreFromString(json, new TypeRef<BalanceResponse>() {
            });
            if (1 == response.getStatus()) {
                final String id = response.getRequest();
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                while (job.getJob().isAlive() && !job.getJob().isSolved()) {
                    q = new UrlQuery();
                    q.appendEncoded("key", config.getApiKey());
                    q.appendEncoded("action", "get");
                    q.appendEncoded("id", id);
                    final String challengeResponse = br.getPage("https://2captcha.com/res.php?" + q.toString());
                    logger.info(challengeResponse);
                    if (handleResponse(job, id, challengeResponse)) {
                        return;
                    }
                }
            } else {
                job.getLogger().warning(json);
            }
        } catch (Exception e) {
            job.getChallenge().sendStatsError(this, e);
            job.getLogger().log(e);
        }
    }

    /* https://2captcha.com/2captcha-api#cutcaptcha-method */
    private void handleCutCaptcha(final CESSolverJob<String> job) throws InterruptedException {
        final CutCaptchaChallenge challenge = (CutCaptchaChallenge) job.getChallenge();
        job.showBubble(this);
        checkInterruption();
        try {
            challenge.sendStatsSolving(this);
            final Browser br = new Browser();
            br.setReadTimeout(5 * 60000);
            job.setStatus(SolverStatus.SOLVING);
            UrlQuery q = new UrlQuery();
            q.appendEncoded("key", config.getApiKey());
            q.appendEncoded("method", "cutcaptcha");
            q.appendEncoded("json", "1");
            q.appendEncoded("soft_id", getSoftID());
            q.appendEncoded("misery_key", challenge.getSiteKey());
            q.appendEncoded("api_key", "TODO");
            q.appendEncoded("pageurl", challenge.getSiteUrl());
            final String json = br.getPage("https://2captcha.com/in.php?" + q.toString());
            final BalanceResponse response = JSonStorage.restoreFromString(json, new TypeRef<BalanceResponse>() {
            });
            if (1 == response.getStatus()) {
                final String id = response.getRequest();
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                while (job.getJob().isAlive() && !job.getJob().isSolved()) {
                    q = this.createQueryForPolling();
                    q.appendEncoded("id", id);
                    final String challengeResponse = br.getPage("https://2captcha.com/res.php?" + q.toString());
                    logger.info(challengeResponse);
                    if (handleResponse(job, id, challengeResponse)) {
                        return;
                    }
                }
            } else {
                job.getLogger().warning(json);
            }
        } catch (Exception e) {
            job.getChallenge().sendStatsError(this, e);
            job.getLogger().log(e);
        }
    }

    protected boolean validateLogins() {
        if (!CFG_TWO_CAPTCHA.ENABLED.isEnabled()) {
            return false;
        } else if (StringUtils.isEmpty(CFG_TWO_CAPTCHA.API_KEY.getValue())) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean setInvalid(final AbstractResponse<?> response) {
        // TODO: Add functionality
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            return false;
        }
        /* https://2captcha.com/api-docs/report-incorrect */
        final Browser br = new Browser();
        try {
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("clientKey", this.config.getApiKey());
            postdata.put("taskId", "TODO");
            final PostRequest req = br.createJSonPostRequest(this.getApiBaseV2() + "/reportIncorrect", postdata);
            br.getPage(req);
            return true;
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean setValid(AbstractResponse<?> response) {
        // TODO: Add functionality
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            return false;
        }
        /* https://2captcha.com/api-docs/report-correct */
        final Browser br = new Browser();
        try {
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("clientKey", this.config.getApiKey());
            postdata.put("taskId", "TODO");
            final PostRequest req = br.createJSonPostRequest(this.getApiBaseV2() + "/reportCorrect", postdata);
            br.getPage(req);
            return true;
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class BalanceResponse implements Storable {
        public BalanceResponse() {
        }

        private int status;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
        }

        private String request;
    }

    public TwoCaptchaAccount loadAccount() {
        TwoCaptchaAccount ret = new TwoCaptchaAccount();
        try {
            final Browser br = new Browser();
            final UrlQuery q = new UrlQuery();
            q.appendEncoded("key", config.getApiKey());
            q.appendEncoded("action", "getbalance");
            q.appendEncoded("json", "1");
            final String json = br.getPage("https://2captcha.com/res.php?" + q.toString());
            final String validcheck = br.getRegex("^([0-9.,]+$)").getMatch(0);
            if (validcheck != null) {
                // capmonster.cloud
                ret.setBalance(Double.parseDouble(validcheck.replace(",", ".")));
            } else {
                final BalanceResponse response = JSonStorage.restoreFromString(json, new TypeRef<BalanceResponse>() {
                });
                if (1 != response.getStatus()) {
                    ret.setError("Bad Login: " + json);
                }
                ret.setBalance(Double.parseDouble(response.getRequest()));
            }
            ret.setUserName(config.getApiKey());
        } catch (Exception e) {
            logger.log(e);
            ret.setError(e.getMessage());
        }
        return ret;
    }
}
