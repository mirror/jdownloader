package org.jdownloader.gui.notify.captcha;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.translate._GUI;

public class CESBubble extends AbstractNotifyWindow<CESBubbleContent> {
    
    private Timer updateTimer;
    
    public CESBubble(CESChallengeSolver<?> solver, CESSolverJob<?> cesSolverJob, int timeoutms) {
        super(_GUI._.CESBubble_CESBubble(solver.getName()), new CESBubbleContent(solver, cesSolverJob, timeoutms));
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
        try {
            updateTimer.stop();
        } finally {
            super.dispose();
        }
    }
    
    @Override
    public void hideBubble(final int timeout) {
        new EDTRunner() {
            
            @Override
            protected void runInEDT() {
                updateTimer.stop();
                CESBubble.super.hideBubble(timeout);
                getContentComponent().stop();
            }
        };
    }
    
    public void update() {
        new EDTRunner() {
            
            @Override
            protected void runInEDT() {
                getContentComponent().update();
            }
        };
        
    }
    
    public void update(final long rest) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                getContentComponent().updateTimer(rest);
            }
        };
    }
}
