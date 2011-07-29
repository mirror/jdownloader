package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.LinkChecker;
import jd.controlling.LinkChecker.LinkCheckJob;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;

public class CheckStatusAction extends ContextMenuAction {

    private static final long             serialVersionUID = 6821943398259956694L;

    private final ArrayList<DownloadLink> links;

    public CheckStatusAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "network-idle";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_check() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        final LinkCheckJob job = LinkChecker.getInstance().check(links.toArray(new DownloadLink[links.size()]), true);
        new Thread() {

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
                System.out.println("job is done: " + job.waitChecked());
            }

        }.start();
    }

    @Override
    public boolean isEnabled() {
        return links != null && links.size() > 0;
    }

}