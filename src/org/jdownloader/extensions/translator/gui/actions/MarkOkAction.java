package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.jdownloader.extensions.translator.gui.TranslatorGui;

public class MarkOkAction extends AbstractAction {

    private TranslatorGui master;

    public MarkOkAction(TranslatorGui master) {
        super("Mark OK");
        this.master = master;
    }

    public void actionPerformed(ActionEvent e) {
        JCheckBoxMenuItem s = (JCheckBoxMenuItem) e.getSource();
        master.setMarkOK(s.isSelected());
    }

}
