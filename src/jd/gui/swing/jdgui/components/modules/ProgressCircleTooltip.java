package jd.gui.swing.jdgui.components.modules;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.ProgressController;
import jd.gui.swing.components.JWindowTooltip;

public class ProgressCircleTooltip extends JWindowTooltip {

    private static final long  serialVersionUID = -3242288593937020385L;

    private ProgressController controller       = null;
    private JLabel             toolTip;

    public ProgressController getController() {
        return controller;
    }

    public void setController(ProgressController controller) {
        this.controller = controller;
        if (this.controller == null) hideTooltip();
    }

    @Override
    protected void addContent(JPanel panel) {
        panel.add(toolTip = new JLabel(""));
    }

    @Override
    protected void updateContent() {
        if (controller != null) {
            if (controller.isIndeterminate()) {
                toolTip.setText(controller.getStatusText());
            } else {
                StringBuilder sb = new StringBuilder();
                if (controller.getStatusText() != null) {
                    sb.append(controller.getStatusText());
                    sb.append(' ');
                }
                sb.append('[');
                sb.append(controller.getPercent());
                sb.append(new char[] { '%', ']' });
                toolTip.setText(sb.toString());
            }
        }
    }

}
