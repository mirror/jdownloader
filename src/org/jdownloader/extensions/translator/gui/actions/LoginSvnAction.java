package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;

import org.jdownloader.extensions.translator.gui.TranslatorGui;

public class LoginSvnAction extends AbstractAction {
    private TranslatorGui gui;

    public LoginSvnAction(TranslatorGui gui) {
        super("Change login");
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {

            public void run() {
                gui.requestSvnLogin();
            }

        });
    }

}
