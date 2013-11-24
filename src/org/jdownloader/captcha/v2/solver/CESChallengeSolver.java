package org.jdownloader.captcha.v2.solver;

import jd.SecondLevelLaunch;
import jd.controlling.captcha.CaptchaSettings;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanelExtender;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public abstract class CESChallengeSolver<T> extends ChallengeSolver<T> implements ServicePanelExtender {

    public CESChallengeSolver(int threadCount) {
        super(threadCount);

    }

    final public void solve(final SolverJob<T> job) throws InterruptedException, SolverException {
        if (!validateLogins()) return;
        if (!canHandle(job.getChallenge())) return;
        job.waitFor(JsonConfig.create(CaptchaSettings.class).getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
        checkInterruption();
        CESSolverJob<T> cesJob = new CESSolverJob<T>(job);
        try {
            solveCES(cesJob);
        } finally {
            cesJob.hideBubble();
        }
    }

    protected abstract void solveCES(CESSolverJob<T> job) throws InterruptedException, SolverException;

    protected abstract boolean validateLogins();

    protected void initServicePanel(final KeyHandler... handlers) {

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            @SuppressWarnings("unchecked")
            public void run() {
                ServicePanel.getInstance().addExtender(CESChallengeSolver.this);

                for (KeyHandler k : handlers) {

                    k.getEventSender().addListener(new GenericConfigEventListener<Object>() {

                        @Override
                        public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
                        }

                        @Override
                        public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                            ServicePanel.getInstance().requestUpdate(true);
                        }
                    });
                }

            }
        });

    }

    public String getAccountStatusString() {
        return null;
    }
}
