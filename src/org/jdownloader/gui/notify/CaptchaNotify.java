package org.jdownloader.gui.notify;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogClickCaptchaSolver;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.CaptchaNotify.CaptchaNotifyPanel;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class CaptchaNotify extends AbstractNotifyWindow<CaptchaNotifyPanel> implements ChallengeSolverJobListener {
    public static class CaptchaNotifyPanel extends MigPanel {

        // private HashMap<ChallengeSolver<?>, JLabel> map;
        private SolverJob<?> job;

        public CaptchaNotifyPanel(SolverJob<?> job) {

            super("ins 0,wrap 2", "[][grow,fill]", "[grow,fill]");

            add(getIconPanel(NewTheme.I().getIcon(IconKey.ICON_OCR, 32)), "spany");
            add(getMessage(_GUI._.CaptchaNotify_CaptchaNotifyPanel_text()));

            SwingUtils.setOpaque(this, false);
            // map = new HashMap<ChallengeSolver<?>, JLabel>();
            // this.job = job;
            // for (ChallengeSolver<?> solver : job.getSolverList()) {
            // JLabel lbl = new JLabel();
            // map.put(solver, lbl);
            // add(new JLabel(solver.getName()), "split 2,pushx,growx");
            // add(lbl);
            // lbl.setHorizontalTextPosition(SwingConstants.LEFT);
            // }
            // update();

        }

        private Component getMessage(String text) {
            ExtTextArea ret = new ExtTextArea();
            SwingUtils.setOpaque(ret, false);
            ret.setText(text);
            ret.setLabelMode(true);
            return ret;
        }

        private Component getIconPanel(ImageIcon icon) {
            JLabel ret = new JLabel(icon);
            ret.setVerticalAlignment(SwingConstants.TOP);
            SwingUtils.setOpaque(ret, false);
            return ret;
        }

        public void update() {
            // ChallengeResponseController.getInstance().setSkipRequest(type, this, job.getChallenge());
            // main: for (ChallengeSolver<?> solver : job.getSolverList()) {
            // JLabel lbl = map.get(solver);
            // if (lbl != null) {
            //
            // lbl.setIcon(NewTheme.I().getIcon(IconKey.ICON_OK, 20));
            // if (solver.isJobDone(job)) {
            // if (job.getResponses() != null) {
            // for (ResponseList<?> rl : job.getResponses()) {
            // for (AbstractResponse ar : rl) {
            // if (solver == ar.getSolver()) {
            // lbl.setIcon(NewTheme.I().getIcon(IconKey.ICON_TRUE, 20));
            //
            // continue main;
            // }
            // }
            // }
            // }
            // lbl.setToolTipText(_GUI._.CaptchaNotify_update());
            // lbl.setIcon(NewTheme.I().getIcon(IconKey.ICON_FALSE, 20));
            // } else {
            // lbl.setToolTipText(_GUI._.CaptchaNotify_pending());
            // lbl.setIcon(NewTheme.I().getIcon(IconKey.ICON_WAIT, 20));
            // }
            //
            // }
            // }
        }
    }

    private SolverJob<?> job;

    public CaptchaNotify(SolverJob<?> job) {
        super(_GUI._.CaptchaNotify_CaptchaNotify_title_(Challenge.getHost(job.getChallenge())), new CaptchaNotifyPanel(job));
        job.getEventSender().addListener(this, true);
        this.job = job;

    }

    @Override
    public void onSolverJobReceivedNewResponse(AbstractResponse<?> response) {
    }

    @Override
    public void onSolverDone(ChallengeSolver<?> solver) {
        getContentComponent().update();
        if (job.isDone()) {
            close();

        }
    }

    @Override
    protected void onMouseClicked(MouseEvent m) {
        super.onMouseClicked(m);

        DialogBasicCaptchaSolver.getInstance().requestFocus(job.getChallenge());
        DialogClickCaptchaSolver.getInstance().requestFocus(job.getChallenge());
        close();
    }

    // protected int getTimeout() {
    // // don't autoclose
    // return 0;
    // }

    private void close() {
        BubbleNotify.getInstance().hide(this);
    }

    @Override
    public void onSolverStarts(ChallengeSolver<?> parameter) {
        getContentComponent().update();
    }

    @Override
    public void onSolverTimedOut(ChallengeSolver<?> parameter) {
        getContentComponent().update();
        if (job.isDone()) {
            close();
        }
    }

}
