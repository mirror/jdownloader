package org.jdownloader.captcha.v2.solver.solver9kw;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaImages;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleChallenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaResponse;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;

public class Captcha9kwSolverPuzzle extends CESChallengeSolver<String> {

    private static final Captcha9kwSolverPuzzle INSTANCE   = new Captcha9kwSolverPuzzle();
    private ThreadPoolExecutor                  threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());
    private Captcha9kwSettings                  config;
    private LinkedList<Integer>                 mouseArray = new LinkedList<Integer>();

    public static Captcha9kwSolverPuzzle getInstance() {
        return INSTANCE;
    }

    @Override
    protected void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws SolverException {

        // not used solveCES Overwritten
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private Captcha9kwSolverPuzzle() {
        super(NineKwSolverService.getInstance(), Math.max(1, Math.min(25, NineKwSolverService.getInstance().getConfig().getThreadpoolSize())));
        config = NineKwSolverService.getInstance().getConfig();
        NineKwSolverService.getInstance().setPuzzleSolver(this);
        threadPool.allowCoreThreadTimeOut(true);
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return c instanceof KeyCaptchaPuzzleChallenge && super.canHandle(c);
    }

    public void setdebug(CESSolverJob<String> job, String logdata) {
        if (config.isDebug() && logdata != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setlong_debuglog('[' + dateFormat.format(new Date()) + ']' + " " + logdata);
        }
        job.getLogger().info(logdata);
    }

    @Override
    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {
        KeyCaptchaPuzzleChallenge challenge = (KeyCaptchaPuzzleChallenge) job.getChallenge();

        int cph = config.gethour();
        int cpm = config.getminute();
        int priothing = config.getprio();
        long timeoutthing = (config.getDefaultTimeout() / 1000);
        boolean selfsolve = config.isSelfsolve();
        boolean confirm = config.isconfirm();

        if (!config.getApiKey().matches("^[a-zA-Z0-9]+$")) {
            setdebug(job, "API Key is not correct! (Puzzle)");
            if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get()) > 30) {
                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.set((System.currentTimeMillis() / 1000));
                jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_wrongapikey1() + "\n" + _GUI.T.NinekwService_createPanel_errortext_wrongapikey2() + "\n");
                Thread.sleep(30000);
            }
            return;
        }

        setdebug(job, "Config(Puzzle) - Prio: " + priothing + " - Timeout: " + timeoutthing + "s/" + (config.getCaptchaOther9kwTimeout() / 1000) + "s - Parallel: " + config.getThreadpoolSize());
        setdebug(job, "Start Captcha - GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
        if (config.getwhitelistcheck()) {
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (config.getwhitelist().contains(challenge.getTypeID())) {
                        setdebug(job, "Hoster on whitelist - " + challenge.getTypeID());
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

        setdebug(job, "Upload Captcha - GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
        try {
            org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counter.incrementAndGet();
            job.showBubble(this);
            checkInterruption();
            job.getChallenge().sendStatsSolving(this);
            KeyCaptchaImages images = challenge.getHelper().getPuzzleData().getImages();
            LinkedList<BufferedImage> piecesAll = new LinkedList<BufferedImage>(images.pieces);

            String allfiledata = "";
            for (int c = 0; c < piecesAll.size(); c++) {
                BufferedImage image = piecesAll.get(c);
                byte[] data = getBytesKeyCaptcha(image);

                // special for 9kw.eu with 3 or more images
                int x = c + 1;
                allfiledata += "&file-upload-0" + x + "=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false));
                data = null;
            }

            Browser br = new Browser();
            br.setAllowedResponseCodes(new int[] { 500 });
            String ret = "";
            job.setStatus(SolverStatus.UPLOADING);
            for (int i = 0; i <= 5; i++) {
                ret = br.postPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi", "action=usercaptchaupload&puzzle=1&jd=2&source=jd2" + moreoptions + "&captchaperhour=" + cph + "&captchapermin=" + cpm + "&prio=" + priothing + "&selfsolve=" + selfsolve + "&confirm=" + confirm + "&oldsource=" + Encoding.urlEncode(challenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&maxtimeout=" + timeoutthing + "&version=1.2&base64=1" + allfiledata);
                if (ret.startsWith("OK-")) {
                    org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterSend.incrementAndGet();
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
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get()) > 30) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocredits() + "\n" + ret);
                    }
                } else if (ret.contains("0008 Kein Captcha gefunden")) {
                    if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get() == 0 || ((System.currentTimeMillis() / 1000) - org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.get()) > 30) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterdialogtime5.set((System.currentTimeMillis() / 1000));
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_nocaptcha() + "\n" + ret);
                    }
                } else if (ret.contains("0015 Captcha zu schnell eingereicht")) {
                    Thread.sleep(15000);
                }
                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterSendError.incrementAndGet();
                throw new SolverException(ret);
            } else {
                setdebug(job, "Send Captcha - Answer: " + ret);
            }
            // Error-No Credits
            String captchaID = ret.substring(3);
            long startTime = System.currentTimeMillis();

            Thread.sleep(10000);
            while (true) {
                ret = br.getPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi?action=usercaptchacorrectdata&puzzle=1&jd=2&source=jd2&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1");
                if (StringUtils.isEmpty(ret) || ret == "No htmlCode read") {
                    setdebug(job, "CaptchaID " + captchaID + " - NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                } else {
                    setdebug(job, "CaptchaID " + captchaID + " - Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                }
                if (ret.startsWith("OK-answered-ERROR NO USER") || ret.startsWith("ERROR NO USER")) {
                    org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterInterrupted.incrementAndGet();
                    Thread.sleep(15000);
                    return;
                } else if (ret.startsWith("OK-answered-")) {
                    // Special answer (x + 465? y + 264?)
                    // Example: 622.289.683.351.705.331.734.351.713.264.734.281.488.275.784.281 (4 coordinates like x1,y1 to x2,y2)
                    mouseArray.clear();

                    boolean changemousexy9kw = true;
                    ArrayList<Integer> marray = new ArrayList<Integer>();
                    marray.addAll(mouseArray);
                    for (String s : ret.substring("OK-answered-".length()).split("\\|")) {
                        if (changemousexy9kw == true) {
                            mouseArray.add(Integer.parseInt(s));// x+465?
                            changemousexy9kw = false;
                        } else {
                            mouseArray.add(Integer.parseInt(s));// y+264?
                            changemousexy9kw = true;
                        }
                    }
                    mouseArray.clear();

                    String token;
                    token = challenge.getHelper().sendPuzzleResult(marray, ret.substring("OK-answered-".length()));

                    org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterSolved.incrementAndGet();
                    job.setAnswer(new KeyCaptchaResponse(challenge, this, token, 95));
                    return;
                } else if (((System.currentTimeMillis() - startTime) / 1000) > (timeoutthing + 10)) {
                    org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterInterrupted.incrementAndGet();
                    return;
                }

                checkInterruption();
                Thread.sleep(3000);
            }

        } catch (IOException e) {
            job.getChallenge().sendStatsError(this, e);
            setdebug(job, "Interrupted: " + e);
            org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterInterrupted.incrementAndGet();
            job.getLogger().log(e);
        }

    }

    private static byte[] getBytesKeyCaptcha(BufferedImage img) throws IOException {
        ByteArrayOutputStream byar = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", byar);
        } finally {
            byar.close();
        }
        return byar.toByteArray();
    }

    @Override
    public boolean isEnabled() {
        return config.ispuzzle() && config.isEnabledGlobally();
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
                        String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        String ret = "";
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=1&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("CaptchaID " + captchaID + ": OK (Feedback 1)");
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterOK.incrementAndGet();
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
                        String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        String ret = "";
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=3&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("CaptchaID " + captchaID + ": Unused (Feedback 3)");
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterUnused.incrementAndGet();
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
                        String captchaID = ((Captcha9kwResponse) response).getCaptcha9kwID();
                        Browser br = new Browser();
                        String ret = "";
                        br.setAllowedResponseCodes(new int[] { 500 });
                        for (int i = 0; i <= 3; i++) {
                            ret = br.getPage(NineKwSolverService.getInstance().getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=2&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().setdebug_short("CaptchaID " + captchaID + ": NotOK (Feedback 2)");
                                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().counterNotOK.incrementAndGet();
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

}
