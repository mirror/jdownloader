package org.jdownloader.captcha.v2.solver.captchabrotherhood;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Icon;

import jd.controlling.captcha.CaptchaSettings;
import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_CBH;

public class CBSolver extends CESChallengeSolver<String> implements ChallengeResponseValidation {
    private CaptchaBrotherHoodSettings config;
    private String                     accountStatusString;
    private static final CBSolver      INSTANCE   = new CBSolver();
    private ThreadPoolExecutor         threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());

    public static CBSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private CBSolver() {
        super(1);
        config = JsonConfig.create(CaptchaBrotherHoodSettings.class);
        AdvancedConfigManager.getInstance().register(config);
        initServicePanel(CFG_CBH.USER, CFG_CBH.PASS, CFG_CBH.ENABLED);
    }

    @Override
    public String getName() {
        return "Captcha Brotherhood";
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return c instanceof BasicCaptchaChallenge && CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED.isEnabled() && config.isEnabled() && super.canHandle(c);
    }

    @Override
    public String getAccountStatusString() {
        return accountStatusString;
    }

    protected boolean validateLogins() {
        if (!CFG_CBH.ENABLED.isEnabled()) return false;
        if (StringUtils.isEmpty(CFG_CBH.USER.getValue())) return false;
        if (StringUtils.isEmpty(CFG_CBH.PASS.getValue())) return false;

        return true;
    }

    @Override
    public void extendServicePabel(LinkedList<ServiceCollection<?>> services) {
        if (validateLogins()) {
            services.add(new ServiceCollection<DeathByCaptchaSolver>() {

                @Override
                public Icon getIcon() {
                    return NewTheme.I().getIcon(IconKey.ICON_CBH, 16);
                }

                @Override
                public boolean isEnabled() {
                    return config.isEnabled();
                }

                @Override
                protected long getLastActiveTimestamp() {
                    return System.currentTimeMillis();
                }

                @Override
                protected String getName() {
                    return "captchabrotherhood.com";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new ServicePanelCBHTooltip(owner, CBSolver.this);
                }

            });
        }
    }

    private AtomicInteger counterSolved      = new AtomicInteger();
    private AtomicInteger counterInterrupted = new AtomicInteger();
    private AtomicInteger counter            = new AtomicInteger();

    @Override
    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {
        CBHAccount acc = loadAccount();
        if (StringUtils.isEmpty(acc.getError())) {
            accountStatusString = acc.getBalance() + " Credits";
        } else {
            accountStatusString = acc.getError();
        }

        BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();

        job.getLogger().info("Start Captcha to CaptchaBrotherHood. Timeout: " + JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout() + " - getTypeID: " + challenge.getTypeID());
        if (config.getWhiteList() != null) {
            if (config.getWhiteList().length() > 5) {
                if (config.getWhiteList().contains(challenge.getTypeID())) {
                    job.getLogger().info("Hoster on WhiteList for CaptchaBrotherHood. - " + challenge.getTypeID());
                } else {
                    job.getLogger().info("Hoster not on WhiteList for CaptchaBrotherHood. - " + challenge.getTypeID());
                    return;
                }
            }
        }
        if (config.getBlackList() != null) {
            if (config.getBlackList().length() > 5) {
                if (config.getBlackList().contains(challenge.getTypeID())) {
                    job.getLogger().info("Hoster on BlackList for CaptchaBrotherHood. - " + challenge.getTypeID());
                    return;
                } else {
                    job.getLogger().info("Hoster not on BlackList for CaptchaBrotherHood. - " + challenge.getTypeID());
                }
            }
        }

        job.showBubble(this);
        checkInterruption();
        try {
            counter.incrementAndGet();
            String url = "http://www.captchabrotherhood.com/sendNewCaptcha.aspx?username=" + Encoding.urlEncode(config.getUser()) + "&password=" + Encoding.urlEncode(config.getPass()) + "&captchaSource=jdPlugin&captchaSite=999&timeout=80&version=1.1.7";
            byte[] data = IO.readFile(challenge.getImageFile());
            job.setStatus(_GUI._.DeathByCaptchaSolver_solveBasicCaptchaChallenge_uploading(), NewTheme.I().getIcon(IconKey.ICON_UPLOAD, 20));

            BasicHTTP http = new BasicHTTP();
            String ret = new String(http.postPage(new URL(url), data), "UTF-8");
            job.setStatus(_GUI._.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_UPLOAD, 20));

            job.getLogger().info("Send Captcha. Answer: " + ret);
            if (!ret.startsWith("OK-")) throw new SolverException(ret);
            // Error-No Credits
            String captchaID = ret.substring(3);
            data = null;
            Thread.sleep(6000);
            while (true) {
                Thread.sleep(1000);
                url = "http://www.captchabrotherhood.com/askCaptchaResult.aspx?username=" + Encoding.urlEncode(config.getUser()) + "&password=" + Encoding.urlEncode(config.getPass()) + "&captchaID=" + Encoding.urlEncode(captchaID) + "&version=1.1.7";
                job.getLogger().info("Ask " + url);
                ret = http.getPage(new URL(url));
                job.getLogger().info("Answer " + ret);
                if (ret.startsWith("OK-answered-")) {
                    counterSolved.incrementAndGet();

                    job.setAnswer(new CaptchaCBHResponse(challenge, this, ret.substring("OK-answered-".length()), 100, captchaID));
                    return;
                }
                checkInterruption();

            }
        } catch (InterruptedException e) {
            counterInterrupted.incrementAndGet();
            throw e;

        } catch (IOException e) {
            job.getLogger().log(e);
            counterInterrupted.incrementAndGet();
        } finally {

        }

    }

    public CBHAccount loadAccount() {
        CBHAccount ret = new CBHAccount();
        ret.setRequests(counter.get());
        ret.setSkipped(counterInterrupted.get());
        ret.setSolved(counterSolved.get());
        try {
            Browser br = new Browser();
            String result = br.postPage("http://www.captchabrotherhood.com/askCredits.aspx?username=" + Encoding.urlEncode(config.getUser()) + "&password=" + Encoding.urlEncode(config.getPass()) + "&version=1.1.8", "");

            if (result.startsWith("OK-")) {
                ret.setBalance(Integer.parseInt(result.substring(3)));
            } else {
                ret.setError(result);
            }

        } catch (Exception e) {

            ret.setError(e.getMessage());
        }
        return ret;
    }

    @Override
    public void setValid(final AbstractResponse<?> response) {
    }

    @Override
    public void setUnused(final AbstractResponse<?> response) {
    }

    @Override
    public void setInvalid(final AbstractResponse<?> response) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String captchaID = ((CaptchaCBHResponse) response).getCaptchaCBHID();
                    Browser br = new Browser();
                    String ret = "";
                    ret = br.getPage(new URL("http://www.captchabrotherhood.com/complainCaptcha.aspx?username=" + Encoding.urlEncode(config.getUser()) + "&password=" + Encoding.urlEncode(config.getPass()) + "&captchaID=" + Encoding.urlEncode(captchaID) + "&version=1.1.8"));
                } catch (final Throwable e) {
                    LogController.CL(true).log(e);
                }
            }
        });
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_CBH, size);
    }

}
