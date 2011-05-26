//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.downloads;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.controlling.DownloadController;
import jd.controlling.DownloadController.MOVE;
import jd.controlling.DownloadControllerEvent;
import jd.controlling.DownloadControllerListener;
import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkInfoCache;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageInfoCache;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class DownloadLinksPanel extends SwitchPanel implements ActionListener, DownloadControllerListener {

    private static final long         serialVersionUID                        = -6029423913449902141L;

    private static final int          NO_JOB                                  = -1;
    public final static int           REFRESH_ALL_DATA_CHANGED                = 1;
    public final static int           REFRESH_DATA_AND_STRUCTURE_CHANGED      = 0;
    public final static int           REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST = 10;
    public static final int           REFRESH_SPECIFIED_LINKS                 = 2;

    private final static int          UPDATE_TIMING                           = 250;

    private static DownloadLinksPanel INSTANCE                                = null;

    private int                       jobID                                   = REFRESH_DATA_AND_STRUCTURE_CHANGED;
    private ArrayList<Object>         jobObjects                              = new ArrayList<Object>();

    protected Logger                  logger                                  = JDLogger.getLogger();

    private DownloadTable             internalTable;

    private Timer                     asyncUpdate;

    private long                      latestAsyncUpdate;

    private FilePackageInfo           filePackageInfo;

    private JScrollPane               scrollPane;

    private boolean                   notvisible                              = true;

    private DownloadLinksPanel() {
        super(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[grow, fill]"));
        internalTable = new DownloadTable(this);
        scrollPane = new JScrollPane(internalTable);
        filePackageInfo = new FilePackageInfo();

        scrollPane.setBorder(null);

        this.add(scrollPane, "cell 0 0");
        JDUtilities.getDownloadController().addListener(this);
        asyncUpdate = new Timer(UPDATE_TIMING, this);
        latestAsyncUpdate = 0;
        asyncUpdate.setInitialDelay(UPDATE_TIMING);
        asyncUpdate.setRepeats(false);
        asyncUpdate.restart();

        internalTable.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ActionController.getToolBarAction("action.downloadview.movetotop").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ActionController.getToolBarAction("action.downloadview.moveup").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ActionController.getToolBarAction("action.downloadview.movedown").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ActionController.getToolBarAction("action.downloadview.movetobottom").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_FOCUSED).remove(ActionController.getToolBarAction("action.downloadview.movetotop").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_FOCUSED).remove(ActionController.getToolBarAction("action.downloadview.moveup").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_FOCUSED).remove(ActionController.getToolBarAction("action.downloadview.movedown").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_FOCUSED).remove(ActionController.getToolBarAction("action.downloadview.movetobottom").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(ActionController.getToolBarAction("action.downloadview.movetotop").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(ActionController.getToolBarAction("action.downloadview.moveup").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(ActionController.getToolBarAction("action.downloadview.movedown").getKeyStroke());
        internalTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(ActionController.getToolBarAction("action.downloadview.movetobottom").getKeyStroke());

        MainTabbedPane.getInstance().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ActionController.getToolBarAction("action.downloadview.movetotop").getKeyStroke());
        MainTabbedPane.getInstance().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ActionController.getToolBarAction("action.downloadview.moveup").getKeyStroke());
        MainTabbedPane.getInstance().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ActionController.getToolBarAction("action.downloadview.movedown").getKeyStroke());
        MainTabbedPane.getInstance().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ActionController.getToolBarAction("action.downloadview.movetobottom").getKeyStroke());
    }

    /**
     * Override the requestFocusInWindow to request the Focus in the underlying
     * internal Table
     */
    @Override
    public boolean requestFocusInWindow() {
        return internalTable.requestFocusInWindow();
    }

    public boolean isNotVisible() {
        return notvisible;
    }

    public static synchronized DownloadLinksPanel getDownloadLinksPanel() {
        if (INSTANCE == null) INSTANCE = new DownloadLinksPanel();
        return INSTANCE;
    }

    public void move(MOVE mode) {
        ArrayList<FilePackage> fps = internalTable.getSelectedFilePackages();
        ArrayList<DownloadLink> links = internalTable.getSelectedDownloadLinks();
        if (fps.size() > 0) DownloadController.getInstance().move(fps, null, mode);
        if (links.size() > 0) DownloadController.getInstance().move(links, null, mode);
    }

    public void showFilePackageInfo(FilePackage fp) {
        filePackageInfo.setPackage(fp);
        new GuiRunnable<Object>() {
            public Object runSave() {
                DownloadView.getInstance().setInfoPanel(filePackageInfo);
                return null;
            }
        }.start();
    }

    public void showDownloadLinkInfo(DownloadLink downloadLink) {
        filePackageInfo.setDownloadLink(downloadLink);
        new GuiRunnable<Object>() {
            public Object runSave() {
                DownloadView.getInstance().setInfoPanel(filePackageInfo);
                return null;
            }
        }.start();
    }

    public void hideFilePackageInfo() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                DownloadView.getInstance().setInfoPanel(null);
                return null;
            }
        }.start();
    }

    public void fireTableChanged(int id, ArrayList<Object> objs) {
        try {
            internalTable.fireTableChanged(id, new ArrayList<Object>(objs));
        } catch (Exception e) {
            logger.severe("TreeTable Exception, complete refresh!");
            updateTableTask(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);
        }
        return;
    }

    public void onShow() {
        DownloadLinkInfoCache.setMaxItems(internalTable);
        FilePackageInfoCache.setMaxItems(internalTable);
        updateTableTask(REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST, null);
        JDUtilities.getDownloadController().addListener(this);
        internalTable.removeKeyListener(internalTable);
        internalTable.addKeyListener(internalTable);
        notvisible = false;
    }

    public void onHide() {
        notvisible = true;
        JDUtilities.getDownloadController().removeListener(this);
        asyncUpdate.stop();
        internalTable.removeKeyListener(internalTable);
    }

    @SuppressWarnings("unchecked")
    public void updateTableTask(int id, Object Param) {
        if (!isShown()) {
            asyncUpdate.stop();
            return;
        }
        boolean changed = false;
        asyncUpdate.stop();
        if (id == REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST) {
            this.jobID = REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST;
            fireTableTask();
            asyncUpdate.restart();
            return;
        }

        synchronized (jobObjects) {
            switch (id) {
            case REFRESH_DATA_AND_STRUCTURE_CHANGED: {
                changed = true;
                this.jobID = REFRESH_DATA_AND_STRUCTURE_CHANGED;
                this.jobObjects.clear();
                break;
            }
            case REFRESH_ALL_DATA_CHANGED: {
                switch (this.jobID) {
                case REFRESH_DATA_AND_STRUCTURE_CHANGED:
                case REFRESH_ALL_DATA_CHANGED:
                    break;
                case NO_JOB:
                case REFRESH_SPECIFIED_LINKS:
                    changed = true;
                    this.jobID = REFRESH_ALL_DATA_CHANGED;
                    this.jobObjects.clear();
                    break;
                }
                break;
            }
            case REFRESH_SPECIFIED_LINKS: {
                switch (this.jobID) {
                case REFRESH_DATA_AND_STRUCTURE_CHANGED:
                case REFRESH_ALL_DATA_CHANGED:
                    break;
                case NO_JOB:
                case REFRESH_SPECIFIED_LINKS:
                    this.jobID = REFRESH_SPECIFIED_LINKS;
                    if (Param instanceof DownloadLink) {
                        if (!jobObjects.contains(Param)) {
                            changed = true;
                            jobObjects.add(Param);
                        }
                        if (!jobObjects.contains(((DownloadLink) Param).getFilePackage())) {
                            changed = true;
                            jobObjects.add(((DownloadLink) Param).getFilePackage());
                        }
                    } else if (Param instanceof ArrayList) {
                        for (DownloadLink dl : (ArrayList<DownloadLink>) Param) {
                            if (!jobObjects.contains(dl)) {
                                changed = true;
                                jobObjects.add(dl);
                            }
                            if (!jobObjects.contains(dl.getFilePackage())) {
                                changed = true;
                                jobObjects.add(dl.getFilePackage());
                            }
                        }
                    }
                    break;
                }
            }
            }

        }
        if (!changed && (System.currentTimeMillis() - latestAsyncUpdate > UPDATE_TIMING + 100)) {
            fireTableTask();
        }
        asyncUpdate.restart();
    }

    private void fireTableTask() {
        latestAsyncUpdate = System.currentTimeMillis();

        synchronized (jobObjects) {
            if (isShown() && jobID != NO_JOB) fireTableChanged(this.jobID, this.jobObjects);
            this.jobID = NO_JOB;
            this.jobObjects.clear();
        }

    }

    public void actionPerformed(final ActionEvent e) {
        new Thread("DownloadLinks: actionPerformed") {
            public void run() {
                if (e.getSource() == DownloadLinksPanel.this.asyncUpdate) {
                    fireTableTask();
                }
            }
        }.start();
    }

    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getEventID()) {
        case DownloadControllerEvent.REMOVE_FILPACKAGE:
            if (filePackageInfo.getPackage() != null && filePackageInfo.getPackage() == ((FilePackage) event.getParameter())) {
                this.hideFilePackageInfo();
            }
            break;
        case DownloadControllerEvent.REMOVE_DOWNLOADLINK:
            if (filePackageInfo.getDownloadLink() != null && filePackageInfo.getDownloadLink() == ((DownloadLink) event.getParameter())) {
                this.hideFilePackageInfo();
            }
            break;
        case DownloadControllerEvent.REFRESH_STRUCTURE:
            updateTableTask(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);
            break;
        case DownloadControllerEvent.REFRESH_SPECIFIC:
            updateTableTask(REFRESH_SPECIFIED_LINKS, event.getParameter());
            break;
        case DownloadControllerEvent.REFRESH_ALL:
            updateTableTask(REFRESH_ALL_DATA_CHANGED, null);
            break;
        }
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public boolean isFilePackageInfoVisible(Object obj) {
        boolean visible = DownloadView.getInstance().getInfoPanel() == filePackageInfo;
        if (obj != null) {
            if (obj instanceof DownloadLink && filePackageInfo.getDownloadLink() == obj && visible) return true;
            if (obj instanceof FilePackage && filePackageInfo.getPackage() == obj && visible) return true;
            return false;
        }
        return visible;
    }
}
