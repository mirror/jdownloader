package org.jdownloader.captcha.v2.solver;

import javax.swing.Icon;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.captcha.CESBubble;
import org.jdownloader.gui.notify.captcha.CESBubbleSupport;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class CESSolverJob<T> {

    private SolverJob<T> job;
    private SolverStatus status;
    private CESBubble    bubble;
    private boolean      answered;

    public SolverJob<T> getJob() {
        return job;
    }

    public CESSolverJob(SolverJob<T> job) {
        this.job = job;
    }

    public Challenge<T> getChallenge() {
        return job.getChallenge();
    }

    public void waitFor(int timeout, ChallengeSolver<?>... instances) throws InterruptedException {
        job.waitFor(timeout, instances);
    }

    public LogSource getLogger() {
        return job.getLogger();
    }

    public void setStatus(SolverStatus solverStatus) {
        this.status = solverStatus;

    }

    public SolverStatus getStatus() {
        return status;
    }

    public void setStatus(String label, Icon icon) {
        setStatus(new SolverStatus(label, icon));
        if (bubble != null) bubble.update();
    }

    public CESBubble showBubble(CESChallengeSolver<T> cbSolver) throws InterruptedException {

        return showBubble(cbSolver, CFG_CAPTCHA.CFG.getCaptchaExchangeChanceToSkipBubbleTimeout());
    }

    public CESBubble showBubble(CESChallengeSolver<T> cbSolver, int timeout) throws InterruptedException {
        bubble = CESBubbleSupport.getInstance().show(cbSolver, this, timeout);
        return bubble;
    }

    public void setAnswer(AbstractResponse<T> abstractResponse) {
        job.addAnswer(abstractResponse);
        answered = true;
        setStatus(_GUI._.DeathByCaptchaSolver_solveBasicCaptchaChallenge_answer(abstractResponse.getValue() + ""), NewTheme.I().getIcon(IconKey.ICON_OK, 20));

    }

    public void hideBubble() {
        if (!answered) {
            setStatus(SolverStatus.UNSOLVED);
            if (bubble != null) bubble.update();
        }
        if (bubble != null) bubble.hideBubble(5000);
    }

}
