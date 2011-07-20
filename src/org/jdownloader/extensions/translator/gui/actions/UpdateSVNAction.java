package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.extensions.translator.gui.TranslatorGui;

public class UpdateSVNAction extends AbstractAction {
    private UpdateSVNThread thread;
    private TranslatorGui   gui;

    public UpdateSVNAction(TranslatorGui gui) {
        super("Update");
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent e) {
        thread = new UpdateSVNThread(gui);
        thread.start();
    }

}
