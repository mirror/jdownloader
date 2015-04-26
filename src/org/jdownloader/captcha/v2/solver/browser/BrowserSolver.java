package org.jdownloader.captcha.v2.solver.browser;

import org.appwork.utils.Application;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.RecaptchaV1Challenge;
import org.jdownloader.captcha.v2.solver.service.BrowserSolverService;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class BrowserSolver extends AbstractBrowserSolver {

    private static final BrowserSolver INSTANCE = new BrowserSolver();

    public static BrowserSolver getInstance() {
        return INSTANCE;
    }

    private BrowserSolver() {
        super(1);
        AdvancedConfigManager.getInstance().register(BrowserSolverService.getInstance().getConfig());
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (Application.isJared(null)) {
            // Not ready yet
            if (c instanceof RecaptchaV1Challenge) {
                return false;
            }
        }
        return c instanceof AbstractBrowserChallenge && super.canHandle(c);
    }

}
