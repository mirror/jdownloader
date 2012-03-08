package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;

public class CopyURLAction extends ContextMenuAction {

    private static final long       serialVersionUID = -8775747188751533463L;
    private ArrayList<AbstractNode> selection;

    public CopyURLAction(ArrayList<AbstractNode> selection) {
        this.selection = selection;
        init();
    }

    @Override
    protected String getIcon() {
        return "cut";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_copyLink();
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                HashSet<String> list = new HashSet<String>();
                for (AbstractNode a : selection) {
                    if (a instanceof FilePackage) {
                        FilePackage fp = (FilePackage) a;
                        List<DownloadLink> children = null;
                        synchronized (fp) {
                            children = new ArrayList<DownloadLink>(fp.getChildren());
                        }
                        for (DownloadLink dl : children) {
                            if (dl.getLinkType() == DownloadLink.LINKTYPE_NORMAL) list.add(dl.getBrowserUrl());
                        }
                    } else if (a instanceof DownloadLink) {
                        DownloadLink dl = (DownloadLink) a;
                        if (dl.getLinkType() == DownloadLink.LINKTYPE_NORMAL) list.add(dl.getBrowserUrl());
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (String link : list) {
                    if (sb.length() > 0) sb.append("\r\n");
                    sb.append(link);
                }
                ClipboardMonitoring.getINSTANCE().setCurrentContent(sb.toString());
            }
        }, true);

    }
}