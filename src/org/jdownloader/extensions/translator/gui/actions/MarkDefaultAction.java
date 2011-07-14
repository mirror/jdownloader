package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.jdownloader.extensions.translator.gui.TranslatorGui;

public class MarkDefaultAction extends AbstractAction {

    private TranslatorGui master;

    public MarkDefaultAction(TranslatorGui master) {
        super("Mark Default");
        this.master = master;
    }

    public void actionPerformed(ActionEvent e) {
        JCheckBoxMenuItem s = (JCheckBoxMenuItem) e.getSource();
        master.setMarkDefault(s.getState());
    }

}
