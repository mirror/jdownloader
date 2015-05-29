package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jd.controlling.TaskQueue;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelEventWrapper;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;

public class ConfirmHashValuesDownloadAction extends CustomizableTableContextAppAction implements ActionContext, ExtTableListener, ExtTableModelListener {

    private IncludedSelectionSetup includedSelection;

    public ConfirmHashValuesDownloadAction() {
        super(true, true);
        setIconKey(IconKey.ICON_HASHSUM);
        setName(_GUI._.ConfirmHashValuesAction());
        addContextSetup(includedSelection = new IncludedSelectionSetup(DownloadsTable.getInstance(), this, this));
    }

    @Override
    protected void initTableContext(boolean empty, boolean selection) {
        super.initTableContext(empty, selection);
    }

    @Override
    public void initContextDefaults() {
        super.initContextDefaults();

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final SelectionInfo<FilePackage, DownloadLink> selection = getSelection();
        final List<DownloadLink> links;
        switch (includedSelection.getSelectionType()) {
        case NONE:
            links = null;
            return;
        case SELECTED:
            links = selection.getChildren();
            break;
        case UNSELECTED:
            if (selection.getUnselectedChildren() != null) {
                links = selection.getUnselectedChildren();
            } else {
                links = null;
            }
            break;
        case ALL:
        default:
            links = DownloadsTable.getInstance().getSelectionInfo(false, true).getChildren();
            break;
        }
        if (links == null || links.size() == 0) {
            return;
        }
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                HashMap<String, List<DownloadLink>> map = new HashMap<String, List<DownloadLink>>();
                for (DownloadLink cl : links) {
                    List<DownloadLink> list = map.get(cl.getName());
                    if (list == null) {
                        list = new ArrayList<DownloadLink>();
                        map.put(cl.getName(), list);
                    }
                    list.add(cl);
                }
                main: for (Entry<String, List<DownloadLink>> se : map.entrySet()) {
                    List<DownloadLink> list = se.getValue();
                    String md5 = null;
                    String sha1 = null;
                    String sha256 = null;
                    for (DownloadLink cl : list) {
                        if (cl.getDownloadLink().getMD5Hash() != null) {
                            if (md5 != null && !StringUtils.equalsIgnoreCase(md5, cl.getDownloadLink().getMD5Hash())) {
                                // hashes do not match
                                continue main;
                            } else {
                                md5 = cl.getDownloadLink().getMD5Hash();
                            }
                        }
                        if (cl.getDownloadLink().getSha1Hash() != null) {
                            if (sha1 != null && !StringUtils.equalsIgnoreCase(sha1, cl.getDownloadLink().getSha1Hash())) {
                                // hashes do not match
                                continue main;
                            } else {
                                sha1 = cl.getDownloadLink().getSha1Hash();
                            }
                        }
                        if (cl.getDownloadLink().getSha256Hash() != null) {
                            if (sha256 != null && !StringUtils.equalsIgnoreCase(sha256, cl.getDownloadLink().getSha256Hash())) {
                                // hashes do not match
                                continue main;
                            } else {
                                sha256 = cl.getDownloadLink().getSha256Hash();
                            }
                        }
                    }
                    for (DownloadLink cl : list) {
                        if (md5 != null) {
                            cl.getDownloadLink().setMD5Hash(md5);
                        }
                        if (sha1 != null) {
                            cl.getDownloadLink().setSha1Hash(sha1);
                        }
                        if (sha256 != null) {
                            cl.getDownloadLink().setSha1Hash(sha256);
                        }
                    }
                }
                return null;
            }
        });
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {

    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper event) {
    }

}
