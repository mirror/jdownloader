package org.jdownloader.captcha.v2.solver;

import java.io.IOException;
import java.net.URL;

import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CBSolver extends ChallengeSolver<String> {
    private CaptchaSettings       config;
    private static final CBSolver INSTANCE = new CBSolver();

    public static CBSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private CBSolver() {
        super(1);
        config = JsonConfig.create(CaptchaSettings.class);
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException {
        if (StringUtils.isEmpty(config.getCBUser()) || StringUtils.isEmpty(config.getCBPass())) return;
        if (job.getChallenge() instanceof BasicCaptchaChallenge) {

            BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();
            BasicHTTP http = new BasicHTTP();

            try {
                String url = "http://www.captchabrotherhood.com/sendNewCaptcha.aspx?username=" + config.getCBUser() + "&password=" + config.getCBPass() + "&captchaSource=jdPlugin&captchaSite=999&timeout=80&version=1.1.7";
                byte[] data = IO.readFile(challenge.getImageFile());

                String ret = new String(http.postPage(new URL(url), data));

                if (ret.startsWith("OK-"))
                ;
                System.out.println(new String(ret));
                data = null;
                while (true) {
                    Thread.sleep(5000);
                    url = "http://www.captchabrotherhood.com/askCaptchaResult.aspx?username=" + config.getCBUser() + "&password=" + config.getCBPass() + "&captchaID=" + ret.substring(3) + "&version=1.1.7";

                    ret = new String(http.getPage(new URL(url)));
                    System.out.println(ret);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
