package org.jdownloader.captcha.v2.solver.jac;

import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.Icon;

import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.controlling.captcha.CaptchaSettings;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class JACSolver extends ChallengeSolver<String> implements ChallengeResponseValidation {
    private static final double            _0_85             = 0.85;
    private CaptchaSettings                config;
    private static final JACSolver         INSTANCE          = new JACSolver();
    private final HashMap<String, Integer> jacMethodTrustMap = new HashMap<String, Integer>();
    private HashMap<String, AutoTrust>     threshold;
    private LogSource                      logger;

    /**
     * get the only existing instance of JACSolver. This is a singleton
     * 
     * @return
     */
    public static JACSolver getInstance() {
        return JACSolver.INSTANCE;
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_OCR, size);
    }

    /**
     * Create a new instance of JACSolver. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */

    private JACSolver() {
        super(5);
        config = JsonConfig.create(CaptchaSettings.class);
        logger = LogController.getInstance().getLogger(JACSolver.class.getName());
        threshold = config.getJACThreshold();
        if (threshold == null) {
            threshold = new HashMap<String, AutoTrust>();
        }
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                config.setJACThreshold(threshold);
            }
        });
    }

    @Override
    public String getName() {
        return "Auto Solver";
    }

    @Override
    public long getTimeout() {

        return 30000;
    }

    public boolean canHandle(Challenge<?> c) {
        return true;
    }

    @Override
    public void enqueue(SolverJob<String> job) {
        if (CFG_CAPTCHA.JANTI_CAPTCHA_ENABLED.isEnabled() && super.canHandle(job.getChallenge())) {
            super.enqueue(job);
        } else {

        }
    }

    @Override
    public void solve(SolverJob<String> job) throws InterruptedException, SolverException {
        try {
            if (job.getChallenge() instanceof BasicCaptchaChallenge && CFG_CAPTCHA.JANTI_CAPTCHA_ENABLED.isEnabled()) {
                BasicCaptchaChallenge captchaChallenge = (BasicCaptchaChallenge) job.getChallenge();
                String host = null;
                if (captchaChallenge.getPlugin() instanceof PluginForHost) {
                    host = ((PluginForHost) captchaChallenge.getPlugin()).getHost();
                } else if (captchaChallenge.getPlugin() instanceof PluginForDecrypt) {
                    host = ((PluginForDecrypt) captchaChallenge.getPlugin()).getHost();
                }
                String trustID = (host + "_" + captchaChallenge.getTypeID()).toLowerCase(Locale.ENGLISH);
                if (StringUtils.isEmpty(captchaChallenge.getTypeID())) {
                    return;
                }
                job.getLogger().info("JACSolver handles " + job);
                job.getLogger().info("JAC: enabled: " + config.isAutoCaptchaRecognitionEnabled() + " Has Method: " + JACMethod.hasMethod(captchaChallenge.getTypeID()));
                if (!config.isAutoCaptchaRecognitionEnabled() || !JACMethod.hasMethod(captchaChallenge.getTypeID())) {
                    return;
                }
                checkInterruption();
                final JAntiCaptcha jac = new JAntiCaptcha(captchaChallenge.getTypeID());
                checkInterruption();
                Image captchaImage;

                captchaImage = ImageProvider.read(captchaChallenge.getImageFile());

                checkInterruption();
                final Captcha captcha = jac.createCaptcha(captchaImage);
                checkInterruption();
                String captchaCode = jac.checkCaptcha(captchaChallenge.getImageFile(), captcha);
                if (StringUtils.isEmpty(captchaCode)) {
                    return;
                }
                if (jac.isExtern()) {
                    /* external captchaCode Response */
                    job.addAnswer(new CaptchaResponse(captchaChallenge, this, captchaCode, 100));
                } else {
                    /* internal captchaCode Response */
                    final LetterComperator[] lcs = captcha.getLetterComperators();
                    double vp = 0.0;
                    if (lcs == null) {

                    } else {

                        for (final LetterComperator element : lcs) {
                            if (element == null) {
                                vp = 0;
                                break;
                            }

                            vp += element.getValityPercent();
                        }
                        vp /= lcs.length;
                    }
                    int trust = 120 - (int) vp;
                    int orgTrust = trust;
                    // StatsManager.I().
                    synchronized (jacMethodTrustMap) {
                        Integer trustMap = jacMethodTrustMap.get(trustID);
                        if (trustMap != null) {
                            if (trust > trustMap) {
                                trust = 100;
                            }
                        }
                        synchronized (threshold) {
                            AutoTrust trustValue = threshold.get(trustID);
                            if (trustValue != null) {
                                if (trust > trustValue.getValue() * _0_85) {
                                    trust = 100;
                                }
                            }
                        }
                    }

                    // we need to invert th
                    job.addAnswer(new JACCaptchaResponse(captchaChallenge, this, captchaCode, trust, orgTrust));
                    // StatsManager.I().logJac(captchaChallenge.getPlugin(), captchaChallenge.getTypeID(), trust);
                }

            }
        } catch (IOException e) {
            throw new SolverException(e);
        }
    }

    public void setMethodTrustThreshold(PluginForHost plugin, String method, int threshold) {
        String trustID = (plugin.getHost() + "_" + method).toLowerCase(Locale.ENGLISH);
        synchronized (jacMethodTrustMap) {
            if (threshold < 0 || threshold > 100) {
                jacMethodTrustMap.remove(trustID);
            } else {
                jacMethodTrustMap.put(trustID, threshold);
            }
        }
    }

    @Override
    public void setValid(AbstractResponse<?> response, SolverJob<?> job) {
        if (response.getSolver() != this) {
            return;
        }
        if (response instanceof JACCaptchaResponse) {
            int priority = ((JACCaptchaResponse) response).getUnmodifiedTrustValue();
            Challenge<?> challenge = response.getChallenge();
            if (challenge instanceof BasicCaptchaChallenge) {
                Plugin plugin = ((BasicCaptchaChallenge) challenge).getPlugin();
                String trustID = (plugin.getHost() + "_" + challenge.getTypeID()).toLowerCase(Locale.ENGLISH);
                synchronized (threshold) {

                    AutoTrust trustValue = threshold.get(trustID);
                    if (trustValue == null) {
                        threshold.put(trustID, new AutoTrust(priority));
                    } else {
                        trustValue.add(priority);
                    }
                    logger.info("New JAC Threshold for " + trustID + " : " + trustValue.getValue() + "(" + trustValue.getCounter() + ")");

                }

            }
        }

    }

    @Override
    public void setUnused(AbstractResponse<?> response, SolverJob<?> job) {
    }

    @Override
    public void setInvalid(AbstractResponse<?> response, SolverJob<?> job) {
        if (response instanceof JACCaptchaResponse) {
            int priority = ((JACCaptchaResponse) response).getUnmodifiedTrustValue();
            Challenge<?> challenge = response.getChallenge();
            if (challenge instanceof BasicCaptchaChallenge) {
                Plugin plugin = ((BasicCaptchaChallenge) challenge).getPlugin();
                String trustID = (plugin.getHost() + "_" + challenge.getTypeID()).toLowerCase(Locale.ENGLISH);
                synchronized (threshold) {

                    AutoTrust trustValue = threshold.get(trustID);

                    if (trustValue != null) {
                        logger.info("JAC Failure for " + trustID + "; : TrustValue " + priority + "; Dynamic Trust: " + trustValue.getValue() + "(" + trustValue.getCounter() + ") Detected: " + response.getValue());
                        // increase trustValue!
                        trustValue.add((int) (priority * (1d + (1d - _0_85) * 2)));
                        logger.info("New JAC Threshold for " + trustID + " : " + trustValue.getValue() + "(" + trustValue.getCounter() + ")");
                    }
                }

            }
        }

    }
}
