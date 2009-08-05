package jd.gui.swing.jdgui.interfaces;

import jd.gui.swing.jdgui.borders.InsideShadowBorder;
import jd.gui.swing.jdgui.borders.JDBorderFactory;

/**
 * A JPanel with an Dropshadow border on top
 */
public abstract class DroppedPanel extends SwitchPanel {

    private static final long serialVersionUID = -4849858185626557726L;

    public DroppedPanel() {
        InsideShadowBorder border = new InsideShadowBorder(5, 0, 0, 0);
      
        this.setBorder(border);
    }
}
