package jd.gui.swing.jdgui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.utils.swing.SwingUtils;

public class IconedProcessIndicator extends CircledProgressBar {

    private boolean      active;
    private ImagePainter activeValuePainter;
    private ImagePainter activeNonValuePainter;
    private ImagePainter valuePainter;
    private ImagePainter nonValuePainter;

    private IconedProcessIndicator() {

    }

    public IconedProcessIndicator(ImageIcon icon) {
        super();
        valuePainter = getPainer(icon, 1.0f);

        valuePainter.setBackground(Color.WHITE);
        valuePainter.setForeground(Color.GRAY);
        nonValuePainter = getPainer(icon, 0.5f);
        activeValuePainter = getPainer(icon, 1.0f);
        activeValuePainter.setBackground(Color.WHITE);
        activeValuePainter.setForeground(Color.GREEN);

        activeNonValuePainter = getPainer(icon, 0.5f);
        activeNonValuePainter.setBackground(Color.LIGHT_GRAY);
        activeNonValuePainter.setForeground(Color.GREEN);
        ToolTipController.getInstance().register(this);
        setActive(false);
    }

    public ExtTooltip createExtTooltip(final Point mousePosition) {
        IconedProcessIndicator comp = new IconedProcessIndicator();

        comp.valuePainter = valuePainter;
        comp.nonValuePainter = nonValuePainter;
        comp.activeValuePainter = activeValuePainter;
        comp.activeNonValuePainter = activeNonValuePainter;
        comp.setActive(active);
        comp.setEnabled(isEnabled());
        comp.setIndeterminate(isIndeterminate());
        comp.setPreferredSize(new Dimension(32, 32));
        comp.setValue(100);
        TooltipPanel panel = new TooltipPanel("ins 0,wrap 2", "[][grow,fill]", "[]0[grow,fill]");

        comp.setOpaque(false);

        JLabel lbl = new JLabel(toString());
        lbl.setForeground(new Color(LookAndFeelController.getInstance().getLAFOptions().getTooltipForegroundColor()));
        JTextArea txt = new JTextArea();
        SwingUtils.setOpaque(txt, false);
        txt.setBorder(null);
        txt.setForeground(new Color(LookAndFeelController.getInstance().getLAFOptions().getTooltipForegroundColor()));
        // p.add(lbl);
        panel.add(comp, "spany 2,aligny top");
        panel.add(SwingUtils.toBold(lbl));
        panel.add(txt);
        lbl.setText(getTitle());
        txt.setText(getDescription());
        return new PanelToolTip(panel);
    }

    private String title       = null;
    private String description = null;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIndeterminate(final boolean newValue) {
        super.setIndeterminate(newValue);
        setActive(newValue);
    }

    private void setActive(boolean newValue) {
        active = newValue;

        if (active) {
            this.setValueClipPainter(activeValuePainter);
            this.setNonvalueClipPainter(activeNonValuePainter);

        } else {
            this.setValueClipPainter(valuePainter);
            this.setNonvalueClipPainter(nonValuePainter);
        }
    }

    private ImagePainter getPainer(ImageIcon icon, float f) {
        ImagePainter ret = new ImagePainter(icon.getImage(), f);
        ret.setBackground(Color.WHITE);
        ret.setForeground(Color.LIGHT_GRAY);
        return ret;
    }

}
