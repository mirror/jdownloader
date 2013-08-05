package org.jdownloader.gui.jdtrayicon;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;

public class Balloner implements ComponentListener {
    private HashMap<GraphicsDevice, ScreenStack> stacks;
    private JFrame                               owner;

    public Balloner(JFrame owner) {
        this.owner = owner;
        stacks = new HashMap<GraphicsDevice, ScreenStack>();
    }

    public void add(final Notify notify) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                ScreenStack screenStack = getScreenStack();

                screenStack.add(notify);
                notify.addComponentListener(Balloner.this);
                layout(screenStack);
                WindowManager.getInstance().setVisible(notify, true, FrameState.TO_FRONT);
            }
        };

    }

    protected void layout(ScreenStack screenStack) {

        Rectangle bounds = getVisibleBounds(screenStack);

        int y = bounds.y + getMargin();
        int x = 0;
        for (Notify n : screenStack) {

            Dimension ps = n.getPreferredSize();

            x -= bounds.x + bounds.width - ps.width - getMargin();
            n.setLocation(x, y);
            y += ps.height + getMargin();

        }
    }

    protected int getMargin() {
        return 10;
    }

    protected Rectangle getVisibleBounds(ScreenStack screenStack) {
        final Rectangle bounds = screenStack.getScreen().getDefaultConfiguration().getBounds();
        final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screenStack.getScreen().getDefaultConfiguration());
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        return bounds;
    }

    private ScreenStack getScreenStack() {
        final GraphicsDevice screen = getScreenDevice();
        synchronized (stacks) {

            ScreenStack ret = stacks.get(screen);
            if (ret == null) {
                ret = new ScreenStack(screen);
                stacks.put(screen, ret);

            }
            return ret;
        }

    }

    protected GraphicsDevice getScreenDevice() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();

        final Rectangle preferedRect = owner.getBounds();
        GraphicsDevice biggestInteresctionScreem = null;
        int biggestIntersection = -1;

        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
            final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= insets.left + insets.right;
            bounds.height -= insets.top + insets.bottom;
            final Rectangle interSec = bounds.intersection(preferedRect);
            if (Math.max(interSec.width, 0) * Math.max(interSec.height, 0) > biggestIntersection || biggestInteresctionScreem == null) {
                biggestIntersection = Math.max(interSec.width, 0) * Math.max(interSec.height, 0);
                biggestInteresctionScreem = screen;
                if (interSec.equals(preferedRect)) {
                    break;
                }
            }
        }
        return biggestInteresctionScreem;
    }

    @Override
    public void componentResized(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {

        List<Notify> screenStack = getScreenStack();
        screenStack.remove(e.getSource());
    }

}
