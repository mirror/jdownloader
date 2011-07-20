package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import jd.nutils.svn.Subversion;

import org.appwork.utils.Application;
import org.jdownloader.extensions.translator.gui.TranslatorGui;
import org.tmatesoft.svn.core.SVNException;

public class UpdateSVNAction extends AbstractAction {

    private TranslatorGui gui;

    public UpdateSVNAction(TranslatorGui gui) {
        super("Update");
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent e) {
        new Thread(new Runnable() {
            public void run() {

                gui.setSvnBusy(true);

                File wd = new File(Application.getRoot());
                if (!wd.exists()) wd.mkdirs();
                Subversion svn = null;
                try {

                    svn = new Subversion("svn://svn.jdownloader.org/jdownloader/trunk/translations");

                    svn.update(wd, null);
                    svn.dispose();

                } catch (SVNException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        svn.dispose();
                    } catch (final Throwable e) {
                    }
                }

                gui.setSvnBusy(false);
            }

        }).start();

    }

}
