package org.jdownloader.gui.notify.captcha;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.translate._GUI;

public class CESBubble extends AbstractNotifyWindow<CESBubbleContent> {

    private Timer updateTimer;

    public CESBubble(ChallengeSolver<?> solver, SolverJob<?> job, int timeoutms) {
        super(_GUI._.CESBubble_CESBubble(solver.getName()), new CESBubbleContent(solver, job, timeoutms));
        getContentComponent().setBubble(this);
        updateTimer = new Timer(1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getContentComponent().update();
            }
        });
        updateTimer.setRepeats(true);
        updateTimer.start();

    }

    @Override
    protected int getTimeout() {
        return 0;
    }

    @Override
    public void dispose() {
        super.dispose();
        updateTimer.stop();
    }

    public void update() {
    }

    public void update(long rest) {
        getContentComponent().updateTimer(rest);
    }

}
