package org.jdownloader.captcha.v2.solver.dbc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
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
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_DBC;

import jd.captcha.gui.BasicWindow;
import jd.http.Browser;
import org.appwork.utils.parser.UrlQuery;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;

public class DeathByCaptchaSolver extends CESChallengeSolver<String> {

    private DeathByCaptchaSettings            config;
    private static final DeathByCaptchaSolver INSTANCE   = new DeathByCaptchaSolver();
    private ThreadPoolExecutor                threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());
    private LogSource                         logger;

    public static DeathByCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    public DeathByCaptchaSolverService getService() {
        return (DeathByCaptchaSolverService) super.getService();
    }

    private DeathByCaptchaSolver() {
        super(new DeathByCaptchaSolverService(), Math.max(1, Math.min(25, JsonConfig.create(DeathByCaptchaSettings.class).getThreadpoolSize())));
        getService().setSolver(this);
        config = JsonConfig.create(DeathByCaptchaSettings.class);
        logger = LogController.getInstance().getLogger(DeathByCaptchaSolver.class.getName());

        threadPool.allowCoreThreadTimeOut(true);

    }

    // protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {
    // Challenge<?> challenge = job.getChallenge();
    // if (challenge instanceof RecaptchaV2Challenge) {
    // challenge = ((RecaptchaV2Challenge) challenge).createBasicCaptchaChallenge();
    // solveRecaptchaCaptchaChallenge(job, (Re) challenge);
    // } else {
    //
    // solveBasicCaptchaChallenge(job, (BasicCaptchaChallenge) challenge);
    // }
    //
    // }

    // private void solveRecaptchaCaptchaChallenge(CESSolverJob<String> job, RecaptchaV2Challenge challenge) {
    // }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (!validateBlackWhite(c)) {
            return false;
        }
        if (c instanceof RecaptchaV2Challenge || c instanceof AbstractRecaptcha2FallbackChallenge) {
            // does not work right now. need to contact DBC
            return false;
        }
        return c instanceof BasicCaptchaChallenge && super.canHandle(c);
    }

    protected void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws InterruptedException, SolverException {

        job.showBubble(this, getBubbleTimeout(challenge));
        checkInterruption();
        // final Client client = getClient();
        try {
            // Captcha captcha = null;
            challenge.sendStatsSolving(this);
            // Put your CAPTCHA image file, file object, input stream,
            // or vector of bytes here:

            job.setStatus(SolverStatus.UPLOADING);

            Browser br = createBrowser();

            PostFormDataRequest r = new PostFormDataRequest("http://api.dbcapi.me/api/captcha");

            r.addFormData(new FormData("username", config.getUserName()));
            r.addFormData(new FormData("password", config.getPassword()));
            r.addFormData(new FormData("swid", "0"));
            r.addFormData(new FormData("challenge", ""));
            if (challenge instanceof AbstractRecaptcha2FallbackChallenge) {
                AbstractRecaptcha2FallbackChallenge fbc = ((AbstractRecaptcha2FallbackChallenge) challenge);

                byte[] banner = null;
                // Icon icon = fbc.getExplainIcon(fbc.getExplain());
                // if (icon != null) {
                // ByteArrayOutputStream baos;
                // ImageIO.write(IconIO.toBufferedImage(icon), "jpg", baos = new ByteArrayOutputStream());
                // banner = baos.toByteArray();
                //
                // }
                // c.isVerbose = true;
                // c.proxy = new Proxy(Type.HTTP, new InetSocketAddress("localhost", 8888));

                // org.jdownloader.captcha.v2.solver.dbc.test.Captcha ca = c.decode(IO.readFile(challenge.getImageFile()), "", 3, banner,
                // fbc.getExplain(), 0);

                r.addFormData(new FormData("banner_text", fbc.getExplain()));

                r.addFormData(new FormData("grid", fbc.getSubChallenge().getGridWidth() + "x" + fbc.getSubChallenge().getGridHeight()));

                r.addFormData(new FormData("type", "3"));
                BasicWindow.showImage(ImageIO.read(challenge.getImageFile()));
                final byte[] bytes = IO.readFile(challenge.getImageFile());
                r.addFormData(new FormData("captchafile", "captcha", "application/octet-stream", bytes));
                // Banner sending does not work right now
                // server says bad request
                // if (icon != null) {
                // ByteArrayOutputStream baos;
                // ImageIO.write(IconIO.toBufferedImage(icon), "jpg", baos = new ByteArrayOutputStream());
                // r.addFormData(new FormData("banner", "banner", "application/octet-stream", baos.toByteArray()));
                //
                // }

            } else {

                final byte[] bytes = IO.readFile(challenge.getImageFile());
                r.addFormData(new FormData("captchafile", "captcha", "application/octet-stream", bytes));

            }

            br.setAllowedResponseCodes(200, 400);

            URLConnectionAdapter conn = br.openRequestConnection(r);
            br.loadConnection(conn);
            DBCUploadResponse uploadStatus = JSonStorage.restoreFromString(br.toString(), DBCUploadResponse.TYPE);
            DBCUploadResponse status = uploadStatus;
            if (status != null && status.getCaptcha() > 0) {
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " uploaded: " + status.getCaptcha());
                long startTime = System.currentTimeMillis();

                while (status != null && !status.isSolved()) {
                    if (System.currentTimeMillis() - startTime > 5 * 60 * 60 * 1000l) {
                        throw new SolverException("Failed:Timeout");
                    }
                    Thread.sleep(1000);
                    job.getLogger().info("deathbycaptcha.eu NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");

                    br.getPage("http://api.dbcapi.me/api/captcha/" + uploadStatus.getCaptcha());
                    status = JSonStorage.restoreFromString(br.toString(), DBCUploadResponse.TYPE);
                }
                if (status != null && status.isSolved()) {
                    job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " solved: " + status.getText());

                    AbstractResponse<String> answer = challenge.parseAPIAnswer(status.getText().replace("[", "").replace("]", ""), this);

                    DeathByCaptchaResponse response = new DeathByCaptchaResponse(challenge, this, status, answer.getValue(), answer.getPriority());

                    job.setAnswer(response);
                } else {
                    job.getLogger().info("Failed solving CAPTCHA");
                    throw new SolverException("Failed:" + JSonStorage.serializeToJson(status));
                }
            }

        } catch (Exception e) {
            job.setStatus(getErrorByException(e), new AbstractIcon(IconKey.ICON_ERROR, 20));
            job.getLogger().log(e);

            challenge.sendStatsError(this, e);
        } finally {
            System.out.println("DBC DONe");
        }

    }

    private int getBubbleTimeout(BasicCaptchaChallenge challenge) {
        HashMap<String, Integer> map = config.getBubbleTimeoutByHostMap();

        Integer ret = map.get(challenge.getHost().toLowerCase(Locale.ENGLISH));
        if (ret == null || ret < 0) {
            ret = CFG_CAPTCHA.CFG.getCaptchaExchangeChanceToSkipBubbleTimeout();
        }
        return ret;
    }

    protected boolean validateLogins() {
        if (!CFG_DBC.ENABLED.isEnabled()) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_DBC.USER_NAME.getValue())) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_DBC.PASSWORD.getValue())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean setUnused(AbstractResponse<?> response) {
        return false;
    }

    @Override
    public boolean setInvalid(final AbstractResponse<?> response) {
        if (config.isFeedBackSendingEnabled() && response instanceof DeathByCaptchaResponse) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {

                    try {
                        DBCUploadResponse captcha = ((DeathByCaptchaResponse) response).getCaptcha();
                        // Report incorrectly solved CAPTCHA if neccessary.
                        // Make sure you've checked if the CAPTCHA was in fact
                        // incorrectly solved, or else you might get banned as
                        // abuser.

                        try {
                            Challenge<?> challenge = response.getChallenge();

                            if (challenge instanceof BasicCaptchaChallenge) {
                                createBrowser().postPage("http://api.dbcapi.me/api/captcha/" + captcha.getCaptcha() + "/report", new UrlQuery().addAndReplace("password", URLEncode.encodeRFC2396(config.getPassword())).addAndReplace("username", URLEncode.encodeRFC2396(config.getUserName())));

                            }
                        } finally {

                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            });
            return true;
        }
        return false;
    }

    public DBCAccount loadAccount() {

        DBCAccount ret = new DBCAccount();

        try {
            DBCGetUserResponse user = getUserData();
            ret.setBalance(user.getBalance());
            ret.setBanned(user.isIs_banned());
            ret.setId(user.getUser());
            ret.setRate(user.getRate());

        } catch (Exception e) {
            logger.log(e);
            ret.setError(getErrorByException(e));

        }
        return ret;

    }

    private String getErrorByException(Exception e) {
        Throwable ee = e;
        String ret = null;
        while (ee != null && StringUtils.isEmpty(ee.getMessage())) {
            ee = ee.getCause();
        }
        if (ee != null) {
            ret = ee.getMessage();
        } else {
            ret = e.getMessage();
        }
        if (StringUtils.isEmpty(ret)) {
            ret = (_GUI.T.DBC_UNKNOWN_ERROR(e.getClass().getSimpleName()));
        }
        return ret;
    }

    private DBCGetUserResponse getUserData() throws UnsupportedEncodingException, IOException {
        String json = createBrowser().postPage("http://api.dbcapi.me/api/user", new UrlQuery().addAndReplace("password", URLEncode.encodeRFC2396(config.getPassword())).addAndReplace("username", URLEncode.encodeRFC2396(config.getUserName())));

        return JSonStorage.restoreFromString(json, DBCGetUserResponse.TYPE);

    }

    private Browser createBrowser() {
        Browser br = new Browser();
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader $Revision$".replace("$Revision$", ""));
        // br.setProxy(new HTTPProxy(TYPE.HTTP, "localhost", 8888));
        return br;
    }

}
