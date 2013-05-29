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
        if (StringUtils.isEmpty(config.getApiKey())) {
            solverJob.getLogger().info("No ApiKey for 9kw.eu found.");
            return;
        }
        if (solverJob.getChallenge() instanceof ClickCaptchaChallenge) {
            solverJob.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
            checkInterruption();
            ClickCaptchaChallenge captchaChallenge = (ClickCaptchaChallenge) solverJob.getChallenge();
            BasicHTTP http = new BasicHTTP();

            solverJob.getLogger().info("Start Captcha to 9kw.eu. Timeout: " + JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout() + " - getTypeID: " + captchaChallenge.getTypeID());
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (config.getwhitelist().contains(captchaChallenge.getTypeID())) {
                        solverJob.getLogger().info("Hoster on whitelist for 9kw.eu. - " + captchaChallenge.getTypeID());
                    } else {
                        solverJob.getLogger().info("Hoster not on whitelist for 9kw.eu. - " + captchaChallenge.getTypeID());
                        return;
                    }
                }
            }
            if (config.getblacklist() != null) {
                if (config.getblacklist().length() > 5) {
                    if (config.getblacklist().contains(captchaChallenge.getTypeID())) {
                        solverJob.getLogger().info("Hoster on blacklist for 9kw.eu. - " + captchaChallenge.getTypeID());
                        return;
                    } else {
                        solverJob.getLogger().info("Hoster not on blacklist for 9kw.eu. - " + captchaChallenge.getTypeID());
                    }
                }
            }

            String https9kw = "";
            if (config.ishttps()) {
                https9kw = "https";
            } else {
                https9kw = "http";
            }

            try {
                String url = https9kw + "://www.9kw.eu/index.cgi";
                byte[] data = IO.readFile(captchaChallenge.getImageFile());
                http.putRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String ret = new String(http.postPage(new URL(url), "action=usercaptchaupload&jd=2&source=jd2&captchaperhour=" + config.gethour() + "&mouse=1&prio=" + config.getprio() + "&confirm=" + config.isconfirm() + "&oldsource=" + Encoding.urlEncode(captchaChallenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&timeout=" + JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout() + "&version=1.1&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false))));
                solverJob.getLogger().info("Send Captcha to 9kw.eu. - " + https9kw + " Answer: " + ret);
                if (!ret.startsWith("OK-")) throw new SolverException(ret);
                // Error-No Credits
                String captchaID = ret.substring(3);
                data = null;
                int count9kw = 3;
                Thread.sleep(3000);
                while (true) {
                    Thread.sleep(1000);
                    count9kw++;
                    url = https9kw + "://www.9kw.eu/index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&mouse=1&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1";
                    solverJob.getLogger().info("9kw.eu Ask " + captchaID);
                    ret = new String(http.getPage(new URL(url)));
                    solverJob.getLogger().info("9kw.eu Answer " + count9kw + "s: " + ret);
                    if (ret.startsWith("OK-answered-")) {
                        String antwort = ret.substring("OK-answered-".length());
                        String[] splitResult = antwort.split("x");

                        solverJob.addAnswer(new ClickCaptchaResponse(captchaChallenge, this, new ClickedPoint(Integer.parseInt(splitResult[0]), Integer.parseInt(splitResult[1])), 100));
                        return;
                    }
                    checkInterruption();

                }

            } catch (IOException e) {
                solverJob.getLogger().log(e);
            } finally {

            }

        } else {
            solverJob.getLogger().info("Problem with Captcha9kwSolverClick.");
        }

    }
}
