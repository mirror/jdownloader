package org.jdownloader.crosssystem.idlegetter;

import java.awt.AWTEvent;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class BasicMousePointerIdleGetter extends IdleGetter implements AWTEventListener, ActionListener {
    private volatile Point lastLocation = null;
    private volatile long  lastChange   = System.currentTimeMillis();

    public BasicMousePointerIdleGetter() {
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        Timer timer = new Timer(5000, this);
        timer.setRepeats(true);
        timer.start();
    }

    @Override
    public long getIdleTimeSinceLastUserInput() {
        return System.currentTimeMillis() - lastChange;
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        lastChange = System.currentTimeMillis();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Point location = MouseInfo.getPointerInfo().getLocation();
        if (lastLocation == null || !lastLocation.equals(location)) {
            lastChange = System.currentTimeMillis();
            lastLocation = location;
        }
    }

}
