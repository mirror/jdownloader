package org.jdownloader.captcha.v2.solver.browser;

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
}
