package org.jdownloader.captcha.v2.solver.cheapcaptcha;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.nutils.encoding.Encoding;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.parser.UrlQuery;
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
import org.jdownloader.settings.staticreferences.CFG_CHEAP_CAPTCHA;
import org.seamless.util.io.IO;

public class CheapCaptchaSolver extends CESChallengeSolver<String> {

    private CheapCaptchaConfigInterface     config;
    private static final CheapCaptchaSolver INSTANCE   = new CheapCaptchaSolver();
    private ThreadPoolExecutor              threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());
    private LogSource                       logger;

    public static CheapCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    public CheapCaptchaSolverService getService() {
        return (CheapCaptchaSolverService) super.getService();
    }

    private CheapCaptchaSolver() {
        super(new CheapCaptchaSolverService(), Math.max(1, Math.min(25, JsonConfig.create(CheapCaptchaConfigInterface.class).getThreadpoolSize())));
        getService().setSolver(this);
        config = JsonConfig.create(CheapCaptchaConfigInterface.class);
        logger = LogController.getInstance().getLogger(CheapCaptchaSolver.class.getName());

        threadPool.allowCoreThreadTimeOut(true);

    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (!validateBlackWhite(c)) {
            return false;
        }
        if (c instanceof RecaptchaV2Challenge || c instanceof AbstractRecaptcha2FallbackChallenge) {
            // does not accept this annoted image yet
            return false;
        }
        return c instanceof BasicCaptchaChallenge && super.canHandle(c);
    }

    protected void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws InterruptedException, SolverException {

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
            PostFormDataRequest r = new PostFormDataRequest("http://api.cheapcaptcha.com/api/captcha");

            r.addFormData(new FormData("username", (config.getUserName())));
            r.addFormData(new FormData("password", (config.getPassword())));

            final byte[] data;
            if (challenge instanceof AbstractRecaptcha2FallbackChallenge) {
                data = challenge.getAnnotatedImageBytes();
            } else {
                data = IO.readBytes(challenge.getImageFile());
            }
            if (true) {
                r.addFormData(new FormData("captchafile", "base64:" + Base64.encodeToString(data)));
            } else {
                r.addFormData(new FormData("captchafile", "ByteData.captcha", data));
            }

            final URLConnectionAdapter conn = br.openRequestConnection(r);
            conn.setAllowedResponseCodes(new int[] { conn.getResponseCode() });
            br.loadConnection(conn);
            // 303 See Other if your CAPTCHA was successfully uploaded: Location HTTP header will point you to the uploaded CAPTCHA status
            // page, you may follow the Location to get the uploaded CAPTCHA status or parse the CAPTCHA unique ID out of Location URL — the
            // scheme is http://api.cheapcaptcha.com/api/captcha/%CAPTCHA_ID%;
            // 403 Forbidden if your Cheap CAPTCHA credentials were rejected, or if you don't have enough credits;
            // 400 Bad Request if your request was not following the specification above, or the CAPTCHA was rejected for not being a valid
            // image;
            // 500 Internal Server Error if something happened on our side preventing you from uploading the CAPTCHA; if you are sure you're
            // sending properly prepared requests, and your CAPTCHA images are valid, yet the problem persists, please contact our live
            // support and tell them in details how to reproduce the issue;
            // 503 Service Temporarily Unavailable when our service is overloaded (usually around 3:00–6:00 PM EST), try again later.

            if (conn.getResponseCode() == 403) {
                CheapCaptchaAccount acc = loadAccount();
                if (acc.isValid()) {
                    throw new SolverException("No Credits");
                } else {
                    throw new SolverException("Wrong Logins");
                }

            } else if (conn.getResponseCode() == 503) {
                // overload
                throw new SolverException("Server Overload");
            }

            // Poll for the uploaded CAPTCHA status.

            String checkUrl = br.getRedirectLocation();

            String id = new Regex(checkUrl, ".*/(\\d+)$").getMatch(0);
            if (null != checkUrl) {
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                while (true) {
                    UrlQuery pollResponse = Request.parseQuery(br.getPage(checkUrl));
                    String txt = Encoding.urlDecode(pollResponse.get("text"), false);
                    boolean solved = StringUtils.isNotEmpty(txt);
                    if (!"1".equals(pollResponse.get("is_correct"))) {
                        job.getLogger().info("Failed solving CAPTCHA");
                        throw new SolverException("Failed:" + id);
                    }
                    job.getLogger().info(br.toString());

                    if (solved) {
                        job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " solved: " + pollResponse.get("text"));
                        job.setAnswer(new CheapCaptchaResponse(challenge, this, id, txt));
                        return;
                    } else {
                        Thread.sleep(1 * 1000);

                    }
                }

            }

        } catch (Exception e) {
            job.getChallenge().sendStatsError(this, e);
            job.getLogger().log(e);
        }

    }

    protected boolean validateLogins() {
        if (!CFG_CHEAP_CAPTCHA.ENABLED.isEnabled()) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_CHEAP_CAPTCHA.USER_NAME.getValue())) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_CHEAP_CAPTCHA.PASSWORD.getValue())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean setInvalid(final AbstractResponse<?> response) {
        if (config.isFeedBackSendingEnabled() && response instanceof CheapCaptchaResponse) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String captcha = ((CheapCaptchaResponse) response).getCaptchaID();
                        Challenge<?> challenge = response.getChallenge();
                        if (challenge instanceof BasicCaptchaChallenge) {
                            Browser br = new Browser();
                            PostFormDataRequest r = new PostFormDataRequest(" http://api.cheapcaptcha.com/api/captcha/" + captcha + "/report");

                            r.addFormData(new FormData("username", (config.getUserName())));
                            r.addFormData(new FormData("password", (config.getPassword())));

                            URLConnectionAdapter conn = br.openRequestConnection(r);
                            br.loadConnection(conn);
                            System.out.println(conn);
                        }

                        // // Report incorrectly solved CAPTCHA if neccessary.
                        // // Make sure you've checked if the CAPTCHA was in fact
                        // // incorrectly solved, or else you might get banned as
                        // // abuser.
                        // Client client = getClient();

                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            });
            return true;
        }
        return false;
    }

    public CheapCaptchaAccount loadAccount() {

        CheapCaptchaAccount ret = new CheapCaptchaAccount();
        try {
            Browser br = new Browser();
            PostFormDataRequest r = new PostFormDataRequest("http://api.cheapcaptcha.com/api/user");

            r.addFormData(new FormData("username", (config.getUserName())));
            r.addFormData(new FormData("password", (config.getPassword())));

            URLConnectionAdapter conn = br.openRequestConnection(r);
            br.loadConnection(conn);
            if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
                throw new WTFException(br.toString());
            }
            UrlQuery response = Request.parseQuery(br.toString());
            ret.setUserName(response.get("user") + " (" + config.getUserName() + ")");
            ret.setBalance(Double.parseDouble(response.get("balance")) / 100);

        } catch (Exception e) {
            logger.log(e);
            ret.setError(e.getMessage());
        }
        return ret;

    }

}
