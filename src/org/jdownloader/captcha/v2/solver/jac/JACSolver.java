package org.jdownloader.captcha.v2.solver.jac;

import java.awt.Image;

import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.SolverJob;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class JACSolver implements ChallengeSolver<String> {
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
        config = JsonConfig.create(CaptchaSettings.class);
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void solve(SolverJob<String> solverJob) {
        try {
            if (solverJob.getChallenge() instanceof BasicCaptchaChallenge) {
                BasicCaptchaChallenge captchaChallenge = (BasicCaptchaChallenge) solverJob.getChallenge();
                if (StringUtils.isEmpty(captchaChallenge.getTypeID())) return;
                if (!config.isAutoCaptchaRecognitionEnabled() || !JACMethod.hasMethod(captchaChallenge.getTypeID())) return;

                final JAntiCaptcha jac = new JAntiCaptcha(captchaChallenge.getTypeID());

                final Image captchaImage = ImageProvider.read(captchaChallenge.getImageFile());
                final Captcha captcha = jac.createCaptcha(captchaImage);
                String captchaCode = jac.checkCaptcha(captchaChallenge.getImageFile(), captcha);

                if (jac.isExtern()) {
                    if (captchaCode == null || captchaCode.trim().length() == 0) {
                        return;
                    } else {
                        solverJob.addAnswer(new CaptchaResponse(captchaCode, 100));
                    }

                }
                final LetterComperator[] lcs = captcha.getLetterComperators();

                double vp = 0.0;
                if (lcs == null) {
                    vp = 100.0;
                } else {
                    for (final LetterComperator element : lcs) {
                        if (element == null) {
                            vp = 100.0;
                            break;
                        }
                        vp = Math.max(vp, element.getValityPercent());
                    }
                }

                solverJob.addAnswer(new CaptchaResponse(captchaCode, (int) vp));

            } else {

            }

        } catch (final Exception e) {
            return;
        }
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

}
