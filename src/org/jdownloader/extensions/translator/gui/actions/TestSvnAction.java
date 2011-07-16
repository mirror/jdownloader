package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JTextField;

import jd.nutils.svn.Subversion;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.images.NewTheme;
import org.tmatesoft.svn.core.SVNException;

public class TestSvnAction extends AbstractAction {
    private JTextField user;
    private JTextField pass;

    public TestSvnAction(JTextField user, JTextField pass) {
        super("Test SVN", NewTheme.I().getIcon("flags/en", 16));
        this.user = user;
        this.pass = pass;
    }

    public void actionPerformed(ActionEvent arg0) {
        String url = "svn://svn.jdownloader.org/jdownloader";
        String user = this.user.getText();
        String pass = this.pass.getText();

        if (user.length() <= 0) {
            Dialog.getInstance().showMessageDialog("SVN Test Error", "Please input a valid username.");
        } else if (pass.length() <= 0)
            Dialog.getInstance().showMessageDialog("SVN Test Error", "Please input a valid password.");
        else
            try {
                Subversion s = new Subversion(url, user, pass);
                Dialog.getInstance().showMessageDialog("SVN Test OK", "Login successful. Username and password are OK.\r\n\r\nServer: " + url);
            } catch (SVNException e) {
                Dialog.getInstance().showMessageDialog("SVN Test Error", "Login failed. Username and/or password are not correct!\r\n\r\nServer: " + url);
            }
    }

}
