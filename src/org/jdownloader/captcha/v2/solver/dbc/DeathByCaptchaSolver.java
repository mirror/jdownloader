package org.jdownloader.captcha.v2.solver.dbc;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import org.jdownloader.captcha.v2.solver.dbc.api.Captcha;
import org.jdownloader.captcha.v2.solver.dbc.api.Client;
import org.jdownloader.captcha.v2.solver.dbc.api.SocketClient;
import org.jdownloader.captcha.v2.solver.dbc.api.User;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_DBC;

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
            return true;
        }
        return c instanceof BasicCaptchaChallenge && super.canHandle(c);
    }

    protected void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws InterruptedException, SolverException {

        job.showBubble(this, getBubbleTimeout(challenge));
        checkInterruption();
        final Client client = getClient();
        try {
            Captcha captcha = null;
            challenge.sendStatsSolving(this);
            // Put your CAPTCHA image file, file object, input stream,
            // or vector of bytes here:

            job.setStatus(SolverStatus.UPLOADING);
            if (challenge instanceof AbstractRecaptcha2FallbackChallenge) {
                captcha = client.upload(challenge.getAnnotatedImageBytes());
            } else {
                captcha = client.upload(challenge.getImageFile());
            }
            if (null != captcha) {
                job.setStatus(new SolverStatus(_GUI.T.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " uploaded: " + captcha.id);
                long startTime = System.currentTimeMillis();
                // Poll for the uploaded CAPTCHA status.
                while (captcha.isUploaded() && !captcha.isSolved()) {

                    job.getLogger().info("deathbycaptcha.eu NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");

                    Thread.sleep(Client.POLLS_INTERVAL * 1000);
                    captcha = client.getCaptcha(captcha);
                }

                if (captcha.isSolved()) {
                    job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " solved: " + captcha.text);

                    AbstractResponse<String> answer = challenge.parseAPIAnswer(captcha.text, this);
                    DeathByCaptchaResponse response = new DeathByCaptchaResponse(challenge, this, captcha, answer.getValue(), answer.getPriority());

                    job.setAnswer(response);

                } else {
                    job.getLogger().info("Failed solving CAPTCHA");
                    throw new SolverException("Failed:" + captcha.toString());
                }
            }

        } catch (Exception e) {
            job.setStatus(e.getMessage(), new AbstractIcon(IconKey.ICON_ERROR, 20));
            job.getLogger().log(e);
            challenge.sendStatsError(this, e);
        } finally {
            client.close();
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

    private synchronized Client getClient() {
        Client client = new SocketClient(config.getUserName(), config.getPassword());
        client.isVerbose = true;
        return client;

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
                        Captcha captcha = ((DeathByCaptchaResponse) response).getCaptcha();
                        // Report incorrectly solved CAPTCHA if neccessary.
                        // Make sure you've checked if the CAPTCHA was in fact
                        // incorrectly solved, or else you might get banned as
                        // abuser.
                        Client client = getClient();
                        try {
                            Challenge<?> challenge = response.getChallenge();
                            if (challenge instanceof BasicCaptchaChallenge) {
                                if (client.report(captcha)) {
                                    logger.info("CAPTCHA " + challenge + " reported as incorrectly solved");
                                } else {
                                    logger.info("Failed reporting incorrectly solved CAPTCHA. Disabled Feedback");
                                    config.setFeedBackSendingEnabled(false);
                                }
                            }
                        } finally {
                            client.close();
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
        Client c = getClient();
        try {
            User user = c.getUser();
            ret.setBalance(user.getBalance());
            ret.setBanned(user.isBanned());
            ret.setId(user.getId());
            ret.setRate(user.getRate());
        } catch (Exception e) {
            logger.log(e);
            ret.setError(e.getMessage());
        } finally {
            c.close();
        }
        return ret;

    }

}
