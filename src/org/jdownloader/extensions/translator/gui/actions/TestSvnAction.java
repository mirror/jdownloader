package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.nutils.svn.Subversion;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.translator.gui.TranslatorGui;
import org.tmatesoft.svn.core.SVNException;

public class TestSvnAction extends AbstractAction {
    private TranslatorGui gui;

    public TestSvnAction(TranslatorGui gui) {
        super("Test login");
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent arg0) {
        String url = "svn://svn.jdownloader.org/jdownloader";

        if (!this.gui.isSvnLoginOK()) {
            this.gui.validateSvnLogin();
            return;
        }

        String user = this.gui.getSvnUser();
        String pass = this.gui.getSvnPass();

        try {
            Subversion s = new Subversion(url, user, pass);
            Dialog.getInstance().showMessageDialog("SVN Test OK", "Login successful. Username and password are OK.\r\n\r\nServer: " + url);
        } catch (SVNException e) {
            Dialog.getInstance().showMessageDialog("SVN Test Error", "Login failed. Username and/or password are not correct!\r\n\r\nServer: " + url);
        }
    }

}
