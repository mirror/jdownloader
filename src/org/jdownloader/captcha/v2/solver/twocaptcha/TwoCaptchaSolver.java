package org.jdownloader.captcha.v2.solver.twocaptcha;

import java.io.IOException;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_TWO_CAPTCHA;

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
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof RecaptchaV2Challenge || c instanceof BasicCaptchaChallenge;
    }

    @Override
    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {
        Challenge<String> captchaChallenge = job.getChallenge();
        if (captchaChallenge instanceof RecaptchaV2Challenge) {
            handleRecaptchaV2(job);
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
            long startTime = System.currentTimeMillis();
            final byte[] data = IO.readFile(((ImageCaptchaChallenge) captchaChallenge).getImageFile());
            UrlQuery qi = createQueryForUpload(job, options, data);
            String json = br.postPage("http://2captcha.com/in.php", qi);
            BalanceResponse response = JSonStorage.restoreFromString(json, new TypeRef<BalanceResponse>() {
            });
            if (1 == response.getStatus()) {
                String id = response.getRequest();
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 10)));
                while (true) {
                    UrlQuery queryPoll = createQueryForPolling();
                    queryPoll.appendEncoded("id", id);
                    String ret = br.getPage("http://2captcha.com/res.php?" + queryPoll.toString());
                    logger.info(ret);
                    if ("CAPCHA_NOT_READY".equals(ret)) {
                        Thread.sleep(5000);
                        continue;
                    }
                    if (ret.startsWith("OK|")) {
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

    private void handleRecaptchaV2(CESSolverJob<String> job) throws InterruptedException {
        RecaptchaV2Challenge challenge = (RecaptchaV2Challenge) job.getChallenge();
        job.showBubble(this);
        checkInterruption();
        try {
            job.getChallenge().sendStatsSolving(this);
            Browser br = new Browser();
            br.setReadTimeout(5 * 60000);
            // Put your CAPTCHA image file, file object, input stream,
            // or vector of bytes here:
            job.setStatus(SolverStatus.SOLVING);
            long startTime = System.currentTimeMillis();
            UrlQuery q = new UrlQuery();
            q.appendEncoded("key", config.getApiKey());
            q.appendEncoded("method", "userrecaptcha");
            q.appendEncoded("googlekey", challenge.getSiteKey());
            q.appendEncoded("pageurl", challenge.getSiteDomain());
            q.appendEncoded("json", "1");
            String json = br.getPage("http://2captcha.com/in.php?" + q.toString());
            BalanceResponse response = JSonStorage.restoreFromString(json, new TypeRef<BalanceResponse>() {
            });
            if (1 == response.getStatus()) {
                String id = response.getRequest();
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                while (true) {
                    q = new UrlQuery();
                    q.appendEncoded("key", config.getApiKey());
                    q.appendEncoded("action", "get");
                    q.appendEncoded("id", id);
                    json = br.getPage("http://2captcha.com/res.php?" + q.toString());
                    logger.info(json);
                    if ("CAPCHA_NOT_READY".equals(json)) {
                        Thread.sleep(1000);
                        continue;
                    }
                    if (json.startsWith("OK|")) {
                        job.setAnswer(new TwoCaptchaResponse(challenge, this, id, json.substring(3)));
                    }
                    return;
                }
            }
            System.out.println(json);
        } catch (Exception e) {
            job.getChallenge().sendStatsError(this, e);
            job.getLogger().log(e);
        }
    }

    protected boolean validateLogins() {
        if (!CFG_TWO_CAPTCHA.ENABLED.isEnabled()) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_TWO_CAPTCHA.API_KEY.getValue())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean setInvalid(final AbstractResponse<?> response) {
        // if (config.isFeedBackSendingEnabled() && response instanceof TwoCaptchaResponse) {
        // threadPool.execute(new Runnable() {
        // @Override
        // public void run() {
        // try {
        // String captcha = ((TwoCaptchaResponse) response).getCaptchaID();
        // Challenge<?> challenge = response.getChallenge();
        // if (challenge instanceof BasicCaptchaChallenge) {
        // Browser br = new Browser();
        // PostFormDataRequest r = new PostFormDataRequest(" http://api.cheapcaptcha.com/api/captcha/" + captcha + "/report");
        // r.addFormData(new FormData("username", (config.getUserName())));
        // r.addFormData(new FormData("password", (config.getPassword())));
        // URLConnectionAdapter conn = br.openRequestConnection(r);
        // br.loadConnection(conn);
        // System.out.println(conn);
        // }
        // // // Report incorrectly solved CAPTCHA if neccessary.
        // // // Make sure you've checked if the CAPTCHA was in fact
        // // // incorrectly solved, or else you might get banned as
        // // // abuser.
        // // Client client = getClient();
        // } catch (final Throwable e) {
        // logger.log(e);
        // }
        // }
        // });
        // return true;
        // }
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
            Browser br = new Browser();
            UrlQuery q = new UrlQuery();
            q.appendEncoded("key", config.getApiKey());
            q.appendEncoded("action", "getbalance");
            q.appendEncoded("json", "1");
            String json = br.getPage("http://2captcha.com/res.php?" + q.toString());
            BalanceResponse response = JSonStorage.restoreFromString(json, new TypeRef<BalanceResponse>() {
            });
            if (1 != response.getStatus()) {
                ret.setError("Bad Login: " + json);
            }
            ret.setUserName(config.getApiKey());
            ret.setBalance(Double.parseDouble(response.getRequest()));
        } catch (Exception e) {
            logger.log(e);
            ret.setError(e.getMessage());
        }
        return ret;
    }
}
