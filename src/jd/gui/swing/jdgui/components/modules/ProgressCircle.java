package jd.gui.swing.jdgui.components.modules;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JPanel;

import jd.controlling.ProgressController;
import jd.nutils.JDImage;

public class ProgressCircle extends JPanel {

    private static final long serialVersionUID = -6877009081026501104L;
    private static final int ICONSIZE = 15;
    private static final int ICONSIZE_CIRCLE = 16;
    private static final double INDETERMINATE_MAXIMUM = 8;

    private double value;
    private double maximum;
    private Color color;
    private Icon icon;
    private Icon iconGrey;
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
            StringBuilder toolTip = new StringBuilder();
            if (controller.getStatusText() != null) {
                toolTip.append(controller.getStatusText());
                toolTip.append(' ');
            }
            toolTip.append('[');
            toolTip.append(controller.getPercent() / 100);
            toolTip.append(new char[] { '%', ']' });
            setToolTipText(toolTip.toString());
        } else {
            maximum = INDETERMINATE_MAXIMUM;
            if (backward) {
                value--;
                if (value <= 0) {
                    value = 0;
                    backward = false;
                }
            } else {
                value++;
                if (value >= INDETERMINATE_MAXIMUM) {
                    backward = true;
                    value = INDETERMINATE_MAXIMUM;
                }
            }
            setToolTipText(controller.getStatusText());
        }
        color = controller.getColor();
        icon = controller.getIcon();
        iconGrey = JDImage.getDisabledIcon(icon);
    }

    public ProgressController getController() {
        return controller;
    }

    @Override
    public Dimension getPreferredSize() {
        if (icon == null || iconGrey == null) {
            return new Dimension(ICONSIZE_CIRCLE + 1, ICONSIZE_CIRCLE + 1);
        } else {
            return new Dimension(ICONSIZE + 1, ICONSIZE + 1);
        }
    }

    @Override
    public void paint(Graphics g) {
        if (maximum == 0) return;
        double progress = value / maximum;
        int degree = (int) (progress * 360);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (icon == null || iconGrey == null) {
            // No Custom Icon: Simply fill a Circle with Green
            Color color = this.color != null ? this.color : Color.GREEN;

            g2.setColor(color);
            if (!backward) {
                g2.fillArc(0, 0, ICONSIZE_CIRCLE, ICONSIZE_CIRCLE, 90 - degree, degree);
            } else {
                g2.fillArc(0, 0, ICONSIZE_CIRCLE, ICONSIZE_CIRCLE, 90, degree);
            }

            g2.setColor(Color.BLACK);
            g2.drawArc(0, 0, ICONSIZE_CIRCLE, ICONSIZE_CIRCLE, 0, 360);
        } else {
            // Custom Icon: Change between Grey and Color for indicating a
            // running Process
            if (!backward) {
                iconGrey.paintIcon(this, g2, 0, 0);
            } else {
                icon.paintIcon(this, g2, 0, 0);
                progress = 1 - progress;
                degree = (int) (progress * 360);
            }

            // Create the Polygon for the "upper" Icon
            Polygon colorArea = new Polygon();
            colorArea.addPoint(ICONSIZE / 2, ICONSIZE / 2);
            colorArea.addPoint(ICONSIZE / 2, 0);
            if (progress >= 0.125) {
                colorArea.addPoint(ICONSIZE, 0);
                if (progress >= 0.375) {
                    colorArea.addPoint(ICONSIZE, ICONSIZE);
                    if (progress >= 0.625) {
                        colorArea.addPoint(0, ICONSIZE);
                        if (progress >= 0.875) {
                            colorArea.addPoint(0, 0);
                            // Between 87.5 and 100.0 (Up-Left)
                            int h = (int) ((ICONSIZE / 2) * tan(degree - 360));
                            colorArea.addPoint(ICONSIZE / 2 + h, 0);
                        } else {
                            // Between 62.5 and 87.5 (Left)
                            int h = (int) ((ICONSIZE / 2) * tan(degree - 270));
                            colorArea.addPoint(0, ICONSIZE / 2 - h);
                        }
                    } else {
                        // Between 37.5 and 62.5 (Down)
                        int h = (int) ((ICONSIZE / 2) * tan(degree - 180));
                        colorArea.addPoint(ICONSIZE / 2 - h, ICONSIZE);
                    }
                } else {
                    // Between 12.5 and 37.5 (Right)
                    int h = (int) ((ICONSIZE / 2) * tan(degree - 90));
                    colorArea.addPoint(ICONSIZE, ICONSIZE / 2 + h);
                }
            } else {
                // Between 0.0 and 12.5 (Up-Right)
                int h = (int) ((ICONSIZE / 2) * tan(degree));
                colorArea.addPoint(ICONSIZE / 2 + h, 0);
            }

            g2.setClip(colorArea);
            if (!backward) {
                icon.paintIcon(this, g2, 0, 0);
            } else {
                iconGrey.paintIcon(this, g2, 0, 0);
            }
        }
    }

    private static double tan(double degree) {
        return Math.tan(degree * Math.PI / 180.0);
    }

}
