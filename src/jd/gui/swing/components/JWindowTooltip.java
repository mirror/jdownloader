package jd.gui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;

public abstract class JWindowTooltip extends JWindow {

    private static final long serialVersionUID = -7191050140766206744L;

    private TooltipUpdater    updater;

    private Point             point;

    public JWindowTooltip() {
        JPanel panel = new JPanel(new MigLayout("ins 0", "[fill, grow]", "[fill, grow]"));
        panel.setOpaque(true);
        panel.setBackground(new Color(0xb9cee9));
        panel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, panel.getBackground().darker()));

        addContent(panel);

        WindowManager.getInstance().setVisible(this, false, FrameState.FOCUS);
        this.setAlwaysOnTop(true);
        this.add(panel);
        this.pack();
    }

    public void showTooltip(Point point) {
        this.point = point;
        if (updater != null) updater.interrupt();
        updater = new TooltipUpdater();
        updater.start();
    }

    public void hideTooltip() {
        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                if (isVisible()) WindowManager.getInstance().setVisible(JWindowTooltip.this, false, FrameState.FOCUS);
                return null;
            }

        }.start();
    }

    private void setLocation() {
        new EDTHelper<Object>() {
            public Object edtRun() {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;
                Point pp = point;
                if (pp.x <= limitX) {
                    if (pp.y <= limitY) {
                        setLocation(pp.x, pp.y);
                    } else {
                        setLocation(pp.x, pp.y - getHeight());
                    }
                } else {
                    if (pp.y <= limitY) {
                        setLocation(pp.x - getWidth(), pp.y);
                    } else {
                        setLocation(pp.x - getWidth(), pp.y - getHeight());
                    }
                }
                return null;
            }
        }.waitForEDT();
    }

    protected abstract void addContent(JPanel panel);

    protected abstract void updateContent();

    private class TooltipUpdater extends Thread implements Runnable {

        public void run() {
            pack();
            setLocation();
            setVisible(true);
            toFront();

            while (isVisible()) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        updateContent();

                        pack();
                    }

                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }

            hideTooltip();
        }
    }

}
