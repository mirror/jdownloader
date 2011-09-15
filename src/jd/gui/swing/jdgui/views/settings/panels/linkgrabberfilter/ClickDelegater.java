package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.appwork.swing.components.ExtCheckBox;

public class ClickDelegater extends MouseAdapter {

    private ExtCheckBox cb;

    public ClickDelegater(ExtCheckBox cbAudio) {
        cb = cbAudio;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        cb.setSelected(!cb.isSelected());
    }

}
