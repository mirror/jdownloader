package jd.gui.swing.jdgui.components.modules;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import jd.controlling.ProgressController;
import jd.gui.swing.GuiRunnable;

public class ProgressCircle extends JPanel {

    private static final long serialVersionUID = -6877009081026501104L;
    private static final int ICONSIZE = 16;

    private double value;
    private double maximum;
    private Color color;
    private ProgressController controller;
    private Thread indeterminateChanger;
    private boolean stopIt = false;
    private boolean backward = false;

    public ProgressCircle() {
        super();
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (!aFlag) stopIt = true;
    }

    /**
     * Sets the controller for this ProgressCircle and updates its state after
     * the state of the {@link ProgressController}
     * 
     * @param controller
     */
    public void setController(ProgressController controller) {
        this.controller = controller;

        if (!controller.isIndeterminate()) {
            stopIt = true;
            maximum = controller.getMax();
            value = controller.getValue();
        } else {
            setIndeterminate();
        }
        color = controller.getColor();

        setToolTipText(controller.getStatusText());
    }

    private void setIndeterminate() {
        if (indeterminateChanger == null) {
            value = 0;
            maximum = 8;
            indeterminateChanger = new Thread(new Runnable() {
                public void run() {
                    while (!stopIt) {
                        if (backward) {
                            --value;
                            if (value == 0) backward = false;
                        } else {
                            ++value;
                            if (value == 8) backward = true;
                        }
                        new GuiRunnable<Object>() {
                            @Override
                            public Object runSave() {
                                revalidate();
                                repaint();
                                return null;
                            }
                        }.start();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    stopIt = false;
                    backward = false;
                    indeterminateChanger = null;
                }
            });
            indeterminateChanger.start();
        }
    }

    public ProgressController getController() {
        return controller;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ICONSIZE + 1, ICONSIZE + 1);
    }

    @Override
    public void paint(Graphics g) {
        if (maximum == 0) return;
        double progress = value / maximum;
        int a = (int) (progress * 360);
        Color color = this.color != null ? this.color : new Color(50, 100 + (int) (155 * (1 - progress)), 50);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(color);
        if (!backward) {
            g2.fillArc(0, 0, ICONSIZE, ICONSIZE, 90 - a, a);
        } else {
            g2.fillArc(0, 0, ICONSIZE, ICONSIZE, 90, a);
        }

        g2.setColor(Color.BLACK);
        g2.drawArc(0, 0, ICONSIZE, ICONSIZE, 0, 360);
    }

}
