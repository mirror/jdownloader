package jd.gui.swing.jdgui.menu;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.appwork.app.gui.MigPanel;

public class MenuEditor extends MigPanel {
    public MenuEditor() {
        super("ins 2", "6[grow,fill][100!,fill]", "[grow,fill]");
        setOpaque(false);
    }

    protected JLabel getLbl(String name, ImageIcon icon) {
        JLabel ret = new JLabel(name, icon, JLabel.LEADING);
        ret.setIconTextGap(7);
        return ret;
    }
}
