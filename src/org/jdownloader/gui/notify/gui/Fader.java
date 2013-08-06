package org.jdownloader.gui.notify.gui;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import com.sun.awt.AWTUtilities;

public class Fader implements ActionListener {

    private static final int     FPS = 25;
    private AbstractNotifyWindow owner;
    private Timer                faderTimer;
    private long                 start;
    private FadeType             type;
    private long                 end;
    private Point                destLocation;
    private float                destAlpha;

    public Fader(AbstractNotifyWindow notify) {
        this.owner = notify;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long timedif = end - System.currentTimeMillis();
        long steps = FPS * timedif / 1000;
        if (steps <= 0) {
            stop();
        }
        float alpha = destAlpha;
        try {
            alpha = AWTUtilities.getWindowOpacity(owner);
        } catch (Exception e1) {

        }
        Point loc = owner.getLocation();
        Point dLoc = destLocation;
        if (dLoc == null) {
            dLoc = loc;
        }
        int dx = dLoc.x - loc.x;
        int dy = dLoc.y - loc.y;

        float d = destAlpha - alpha;
        if (Math.abs(d) <= 0.01d && Math.abs(dx) < 2 && Math.abs(dy) < 2) {

            stop();
            return;
        }
        double f = Math.sqrt(steps);
        alpha += d / f;
        loc.x += dx / f;
        loc.y += dy / f;

        owner.setLocation(loc);
        try {
            AWTUtilities.setWindowOpacity(owner, alpha);
        } catch (Exception e1) {

        }
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

    public enum FadeType {
        IN,
        OUT

    }

    public void moveTo(int x, int y, int time) {
        start = System.currentTimeMillis();
        end = start + time;
        ensureFader();
        destLocation = new Point(x, y);
    }

    public void fadeIn(int i) {
        start = System.currentTimeMillis();
        end = start + i;
        type = FadeType.IN;
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
        type = FadeType.OUT;
        destAlpha = 0f;
        ensureFader();
    }

}
