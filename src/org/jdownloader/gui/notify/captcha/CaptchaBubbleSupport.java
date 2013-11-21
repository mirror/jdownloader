package org.jdownloader.gui.notify.captcha;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.jdownloader.captcha.event.ChallengeResponseListener;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogClickCaptchaSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class CaptchaBubbleSupport extends AbstractBubbleSupport implements ChallengeResponseListener {

    private ArrayList<Element> elements;

    public CaptchaBubbleSupport() {
        super(_GUI._.plugins_optional_JDLightTray_ballon_captcha2(), CFG_BUBBLE.BUBBLE_NOTIFY_ON_CAPTCHA_IN_BACKGROUND_ENABLED);

        ChallengeResponseController.getInstance().getEventSender().addListener(this);

    }

    @Override
    public List<Element> getElements() {
        return null;
    }

    @Override
    public void onNewJobAnswer(SolverJob<?> job, AbstractResponse<?> response) {
    }

    @Override
    public void onJobDone(SolverJob<?> job) {
    }

    @Override
    public void onNewJob(final SolverJob<?> job) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                switch (CFG_GUI.CFG.getNewDialogFrameState()) {
                case TO_BACK:
                    CaptchaNotify notify = new CaptchaNotify(CaptchaBubbleSupport.this, job);
                    show(notify);
                    return;
                case OS_DEFAULT:
                    if (!WindowManager.getInstance().hasFocus()) {
                        notify = new CaptchaNotify(CaptchaBubbleSupport.this, job);
                        show(notify);

                    } else {
                        if (DialogBasicCaptchaSolver.getInstance().hasToWaitForInvisibleSolvers() || DialogClickCaptchaSolver.getInstance().hasToWaitForInvisibleSolvers()) {
                            // show the bubble. because the dialog will probably get blocked by CES services
                            // a click on the bubble will show the dialog then.
                            notify = new CaptchaNotify(CaptchaBubbleSupport.this, job);
                            show(notify);
                            return;
                        }
                    }
                    return;
                default:

                    if (DialogBasicCaptchaSolver.getInstance().hasToWaitForInvisibleSolvers() || DialogClickCaptchaSolver.getInstance().hasToWaitForInvisibleSolvers()) {
                        // show the bubble. because the dialog will probably get blocked by CES services
                        // a click on the bubble will show the dialog then.
                        notify = new CaptchaNotify(CaptchaBubbleSupport.this, job);
                        show(notify);
                        return;
                    }
                    return;
                }

            }
        };

    }

    @Override
    public void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job) {
    }

    @Override
    public void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job) {
    }
}
