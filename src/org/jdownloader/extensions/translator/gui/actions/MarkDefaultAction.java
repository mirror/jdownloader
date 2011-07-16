package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.jdownloader.extensions.translator.gui.TranslatorGui;
import org.jdownloader.images.NewTheme;

public class MarkDefaultAction extends AbstractAction {

    private TranslatorGui master;

    public MarkDefaultAction(TranslatorGui master) {
        super("Mark Default", NewTheme.I().getIcon("flags/en", 16));
        this.master = master;
    }

    public void actionPerformed(ActionEvent e) {
        JCheckBoxMenuItem s = (JCheckBoxMenuItem) e.getSource();
        master.setMarkDefault(s.getState());
    }

}
