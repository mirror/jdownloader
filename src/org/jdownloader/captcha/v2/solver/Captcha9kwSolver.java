package org.jdownloader.captcha.v2.solver;

import java.io.IOException;
import java.net.URL;

import jd.controlling.captcha.CaptchaSettings;
import jd.nutils.encoding.Encoding;
import jd.utils.JDUtilities;

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

public class Captcha9kwSolver extends ChallengeSolver<String> {
    private Captcha9kwSettings            config;
    private static final Captcha9kwSolver INSTANCE = new Captcha9kwSolver();

    public static Captcha9kwSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private Captcha9kwSolver() {
        super(1);
        config = JsonConfig.create(Captcha9kwSettings.class);
        AdvancedConfigManager.getInstance().register(config);

    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return config.isEnabled() && super.canHandle(c);
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SolverException {
        if (StringUtils.isEmpty(config.getApiKey())) {
            job.getLogger().info("No ApiKey for 9kw.eu found.");
            return;
        }
        if (job.getChallenge() instanceof BasicCaptchaChallenge) {
            job.waitFor(JsonConfig.create(CaptchaSettings.class).getJAntiCaptchaTimeout(), JACSolver.getInstance());
            checkInterruption();
            BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();
            BasicHTTP http = new BasicHTTP();

            job.getLogger().info("Start Captcha to 9kw.eu. Timeout: " + JsonConfig.create(CaptchaSettings.class).getJAntiCaptchaTimeout() + " - getTypeID: " + challenge.getTypeID());
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (challenge.getTypeID().contains(config.getwhitelist())) {
                        job.getLogger().info("Hoster on whitelist for 9kw.eu. - " + challenge.getTypeID());
                    } else {
                        job.getLogger().info("Hoster not on whitelist for 9kw.eu. - " + challenge.getTypeID());
                        return;
                    }
                }
            }
            if (config.getblacklist() != null) {
                if (config.getblacklist().length() > 5) {
                    if (challenge.getTypeID().contains(config.getblacklist())) {
                        job.getLogger().info("Hoster on blacklist for 9kw.eu. - " + challenge.getTypeID());
                        return;
                    } else {
                        job.getLogger().info("Hoster not on blacklist for 9kw.eu. - " + challenge.getTypeID());
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
                byte[] data = IO.readFile(challenge.getImageFile());
                http.putRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String ret = new String(http.postPage(new URL(url), "action=usercaptchaupload&jd=2&source=jd2-" + Encoding.urlEncode(JDUtilities.getRevision()) + "&prio=" + config.getprio() + "&confirm=" + config.isconfirm() + "&oldsource=" + Encoding.urlEncode(challenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&timeout=" + JsonConfig.create(CaptchaSettings.class).getJAntiCaptchaTimeout() + "&version=1.1&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false))));
                job.getLogger().info("Send Captcha to 9kw.eu. - " + https9kw + " Answer: " + ret);
                if (!ret.startsWith("OK-")) throw new SolverException(ret);
                // Error-No Credits
                String captchaID = ret.substring(3);
                data = null;
                int count9kw = 3;
                Thread.sleep(3000);
                while (true) {
                    Thread.sleep(1000);
                    count9kw++;
                    url = https9kw + "://www.9kw.eu/index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1";
                    job.getLogger().info("9kw.eu Ask " + captchaID);
                    ret = new String(http.getPage(new URL(url)));
                    job.getLogger().info("9kw.eu Answer " + count9kw + "s: " + ret);
                    if (ret.startsWith("OK-answered-")) {
                        job.addAnswer(new CaptchaResponse(this, ret.substring("OK-answered-".length()), 100));
                        return;
                    }
                    checkInterruption();

                }

            } catch (IOException e) {
                job.getLogger().log(e);
            } finally {

                System.out.println(1);
            }

        } else {
            job.getLogger().info("Problem with Captcha9kwSolver.");
        }

    }
}
