package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public abstract class AbstractCaptcha9kwSolver<T> extends CESChallengeSolver<T> {
    private String                     accountStatusString;
    protected final Captcha9kwSettings config;
    AtomicInteger                      counter            = new AtomicInteger();
    AtomicInteger                      counterInterrupted = new AtomicInteger();
    AtomicInteger                      counterNotOK       = new AtomicInteger();
    AtomicInteger                      counterOK          = new AtomicInteger();
    AtomicInteger                      counterSend        = new AtomicInteger();
    AtomicInteger                      counterSendError   = new AtomicInteger();
    AtomicInteger                      counterSolved      = new AtomicInteger();
    AtomicInteger                      counterUnused      = new AtomicInteger();
    private volatile NineKWAccount     lastAccount        = null;
    private String                     long_debuglog      = "";

    public AbstractCaptcha9kwSolver() {
        super(NineKwSolverService.getInstance(), Math.max(1, Math.min(25, NineKwSolverService.getInstance().getConfig().getThreadpoolSize())));
        config = NineKwSolverService.getInstance().getConfig();
        threadPool.allowCoreThreadTimeOut(true);
    }

    protected Challenge<T> getChallenge(SolverJob<?> job) throws SolverException {
        final Challenge<?> challenge = job.getChallenge();
        return (Challenge<T>) challenge;
    }

    protected Challenge<T> getChallenge(CESSolverJob<?> solverJob) throws SolverException {
        return getChallenge(solverJob.getJob());
    }

    protected void checkForEnoughCredits() throws SolverException {
        final NineKWAccount lLastAccount = lastAccount;
        if (lLastAccount != null) {
            // valid cached account
            if (StringUtils.equals(config.getApiKey(), lLastAccount.getUser())) {
                // user did not change
                if ((System.currentTimeMillis() - lLastAccount.getCreateTime()) < 5 * 60 * 1000l) {
                    // cache is not older than 5 minutes
                    if (lLastAccount.getCreditBalance() < 10) {
                        if (config.getlowcredits()) {
                            showMessageAndQuit(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocredits());
                        }
                        throw new SolverException("Not Enough Credits for Task");
                    }
                    if (lLastAccount.getError() != null) {
                        throw new SolverException("9kw.eu: " + lLastAccount.getError());
                    }
                }
            }
        }
    }

    public synchronized void dellong_debuglog() {
        this.long_debuglog = "";
    }

    @Override
    public String getAccountStatusString() {
        return accountStatusString;
    }

    protected int getBubbleTimeout(Challenge<?> challenge) {
        final HashMap<String, Integer> map = config.getBubbleTimeoutByHostMap();
        Integer ret = map.get(challenge.getHost().toLowerCase(Locale.ENGLISH));
        if (ret == null || ret < 0) {
            ret = CFG_CAPTCHA.CFG.getCaptchaExchangeChanceToSkipBubbleTimeout();
        }
        return ret;
    }

    public synchronized String getlong_debuglog() {
        return this.long_debuglog;
    }

    @Override
    public SolverService getService() {
        return super.getService();
    }

    public String getAPIROOT() {
        if (config.ishttps()) {
            return "https://www.9kw.eu/";
        } else {
            return "http://www.9kw.eu/";
        }
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && config.isEnabledGlobally();
    }

    public NineKWAccount loadAccount() {
        final NineKWAccount ret = new NineKWAccount();
        ret.setRequests(counter.get());
        ret.setSkipped(counterInterrupted.get());
        ret.setSolved(counterSolved.get());
        ret.setUser(config.getApiKey());
        try {
            final Browser br = new Browser();
            br.setDebug(true);
            br.setVerbose(true);
            String result = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchaguthaben&cbh=1&apikey=" + Encoding.urlEncode(config.getApiKey()));
            if (result.startsWith("OK-")) {
                String balance = result.substring(3);
                balance = balance.replace(".-", "");
                ret.setCreditBalance(Float.valueOf(balance).intValue());
            } else {
                ret.setError(result);
            }
        } catch (Exception e) {
            ret.setError(e.getMessage());
        } finally {
            if (StringUtils.isEmpty(ret.getError())) {
                accountStatusString = ret.getCreditBalance() + " Credits";
            } else {
                accountStatusString = ret.getError();
            }
        }
        lastAccount = ret;
        return ret;
    }

    public void setdebug(CESSolverJob<T> job, String logdata) {
        if (config.isDebug() && logdata != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            setlong_debuglog('[' + dateFormat.format(new Date()) + ']' + " " + logdata);
        }
        job.getLogger().info(logdata);
    }

    public void setdebug_short(String logdata) {
        if (config.isDebug() && logdata != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            setlong_debuglog('[' + dateFormat.format(new Date()) + ']' + " " + logdata);
        }
    }

    @Override
    public boolean setInvalid(final AbstractResponse<?> response) {
        if (config.isfeedback() && response instanceof Captcha9KWResponseInterface) {
            final String captchaID = ((Captcha9KWResponseInterface) response).getCaptcha9kwID();
            setFeedback(ResponseFeedback.INVALID, captchaID);
            return true;
        } else {
            return false;
        }
    }

    public synchronized void setlong_debuglog(String long_debuglog) {
        this.long_debuglog += long_debuglog + "\n";
    }

    protected static enum ResponseFeedback {
        UNUSED(3),
        VALID(1),
        INVALID(2);
        protected final int code;

        private ResponseFeedback(int code) {
            this.code = code;
        }
    }

    @Override
    public boolean setUnused(final AbstractResponse<?> response) {
        if (config.isfeedback() && response instanceof Captcha9KWResponseInterface) {
            final String captchaID = ((Captcha9KWResponseInterface) response).getCaptcha9kwID();
            setFeedback(ResponseFeedback.UNUSED, captchaID);
            return true;
        } else {
            return false;
        }
    }

    protected void setFeedback(final ResponseFeedback feedback, final String captchaID) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Browser br = new Browser();
                    br.setAllowedResponseCodes(new int[] { 500 });
                    for (int i = 0; i <= 3; i++) {
                        final String ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=" + feedback.code + "&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                        LoggerFactory.getDefaultLogger().info("\r\n" + br.getRequest());
                        if (ret.startsWith("OK")) {
                            setdebug_short("CaptchaID " + captchaID + ":" + feedback.name());
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

    @Override
    public boolean setValid(final AbstractResponse<?> response) {
        if (config.isfeedback() && response instanceof Captcha9KWResponseInterface) {
            final String captchaID = ((Captcha9KWResponseInterface) response).getCaptcha9kwID();
            setFeedback(ResponseFeedback.VALID, captchaID);
            return true;
        } else {
            return false;
        }
    }

    protected void showMessageAndQuit(String title, String msg) throws SolverException {
        MessageDialogImpl d = new MessageDialogImpl(0, title, msg, new AbstractIcon(IconKey.ICON_LOGO_9KW, 32), null);
        UIOManager.I().show(MessageDialogInterface.class, d);
        throw new SolverException(title);
    }

    protected UrlQuery createQueryForPolling() {
        UrlQuery queryPoll = new UrlQuery().appendEncoded("action", "usercaptchacorrectdata");
        queryPoll.appendEncoded("jd", "2");
        queryPoll.appendEncoded("source", "jd2");
        queryPoll.appendEncoded("apikey", config.getApiKey());
        queryPoll.appendEncoded("version", "1.1");
        return queryPoll;
    }

    protected void poll(Browser br, RequestOptions options, CESSolverJob<T> solverJob, String captchaID, UrlQuery queryPoll) throws InterruptedException, IOException, SolverException {
        final long startTime = System.currentTimeMillis();
        boolean setUnused = true;
        try {
            Thread.sleep(10000);
            final Challenge<T> captchaChallenge = getChallenge(solverJob);
            queryPoll.appendEncoded("id", captchaID);
            while (true) {
                final String ret = br.getPage(getAPIROOT() + "index.cgi?" + queryPoll.toString());
                if (StringUtils.isEmpty(ret) || ret == "No htmlCode read") {
                    setdebug(solverJob, "CaptchaID " + captchaID + " - NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                } else {
                    setdebug(solverJob, "CaptchaID " + captchaID + " - Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                }
                if (ret.startsWith("OK-answered-ERROR NO USER") || ret.startsWith("ERROR NO USER")) {
                    counterInterrupted.incrementAndGet();
                    Thread.sleep(15000);
                    return;
                } else if (ret.startsWith("OK-answered-")) {
                    counterSolved.incrementAndGet();
                    String antwort = ret.substring("OK-answered-".length());
                    parseResponse(solverJob, captchaChallenge, captchaID, antwort);
                    setUnused = false;
                    return;
                } else if (((System.currentTimeMillis() - startTime) / 1000) > (options.getTimeoutthing() + 10)) {
                    counterInterrupted.incrementAndGet();
                    return;
                } else if (ret.matches("\\d\\d\\d\\d .*")) {
                    // throw new SolverException(ret);
                }
                checkInterruption();
                Thread.sleep(2000);
            }
        } finally {
            if (setUnused) {
                setFeedback(ResponseFeedback.UNUSED, captchaID);
            }
        }
    }

    abstract protected void parseResponse(CESSolverJob<T> solverJob, Challenge<T> captchaChallenge, String captchaID, String antwort) throws IOException;

    protected String upload(Browser br, CESSolverJob<T> solverJob, UrlQuery qi) throws InterruptedException, IOException, SolverException {
        String ret = "";
        Challenge<T> captchaChallenge = getChallenge(solverJob);
        setdebug(solverJob, "Upload Captcha - GetTypeID: " + captchaChallenge.getTypeID() + " - Plugin: " + captchaChallenge.getPlugin());
        solverJob.showBubble(this, getBubbleTimeout(captchaChallenge));
        counter.incrementAndGet();
        solverJob.setStatus(SolverStatus.UPLOADING);
        captchaChallenge.sendStatsSolving(this);
        for (int i = 0; i <= 5; i++) {
            ret = br.postPage(getAPIROOT() + "index.cgi", qi);
            if (ret.startsWith("OK-")) {
                counterSend.incrementAndGet();
                break;
            } else {
                setdebug(solverJob, "Upload Captcha(" + i + ") - GetTypeID: " + captchaChallenge.getTypeID() + " - Plugin: " + captchaChallenge.getPlugin());
                if (ret.contains("0015 Captcha zu schnell eingereicht")) {
                    Thread.sleep(15000);
                } else if (ret.contains("0009 Kein Bild gefunden") || ret.contains("0008 Kein Captcha gefunden") || ret.matches("0010 .*")) {
                    break;
                } else {
                    Thread.sleep(5000);
                }
            }
        }
        solverJob.setStatus(SolverStatus.SOLVING);
        if (!ret.startsWith("OK-")) {
            setdebug(solverJob, "Errormessage - " + ret);
            if (ret.contains("0011 Guthaben ist nicht ausreichend") && config.getlowcredits()) {
                showMessageAndQuit(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocredits() + "\n" + ret);
            } else if (ret.contains("0008 Kein Captcha gefunden") || ret.contains("0009 Kein Bild gefunden") || ret.matches("0010 .*")) {
                showMessageAndQuit(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocaptcha() + "\n" + ret);
            } else if (ret.contains("0015 Captcha zu schnell eingereicht")) {
                Thread.sleep(15000);
            }
            counterSendError.incrementAndGet();
            throw new SolverException(ret);
        } else {
            setdebug(solverJob, "Send Captcha - Answer: " + ret);
        }
        return ret.substring(3);
    }

    protected UrlQuery createQueryForUpload(CESSolverJob<T> job, RequestOptions options, final byte[] data) throws SolverException {
        UrlQuery qi = new UrlQuery();
        qi.appendEncoded("action", "usercaptchaupload");
        qi.appendEncoded("jd", "2");
        qi.appendEncoded("source", "jd2");
        qi.appendEncoded("captchaperhour", options.getCph() + "");
        qi.appendEncoded("captchapermin", options.getCpm() + "");
        qi.appendEncoded("prio", options.getPriothing() + "");
        qi.appendEncoded("selfsolve", options.isSelfsolve() + "");
        qi.appendEncoded("confirm", options.isConfirm() + "");
        qi.appendEncoded("maxtimeout", options.getTimeoutthing() + "");
        qi.addAll(options.getMoreoptions().list());
        qi.appendEncoded("oldsource", getChallenge(job).getTypeID() + "");
        qi.appendEncoded("apikey", config.getApiKey() + "");
        qi.appendEncoded("captchaSource", "jdPlugin");
        qi.appendEncoded("version", "1.2");
        qi.appendEncoded("base64", "1");
        if (data != null) {
            qi.appendEncoded("file-upload-01", org.appwork.utils.encoding.Base64.encodeToString(data, false));
        }
        return qi;
    }

    protected RequestOptions prepare(CESSolverJob<T> solverJob) throws SolverException, InterruptedException {
        RequestOptions options = new RequestOptions(config);
        Challenge<T> captchaChallenge = getChallenge(solverJob);
        validateApiKey(solverJob);
        setdebug(solverJob, "Config - Prio: " + options.getPriothing() + " - Timeout: " + options.getTimeoutthing() + "s - Parallel: " + config.getThreadpoolSize());
        setdebug(solverJob, "Start Captcha - GetTypeID: " + captchaChallenge.getTypeID() + " - Plugin: " + captchaChallenge.getPlugin());
        if (config.getwhitelistcheck()) {
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (config.getwhitelist().contains(captchaChallenge.getTypeID())) {
                        setdebug(solverJob, "Hoster on whitelist - " + captchaChallenge.getTypeID());
                    } else {
                        setdebug(solverJob, "Hoster not on whitelist - " + captchaChallenge.getTypeID());
                        Thread.sleep(2000);
                        throw new SolverException("Hoster is NOT on whitelist");
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
                        throw new SolverException("Hoster is on blacklist");
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
                    options.setPriothing(0);
                }
            }
        }
        if (config.getblacklistprio() != null) {
            if (config.getblacklistprio().length() > 5) {
                if (config.getblacklistprio().contains(captchaChallenge.getTypeID())) {
                    options.setPriothing(0);
                    setdebug(solverJob, "Hoster on blacklist with prio - " + captchaChallenge.getTypeID());
                } else {
                    setdebug(solverJob, "Hoster not on blacklist with prio - " + captchaChallenge.getTypeID());
                }
            }
        }
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
                                options.setTimeoutthing(Integer.parseInt(detailvalue[1]));
                            }
                            if (detailvalue[0].equals("prio") && detailvalue[1].matches("^[0-9]+$")) {
                                options.setPriothing(Integer.parseInt(detailvalue[1]));
                            }
                            if (detailvalue[0].equals("cph") && detailvalue[1].matches("^[0-9]+$")) {
                                options.setCph(Integer.parseInt(detailvalue[1]));
                            }
                            if (detailvalue[0].equals("cpm") && detailvalue[1].matches("^[0-9]+$")) {
                                options.setCpm(Integer.parseInt(detailvalue[1]));
                            }
                            if (detailvalue[0].equals("nomd5") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("nomd5", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("ocr") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("ocr", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("nospace") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("nospace", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("min") && detailvalue[1].matches("^[0-9]+$") || detailvalue[0].equals("min_length") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("min_len", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("max") && detailvalue[1].matches("^[0-9]+$") || detailvalue[0].equals("max_length") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("max_len", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("phrase") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("phrase", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("math") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("math", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("proxy")) {
                                options.getMoreoptions().appendEncoded("proxy", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("proxytype")) {
                                options.getMoreoptions().appendEncoded("proxytype", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("numeric") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("numeric", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("case-sensitive") && detailvalue[1].matches("^[0-9]+$")) {
                                options.getMoreoptions().appendEncoded("case-sensitive", detailvalue[1]);
                            }
                            if (detailvalue[0].equals("confirm") && detailvalue[1].matches("^[0-9]+$")) {
                                options.setConfirm(detailvalue[1].equals("1"));
                                options.getMoreoptions().appendEncoded("userconfirm", "1");
                            }
                            if (detailvalue[0].equals("selfsolve") && detailvalue[1].matches("^[0-9]+$")) {
                                options.setSelfsolve(detailvalue[1].equals("1"));
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
                showMessageAndQuit(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_notification_highqueue_errortext());
            }
        }
        return options;
    }

    @Override
    protected void solveBasicCaptchaChallenge(CESSolverJob<T> job, BasicCaptchaChallenge challenge) throws SolverException {
        // not used. solveCEs is overwritten
    }

    protected void validateApiKey(CESSolverJob<T> job) throws SolverException {
        if (!config.getApiKey().matches("^[a-zA-Z0-9]+$")) {
            setdebug(job, "API Key is not correct! (Text)");
            showMessageAndQuit(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_wrongapikey1() + "\n" + _GUI.T.NinekwService_createPanel_errortext_wrongapikey2());
        }
    }

    protected boolean validateLogins() {
        return StringUtils.isNotEmpty(config.getApiKey()) && isEnabled();
    }
}
