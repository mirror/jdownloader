package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;

import jd.SecondLevelLaunch;
import jd.controlling.captcha.CaptchaSettings;
import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanelExtender;
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
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class Captcha9kwSolver extends ChallengeSolver<String> implements ChallengeResponseValidation, ServicePanelExtender {
    private Captcha9kwSettings            config;
    private static final Captcha9kwSolver INSTANCE   = new Captcha9kwSolver();
    private ThreadPoolExecutor            threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());

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
        return CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED.isEnabled() && config.isEnabled() && super.canHandle(c);
    }

    public String getAPIROOT() {
        if (config.ishttps()) {
            return "https://www.9kw.eu/";
        } else {
            return "http://www.9kw.eu/";
        }
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SolverException {
        if (StringUtils.isEmpty(config.getApiKey())) {
            job.getLogger().info("No ApiKey for 9kw.eu found.");
            return;
        }
        if (job.getChallenge() instanceof BasicCaptchaChallenge && CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED.isEnabled()) {
            job.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
            checkInterruption();
            BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();

            job.getLogger().info("Start Captcha to 9kw.eu. Timeout: " + JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout() + " - getTypeID: " + challenge.getTypeID());
            if (config.getwhitelist() != null) {
                if (config.getwhitelist().length() > 5) {
                    if (config.getwhitelist().contains(challenge.getTypeID())) {
                        job.getLogger().info("Hoster on whitelist for 9kw.eu. - " + challenge.getTypeID());
                    } else {
                        job.getLogger().info("Hoster not on whitelist for 9kw.eu. - " + challenge.getTypeID());
                        return;
                    }
                }
            }
            if (config.getblacklist() != null) {
                if (config.getblacklist().length() > 5) {
                    if (config.getblacklist().contains(challenge.getTypeID())) {
                        job.getLogger().info("Hoster on blacklist for 9kw.eu. - " + challenge.getTypeID());
                        return;
                    } else {
                        job.getLogger().info("Hoster not on blacklist for 9kw.eu. - " + challenge.getTypeID());
                    }
                }
            }

            try {
                byte[] data = IO.readFile(challenge.getImageFile());
                Browser br = new Browser();
                br.setAllowedResponseCodes(new int[] { 500 });
                String ret = "";

                for (int i = 0; i <= 5; i++) {
                    ret = br.postPage(getAPIROOT() + "index.cgi", "action=usercaptchaupload&jd=2&source=jd2&captchaperhour=" + config.gethour() + "&prio=" + config.getprio() + "&selfsolve=" + config.isSelfsolve() + "&confirm=" + config.isconfirm() + "&oldsource=" + Encoding.urlEncode(challenge.getTypeID()) + "&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&maxtimeout=" + (JsonConfig.create(CaptchaSettings.class).getCaptchaDialog9kwTimeout() / 1000) + "&version=1.2&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false)));
                    if (ret.startsWith("OK-")) {
                        break;
                    } else {
                        Thread.sleep(3000);
                    }
                }
                job.getLogger().info("Send Captcha to 9kw.eu. - Answer: " + ret);
                if (!ret.startsWith("OK-")) throw new SolverException(ret);
                // Error-No Credits
                String captchaID = ret.substring(3);
                data = null;
                long startTime = System.currentTimeMillis();

                Thread.sleep(5000);

                while (true) {

                    job.getLogger().info("9kw.eu Ask " + captchaID);
                    ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectdata&jd=2&source=jd2&apikey=" + Encoding.urlEncode(config.getApiKey()) + "&id=" + Encoding.urlEncode(captchaID) + "&version=1.1");
                    if (StringUtils.isEmpty(ret)) {
                        job.getLogger().info("9kw.eu NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                    } else {
                        job.getLogger().info("9kw.eu Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                    }
                    if (ret.startsWith("OK-answered-")) {
                        job.addAnswer(new Captcha9kwResponse(challenge, this, ret.substring("OK-answered-".length()), 100, captchaID));
                        return;
                    }
                    checkInterruption();
                    Thread.sleep(3000);
                }

            } catch (IOException e) {
                job.getLogger().log(e);
            }
        } else {
            job.getLogger().info("Problem with Captcha9kwSolver.");
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
                        for (int i = 0; i <= 5; i++) {
                            ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=1&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                break;
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
    public void setUnused(AbstractResponse<?> response) {
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
                        for (int i = 0; i <= 5; i++) {
                            ret = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchacorrectback&source=jd2&correct=2&id=" + captchaID + "&apikey=" + Encoding.urlEncode(config.getApiKey()));
                            if (ret.startsWith("OK")) {
                                break;
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

                @Override
                public Icon getIcon() {
                    return DomainInfo.getInstance("9kw.eu").getFavIcon();
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

        credits = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchaguthaben&apikey=" + Encoding.urlEncode(config.getApiKey()));

        try {
            ret.setCreditBalance(Integer.parseInt(credits.trim()));
            String userhistory1 = br.getPage(getAPIROOT() + "index.cgi?action=userhistory&short=1&apikey=" + Encoding.urlEncode(config.getApiKey()));
            String userhistory2 = br.getPage(getAPIROOT() + "index.cgi?action=userhistory2&short=1&apikey=" + Encoding.urlEncode(config.getApiKey()));

            ret.setAnswered(Integer.parseInt(Regex.getLines(userhistory2)[0]));
            ret.setSolved(Integer.parseInt(Regex.getLines(userhistory1)[0]));

        } catch (NumberFormatException e) {
            ret.setError(credits);
        }
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

        } catch (NumberFormatException e) {
            ret.setError("API Error!");
        }
        return ret;

    }
}
