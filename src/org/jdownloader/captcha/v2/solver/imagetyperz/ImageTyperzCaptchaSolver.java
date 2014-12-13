package org.jdownloader.captcha.v2.solver.imagetyperz;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_IMAGE_TYPERZ;

public class ImageTyperzCaptchaSolver extends CESChallengeSolver<String> implements ChallengeResponseValidation {

    private ImageTyperzConfigInterface            config;
    private static final ImageTyperzCaptchaSolver INSTANCE   = new ImageTyperzCaptchaSolver();
    private ThreadPoolExecutor                    threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());
    private LogSource                             logger;

    public static ImageTyperzCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    public ImageTyperzSolverService getService() {
        return (ImageTyperzSolverService) super.getService();
    }

    private ImageTyperzCaptchaSolver() {
        super(new ImageTyperzSolverService(), Math.max(1, Math.min(25, JsonConfig.create(ImageTyperzConfigInterface.class).getThreadpoolSize())));
        getService().setSolver(this);
        config = JsonConfig.create(ImageTyperzConfigInterface.class);
        logger = LogController.getInstance().getLogger(ImageTyperzCaptchaSolver.class.getName());

        threadPool.allowCoreThreadTimeOut(true);

    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return c instanceof BasicCaptchaChallenge && super.canHandle(c);
    }

    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {

        solveBasicCaptchaChallenge(job, (BasicCaptchaChallenge) job.getChallenge());

    }

    private void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws InterruptedException {

        if (config.getWhiteList() != null) {
            if (config.getWhiteList().length() > 5) {
                if (config.getWhiteList().contains(challenge.getTypeID())) {
                    job.getLogger().info("Hoster on WhiteList for ImageTyperz.com. - " + challenge.getTypeID());
                } else {
                    job.getLogger().info("Hoster not on WhiteList for ImageTyperz.com. - " + challenge.getTypeID());
                    return;
                }
            }
        }
        if (config.getBlackList() != null) {
            if (config.getBlackList().length() > 5) {
                if (config.getBlackList().contains(challenge.getTypeID())) {
                    job.getLogger().info("Hoster on BlackList for ImageTyperz.com. - " + challenge.getTypeID());
                    return;
                } else {
                    job.getLogger().info("Hoster not on BlackList for ImageTyperz.com. - " + challenge.getTypeID());
                }
            }
        }
        job.showBubble(this);
        checkInterruption();
        try {

            Browser br = new Browser();
            br.setReadTimeout(5 * 60000);
            // Put your CAPTCHA image file, file object, input stream,
            // or vector of bytes here:
            job.setStatus(SolverStatus.SOLVING);
            long startTime = System.currentTimeMillis();
            PostFormDataRequest r = new PostFormDataRequest("http://captchatypers.com/Forms/UploadFileAndGetTextNew.ashx");

            r.addFormData(new FormData("action", "UPLOADCAPTCHA"));
            r.addFormData(new FormData("username", (config.getUserName())));
            r.addFormData(new FormData("password", (config.getPassword())));
            r.addFormData(new FormData("chkCase", "0"));
            r.addFormData(new FormData("file", org.appwork.utils.encoding.Base64.encodeToString(IO.readFile(challenge.getImageFile()), false)));
            URLConnectionAdapter conn = br.openRequestConnection(r);

            // ERROR: INVALID_REQUEST = It will be returned when the program tries to send the invalid request.
            // ERROR: INVALID_USERNAME = If the username is not provided, this will be returned.
            // ERROR: INVALID_PASSWORD = if the password is not provide, this will be returned.
            // ERROR: INVALID_IMAGE_FILE = No file uploaded or No image type file uploaded.
            // ERROR: AUTHENTICATION_FAILED = Provided username and password are invalid.
            // ERROR: INVALID_IMAGE_SIZE_30_KB = The uploading image file must be 30 KB.
            // ERROR: UNKNOWN = Unknown error happened, close the program and reopen.
            // ERROR: NOT_DECODED = The captcha is timedout
            // if success the captcha decoded text along with image id will be returned.
            // Example of output: "1245986|HGFJD"
            // Using the captcha id you can set the captcha as bad.

            // Poll for the uploaded CAPTCHA status.
            br.loadConnection(conn);
            if (br.toString().startsWith("ERROR: ")) {
                throw new WTFException(br.toString().substring("ERROR: ".length()));
            }
            String[] result = br.getRegex("(\\d+)\\|(.*)").getRow(0);
            if (result != null) {
                job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " solved: " + br.toString());
                job.setAnswer(new ImageTyperzResponse(challenge, this, result[0], result[1]));

            } else {
                job.getLogger().info("Failed solving CAPTCHA");
                throw new SolverException("Failed:" + br.toString());
            }

        } catch (Exception e) {
            job.getLogger().log(e);
        }

    }

    protected boolean validateLogins() {
        if (!CFG_IMAGE_TYPERZ.ENABLED.isEnabled()) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_IMAGE_TYPERZ.USER_NAME.getValue())) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_IMAGE_TYPERZ.PASSWORD.getValue())) {
            return false;
        }

        return true;
    }

    @Override
    public void setUnused(AbstractResponse<?> response, SolverJob<?> job) {
    }

    @Override
    public void setInvalid(final AbstractResponse<?> response, SolverJob<?> job) {
        if (config.isFeedBackSendingEnabled() && response instanceof ImageTyperzResponse) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String captcha = ((ImageTyperzResponse) response).getCaptchaID();
                        Challenge<?> challenge = response.getChallenge();
                        if (challenge instanceof BasicCaptchaChallenge) {
                            Browser br = new Browser();
                            PostFormDataRequest r = new PostFormDataRequest("http://captchatypers.com/Forms/SetBadImage.ashx");

                            r.addFormData(new FormData("action", "SETBADIMAGE"));
                            r.addFormData(new FormData("username", (config.getUserName())));
                            r.addFormData(new FormData("password", (config.getPassword())));
                            r.addFormData(new FormData("imageID", (captcha)));
                            URLConnectionAdapter conn = br.openRequestConnection(r);
                            br.loadConnection(conn);

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
        }
    }

    public ImageTyperzAccount loadAccount() {

        ImageTyperzAccount ret = new ImageTyperzAccount();
        try {
            Browser br = new Browser();
            PostFormDataRequest r = new PostFormDataRequest("http://captchatypers.com/Forms/RequestBalance.ashx");

            r.addFormData(new FormData("action", "REQUESTBALANCE"));
            r.addFormData(new FormData("username", (config.getUserName())));
            r.addFormData(new FormData("password", (config.getPassword())));

            URLConnectionAdapter conn = br.openRequestConnection(r);
            br.loadConnection(conn);
            if (br.toString().startsWith("ERROR: ")) {
                throw new WTFException(br.toString().substring("Error: ".length()));
            }
            ret.setUserName(config.getUserName());
            ret.setBalance(100 * Double.parseDouble(br.toString()));

        } catch (Exception e) {
            logger.log(e);
            ret.setError(e.getMessage());
        }
        return ret;

    }

    @Override
    public void setValid(AbstractResponse<?> response, SolverJob<?> job) {
    }

}
