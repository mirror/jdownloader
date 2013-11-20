package org.jdownloader.captcha.v2.solver.captchabrotherhood;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_CBH;

public class CBSolver extends ChallengeSolver<String> implements ServicePanelExtender {
    private CaptchaBrotherHoodSettings config;
    private static final CBSolver      INSTANCE = new CBSolver();

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
        initServicePanel();

        AdvancedConfigManager.getInstance().register(config);

    }

    @Override
    public String getName() {
        return "Captcha Brotherhood";
    }

    @Override
    public boolean canHandle(Challenge<?> c) {

        return CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED.isEnabled() && config.isEnabled() && super.canHandle(c);
    }

    public void initServicePanel() {
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                ServicePanel.getInstance().addExtender(CBSolver.this);

                CFG_CBH.USER.getEventSender().addListener(new GenericConfigEventListener<String>() {

                    @Override
                    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
                    }

                    @Override
                    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                        ServicePanel.getInstance().requestUpdate(true);
                    }
                });
                CFG_CBH.PASS.getEventSender().addListener(new GenericConfigEventListener<String>() {

                    @Override
                    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
                    }

                    @Override
                    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                        ServicePanel.getInstance().requestUpdate(true);
                    }
                });
                CFG_CBH.ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

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

    private boolean validateLogins() {
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
    public void solve(final SolverJob<String> job) throws InterruptedException, SolverException {
        if (StringUtils.isEmpty(config.getUser()) || StringUtils.isEmpty(config.getPass())) return;
        if (job.getChallenge() instanceof BasicCaptchaChallenge && CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED.isEnabled()) {
            job.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
            checkInterruption();
            BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();

            try {
                counter.incrementAndGet();
                String url = "http://www.captchabrotherhood.com/sendNewCaptcha.aspx?username=" + Encoding.urlEncode(config.getUser()) + "&password=" + Encoding.urlEncode(config.getPass()) + "&captchaSource=jdPlugin&captchaSite=999&timeout=80&version=1.1.7";
                byte[] data = IO.readFile(challenge.getImageFile());

                BasicHTTP http = new BasicHTTP();
                String ret = new String(http.postPage(new URL(url), data), "UTF-8");

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
                        job.addAnswer(new CaptchaResponse(challenge, this, ret.substring("OK-answered-".length()), 100));
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
}
