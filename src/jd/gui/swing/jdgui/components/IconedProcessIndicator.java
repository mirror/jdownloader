package jd.gui.swing.jdgui.components;

import java.awt.Color;

import javax.swing.ImageIcon;

import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;

public class IconedProcessIndicator extends CircledProgressBar {

    private boolean      active;
    private ImagePainter activeValuePainter;
    private ImagePainter activeNonValuePainter;
    private ImagePainter valuePainter;
    private ImagePainter nonValuePainter;

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
        setActive(false);
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
