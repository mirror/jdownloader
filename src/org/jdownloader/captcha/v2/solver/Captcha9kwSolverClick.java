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
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ClickCaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class Captcha9kwSolverClick extends ChallengeSolver<ClickedPoint> {
    private Captcha9kwSettings                 config;
    private static final Captcha9kwSolverClick INSTANCE = new Captcha9kwSolverClick();

    public static Captcha9kwSolverClick getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<ClickedPoint> getResultType() {
        return ClickedPoint.class;
    }

    private Captcha9kwSolverClick() {
        super(1);
        config = JsonConfig.create(Captcha9kwSettings.class);
        // AdvancedConfigManager.getInstance().register(config);
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return config.ismouse() && super.canHandle(c);
    }

    @Override
    public void solve(SolverJob<ClickedPoint> solverJob) throws InterruptedException, SolverException {
        if (StringUtils.isEmpty(config.getApiKey())) return;
        if (solverJob.getChallenge() instanceof ClickCaptchaChallenge) {
            solverJob.waitFor(JsonConfig.create(CaptchaSettings.class).getJAntiCaptchaTimeout(), JACSolver.getInstance());
            checkInterruption();
            ClickCaptchaChallenge captchaChallenge = (ClickCaptchaChallenge) solverJob.getChallenge();
            BasicHTTP http = new BasicHTTP();

            // job.getLogger().info("9kw.gettypeid: " + challenge.getTypeID());
            // challenge.getTypeID() - rapidgator.net

            try {
                String url = "http://www.9kw.eu/index.cgi";
                byte[] data = IO.readFile(captchaChallenge.getImageFile());
                http.putRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String ret = new String(http.postPage(new URL(url), "action=usercaptchaupload&jd=2&source=jd2&mouse=1&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&timeout=250&version=1.0&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false))));
                solverJob.getLogger().info("Send Captcha. Answer: " + ret);
                if (!ret.startsWith("OK-")) throw new SolverException(ret);
                // Error-No Credits
                String captchaID = ret.substring(3);
                data = null;
                Thread.sleep(6000);
                while (true) {

                    Thread.sleep(1000);
                    url = "http://www.9kw.eu/index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&mouse=1&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.0";
                    solverJob.getLogger().info("Ask " + url);
                    ret = new String(http.getPage(new URL(url)));
                    solverJob.getLogger().info("Answer " + ret);
                    if (ret.startsWith("OK-answered-")) {
                        // solverJob.addAnswer(new ClickCaptchaResponse(this, resultPoint, 100));
                        String antwort = ret.substring("OK-answered-".length());
                        String[] splitResult = antwort.split("x");

                        solverJob.addAnswer(new ClickCaptchaResponse(this, new ClickedPoint(Integer.parseInt(splitResult[0]), Integer.parseInt(splitResult[1])), 100));
                        return;
                    }
                    checkInterruption();

                }

            } catch (IOException e) {
                solverJob.getLogger().log(e);
            } finally {

                System.out.println(1);
            }

        }

    }

}
