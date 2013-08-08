package org.jdownloader.captcha.v2.solver.jac;

import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.controlling.captcha.CaptchaSettings;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class JACSolver extends ChallengeSolver<String> {
    private CaptchaSettings                config;
    private static final JACSolver         INSTANCE          = new JACSolver();
    private final HashMap<String, Integer> jacMethodTrustMap = new HashMap<String, Integer>();

    /**
     * get the only existing instance of JACSolver. This is a singleton
     * 
     * @return
     */
    public static JACSolver getInstance() {
        return JACSolver.INSTANCE;
    }

    /**
     * Create a new instance of JACSolver. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */

    private JACSolver() {
        super(5);
        config = JsonConfig.create(CaptchaSettings.class);

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
                if (StringUtils.isEmpty(captchaChallenge.getTypeID())) return;
                job.getLogger().info("JACSolver handles " + job);
                job.getLogger().info("JAC: enabled: " + config.isAutoCaptchaRecognitionEnabled() + " Has Method: " + JACMethod.hasMethod(captchaChallenge.getTypeID()));
                if (!config.isAutoCaptchaRecognitionEnabled() || !JACMethod.hasMethod(captchaChallenge.getTypeID())) return;
                checkInterruption();
                final JAntiCaptcha jac = new JAntiCaptcha(captchaChallenge.getTypeID());
                checkInterruption();
                Image captchaImage;

                captchaImage = ImageProvider.read(captchaChallenge.getImageFile());

                checkInterruption();
                final Captcha captcha = jac.createCaptcha(captchaImage);
                checkInterruption();
                String captchaCode = jac.checkCaptcha(captchaChallenge.getImageFile(), captcha);
                if (StringUtils.isEmpty(captchaCode)) return;
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
                    synchronized (jacMethodTrustMap) {
                        Integer trustMap = jacMethodTrustMap.get(trustID);
                        if (trustMap != null) {
                            if (trust > trustMap) {
                                trust = 100;
                            }
                        } else if (trust > config.getDefaultJACTrustThreshold()) {
                            trust = 100;
                        }
                    }
                    // we need to invert th
                    job.addAnswer(new CaptchaResponse(captchaChallenge, this, captchaCode, trust));
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
}
