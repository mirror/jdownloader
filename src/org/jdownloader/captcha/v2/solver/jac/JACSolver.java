package org.jdownloader.captcha.v2.solver.jac;

import java.awt.Image;
import java.io.IOException;

import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class JACSolver extends ChallengeSolver<String> {
    private CaptchaSettings        config;
    private static final JACSolver INSTANCE = new JACSolver();

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
    public long getTimeout() {
        return 30000;
    }

    @Override
    public void solve(SolverJob<String> job) throws InterruptedException, SolverException {
        try {
            if (job.getChallenge() instanceof BasicCaptchaChallenge) {

                BasicCaptchaChallenge captchaChallenge = (BasicCaptchaChallenge) job.getChallenge();
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
                checkInterruption();
                if (jac.isExtern()) {
                    if (captchaCode == null || captchaCode.trim().length() == 0) {
                        return;
                    } else {
                        job.addAnswer(new CaptchaResponse(this, captchaCode, 100));
                    }

                }
                checkInterruption();
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

                // we need to invert th
                job.addAnswer(new CaptchaResponse(this, captchaCode, 120 - (int) vp));

            }
        } catch (IOException e) {
            throw new SolverException(e);
        }

    }
}
