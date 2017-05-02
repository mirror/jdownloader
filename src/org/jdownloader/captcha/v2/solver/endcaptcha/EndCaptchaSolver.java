package org.jdownloader.captcha.v2.solver.endcaptcha;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_END_CAPTCHA;
import org.seamless.util.io.IO;

public class EndCaptchaSolver extends CESChallengeSolver<String> {

    private final EndCaptchaConfigInterface config;
    private static final EndCaptchaSolver   INSTANCE   = new EndCaptchaSolver();
    private final ThreadPoolExecutor        threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());
    private final LogSource                 logger;

    public static EndCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    public EndCaptchaSolverService getService() {
        return (EndCaptchaSolverService) super.getService();
    }

    private EndCaptchaSolver() {
        super(new EndCaptchaSolverService(), Math.max(1, Math.min(25, JsonConfig.create(EndCaptchaConfigInterface.class).getThreadpoolSize())));
        getService().setSolver(this);
        config = JsonConfig.create(EndCaptchaConfigInterface.class);
        logger = LogController.getInstance().getLogger(EndCaptchaSolver.class.getName());
        threadPool.allowCoreThreadTimeOut(true);
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (!validateBlackWhite(c)) {
            return false;
        }
        if (c instanceof RecaptchaV2Challenge || c instanceof AbstractRecaptcha2FallbackChallenge) {
            // endcaptcha does not support them
            return false;
        }
        return c instanceof BasicCaptchaChallenge && super.canHandle(c);
    }

    private void errorHandling(Browser br) throws Exception {
        if (br.containsHTML("ERROR:NOT AUTHENTICATED")) {
            throw new SolverException("Wrong Logins");
        } else if (br.containsHTML("ERROR:NOT ENOUGH BALANCE")) {
            throw new SolverException("No Credits");
        } else if (br.containsHTML("ERROR:NOT A VALID CAPTCHA")) {
            throw new SolverException("Bad Captcha Image");
        } else if (br.containsHTML("ERROR:SERVICE EXTREMELY LOADED")) {
            throw new SolverException("Service Overloaded");
        } else if (br.containsHTML("ERROR:UNSUCCESSFUL UPLOAD")) {
            throw new SolverException("Upload error");
        } else if (br.containsHTML("ERROR:INCORRECT CAPTCHA ID")) {
            throw new SolverException("Bad Captcha ID");
        } else if (br.containsHTML("ERROR:")) {
            throw new SolverException("Unknown Error:" + br.toString());
        }
    }

    protected void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws InterruptedException, SolverException {
        job.showBubble(this);
        checkInterruption();
        job.getChallenge().sendStatsSolving(this);
        URLConnectionAdapter conn = null;
        try {
            final Browser br = new Browser();
            br.setReadTimeout(5 * 60000);
            // Put your CAPTCHA image file, file object, input stream,
            // or vector of bytes here:
            job.setStatus(SolverStatus.SOLVING);
            final PostFormDataRequest r = new PostFormDataRequest("http://api.endcaptcha.com/upload");
            r.addFormData(new FormData("username", (config.getUserName())));
            r.addFormData(new FormData("password", (config.getPassword())));
            final byte[] data;
            if (challenge instanceof AbstractRecaptcha2FallbackChallenge) {
                data = challenge.getAnnotatedImageBytes();
            } else {
                data = IO.readBytes(challenge.getImageFile());
            }
            r.addFormData(new FormData("image", "ByteData.captcha", data));
            conn = br.openRequestConnection(r);
            br.loadConnection(conn);
            String pollID = null;
            if (br.containsHTML("UNSOLVED_YET:/poll/")) {
                // do poll
                pollID = br.getRegex("/poll/(.+)").getMatch(0);
            } else {
                errorHandling(br);
                job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " solved: " + br.toString());
                job.setAnswer(new EndCaptchaResponse(challenge, this, pollID, br.toString()));
                return;
            }
            if (pollID != null) {
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                while (pollID != null) {
                    Thread.sleep(3 * 1000);
                    final PostFormDataRequest poll = new PostFormDataRequest("http://api.endcaptcha.com/poll/" + pollID);
                    r.addFormData(new FormData("username", (config.getUserName())));
                    r.addFormData(new FormData("password", (config.getPassword())));
                    conn = br.openRequestConnection(poll);
                    br.loadConnection(conn);
                    if (br.containsHTML("UNSOLVED_YET:/poll/")) {
                        pollID = br.getRegex("/poll/(.+)").getMatch(0);
                    } else {
                        errorHandling(br);
                        job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " solved: " + br.toString());
                        job.setAnswer(new EndCaptchaResponse(challenge, this, pollID, br.toString()));
                        return;
                    }
                }
            }
            throw new SolverException("Unknown Error:" + br.toString());
        } catch (Exception e) {
            job.getChallenge().sendStatsError(this, e);
            job.getLogger().log(e);
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (final Throwable e) {
            }
        }
    }

    protected boolean validateLogins() {
        if (!CFG_END_CAPTCHA.ENABLED.isEnabled()) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_END_CAPTCHA.USER_NAME.getValue())) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_END_CAPTCHA.PASSWORD.getValue())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean setInvalid(final AbstractResponse<?> response) {
        if (config.isFeedBackSendingEnabled() && response instanceof EndCaptchaResponse) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    URLConnectionAdapter conn = null;
                    try {
                        final String captcha = ((EndCaptchaResponse) response).getCaptchaID();
                        final Challenge<?> challenge = response.getChallenge();
                        if (challenge instanceof BasicCaptchaChallenge) {
                            final Browser br = new Browser();
                            final PostFormDataRequest r = new PostFormDataRequest(" http://api.endcaptcha.com/api/captcha/" + captcha + "/report");
                            r.addFormData(new FormData("username", (config.getUserName())));
                            r.addFormData(new FormData("password", (config.getPassword())));
                            conn = br.openRequestConnection(r);
                            br.loadConnection(conn);
                        }
                        // // Report incorrectly solved CAPTCHA if neccessary.
                        // // Make sure you've checked if the CAPTCHA was in fact
                        // // incorrectly solved, or else you might get banned as
                        // // abuser.
                        // Client client = getClient();
                    } catch (final Throwable e) {
                        logger.log(e);
                    } finally {
                        try {
                            if (conn != null) {
                                conn.disconnect();
                            }
                        } catch (final Throwable e) {
                        }
                    }
                }
            });
            return true;
        }
        return false;
    }

    public EndCaptchaAccount loadAccount() {
        final EndCaptchaAccount ret = new EndCaptchaAccount();
        final Browser br = new Browser();
        URLConnectionAdapter conn = null;
        try {
            final PostFormDataRequest r = new PostFormDataRequest("http://api.endcaptcha.com/balance");
            r.addFormData(new FormData("username", (config.getUserName())));
            r.addFormData(new FormData("password", (config.getPassword())));
            conn = br.openRequestConnection(r);
            br.loadConnection(conn);
            errorHandling(br);
            if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
                throw new SolverException(br.toString());
            }
            ret.setUserName(config.getUserName());
            ret.setBalance(Double.parseDouble(br.toString()) / 100);
        } catch (Exception e) {
            logger.log(e);
            ret.setError(br.toString());
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (final Throwable e) {
            }
        }
        return ret;

    }

}
