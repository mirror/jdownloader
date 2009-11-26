package jd.gui.swing.jdgui.components.modules;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import jd.controlling.ProgressController;

public class ProgressCircle extends JPanel {

    private static final long serialVersionUID = -6877009081026501104L;
    private static final int ICONSIZE = 16;

    private double value;
    private double maximum;
    private double indeterminateValue = 0;
    private double indeterminateMaximum = 8;
    private Color color;
    private ProgressController controller;
    private boolean backward = false;

    public ProgressCircle() {
        super();
    }

    /**
     * Sets the controller for this ProgressCircle and updates its state after
     * the state of the {@link ProgressController}
     * 
     * @param controller
     */
    public void setController(ProgressController controller) {
        this.controller = controller;
        if (this.controller == null) return;
        if (!controller.isIndeterminate()) {
            maximum = controller.getMax();
            value = controller.getValue();
        } else {
            maximum = indeterminateMaximum;
            value = indeterminateValue;
            if (backward) {
                value--;
                if (value == 0) backward = false;
            } else {
                value++;
                if (value == indeterminateMaximum) backward = true;
            }
            indeterminateValue = value;
        }
        color = controller.getColor();
        setToolTipText(controller.getStatusText());
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
