package org.jdownloader.gui.vote;

import jd.gui.swing.jdgui.AbstractBugFinderWindow;

import org.appwork.utils.swing.EDTRunner;

public class VoteWindowController {
    private static final VoteWindowController INSTANCE = new VoteWindowController();

    /**
     * get the only existing instance of VoteWindowController. This is a singleton
     * 
     * @return
     */
    public static VoteWindowController getInstance() {
        return VoteWindowController.INSTANCE;
    }

    private AbstractBugFinderWindow votewindow;

    /**
     * Create a new instance of VoteWindowController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private VoteWindowController() {

    }

    public void show() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (votewindow != null && votewindow.isVisible()) {
                    votewindow.setVisible(false);
                    votewindow.dispose();
                } else {
                    votewindow = new DownloadBugFinderWindow();
                }

            }
        };

    }
}
