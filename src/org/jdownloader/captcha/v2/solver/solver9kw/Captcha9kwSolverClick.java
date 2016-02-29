package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByLink;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class Captcha9kwSolverClick extends CESChallengeSolver<ClickedPoint> {

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
    public Class<ClickedPoint> getResultType() {
        return ClickedPoint.class;
    }

    private Captcha9kwSolverClick() {
        super(NineKwSolverService.getInstance(), Math.max(1, Math.min(25, NineKwSolverService.getInstance().getConfig().getThreadpoolSize())));
        config = NineKwSolverService.getInstance().getConfig();
        NineKwSolverService.getInstance().setClickSolver(this);
        threadPool.allowCoreThreadTimeOut(true);
    }

    @Override
    public boolean isEnabled() {
        return config.ismouse() && config.isEnabledGlobally();
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        // do not use && config.isEnabled() here. config.ismouse() is the enable config for the mouse solver
        return c instanceof ClickCaptchaChallenge && super.canHandle(c);
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
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setlong_debuglog('[' + dateFormat.format(new Date()) + ']' + " " + logdata);
        }
        job.getLogger().info(logdata);
    }

    @Override
    protected void solveCES(CESSolverJob<ClickedPoint> solverJob) throws InterruptedException, SolverException {

        // solverJob.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
        checkInterruption();
        ClickCaptchaChallenge captchaChallenge = (ClickCaptchaChallenge) solverJob.getChallenge();

        int cph = config.gethour();
        int cpm = config.getminute();
        int priothing = config.getprio();
        long timeoutthing = (config.getDefaultTimeout() / 1000);
        boolean selfsolve = config.isSelfsolve();
        boolean confirm = config.ismouseconfirm();

        if (!config.getApiKey().matches("^[a-zA-Z0-9]+$")) {
            setdebug(solverJob, "API Key is not correct! (Mouse)");
            if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get()) > 30) {
                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.set((System.currentTimeMillis() / 1000));
                jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_wrongapikey1() + "\n" + _GUI.T.NinekwService_createPanel_errortext_wrongapikey2() + "\n");
                Thread.sleep(30000);
            }
            return;
        }

        setdebug(solverJob, "Config(Mouse) - Prio: " + priothing + " - Timeout: " + timeoutthing + "s/" + (config.getCaptchaOther9kwTimeout() / 1000) + "s - Parallel: " + config.getThreadpoolSize());
        setdebug(solverJob, "Start Captcha - GetTypeID: " + captchaChallenge.getTypeID() + " - Plugin: " + captchaChallenge.getPlugin());
        if (config.getwhitelistcheck()) {
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (config.getwhitelist().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on whitelist - " + captchaChallenge.getTypeID());
                    } else {
                        setdebug(solverJob, "Hoster not on whitelist - " + captchaChallenge.getTypeID());
                        Thread.sleep(2000);
                        return;
                    }
                }
            }
        }

        if (config.getblacklistcheck()) {
            if (config.getblacklist() != null) {
                if (config.getblacklist().length() > 5) {
                    if (config.getblacklist().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on blacklist - " + captchaChallenge.getTypeID());
                        Thread.sleep(2000);
                        return;
                    } else {
                        setdebug(solverJob, "Hoster not on blacklist - " + captchaChallenge.getTypeID());
                    }
                }
            }
        }

        if (config.getwhitelistprio() != null) {
            if (config.getwhitelistprio().length() > 5) {
                if (config.getwhitelistprio().contains(captchaChallenge.getTypeID())) {
                    setdebug(solverJob, "Hoster on whitelist with prio - " + captchaChallenge.getTypeID());
                } else {
                    setdebug(solverJob, "Hoster not on whitelist with prio - " + captchaChallenge.getTypeID());
                    priothing = 0;
                }
            }
        }

        if (config.getblacklistprio() != null) {
            if (config.getblacklistprio().length() > 5) {
                if (config.getblacklistprio().contains(captchaChallenge.getTypeID())) {
                    priothing = 0;
                    setdebug(solverJob, "Hoster on blacklist with prio - " + captchaChallenge.getTypeID());
                } else {
                    setdebug(solverJob, "Hoster not on blacklist with prio - " + captchaChallenge.getTypeID());
                }
            }
        }

        if (config.getwhitelisttimeoutcheck()) {
            if (config.getwhitelisttimeout() != null) {
                if (config.getwhitelisttimeout().length() > 5) {
                    if (config.getwhitelisttimeout().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on whitelist with other 9kw timeout - " + captchaChallenge.getTypeID());
                        timeoutthing = (config.getCaptchaOther9kwTimeout() / 1000);
                    } else {
                        setdebug(solverJob, "Hoster not on whitelist with other 9kw timeout - " + captchaChallenge.getTypeID());
                    }
                }
            }
        }

        if (config.getblacklisttimeoutcheck()) {
            if (config.getblacklisttimeout() != null) {
                if (config.getblacklisttimeout().length() > 5) {
                    if (config.getblacklisttimeout().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on blacklist with other 9kw timeout - " + captchaChallenge.getTypeID());
                    } else {
                        timeoutthing = (config.getCaptchaOther9kwTimeout() / 1000);
                        setdebug(solverJob, "Hoster not on blacklist with other 9kw timeout - " + captchaChallenge.getTypeID());
                    }
                }
            }
        }

        if (config.getmaxcaptcha() == true) {
            if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().captcha_map9kw.containsKey(captchaChallenge.getDownloadLink())) {
                if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().captcha_map9kw.get(captchaChallenge.getDownloadLink()) >= config.getmaxcaptchaperdl()) {
                    setdebug(solverJob, "Link to BlacklistByLink: " + captchaChallenge.getDownloadLink().getPluginPatternMatcher() + "\n");
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(captchaChallenge.getDownloadLink()));
                    return;
                } else {
                    setdebug(solverJob, "BlacklistByLink +1: " + captchaChallenge.getDownloadLink().getPluginPatternMatcher() + "\n");
                    org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().captcha_map9kw.put(captchaChallenge.getDownloadLink(), org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().captcha_map9kw.get(captchaChallenge.getDownloadLink()) + 1);
                }
            } else {
                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().captcha_map9kw.put(captchaChallenge.getDownloadLink(), 1);
            }
        }

        boolean badfeedbackstemp = config.getbadfeedbacks();
        if (badfeedbackstemp == true && config.isfeedback() == true) {
            if (click9kw_counterNotOK.get() > 10 && click9kw_counterSend.get() > 10 && click9kw_counterSolved.get() > 10 && click9kw_counter.get() > 10 || click9kw_counterOK.get() < 10 && click9kw_counterNotOK.get() > 10 && click9kw_counterSolved.get() > 10 && click9kw_counter.get() > 10) {
                if ((click9kw_counterNotOK.get() / click9kw_counter.get() * 100) > 30 || click9kw_counterOK.get() < 10 && click9kw_counterNotOK.get() > 10 && click9kw_counterSolved.get() > 10 && click9kw_counter.get() > 10) {
                    setdebug(solverJob, "Too many bad feedbacks like 30% captchas with NotOK. - " + "OK: " + click9kw_counterOK.get() + " NotOK: " + click9kw_counterNotOK.get() + " Solved: " + click9kw_counterSolved.get() + " All: " + click9kw_counter.get());
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime1.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime1.get()) > 300) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime1.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_badfeedback_errortext() + "\n\n" + "OK: " + click9kw_counterOK.get() + "\nNotOK: " + click9kw_counterNotOK.get() + "\nSolved: " + click9kw_counterSolved.get() + "\nAll: " + click9kw_counter.get());
                    }
                    return;
                }
            }
        }

        boolean badnofeedbackstemp = config.getbadnofeedbacks();
        if (badnofeedbackstemp == true && config.isfeedback() == true) {
            if (click9kw_counterSend.get() > 10 && click9kw_counter.get() > 10) {
                if (((click9kw_counterOK.get() + click9kw_counterNotOK.get() + click9kw_counterInterrupted.get()) / click9kw_counter.get() * 100) < 50) {
                    setdebug(solverJob, "Too many captchas without feedbacks like OK or NotOK. - " + "OK: " + click9kw_counterOK.get() + " NotOK: " + click9kw_counterNotOK.get() + " Solved: " + click9kw_counterSolved.get() + " All: " + click9kw_counter.get());
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime2.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime2.get()) > 300) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime2.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_badfeedback_errortext() + "\n\n" + "OK: " + click9kw_counterOK.get() + "\nNotOK: " + click9kw_counterNotOK.get() + "\nSolved: " + click9kw_counterSolved.get() + "\nAll: " + click9kw_counter.get());
                    }
                    // return;
                }
            }
        }

        boolean getbadtimeouttemp = config.getbadtimeout();
        if (getbadtimeouttemp == true) {
            if (click9kw_counterSend.get() > 5 && click9kw_counter.get() > 5 && click9kw_counterSolved.get() > 5) {
                if (priothing == 0 && (config.getDefaultTimeout() / 1000) < 90) {
                    setdebug(solverJob, "Your max. timeout for 9kw.eu is really low. - " + "Timeout: " + (config.getDefaultTimeout() / 1000) + "s");
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime3.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime3.get()) > 300) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime3.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_badtimeout_errortext() + "\n");
                    }
                    return;
                } else if ((config.getDefaultTimeout() / 1000) < (config.getCaptchaOther9kwTimeout() / 1000)) {
                    setdebug(solverJob, "Othertimeout as second max. timeout from the black-/whitelist is higher than your default timeout. - " + "Timeout: " + (config.getDefaultTimeout() / 1000) + "s" + " - OtherTimeout: " + (config.getCaptchaOther9kwTimeout() / 1000) + "s");
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime3.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime3.get()) > 300) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime3.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_baderrorsanduploads_errortext() + "\n");
                    }
                    return;
                }
            }
        }

        boolean getbaderrorsanduploadstemp = config.getbaderrorsanduploads();
        if (getbaderrorsanduploadstemp == true) {
            if (click9kw_counterSendError.get() > 10 || click9kw_counterInterrupted.get() > 10) {
                if (((click9kw_counterSendError.get() + click9kw_counterInterrupted.get()) / click9kw_counter.get() * 100) > 50 || click9kw_counterOK.get() < 10 && click9kw_counterNotOK.get() > 100) {
                    setdebug(solverJob, "You have many send errors or interrupted captchas. - " + "OK: " + click9kw_counterOK.get() + " NotOK: " + click9kw_counterNotOK.get() + " Solved: " + click9kw_counterSolved.get() + " All: " + click9kw_counter.get());
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime4.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime4.get()) > 300) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime4.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_baderrorsanduploads_errortext() + "\n");
                    }
                }
            }
        }

        String moreoptions = "";
        String hosterOptions = config.gethosteroptions();
        if (hosterOptions != null && hosterOptions.length() > 5) {
            String[] list = hosterOptions.split(";");
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
                            if (detailvalue[0].equals("cpm") && detailvalue[1].matches("^[0-9]+$")) {
                                cpm = Integer.parseInt(detailvalue[1]);
                            }
                            if (detailvalue[0].equals("nomd5") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&nomd5=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("ocr") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&ocr=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("min") && detailvalue[1].matches("^[0-9]+$") || detailvalue[0].equals("min_length") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&min_len=" + detailvalue[1];
                            }
                            if (detailvalue[0].equals("max") && detailvalue[1].matches("^[0-9]+$") || detailvalue[0].equals("max_length") && detailvalue[1].matches("^[0-9]+$")) {
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

        boolean check_highqueue = config.gethighqueue();
        if (check_highqueue == true) {
            String servercheck = "";
            try {
                Browser br_short = new Browser();
                servercheck = br_short.getPage(NineKwSolverService.getInstance().getAPIROOT() + "grafik/servercheck.txt");
            } catch (IOException e) {

            }

            if (Integer.parseInt(new Regex(servercheck, "queue=(\\d+)").getMatch(0)) > 100) {
                if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime6.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime6.get()) > config.getDefaultTimeoutNotification()) {
                    org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime6.set((System.currentTimeMillis() / 1000));
                    jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_highqueue_errortext());
                }
            }
        }

        setdebug(solverJob, "Upload Captcha - GetTypeID: " + captchaChallenge.getTypeID() + " - Plugin: " + captchaChallenge.getPlugin());
        try {
            solverJob.showBubble(this, getBubbleTimeout(captchaChallenge));
            click9kw_counter.incrementAndGet();
            solverJob.setStatus(SolverStatus.UPLOADING);
            solverJob.getChallenge().sendStatsSolving(this);
            final byte[] data = IO.readFile(captchaChallenge.getImageFile());
            Browser br = new Browser();
            br.setAllowedResponseCodes(new int[] { 500 });
            String ret = "";
            for (int i = 0; i <= 5; i++) {
                ret = br.postPage(getAPIROOT() + "index.cgi", "action=usercaptchaupload&jd=2&source=jd2" + moreoptions + "&captchaperhour=" + cph + "&captchapermin=" + cpm + "&mouse=1&prio=" + priothing + "&selfsolve=" + selfsolve + "&confirm=" + confirm + "&oldsource=" + Encoding.urlEncode(captchaChallenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&maxtimeout=" + timeoutthing + "&version=1.2&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false)));
                if (ret.startsWith("OK-")) {
                    click9kw_counterSend.incrementAndGet();
                    break;
                } else {
                    setdebug(solverJob, "Upload Captcha(" + i + ") - GetTypeID: " + captchaChallenge.getTypeID() + " - Plugin: " + captchaChallenge.getPlugin());
                    if (ret.contains("0015 Captcha zu schnell eingereicht")) {
                        Thread.sleep(15000);
                    } else {
                        Thread.sleep(5000);
                    }
                }
            }
            solverJob.setStatus(SolverStatus.SOLVING);
            if (!ret.startsWith("OK-")) {
                setdebug(solverJob, "Errormessage - " + ret);
                if (ret.contains("0011 Guthaben ist nicht ausreichend") && config.getlowcredits()) {
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get()) > 30) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocredits() + "\n" + ret);
                    }
                    Thread.sleep(30000);
                } else if (ret.contains("0008 Kein Captcha gefunden")) {
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get()) > 30) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocaptcha() + "\n" + ret);
                    }
                    Thread.sleep(5000);
                } else if (ret.contains("0015 Captcha zu schnell eingereicht")) {
                    Thread.sleep(15000);
                }
                click9kw_counterSendError.incrementAndGet();
                throw new SolverException(ret);
            } else {
                setdebug(solverJob, "Send Captcha - Answer: " + ret);
            }
            // Error-No Credits
            String captchaID = ret.substring(3);
            long startTime = System.currentTimeMillis();

            Thread.sleep(10000);
            while (true) {
                ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&mouse=1&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1");
                if (StringUtils.isEmpty(ret) || ret == "No htmlCode read") {
                    setdebug(solverJob, "CaptchaID " + captchaID + " - NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                } else {
                    setdebug(solverJob, "CaptchaID " + captchaID + " - Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                }
                if (ret.startsWith("OK-answered-ERROR NO USER") || ret.startsWith("ERROR NO USER")) {
                    click9kw_counterInterrupted.incrementAndGet();
                    Thread.sleep(15000);
                    return;
                } else if (ret.startsWith("OK-answered-")) {
                    click9kw_counterSolved.incrementAndGet();
                    String antwort = ret.substring("OK-answered-".length());
                    String[] splitResult = antwort.split("x");

                    solverJob.setAnswer(new Captcha9kwClickResponse(captchaChallenge, this, new ClickedPoint(Integer.parseInt(splitResult[0]), Integer.parseInt(splitResult[1])), 100, captchaID));
                    return;
                } else if (((System.currentTimeMillis() - startTime) / 1000) > (timeoutthing + 10)) {
                    click9kw_counterInterrupted.incrementAndGet();
                    return;
                }

                checkInterruption();
                Thread.sleep(3000);
            }

        } catch (IOException e) {
            solverJob.getChallenge().sendStatsError(this, e);
            setdebug(solverJob, "Interrupted: " + e);
            click9kw_counterInterrupted.incrementAndGet();
            solverJob.getLogger().log(e);
        } finally {

        }

    }

    private int getBubbleTimeout(ClickCaptchaChallenge challenge) {
        HashMap<String, Integer> map = config.getBubbleTimeoutByHostMap();

        Integer ret = map.get(challenge.getHost().toLowerCase(Locale.ENGLISH));
        if (ret == null || ret < 0) {
            ret = CFG_CAPTCHA.CFG.getCaptchaExchangeChanceToSkipBubbleTimeout();
        }
        return ret;
    }

    @Override
    public boolean setValid(final AbstractResponse<?> response) {
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
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("CaptchaID " + captchaID + ": OK (Feedback 1)");
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
            return true;
        }
        return false;
    }

    @Override
    public boolean setUnused(final AbstractResponse<?> response) {
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
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("CaptchaID " + captchaID + ": Unused (Feedback 3)");
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
            return true;
        }
        return false;
    }

    @Override
    public boolean setInvalid(final AbstractResponse<?> response) {
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
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("CaptchaID " + captchaID + ": NotOK (Feedback 2)");
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
            return true;
        }
        return false;
    }

    @Override
    protected boolean validateLogins() {
        return StringUtils.isNotEmpty(config.getApiKey()) && config.ismouse();
    }

    @Override
    protected void solveBasicCaptchaChallenge(CESSolverJob<ClickedPoint> job, BasicCaptchaChallenge challenge) throws SolverException {

        // not used solveCES Overwritten
    }

}
