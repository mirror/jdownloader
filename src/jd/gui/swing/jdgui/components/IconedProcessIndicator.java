package jd.gui.swing.jdgui.components;

import java.awt.Color;

import javax.swing.ImageIcon;

import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;

public class IconedProcessIndicator extends CircledProgressBar {

    public IconedProcessIndicator(ImageIcon icon) {
        super();
        ImagePainter value = getPainer(icon, 1.0f);
        this.setValueClipPainter(value);
        value.setBackground(Color.WHITE);
        value.setForeground(Color.GRAY);
        this.setNonvalueClipPainter(getPainer(icon, 0.5f));

    }

    private ImagePainter getPainer(ImageIcon icon, float f) {
        ImagePainter ret = new ImagePainter(icon.getImage(), f);
        ret.setBackground(Color.WHITE);
        ret.setForeground(Color.LIGHT_GRAY);
        return ret;
    }

}
