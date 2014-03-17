package org.jdownloader.gui.notify.gui;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class Fader implements ActionListener {

    private static final int     FPS = 25;

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
        if (timedif > end - start || owner.isDisposed() || owner.isClosed()) {
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
        AbstractNotifyWindow.setWindowOpacity(owner, destAlpha);
        owner.setVisible(destAlpha > 0);

        srcAlpha = destAlpha;
        srcLocation = destLocation;
        if (destLocation != null) {
            owner.setLocation(destLocation);
        }

        if (faderTimer != null) {
            faderTimer.stop();

            faderTimer = null;
        }
    }

    public void moveTo(int x, int y, int time) {
        Point p = new Point(x, y);
        if (destLocation != null && destLocation.equals(p)) return;
        udpateLocation();
        updateAlpha();

        destLocation = p;

        if (destLocation.equals(srcLocation)) return;

        updateTimer(time);
        ensureFader();

    }

    protected void updateTimer(int time) {
        start = System.currentTimeMillis();
        end = start + time;

    }

    protected void udpateLocation() {
        srcLocation = owner.getLocation();
    }

    public void fadeIn(int i) {
        if (destAlpha == owner.getFinalTransparency()) return;

        udpateLocation();
        updateAlpha();

        destAlpha = owner.getFinalTransparency();
        if (srcAlpha == destAlpha) return;

        updateTimer(i);
        ensureFader();
    }

    protected void updateAlpha() {
        try {
            Float ret = AbstractNotifyWindow.getWindowOpacity(owner);
            if (ret == null) ret = destAlpha;
            srcAlpha = ret;
        } catch (Throwable e1) {
            srcAlpha = destAlpha;
        }
    }

    protected void ensureFader() {
        if (faderTimer == null) {
            faderTimer = new Timer(1000 / FPS, this);
            faderTimer.setRepeats(true);
            faderTimer.start();

        }
    }

    public void fadeOut(int i) {
        if (destAlpha == 0f) return;

        updateAlpha();
        udpateLocation();
        destAlpha = 0f;
        if (srcAlpha == destAlpha) return;

        updateTimer(i);
        ensureFader();
    }

}
