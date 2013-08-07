package org.jdownloader.gui.notify.gui;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import com.sun.awt.AWTUtilities;

public class Fader implements ActionListener {

    private static final int     FPS = 15;
    private AbstractNotifyWindow owner;
    private Timer                faderTimer;
    private long                 start;
    private long                 end;
    private Point                destLocation;
    private float                destAlpha;
    private float                srcAlpha;
    private Point                srcLocation;

    public Fader(AbstractNotifyWindow notify) {
        this.owner = notify;
    }

    public static double getValue(double t) {
        // seriously...
        return (6 * t * t * t * t * t + -15 * t * t * t * t + 10 * t * t * t);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        long timedif = System.currentTimeMillis() - start;
        if (timedif > end - start) {
            stop();
            return;
        }

        double percent = (timedif) / (double) (end - start);
        double factor = getValue(percent);
        float alpha = (float) (srcAlpha + factor * (destAlpha - srcAlpha));
        Point loc = new Point();
        loc.x = (int) (srcLocation.x + factor * (destLocation.x - srcLocation.x));
        loc.y = (int) (srcLocation.y + factor * (destLocation.y - srcLocation.y));
        owner.setLocation(loc);

        AbstractNotifyWindow.setWindowOpacity(owner, alpha);

    }

    private void stop() {
        AWTUtilities.setWindowOpacity(owner, destAlpha);
        owner.setVisible(destAlpha > 0);
        if (destLocation != null) {
            owner.setLocation(destLocation);
        }

        if (faderTimer != null) {
            faderTimer.stop();
            faderTimer = null;
        }
    }

    public void moveTo(int x, int y, int time) {
        start = System.currentTimeMillis();
        end = start + time;
        ensureFader();
        srcLocation = owner.getLocation();
        destLocation = new Point(x, y);
    }

    public void fadeIn(int i) {
        start = System.currentTimeMillis();
        end = start + i;
        float alpha = destAlpha;
        try {
            alpha = AbstractNotifyWindow.getWindowOpacity(owner);
        } catch (Exception e1) {

        }
        srcAlpha = alpha;
        destAlpha = 1.0f;
        ensureFader();
    }

    protected void ensureFader() {
        if (faderTimer == null) {
            faderTimer = new Timer(1000 / FPS, this);
            faderTimer.setRepeats(true);
            faderTimer.start();
        }
    }

    public void fadeOut(int i) {
        start = System.currentTimeMillis();
        end = start + i;
        float alpha = destAlpha;
        try {
            alpha = AbstractNotifyWindow.getWindowOpacity(owner);
        } catch (Exception e1) {

        }
        srcAlpha = alpha;
        destAlpha = 0f;
        ensureFader();
    }

}
