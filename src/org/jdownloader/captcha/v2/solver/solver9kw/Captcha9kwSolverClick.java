package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final Captcha9kwSolverClick INSTANCE                    = new Captcha9kwSolverClick();
    private ThreadPoolExecutor                 threadPool                  = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());
    AtomicInteger                              click9kw_counterSolved      = new AtomicInteger();
    AtomicInteger                              click9kw_counterInterrupted = new AtomicInteger();
    AtomicInteger                              click9kw_counter            = new AtomicInteger();
    AtomicInteger                              click9kw_counterSend        = new AtomicInteger();
    AtomicInteger                              click9kw_counterSendError   = new AtomicInteger();
    AtomicInteger                              click9kw_counterOK          = new AtomicInteger();
    AtomicInteger                              click9kw_counterNotOK       = new AtomicInteger();
    AtomicInteger                              click9kw_counterUnused      = new AtomicInteger();

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

    public void setdebug(CESSolverJob<ClickedPoint> job, String logdata) {
        if (config.isDebug() && logdata != null) {
            org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setlong_debuglog(logdata);
        }
        job.getLogger().info(logdata);
    }

    @Override
    protected void solveCES(CESSolverJob<ClickedPoint> solverJob) throws InterruptedException, SolverException {

        solverJob.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
        checkInterruption();
        ClickCaptchaChallenge captchaChallenge = (ClickCaptchaChallenge) solverJob.getChallenge();

        int cph = config.gethour();
        int priothing = config.getprio();
        int timeoutthing = (JsonConfig.create(CaptchaSettings.class).getCaptchaDialog9kwTimeout() / 1000);
        boolean selfsolve = config.isSelfsolve();
        boolean confirm = config.ismouseconfirm();

        if (!config.getApiKey().matches("^[a-zA-Z0-9]+$")) {
            jd.gui.UserIO.getInstance().requestMessageDialog("API Key is not correct!\nOnly a-z, A-Z and 0-9\n");
            return;
        }

        setdebug(solverJob, "Start Captcha to 9kw.eu. Timeout: " + JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout() + " - getTypeID: " + captchaChallenge.getTypeID());
        if (config.getwhitelistcheck()) {
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (config.getwhitelist().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on whitelist for 9kw.eu. - " + captchaChallenge.getTypeID());
                    } else {
                        setdebug(solverJob, "Hoster not on whitelist for 9kw.eu. - " + captchaChallenge.getTypeID());
                        return;
                    }
                }
            }
        }

        if (config.getblacklistcheck()) {
            if (config.getblacklist() != null) {
                if (config.getblacklist().length() > 5) {
                    if (config.getblacklist().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on blacklist for 9kw.eu. - " + captchaChallenge.getTypeID());
                        return;
                    } else {
                        setdebug(solverJob, "Hoster not on blacklist for 9kw.eu. - " + captchaChallenge.getTypeID());
                    }
                }
            }
        }

        if (config.getwhitelistprio() != null) {
            if (config.getwhitelistprio().length() > 5) {
                if (config.getwhitelistprio().contains(captchaChallenge.getTypeID())) {
                    setdebug(solverJob, "Hoster on whitelist with prio for 9kw.eu. - " + captchaChallenge.getTypeID());
                } else {
                    setdebug(solverJob, "Hoster not on whitelist with prio for 9kw.eu. - " + captchaChallenge.getTypeID());
                    priothing = 0;
                }
            }
        }

        if (config.getblacklistprio() != null) {
            if (config.getblacklistprio().length() > 5) {
                if (config.getblacklistprio().contains(captchaChallenge.getTypeID())) {
                    priothing = 0;
                    setdebug(solverJob, "Hoster on blacklist with prio for 9kw.eu. - " + captchaChallenge.getTypeID());
                } else {
                    setdebug(solverJob, "Hoster not on blacklist with prio for 9kw.eu. - " + captchaChallenge.getTypeID());
                }
            }
        }

        if (config.getwhitelisttimeoutcheck()) {
            if (config.getwhitelisttimeout() != null) {
                if (config.getwhitelisttimeout().length() > 5) {
                    if (config.getwhitelisttimeout().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on whitelist with other 9kw timeout for 9kw.eu. - " + captchaChallenge.getTypeID());
                        timeoutthing = (config.getCaptchaOther9kwTimeout() / 1000);
                    } else {
                        setdebug(solverJob, "Hoster not on whitelist with other 9kw timeout for 9kw.eu. - " + captchaChallenge.getTypeID());
                    }
                }
            }
        }

        if (config.getblacklisttimeoutcheck()) {
            if (config.getblacklisttimeout() != null) {
                if (config.getblacklisttimeout().length() > 5) {
                    if (config.getblacklisttimeout().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on blacklist with other 9kw timeout for 9kw.eu. - " + captchaChallenge.getTypeID());
                    } else {
                        timeoutthing = (config.getCaptchaOther9kwTimeout() / 1000);
                        setdebug(solverJob, "Hoster not on blacklist with other 9kw timeout for 9kw.eu. - " + captchaChallenge.getTypeID());
                    }
                }
            }
        }

        String moreoptions = "";
        if (config.gethosteroptions().length() > 5) {
            String[] list = config.gethosteroptions().split(";");
            for (String hosterline : list) {
                if (hosterline.contains(captchaChallenge.getTypeID())) {
                    String[] listdetail = hosterline.split(":");
                    for (String hosterlinedetail : listdetail) {
                        if (!listdetail[0].equals(hosterlinedetail)) {
                            String[] detailvalue = hosterlinedetail.split("=");
                            if (detailvalue[0].equals("timeout") && detailvalue[1].matches("^[0-9]+$")) {
                                timeoutthing = Integer.parseInt(detailvalue[1]);
                            }
                            if (detailvalue[0].equals("prio") && detailvalue[1].matches("^[0-9]+$")) {
                                priothing = Integer.parseInt(detailvalue[1]);
                            }
                            if (detailvalue[0].equals("cph") && detailvalue[1].matches("^[0-9]+$")) {
                                cph = Integer.parseInt(detailvalue[1]);
                            }
                            if (detailvalue[0].equals("nomd5") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&nomd5=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("ocr") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&ocr=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("min") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&min_len=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("max") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&max_len=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("phrase") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&phrase=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("math") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&math=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("numeric") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&numeric=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("case-sensitive") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&case-sensitive=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("confirm") && detailvalue[1].matches("^[0-9]+$")) {
                                if (detailvalue[1].equals("1")) {
                                    confirm = true;
                                } else {
                                    confirm = false;
                                }
                            }
                            if (detailvalue[0].equals("selfsolve") && detailvalue[1].matches("^[0-9]+$")) {
                                if (detailvalue[1].equals("1")) {
                                    selfsolve = true;
                                } else {
                                    selfsolve = false;
                                }
                            }
                        }
                    }
                }
            }
        }

        setdebug(solverJob, "Upload Captcha to 9kw.eu. GetTypeID: " + captchaChallenge.getTypeID() + " - Plugin: " + captchaChallenge.getPlugin());
        try {
            solverJob.showBubble(this);
            click9kw_counter.incrementAndGet();
            solverJob.setStatus(SolverStatus.UPLOADING);
            byte[] data = IO.readFile(captchaChallenge.getImageFile());
            Browser br = new Browser();
            br.setAllowedResponseCodes(new int[] { 500 });
            String ret = "";
            for (int i = 0; i <= 5; i++) {
                ret = br.postPage(getAPIROOT() + "index.cgi", "action=usercaptchaupload&jd=2&source=jd2" + moreoptions + "&captchaperhour=" + cph + "&mouse=1&prio=" + priothing + "&selfsolve=" + selfsolve + "&confirm=" + confirm + "&oldsource=" + Encoding.urlEncode(captchaChallenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&maxtimeout=" + timeoutthing + "&version=1.2&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false)));
                if (ret.startsWith("OK-")) {
                    click9kw_counterSend.incrementAndGet();
                    break;
                } else {
                    setdebug(solverJob, "Upload Captcha(" + i + ") to 9kw.eu. GetTypeID: " + captchaChallenge.getTypeID() + " - Plugin: " + captchaChallenge.getPlugin());
                    Thread.sleep(3000);

                }
            }
            solverJob.setStatus(SolverStatus.SOLVING);
            setdebug(solverJob, "Send Captcha to 9kw.eu. - " + getAPIROOT() + " Answer: " + ret);
            if (!ret.startsWith("OK-")) {
                if (ret.contains("0011 Guthaben ist nicht ausreichend") && config.getlowcredits()) {
                    jd.gui.UserIO.getInstance().requestMessageDialog("9kw error ", "0011 Not enough credits.\n" + ret);
                }
                click9kw_counterSendError.incrementAndGet();
                throw new SolverException(ret);
            }
            // Error-No Credits
            String captchaID = ret.substring(3);
            data = null;
            long startTime = System.currentTimeMillis();

            Thread.sleep(5000);
            while (true) {
                setdebug(solverJob, "9kw.eu CaptchaID " + captchaID + ": Ask");
                ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&mouse=1&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1");
                if (StringUtils.isEmpty(ret)) {
                    setdebug(solverJob, "9kw.eu CaptchaID " + captchaID + " - NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                } else {
                    setdebug(solverJob, "9kw.eu CaptchaID " + captchaID + " - Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                }
                if (ret.startsWith("OK-answered-")) {
                    click9kw_counterSolved.incrementAndGet();
                    String antwort = ret.substring("OK-answered-".length());
                    String[] splitResult = antwort.split("x");

                    solverJob.setAnswer(new Captcha9kwClickResponse(captchaChallenge, this, new ClickedPoint(Integer.parseInt(splitResult[0]), Integer.parseInt(splitResult[1])), 100, captchaID));
                    return;
                }

                checkInterruption();
                Thread.sleep(3000);
            }

        } catch (IOException e) {
            setdebug(solverJob, "9kw.eu Interrupted: " + e);
            click9kw_counterInterrupted.incrementAndGet();
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
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("9kw.eu CaptchaID " + captchaID + ": OK (Feedback)");
                                click9kw_counterOK.incrementAndGet();
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
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("9kw.eu CaptchaID " + captchaID + ": Unused (Feedback)");
                                click9kw_counterUnused.incrementAndGet();
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
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("9kw.eu CaptchaID " + captchaID + ": NotOK (Feedback)");
                                click9kw_counterNotOK.incrementAndGet();
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
