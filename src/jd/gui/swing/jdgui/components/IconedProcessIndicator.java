package jd.gui.swing.jdgui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.utils.swing.SwingUtils;

public class IconedProcessIndicator extends CircledProgressBar implements MouseListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1627427388265904122L;
    private boolean           active;

    protected boolean isActive() {
        return active;
    }

    protected ImagePainter activeValuePainter;
    protected ImagePainter activeNonValuePainter;
    protected ImagePainter valuePainter;
    protected ImagePainter nonValuePainter;

    @Override
    public boolean isTooltipWithoutFocusEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    protected IconedProcessIndicator() {
        super();

    }

    public IconedProcessIndicator(ImageIcon icon) {
        super();

        updatePainter(icon, Color.WHITE, Color.GRAY, Color.WHITE, Color.GREEN, Color.LIGHT_GRAY, Color.GREEN);
        ToolTipController.getInstance().register(this);
        setActive(false);
        addMouseListener(this);
    }

    public void updatePainter(ImageIcon icon, Color c1, Color c2, Color c3, Color c4, Color c5, Color c6) {
        valuePainter = getPainer(icon, 1.0f);
        valuePainter.setBackground(c1);
        valuePainter.setForeground(c2);
        nonValuePainter = getPainer(icon, 0.5f);
        activeValuePainter = getPainer(icon, 1.0f);
        activeValuePainter.setBackground(c3);
        activeValuePainter.setForeground(c4);

        activeNonValuePainter = getPainer(icon, 0.5f);
        activeNonValuePainter.setBackground(c5);
        activeNonValuePainter.setForeground(c6);
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
        comp.setValue(getValue());
        TooltipPanel panel = new TooltipPanel("ins 0,wrap 2", "[][grow,fill]", "[]0[grow,fill]");

        comp.setOpaque(false);

        JLabel lbl = new JLabel(toString());
        lbl.setForeground(LAFOptions.createColor(LookAndFeelController.getInstance().getLAFOptions().getColorForTooltipForeground()));
        JTextArea txt = new JTextArea();
        SwingUtils.setOpaque(txt, false);
        txt.setBorder(null);
        txt.setForeground(LAFOptions.createColor(LookAndFeelController.getInstance().getLAFOptions().getColorForTooltipForeground()));
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

    protected void setActive(boolean newValue) {
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

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            ToolTipController.getInstance().hideTooltip();

        } else {
            ToolTipController.getInstance().show(this);
        }

    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

}
