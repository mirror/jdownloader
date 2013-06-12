package org.jdownloader.captcha.v2.solver;

import java.io.IOException;
import java.net.URL;

import jd.controlling.captcha.CaptchaSettings;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class CBSolver extends ChallengeSolver<String> {
    private CaptchaBrotherHoodSettings config;
    private static final CBSolver      INSTANCE = new CBSolver();

    public static CBSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private CBSolver() {
        super(1);
        config = JsonConfig.create(CaptchaBrotherHoodSettings.class);
        AdvancedConfigManager.getInstance().register(config);

    }

    @Override
    public boolean canHandle(Challenge<?> c) {

        return config.isEnabled() && super.canHandle(c);
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SolverException {
        if (StringUtils.isEmpty(config.getUser()) || StringUtils.isEmpty(config.getPass())) return;
        if (job.getChallenge() instanceof BasicCaptchaChallenge) {
            job.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
            checkInterruption();
            BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();
            BasicHTTP http = new BasicHTTP();

            try {
                String url = "http://www.captchabrotherhood.com/sendNewCaptcha.aspx?username=" + Encoding.urlEncode(config.getUser()) + "&password=" + Encoding.urlEncode(config.getPass()) + "&captchaSource=jdPlugin&captchaSite=999&timeout=80&version=1.1.7";
                byte[] data = IO.readFile(challenge.getImageFile());

                String ret = new String(http.postPage(new URL(url), data), "UTF-8");
                job.getLogger().info("Send Captcha. Answer: " + ret);
                if (!ret.startsWith("OK-")) throw new SolverException(ret);
                // Error-No Credits
                String captchaID = ret.substring(3);
                data = null;
                Thread.sleep(6000);
                while (true) {

                    Thread.sleep(1000);
                    url = "http://www.captchabrotherhood.com/askCaptchaResult.aspx?username=" + Encoding.urlEncode(config.getUser()) + "&password=" + Encoding.urlEncode(config.getPass()) + "&captchaID=" + Encoding.urlEncode(captchaID) + "&version=1.1.7";
                    job.getLogger().info("Ask " + url);
                    ret = new String(http.getPage(new URL(url)));
                    job.getLogger().info("Answer " + ret);
                    if (ret.startsWith("OK-answered-")) {
                        job.addAnswer(new CaptchaResponse(challenge, this, ret.substring("OK-answered-".length()), 100));
                        return;
                    }
                    checkInterruption();

                }

            } catch (IOException e) {
                job.getLogger().log(e);
            } finally {

            }

        }

    }

}
