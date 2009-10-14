package jd.plugins.optional.jdtrayicon;

import java.awt.event.MouseEvent;

import javax.swing.JToggleButton;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;

public class HoverEffect extends JDMouseAdapter {

    private final JToggleButton comp;

    public HoverEffect(JToggleButton comp) {
        this.comp = comp;
    }

    @Override
    public void mouseEntered(MouseEvent evt) {
        comp.setOpaque(true);
        comp.setContentAreaFilled(true);
        comp.setBorderPainted(true);
    }

    @Override
    public void mouseExited(MouseEvent evt) {
        comp.setOpaque(false);
        comp.setContentAreaFilled(false);
        comp.setBorderPainted(false);
    }

}