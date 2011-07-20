package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.translator.gui.TranslatorGui;

public class TestSvnAction extends AbstractAction {
    private TranslatorGui gui;

    public TestSvnAction(TranslatorGui gui) {
        super("Test login");
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent arg0) {
        IOEQ.add(new Runnable() {

            public void run() {
                String url = "svn://svn.jdownloader.org/jdownloader";
                boolean isOkay = false;
                if (!gui.isSvnLoginOK()) {
                    if (gui.validateSvnLogin(gui.getSvnUser(), gui.getSvnPass())) {
                        gui.setSvnLoginOK(true);
                        isOkay = true;
                    }
                } else {
                    isOkay = true;
                }

                if (isOkay) {
                    Dialog.getInstance().showMessageDialog("SVN Test OK", "Login successful. Username and password are OK.\r\n\r\nServer: " + url);
                } else {
                    Dialog.getInstance().showMessageDialog("SVN Test Error", "Login failed. Username and/or password are not correct!\r\n\r\nServer: " + url);
                }
            }

        }, true);

    }
}
