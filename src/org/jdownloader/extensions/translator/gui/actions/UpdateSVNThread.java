package org.jdownloader.extensions.translator.gui.actions;

import java.io.File;

import jd.nutils.svn.Subversion;

import org.appwork.utils.Application;
import org.jdownloader.extensions.translator.gui.TranslatorGui;
import org.tmatesoft.svn.core.SVNException;

public class UpdateSVNThread extends Thread {
    private TranslatorGui gui;

    public UpdateSVNThread(TranslatorGui gui) {
        super();
        this.gui = gui;
    }

    @Override
    public void run() {
        this.gui.setSvnBusy(true);

        File wd = new File(Application.getRoot());
        if (!wd.exists()) wd.mkdirs();

        try {
            Subversion svn;
            svn = new Subversion("svn://svn.jdownloader.org/jdownloader/trunk/translations");

            // svn.export(wd);
            svn.update(wd, null);
            svn.dispose();

        } catch (SVNException e) {
            e.printStackTrace();
        }

        this.gui.setSvnBusy(false);
    }
}
