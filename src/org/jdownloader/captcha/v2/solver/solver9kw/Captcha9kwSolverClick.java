package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;

import jd.controlling.captcha.CaptchaSettings;
import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class Captcha9kwSolverClick extends CESChallengeSolver<ClickedPoint> implements ChallengeResponseValidation {
    private Captcha9kwSettings                 config;
    private static final Captcha9kwSolverClick INSTANCE   = new Captcha9kwSolverClick();
    private ThreadPoolExecutor                 threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());

    public static Captcha9kwSolverClick getInstance() {
        return INSTANCE;
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_9KW, size);
    }

    @Override
    public String getName() {
        return "9kw.eu";
    }

    @Override
    public Class<ClickedPoint> getResultType() {
        return ClickedPoint.class;
    }

    private Captcha9kwSolverClick() {
        super(JsonConfig.create(Captcha9kwSettings.class).getThreadpoolSize());
        config = JsonConfig.create(Captcha9kwSettings.class);
        // AdvancedConfigManager.getInstance().register(config);
        threadPool.allowCoreThreadTimeOut(true);
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        // do not use && config.isEnabled() here. config.ismouse() is the enable config for the mouse solver
        return c instanceof ClickCaptchaChallenge && CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED.isEnabled() && config.ismouse() && super.canHandle(c);
    }

    public String getAPIROOT() {
        if (config.ishttps()) {
            return "https://www.9kw.eu/";
        } else {
            return "http://www.9kw.eu/";
        }
    }

    @Override
    protected void solveCES(CESSolverJob<ClickedPoint> solverJob) throws InterruptedException, SolverException {

        solverJob.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
        checkInterruption();
        ClickCaptchaChallenge captchaChallenge = (ClickCaptchaChallenge) solverJob.getChallenge();

        int priothing = config.getprio();

        solverJob.getLogger().info("Start Captcha to 9kw.eu. Timeout: " + JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout() + " - getTypeID: " + captchaChallenge.getTypeID());
        if (config.getwhitelistcheck()) {
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
        }

        if (config.getblacklistcheck()) {
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
        }

        if (config.getwhitelistprio() != null) {
            if (config.getwhitelistprio().length() > 5) {
                if (config.getwhitelistprio().contains(captchaChallenge.getTypeID())) {
                    solverJob.getLogger().info("Hoster on whitelist with prio for 9kw.eu. - " + captchaChallenge.getTypeID());
                } else {
                    solverJob.getLogger().info("Hoster not on whitelist with prio for 9kw.eu. - " + captchaChallenge.getTypeID());
                    priothing = 0;
                }
            }
        }

        if (config.getblacklistprio() != null) {
            if (config.getblacklistprio().length() > 5) {
                if (config.getblacklistprio().contains(captchaChallenge.getTypeID())) {
                    priothing = 0;
                    solverJob.getLogger().info("Hoster on blacklist with prio for 9kw.eu. - " + captchaChallenge.getTypeID());
                } else {
                    solverJob.getLogger().info("Hoster not on blacklist with prio for 9kw.eu. - " + captchaChallenge.getTypeID());
                }
            }
        }

        solverJob.showBubble(this);
        try {
            solverJob.setStatus(SolverStatus.UPLOADING);
            byte[] data = IO.readFile(captchaChallenge.getImageFile());
            Browser br = new Browser();
            br.setAllowedResponseCodes(new int[] { 500 });
            String ret = "";
            for (int i = 0; i <= 5; i++) {
                ret = br.postPage(getAPIROOT() + "index.cgi", "action=usercaptchaupload&jd=2&source=jd2&captchaperhour=" + config.gethour() + "&mouse=1&prio=" + priothing + "&selfsolve=" + config.isSelfsolve() + "&confirm=" + config.ismouseconfirm() + "&oldsource=" + Encoding.urlEncode(captchaChallenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&maxtimeout=" + (JsonConfig.create(CaptchaSettings.class).getCaptchaDialog9kwTimeout() / 1000) + "&version=1.2&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false)));
                if (ret.startsWith("OK-")) {
                    break;
                } else {
                    Thread.sleep(3000);

                }
            }
            solverJob.setStatus(SolverStatus.SOLVING);
            solverJob.getLogger().info("Send Captcha to 9kw.eu. - " + getAPIROOT() + " Answer: " + ret);
            if (!ret.startsWith("OK-")) throw new SolverException(ret);
            // Error-No Credits
            String captchaID = ret.substring(3);
            data = null;
            int count9kw = 5;
            Thread.sleep(5000);
            while (true) {
                count9kw += 2;
                solverJob.getLogger().info("9kw.eu Ask " + captchaID);
                ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&mouse=1&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1");
                solverJob.getLogger().info("9kw.eu Answer " + count9kw + "s: " + ret);
                if (ret.startsWith("OK-answered-")) {
                    String antwort = ret.substring("OK-answered-".length());
                    String[] splitResult = antwort.split("x");

                    solverJob.setAnswer(new Captcha9kwClickResponse(captchaChallenge, this, new ClickedPoint(Integer.parseInt(splitResult[0]), Integer.parseInt(splitResult[1])), 100, captchaID));
                    return;
                }
                checkInterruption();
                Thread.sleep(3000);
            }

        } catch (IOException e) {
            solverJob.getLogger().log(e);
        } finally {

        }

    }

    @Override
    public void setValid(final AbstractResponse<?> response) {
        if (config.isfeedback()) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String captchaID = ((Captcha9kwClickResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        br.setAllowedResponseCodes(new int[] { 500 });
                        String ret = "";
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=1&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                break;
                            } else {
                                Thread.sleep(2000);
                            }
                        }
                    } catch (final Throwable e) {
                        LogController.CL(true).log(e);
                    }
                }
            });
        }
    }

    @Override
    public void setUnused(final AbstractResponse<?> response) {
        if (config.isfeedback()) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String captchaID = ((Captcha9kwClickResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        br.setAllowedResponseCodes(new int[] { 500 });
                        String ret = "";
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=3&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                break;
                            } else {
                                Thread.sleep(2000);
                            }
                        }
                    } catch (final Throwable e) {
                        LogController.CL(true).log(e);
                    }
                }
            });
        }
    }

    @Override
    public void setInvalid(final AbstractResponse<?> response) {
        if (config.isfeedback()) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String captchaID = ((Captcha9kwClickResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        br.setAllowedResponseCodes(new int[] { 500 });
                        String ret = "";
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=2&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                break;
                            } else {
                                Thread.sleep(2000);
                            }
                        }
                    } catch (final Throwable e) {
                        LogController.CL(true).log(e);
                    }
                }
            });
        }
    }

    @Override
    public void extendServicePabel(LinkedList<ServiceCollection<?>> services) {
    }

    @Override
    protected boolean validateLogins() {
        return StringUtils.isNotEmpty(config.getApiKey()) && config.ismouse();
    }
}
