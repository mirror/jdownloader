package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Icon;

import jd.SecondLevelLaunch;
import jd.controlling.captcha.CaptchaSettings;
import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class Captcha9kwSolver extends CESChallengeSolver<String> implements ChallengeResponseValidation {
    private Captcha9kwSettings            config;
    private static final Captcha9kwSolver INSTANCE           = new Captcha9kwSolver();
    private ThreadPoolExecutor            threadPool         = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());

    private AtomicInteger                 counterSolved      = new AtomicInteger();
    private AtomicInteger                 counterInterrupted = new AtomicInteger();
    private AtomicInteger                 counter            = new AtomicInteger();
    private AtomicInteger                 counterSend        = new AtomicInteger();
    private AtomicInteger                 counterSendError   = new AtomicInteger();
    private AtomicInteger                 counterOK          = new AtomicInteger();
    private AtomicInteger                 counterNotOK       = new AtomicInteger();
    private AtomicInteger                 counterUnused      = new AtomicInteger();
    private String                        long_debuglog      = "";

    public static Captcha9kwSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private Captcha9kwSolver() {
        super(Math.max(1, Math.min(25, JsonConfig.create(Captcha9kwSettings.class).getThreadpoolSize())));

        config = JsonConfig.create(Captcha9kwSettings.class);
        AdvancedConfigManager.getInstance().register(config);
        threadPool.allowCoreThreadTimeOut(true);

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                ServicePanel.getInstance().addExtender(Captcha9kwSolver.this);
                CFG_9KWCAPTCHA.API_KEY.getEventSender().addListener(new GenericConfigEventListener<String>() {

                    @Override
                    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                        ServicePanel.getInstance().requestUpdate(true);
                    }

                    @Override
                    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
                    }
                });

                CFG_9KWCAPTCHA.ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

                    @Override
                    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                    }

                    @Override
                    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                        ServicePanel.getInstance().requestUpdate(true);
                    }
                });
                CFG_9KWCAPTCHA.MOUSE.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

                    @Override
                    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                    }

                    @Override
                    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                        ServicePanel.getInstance().requestUpdate(true);
                    }
                });
            }

        });

    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return c instanceof BasicCaptchaChallenge && CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED.isEnabled() && config.isEnabled() && super.canHandle(c);
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

    public String getAPIROOT() {
        if (config.ishttps()) {
            return "https://www.9kw.eu/";
        } else {
            return "http://www.9kw.eu/";
        }
    }

    @Override
    public Icon getIcon(int size) {
        return new AbstractIcon(IconKey.ICON_9KW, size);
    }

    public void setdebug_short(String logdata) {
        if (config.isDebug() && logdata != null) {
            setlong_debuglog(logdata);
        }
    }

    public void setdebug(CESSolverJob<String> job, String logdata) {
        if (config.isDebug() && logdata != null) {
            setlong_debuglog(logdata);
        }
        job.getLogger().info(logdata);
    }

    @Override
    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {

        BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();

        int priothing = config.getprio();
        int timeoutthing = (JsonConfig.create(CaptchaSettings.class).getCaptchaDialog9kwTimeout() / 1000);

        setdebug(job, "Start Captcha to 9kw.eu. GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
        if (config.getwhitelistcheck()) {
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (config.getwhitelist().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on whitelist for 9kw.eu. - " + challenge.getTypeID());
                    } else {
                        setdebug(job, "Hoster not on whitelist for 9kw.eu. - " + challenge.getTypeID());
                        return;
                    }
                }
            }
        }

        if (config.getblacklistcheck()) {
            if (config.getblacklist() != null) {
                if (config.getblacklist().length() > 5) {
                    if (config.getblacklist().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on blacklist for 9kw.eu. - " + challenge.getTypeID());
                        return;
                    } else {
                        setdebug(job, "Hoster not on blacklist for 9kw.eu. - " + challenge.getTypeID());
                    }
                }
            }
        }

        if (config.getwhitelistpriocheck()) {
            if (config.getwhitelistprio() != null) {
                if (config.getwhitelistprio().length() > 5) {
                    if (config.getwhitelistprio().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on whitelist with prio for 9kw.eu. - " + challenge.getTypeID());
                    } else {
                        setdebug(job, "Hoster not on whitelist with prio for 9kw.eu. - " + challenge.getTypeID());
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
                        setdebug(job, "Hoster on blacklist with prio for 9kw.eu. - " + challenge.getTypeID());
                    } else {
                        setdebug(job, "Hoster not on blacklist with prio for 9kw.eu. - " + challenge.getTypeID());
                    }
                }
            }
        }

        if (config.getwhitelisttimeoutcheck()) {
            if (config.getwhitelisttimeout() != null) {
                if (config.getwhitelisttimeout().length() > 5) {
                    if (config.getwhitelisttimeout().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on whitelist with other 9kw timeout for 9kw.eu. - " + challenge.getTypeID());
                        timeoutthing = (config.getCaptchaOther9kwTimeout() / 1000);
                    } else {
                        setdebug(job, "Hoster not on whitelist with other 9kw timeout for 9kw.eu. - " + challenge.getTypeID());
                    }
                }
            }
        }

        if (config.getblacklisttimeoutcheck()) {
            if (config.getblacklisttimeout() != null) {
                if (config.getblacklisttimeout().length() > 5) {
                    if (config.getblacklisttimeout().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on blacklist with other 9kw timeout for 9kw.eu. - " + challenge.getTypeID());
                    } else {
                        timeoutthing = (config.getCaptchaOther9kwTimeout() / 1000);
                        setdebug(job, "Hoster not on blacklist with other 9kw timeout for 9kw.eu. - " + challenge.getTypeID());
                    }
                }
            }
        }

        try {
            counter.incrementAndGet();
            job.showBubble(this);
            checkInterruption();

            byte[] data = IO.readFile(challenge.getImageFile());
            Browser br = new Browser();
            br.setAllowedResponseCodes(new int[] { 500 });
            String ret = "";
            job.setStatus(SolverStatus.UPLOADING);
            for (int i = 0; i <= 5; i++) {
                ret = br.postPage(getAPIROOT() + "index.cgi", "action=usercaptchaupload&jd=2&source=jd2&captchaperhour=" + config.gethour() + "&prio=" + priothing + "&selfsolve=" + config.isSelfsolve() + "&confirm=" + config.isconfirm() + "&oldsource=" + Encoding.urlEncode(challenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&maxtimeout=" + timeoutthing + "&version=1.2&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false)));
                if (ret.startsWith("OK-")) {
                    counterSend.incrementAndGet();
                    break;
                } else {
                    Thread.sleep(3000);

                }
            }
            job.setStatus(SolverStatus.SOLVING);
            setdebug(job, "Send Captcha to 9kw.eu. - Answer: " + ret);
            if (!ret.startsWith("OK-")) {
                if (ret.contains("0011 Guthaben ist nicht ausreichend") && config.getlowcredits()) {
                    jd.gui.UserIO.getInstance().requestMessageDialog("9kw error ", "0011 Not enough credits.\n" + ret);
                }
                counterSendError.incrementAndGet();
                throw new SolverException(ret);
            }
            // Error-No Credits
            String captchaID = ret.substring(3);
            data = null;
            long startTime = System.currentTimeMillis();

            Thread.sleep(5000);
            while (true) {
                setdebug(job, "9kw.eu Ask " + captchaID);
                ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1");
                if (StringUtils.isEmpty(ret)) {
                    setdebug(job, "9kw.eu NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                } else {
                    setdebug(job, "9kw.eu Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                }
                if (ret.startsWith("OK-answered-")) {
                    counterSolved.incrementAndGet();
                    job.setAnswer(new Captcha9kwResponse(challenge, this, ret.substring("OK-answered-".length()), 100, captchaID));
                    return;
                }

                checkInterruption();
                Thread.sleep(3000);
            }

        } catch (IOException e) {
            setdebug_short("9kw.eu Interrupted: " + e);
            counterInterrupted.incrementAndGet();
            job.getLogger().log(e);
        }

    }

    @Override
    public void setValid(final AbstractResponse<?> response) {
        if (config.isfeedback()) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        String ret = "";
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=1&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                setdebug_short("9kw.eu CaptchaID " + captchaID + ": OK (Feedback)");
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
        }
    }

    @Override
    public void setUnused(final AbstractResponse<?> response) {
        if (config.isfeedback()) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        String ret = "";
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=3&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                setdebug_short("9kw.eu CaptchaID " + captchaID + ": Unused (Feedback)");
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
        }
    }

    @Override
    public void setInvalid(final AbstractResponse<?> response) {
        if (config.isfeedback()) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        String ret = "";
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=2&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                setdebug_short("9kw.eu CaptchaID " + captchaID + ": NotOK (Feedback)");
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
        }
    }

    @Override
    public String getName() {
        return "9kw.eu";
    }

    @Override
    public void extendServicePabel(LinkedList<ServiceCollection<?>> services) {
        if (StringUtils.isNotEmpty(config.getApiKey())) {
            services.add(new ServiceCollection<Captcha9kwSolver>() {

                /**
                 * 
                 */
                private static final long serialVersionUID = 5569965026755271172L;

                @Override
                public Icon getIcon() {
                    return new AbstractIcon(IconKey.ICON_9KW, 18);
                }

                @Override
                public boolean isEnabled() {
                    return config.isEnabled() || config.ismouse();
                }

                @Override
                protected long getLastActiveTimestamp() {
                    return System.currentTimeMillis();
                }

                @Override
                protected String getName() {
                    return "9kw.eu";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new ServicePanel9kwTooltip(owner, Captcha9kwSolver.this);
                }

            });
        }
    }

    public NineKWAccount loadAccount() throws IOException {
        Browser br = new Browser();
        NineKWAccount ret = new NineKWAccount();
        String credits;

        ret.setRequests(counter.get() + org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick.getInstance().click9kw_counter.get());
        ret.setSkipped(counterInterrupted.get() + org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick.getInstance().click9kw_counterInterrupted.get());
        ret.setSolved(counterSolved.get() + org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick.getInstance().click9kw_counterSolved.get());

        ret.setSend(counterSend.get() + org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick.getInstance().click9kw_counterSend.get());
        ret.setSendError(counterSendError.get() + org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick.getInstance().click9kw_counterSendError.get());
        ret.setOK(counterOK.get() + org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick.getInstance().click9kw_counterOK.get());
        ret.setNotOK(counterNotOK.get() + org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick.getInstance().click9kw_counterNotOK.get());
        ret.setUnused(counterUnused.get() + org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick.getInstance().click9kw_counterUnused.get());

        try {
            String servercheck = br.getPage(getAPIROOT() + "index.cgi?action=userservercheck");
            ret.setWorker(Integer.parseInt(new Regex(servercheck, "worker=(\\d+)").getMatch(0)));
            ret.setAvgSolvtime(Integer.parseInt(new Regex(servercheck, "avg1h=(\\d+)").getMatch(0)));
            ret.setQueue(Integer.parseInt(new Regex(servercheck, "queue=(\\d+)").getMatch(0)));
            ret.setQueue1(Integer.parseInt(new Regex(servercheck, "queue1=(\\d+)").getMatch(0)));
            ret.setQueue2(Integer.parseInt(new Regex(servercheck, "queue2=(\\d+)").getMatch(0)));
            ret.setInWork(Integer.parseInt(new Regex(servercheck, "inwork=(\\d+)").getMatch(0)));
            ret.setWorkerMouse(Integer.parseInt(new Regex(servercheck, "workermouse=(\\d+)").getMatch(0)));
            ret.setWorkerConfirm(Integer.parseInt(new Regex(servercheck, "workerconfirm=(\\d+)").getMatch(0)));
            ret.setWorkerText(Integer.parseInt(new Regex(servercheck, "workertext=(\\d+)").getMatch(0)));
        } catch (NumberFormatException e) {
            ret.setError("API Error!");
        }

        credits = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchaguthaben&apikey=" + Encoding.urlEncode(config.getApiKey()));

        try {
            ret.setCreditBalance(Integer.parseInt(credits.trim()));
            String userhistory1 = br.getPage(getAPIROOT() + "index.cgi?action=userhistory&short=1&apikey=" + Encoding.urlEncode(config.getApiKey()));
            String userhistory2 = br.getPage(getAPIROOT() + "index.cgi?action=userhistory2&short=1&apikey=" + Encoding.urlEncode(config.getApiKey()));

            ret.setAnswered9kw(Integer.parseInt(Regex.getLines(userhistory2)[0]));
            ret.setSolved9kw(Integer.parseInt(Regex.getLines(userhistory1)[0]));
        } catch (NumberFormatException e) {
            ret.setError(credits);
        }
        return ret;

    }

    @Override
    protected boolean validateLogins() {
        return StringUtils.isNotEmpty(config.getApiKey()) && config.isEnabled();

    }
}
