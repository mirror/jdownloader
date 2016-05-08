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
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByLink;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.seamless.util.io.IO;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;

public class Captcha9kwSolver extends CESChallengeSolver<String> {

    private static final Captcha9kwSolver INSTANCE           = new Captcha9kwSolver();
    private final ThreadPoolExecutor      threadPool         = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());

    AtomicInteger                         counterSolved      = new AtomicInteger();
    AtomicInteger                         counterInterrupted = new AtomicInteger();
    AtomicInteger                         counter            = new AtomicInteger();
    AtomicInteger                         counterSend        = new AtomicInteger();
    AtomicInteger                         counterSendError   = new AtomicInteger();
    AtomicInteger                         counterOK          = new AtomicInteger();
    AtomicInteger                         counterNotOK       = new AtomicInteger();
    AtomicInteger                         counterUnused      = new AtomicInteger();
    AtomicLong                            counterdialogtime1 = new AtomicLong();
    AtomicLong                            counterdialogtime2 = new AtomicLong();
    AtomicLong                            counterdialogtime3 = new AtomicLong();
    AtomicLong                            counterdialogtime4 = new AtomicLong();
    AtomicLong                            counterdialogtime5 = new AtomicLong();
    AtomicLong                            counterdialogtime6 = new AtomicLong();

    private String                        long_debuglog      = "";
    HashMap<DownloadLink, Integer>        captcha_map9kw     = new HashMap<DownloadLink, Integer>();

    private final Captcha9kwSettings      config;

    public static Captcha9kwSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private Captcha9kwSolver() {
        super(NineKwSolverService.getInstance(), Math.max(1, Math.min(25, NineKwSolverService.getInstance().getConfig().getThreadpoolSize())));
        config = NineKwSolverService.getInstance().getConfig();
        NineKwSolverService.getInstance().setTextSolver(this);
        threadPool.allowCoreThreadTimeOut(true);
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (c instanceof RecaptchaV2Challenge || c instanceof AbstractRecaptcha2FallbackChallenge) {

            return true;
        }
        return c instanceof BasicCaptchaChallenge && super.canHandle(c);
    }

    public synchronized void setlong_debuglog(String long_debuglog) {
        this.long_debuglog += long_debuglog + "\n";
    }

    public synchronized void dellong_debuglog() {
        this.long_debuglog = "";
    }

    public synchronized String getlong_debuglog() {
        return this.long_debuglog;
    }

    public void setdebug_short(String logdata) {
        if (config.isDebug() && logdata != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            setlong_debuglog('[' + dateFormat.format(new Date()) + ']' + " " + logdata);
        }
    }

    public void setdebug(CESSolverJob<String> job, String logdata) {
        if (config.isDebug() && logdata != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            setlong_debuglog('[' + dateFormat.format(new Date()) + ']' + " " + logdata);
        }
        job.getLogger().info(logdata);
    }

    private Challenge<?> getChallenge(SolverJob<?> job) {
        final Challenge<?> challenge = job.getChallenge();
        if (challenge instanceof RecaptchaV2Challenge) {
            return ((RecaptchaV2Challenge) challenge).createBasicCaptchaChallenge();
        } else {
            return challenge;
        }
    }

    @Override
    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {
        BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) getChallenge(job.getJob());

        int cph = config.gethour();
        int cpm = config.getminute();
        int priothing = config.getprio();
        long timeoutthing = (config.getDefaultTimeout() / 1000);
        boolean selfsolve = config.isSelfsolve();
        boolean confirm = config.isconfirm();

        if (!config.getApiKey().matches("^[a-zA-Z0-9]+$")) {
            setdebug(job, "API Key is not correct! (Text)");
            if (counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime5.get()) > 30) {
                counterdialogtime5.set((System.currentTimeMillis() / 1000));
                jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_wrongapikey1() + "\n" + _GUI.T.NinekwService_createPanel_errortext_wrongapikey2() + "\n");
                Thread.sleep(30000);
            }
            return;
        }

        setdebug(job, "Config(Text) - Prio: " + priothing + " - Timeout: " + timeoutthing + "s/" + (config.getCaptchaOther9kwTimeout() / 1000) + "s - Parallel: " + config.getThreadpoolSize());
        setdebug(job, "Start Captcha - GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
        if (config.getwhitelistcheck()) {
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (config.getwhitelist().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on whitelist for 9kw.eu. - " + challenge.getTypeID());
                    } else {
                        setdebug(job, "Hoster not on whitelist - " + challenge.getTypeID());
                        Thread.sleep(2000);
                        return;
                    }
                }
            }
        }

        if (config.getblacklistcheck()) {
            if (config.getblacklist() != null) {
                if (config.getblacklist().length() > 5) {
                    if (config.getblacklist().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on blacklist - " + challenge.getTypeID());
                        Thread.sleep(2000);
                        return;
                    } else {
                        setdebug(job, "Hoster not on blacklist - " + challenge.getTypeID());
                    }
                }
            }
        }

        if (config.getwhitelistpriocheck()) {
            if (config.getwhitelistprio() != null) {
                if (config.getwhitelistprio().length() > 5) {
                    if (config.getwhitelistprio().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on whitelist with prio - " + challenge.getTypeID());
                    } else {
                        setdebug(job, "Hoster not on whitelist with prio - " + challenge.getTypeID());
                        priothing = 0;
                    }
                }
            }
        }

        if (config.getblacklistpriocheck()) {
            if (config.getblacklistprio() != null) {
                if (config.getblacklistprio().length() > 5) {
                    if (config.getblacklistprio().contains(challenge.getTypeID())) {
                        priothing = 0;
                        setdebug(job, "Hoster on blacklist with prio - " + challenge.getTypeID());
                    } else {
                        setdebug(job, "Hoster not on blacklist with prio - " + challenge.getTypeID());
                    }
                }
            }
        }

        if (config.getwhitelisttimeoutcheck()) {
            if (config.getwhitelisttimeout() != null) {
                if (config.getwhitelisttimeout().length() > 5) {
                    if (config.getwhitelisttimeout().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on whitelist with other 9kw timeout - " + challenge.getTypeID());
                        timeoutthing = (config.getCaptchaOther9kwTimeout() / 1000);
                    } else {
                        setdebug(job, "Hoster not on whitelist with other 9kw timeout - " + challenge.getTypeID());
                    }
                }
            }
        }

        if (config.getblacklisttimeoutcheck()) {
            if (config.getblacklisttimeout() != null) {
                if (config.getblacklisttimeout().length() > 5) {
                    if (config.getblacklisttimeout().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on blacklist with other 9kw timeout - " + challenge.getTypeID());
                    } else {
                        timeoutthing = (config.getCaptchaOther9kwTimeout() / 1000);
                        setdebug(job, "Hoster not on blacklist with other 9kw timeout - " + challenge.getTypeID());
                    }
                }
            }
        }

        if (config.getmaxcaptcha() == true) {
            if (captcha_map9kw.containsKey(challenge.getDownloadLink())) {
                if (captcha_map9kw.get(challenge.getDownloadLink()) >= config.getmaxcaptchaperdl()) {
                    setdebug(job, "Too many captchas for one link (BlacklistByLink): " + challenge.getDownloadLink().getPluginPatternMatcher());
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(challenge.getDownloadLink()));
                    return;
                } else {
                    setdebug(job, "BlacklistByLink +1: " + challenge.getDownloadLink().getPluginPatternMatcher() + "\n");
                    captcha_map9kw.put(challenge.getDownloadLink(), captcha_map9kw.get(challenge.getDownloadLink()) + 1);
                }
            } else {
                captcha_map9kw.put(challenge.getDownloadLink(), 1);
            }
        }

        boolean badfeedbackstemp = config.getbadfeedbacks();
        if (badfeedbackstemp == true && config.isfeedback() == true) {
            if (counterNotOK.get() > 10 && counterSend.get() > 10 && counterSolved.get() > 10 && counter.get() > 10 || counterOK.get() < 10 && counterNotOK.get() > 10 && counterSolved.get() > 10 && counter.get() > 10) {
                if ((counterNotOK.get() / counter.get() * 100) > 30 || counterOK.get() < 10 && counterNotOK.get() > 10 && counterSolved.get() > 10 && counter.get() > 10) {
                    setdebug(job, "Too many bad feedbacks like 30% captchas with NotOK. - " + "OK: " + counterOK.get() + " NotOK: " + counterNotOK.get() + " Solved: " + counterSolved.get() + " All: " + counter.get());
                    if (counterdialogtime1.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime1.get()) > config.getDefaultTimeoutNotification()) {
                        counterdialogtime1.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_badfeedback_errortext() + "\n\n" + "OK: " + counterOK.get() + "\nNotOK: " + counterNotOK.get() + "\nSolved: " + counterSolved.get() + "\nAll: " + counter.get());
                    }
                    return;
                }
            }
        }

        boolean badnofeedbackstemp = config.getbadnofeedbacks();
        if (badnofeedbackstemp == true && config.isfeedback() == true) {
            if (counterSend.get() > 10 && counter.get() > 10) {
                if (((counterOK.get() + counterNotOK.get() + counterInterrupted.get()) / counter.get() * 100) < 50) {
                    setdebug(job, "Too many captchas without feedbacks like OK or NotOK. - " + "OK: " + counterOK.get() + " NotOK: " + counterNotOK.get() + " Solved: " + counterSolved.get() + " All: " + counter.get());
                    if (counterdialogtime2.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime2.get()) > config.getDefaultTimeoutNotification()) {
                        counterdialogtime2.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_badnofeedback_errortext() + "\n\n" + "OK: " + counterOK.get() + "\nNotOK: " + counterNotOK.get() + "\nSolved: " + counterSolved.get() + "\nAll: " + counter.get());
                    }
                    // return;
                }
            }
        }

        boolean getbadtimeouttemp = config.getbadtimeout();
        if (getbadtimeouttemp == true) {
            if (counterSend.get() > 5 && counter.get() > 5 && counterSolved.get() > 5) {
                if ((config.getDefaultTimeout() / 1000) < 90) {
                    if (priothing == 0) {
                        setdebug(job, "Your max. timeout for 9kw.eu is really low. - " + "Timeout: " + (config.getDefaultTimeout() / 1000) + "s");
                        if (counterdialogtime3.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime3.get()) > config.getDefaultTimeoutNotification()) {
                            counterdialogtime3.set((System.currentTimeMillis() / 1000));
                            jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_badtimeout_errortext() + "\n");
                        }
                        return;
                    }
                } else if ((config.getDefaultTimeout() / 1000) < (config.getCaptchaOther9kwTimeout() / 1000)) {
                    setdebug(job, "Othertimeout as second max. timeout from the black-/whitelist is higher than your default timeout. - " + "Timeout: " + (config.getDefaultTimeout() / 1000) + "s" + " - OtherTimeout: " + (config.getCaptchaOther9kwTimeout() / 1000) + "s");
                    if (counterdialogtime3.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime3.get()) > config.getDefaultTimeoutNotification()) {
                        counterdialogtime3.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_badtimeout_errortext2() + "\n");
                    }
                    return;
                }
            }
        }

        boolean getbaderrorsanduploadstemp = config.getbaderrorsanduploads();
        if (getbaderrorsanduploadstemp == true) {
            if (counterSendError.get() > 10 || counterInterrupted.get() > 10) {
                if (((counterSendError.get() + counterInterrupted.get()) / counter.get() * 100) > 50) {
                    setdebug(job, "You have many send errors or interrupted captchas. - " + "OK: " + counterOK.get() + " NotOK: " + counterNotOK.get() + " Solved: " + counterSolved.get() + " All: " + counter.get());
                    if (counterdialogtime4.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime4.get()) > config.getDefaultTimeoutNotification()) {
                        counterdialogtime4.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_baderrorsanduploads_errortext() + "\n");
                    }
                }
            }
        }

        // for debug
        // counterSolved.set(10);
        // counterSend.set(10);
        // counterNotOK.set(10);
        // counter.set(10);

        String moreoptions = "";
        String hosterOptions = config.gethosteroptions();
        if (hosterOptions != null && hosterOptions.length() > 5) {
            String[] list = hosterOptions.split(";");
            for (String hosterline : list) {
                if (hosterline.contains(challenge.getTypeID())) {
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
                            if (detailvalue[0].equals("nospace") && detailvalue[1].matches("^[0-9]+$")) {
                                moreoptions += "&nospace=" + detailvalue[1];
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
            Browser br_short = new Browser();
            try {
                servercheck = br_short.getPage(NineKwSolverService.getInstance().getAPIROOT() + "grafik/servercheck.txt");
            } catch (IOException e) {
            }

            if (Integer.parseInt(new Regex(servercheck, "queue=(\\d+)").getMatch(0)) > 100) {
                if (counterdialogtime6.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime6.get()) > config.getDefaultTimeoutNotification()) {
                    counterdialogtime6.set((System.currentTimeMillis() / 1000));
                    jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_highqueue_errortext());
                }
            }
        }

        setdebug(job, "Upload Captcha - GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
        try {
            counter.incrementAndGet();
            job.showBubble(this, getBubbleTimeout(challenge));
            checkInterruption();
            job.getChallenge().sendStatsSolving(this);
            final byte[] data;
            if (challenge instanceof AbstractRecaptcha2FallbackChallenge) {
                data = challenge.getAnnotatedImageBytes();
            } else {
                data = IO.readBytes(challenge.getImageFile());
            }
            final Browser br = new Browser();

            br.setAllowedResponseCodes(new int[] { 500 });
            String ret = "";
            job.setStatus(SolverStatus.UPLOADING);
            for (int i = 0; i <= 5; i++) {
                ret = br.postPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi", "action=usercaptchaupload&jd=2&source=jd2" + moreoptions + "&captchaperhour=" + cph + "&captchapermin=" + cpm + "&prio=" + priothing + "&selfsolve=" + selfsolve + "&confirm=" + confirm + "&oldsource=" + Encoding.urlEncode(challenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&maxtimeout=" + timeoutthing + "&version=1.2&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false)));
                LoggerFactory.getDefaultLogger().info("\r\n" + br.getRequest());
                if (ret.startsWith("OK-")) {
                    counterSend.incrementAndGet();
                    break;
                } else {
                    setdebug(job, "Upload Captcha(" + i + ") - GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
                    if (ret.contains("0015 Captcha zu schnell eingereicht")) {
                        Thread.sleep(15000);
                    } else {
                        Thread.sleep(5000);
                    }
                }
            }
            job.setStatus(SolverStatus.SOLVING);
            if (!ret.startsWith("OK-")) {
                setdebug(job, "Errormessage - " + ret);
                if (ret.contains("0011 Guthaben ist nicht ausreichend") && config.getlowcredits()) {
                    if (counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime5.get()) > 30) {
                        counterdialogtime5.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocredits() + "\n" + ret);
                    }
                    Thread.sleep(30000);
                } else if (ret.contains("0008 Kein Captcha gefunden")) {
                    if (counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - counterdialogtime5.get()) > 30) {
                        counterdialogtime5.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocaptcha() + "\n" + ret);
                    }
                    Thread.sleep(5000);
                } else if (ret.contains("0015 Captcha zu schnell eingereicht")) {
                    Thread.sleep(15000);
                }
                counterSendError.incrementAndGet();
                throw new SolverException(ret);
            } else {
                setdebug(job, "Send Captcha - Answer: " + ret);
            }
            // Error-No Credits
            String captchaID = ret.substring(3);
            long startTime = System.currentTimeMillis();

            Thread.sleep(10000);
            while (true) {
                ret = br.getPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1");
                LoggerFactory.getDefaultLogger().info("\r\n" + br.getRequest());
                if (StringUtils.isEmpty(ret) || ret == "No htmlCode read") {
                    setdebug(job, "CaptchaID " + captchaID + " - NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                } else {
                    setdebug(job, "CaptchaID " + captchaID + " - Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                }
                if (ret.startsWith("OK-answered-ERROR NO USER") || ret.startsWith("ERROR NO USER")) {
                    counterInterrupted.incrementAndGet();
                    Thread.sleep(15000);
                    return;
                } else if (ret.startsWith("OK-answered-")) {
                    counterSolved.incrementAndGet();
                    ret = ret.substring("OK-answered-".length());
                    AbstractResponse<String> answer = challenge.parseAPIAnswer(ret, this);

                    job.setAnswer(new Captcha9kwResponse(challenge, this, answer.getValue(), answer.getPriority(), captchaID));

                    return;
                } else if (((System.currentTimeMillis() - startTime) / 1000) > (timeoutthing + 10)) {
                    counterInterrupted.incrementAndGet();
                    return;
                }

                checkInterruption();
                Thread.sleep(3000);
            }

        } catch (IOException e) {
            job.getChallenge().sendStatsError(this, e);
            setdebug_short("Interrupted: " + e);
            counterInterrupted.incrementAndGet();
            job.getLogger().log(e);
        }

    }

    private int getBubbleTimeout(BasicCaptchaChallenge challenge) {
        final HashMap<String, Integer> map = config.getBubbleTimeoutByHostMap();
        Integer ret = map.get(challenge.getHost().toLowerCase(Locale.ENGLISH));
        if (ret == null || ret < 0) {
            ret = CFG_CAPTCHA.CFG.getCaptchaExchangeChanceToSkipBubbleTimeout();
        }
        return ret;
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && config.isEnabledGlobally();
    }

    @Override
    public SolverService getService() {
        return super.getService();
    }

    @Override
    public boolean setValid(final AbstractResponse<?> response) {
        if (config.isfeedback()) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        final String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        final Browser br = new Browser();
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            final String ret = br.getPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=1&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            LoggerFactory.getDefaultLogger().info("\r\n" + br.getRequest());
                            if (ret.startsWith("OK")) {
                                setdebug_short("CaptchaID " + captchaID + ": OK (Feedback 1)");
                                counterOK.incrementAndGet();
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
                        final String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        final Browser br = new Browser();
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            final String ret = br.getPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=3&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            LoggerFactory.getDefaultLogger().info("\r\n" + br.getRequest());
                            if (ret.startsWith("OK")) {
                                setdebug_short("CaptchaID " + captchaID + ": Unused (Feedback 3)");
                                counterUnused.incrementAndGet();
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
                        final String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        final Browser br = new Browser();
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            final String ret = br.getPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=2&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            LoggerFactory.getDefaultLogger().info("\r\n" + br.getRequest());
                            if (ret.startsWith("OK")) {
                                setdebug_short("CaptchaID " + captchaID + ": NotOK (Feedback 2)");
                                counterNotOK.incrementAndGet();
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
        return StringUtils.isNotEmpty(config.getApiKey()) && isEnabled();

    }

    @Override
    protected void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws SolverException {
        // not used. solveCEs is overwritten
    }

}
