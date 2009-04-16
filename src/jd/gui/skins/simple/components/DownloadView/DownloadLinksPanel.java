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

package jd.gui.skins.simple.components.DownloadView;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadControllerEvent;
import jd.controlling.DownloadControllerListener;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.Linkgrabber.LinkCheck;
import jd.gui.skins.simple.components.Linkgrabber.LinkCheckEvent;
import jd.gui.skins.simple.components.Linkgrabber.LinkCheckListener;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class DownloadLinksPanel extends JTabbedPanel implements ActionListener, DownloadControllerListener, LinkCheckListener {

    /**
     * 
     */
    private static final long serialVersionUID = -6029423913449902141L;

    private final int NO_JOB = -1;
    public final static int REFRESH_ALL_DATA_CHANGED = 1;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED = 0;
    public static final int REFRESH_SPECIFIED_LINKS = 2;

    private final static int UPDATE_TIMING = 100;

    private int job_ID = REFRESH_DATA_AND_STRUCTURE_CHANGED;
    private ArrayList<DownloadLink> job_links = new ArrayList<DownloadLink>();

    private boolean lastSort = false;

    protected Logger logger = jd.controlling.JDLogger.getLogger();

    private DownloadTreeTable internalTreeTable;

    private Timer Update_Async;

    private long last_async_update;

    public DownloadLinksPanel() {
        super(new BorderLayout());
        internalTreeTable = new DownloadTreeTable(new DownloadTreeTableModel(), this);
        JScrollPane scrollPane = new JScrollPane(internalTreeTable);
        this.add(scrollPane);
        JDUtilities.getDownloadController().getBroadcaster().addListener(this);
        Update_Async = new Timer(UPDATE_TIMING, this);
        last_async_update = 0;
        Update_Async.setInitialDelay(UPDATE_TIMING);
        Update_Async.setRepeats(false);
        Update_Async.restart();
    }

    public synchronized void fireTableChanged(int id, Object param) {
        synchronized (JDUtilities.getController().getPackages()) {
            internalTreeTable.fireTableChanged(id, param);
        }
    }

    @Override
    public void onDisplay() {
        internalTreeTable.removeKeyListener(internalTreeTable);
        internalTreeTable.addKeyListener(internalTreeTable);
    }

    @Override
    public void onHide() {
        internalTreeTable.removeKeyListener(internalTreeTable);
    }

    @SuppressWarnings("unchecked")
    private synchronized void updateTableTask(int id, Object Param) {
        boolean changed = false;
        Update_Async.stop();
        switch (id) {
        case REFRESH_DATA_AND_STRUCTURE_CHANGED: {
            changed = true;
            this.job_ID = REFRESH_DATA_AND_STRUCTURE_CHANGED;
            this.job_links.clear();
            break;
        }
        case REFRESH_ALL_DATA_CHANGED: {
            switch (this.job_ID) {
            case REFRESH_DATA_AND_STRUCTURE_CHANGED:
            case REFRESH_ALL_DATA_CHANGED:
                break;
            case NO_JOB:
            case REFRESH_SPECIFIED_LINKS:
                changed = true;
                this.job_ID = REFRESH_ALL_DATA_CHANGED;
                this.job_links.clear();
                break;
            }
            break;
        }
        case REFRESH_SPECIFIED_LINKS: {
            switch (this.job_ID) {
            case REFRESH_DATA_AND_STRUCTURE_CHANGED:
            case REFRESH_ALL_DATA_CHANGED:
                break;
            case NO_JOB:
            case REFRESH_SPECIFIED_LINKS:
                this.job_ID = REFRESH_SPECIFIED_LINKS;
                if (Param instanceof DownloadLink) {
                    if (!job_links.contains(Param)) {
                        changed = true;
                        job_links.add((DownloadLink) Param);
                    }
                } else if (Param instanceof ArrayList) {
                    for (DownloadLink dl : (ArrayList<DownloadLink>) Param) {
                        if (!job_links.contains(dl)) {
                            changed = true;
                            job_links.add(dl);
                        }
                    }
                }
                break;
            }
        }
        }
        if (!changed && (System.currentTimeMillis() - last_async_update > UPDATE_TIMING + 100)) {
            fireTableTask();
        }
        Update_Async.restart();
    }

    private synchronized void fireTableTask() {
        last_async_update = System.currentTimeMillis();
        fireTableChanged(this.job_ID, this.job_links);
        this.job_ID = this.NO_JOB;
        this.job_links.clear();
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.Update_Async) {
            fireTableTask();
            return;
        }
        Vector<FilePackage> selected_packages = new Vector<FilePackage>();
        Vector<DownloadLink> selected_links = new Vector<DownloadLink>();
        HashMap<String, Object> prop = new HashMap<String, Object>();
        FilePackage fp = null;
        DownloadLink link = null;
        File folder = null;
        int col = 0;
        if (e.getSource() instanceof JMenuItem) {
            switch (e.getID()) {
            case TreeTableAction.EDIT_NAME:
            case TreeTableAction.EDIT_DIR:
                selected_packages = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");
                break;
            case TreeTableAction.SORT:
                col = (Integer) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("col");
                selected_packages = new Vector<FilePackage>(this.internalTreeTable.getSelectedFilePackages());
                break;
            case TreeTableAction.DOWNLOAD_PRIO:
            case TreeTableAction.DE_ACTIVATE:
                prop = (HashMap<String, Object>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("infos");
                selected_links = (Vector<DownloadLink>) prop.get("links");
                break;
            case TreeTableAction.DELETE:
            case TreeTableAction.SET_PW:
            case TreeTableAction.NEW_PACKAGE:
            case TreeTableAction.CHECK:
            case TreeTableAction.DOWNLOAD_COPY_URL:
            case TreeTableAction.DOWNLOAD_COPY_PASSWORD:
            case TreeTableAction.DOWNLOAD_RESET:
            case TreeTableAction.DOWNLOAD_DLC:
            case TreeTableAction.DOWNLOAD_RESUME:
                selected_links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("links");
                break;
            case TreeTableAction.DOWNLOAD_DIR:
                folder = (File) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("folder");
                break;
            case TreeTableAction.DOWNLOAD_BROWSE_LINK:
                link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
                break;
            }
        } else if (e.getSource() instanceof TreeTableAction) {
            switch (e.getID()) {
            case TreeTableAction.SORT_ALL:
                col = (Integer) ((TreeTableAction) e.getSource()).getProperty().getProperty("col");
                break;
            case TreeTableAction.DELETE:
                selected_links = (Vector<DownloadLink>) ((TreeTableAction) e.getSource()).getProperty().getProperty("links");
                break;
            }
        }
        switch (e.getID()) {
        case TreeTableAction.EDIT_DIR: {
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText(JDLocale.L("gui.btn_ok", "OK"));
            fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
            fc.setCurrentDirectory(selected_packages.get(0).getDownloadDirectory() != null ? new File(selected_packages.get(0).getDownloadDirectory()) : JDUtilities.getResourceFile("downloads"));
            if (fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION) {
                File ret = fc.getSelectedFile();
                if (ret != null) {
                    for (int i = 0; i < selected_packages.size(); i++) {
                        selected_packages.elementAt(i).setDownloadDirectory(ret.getAbsolutePath());
                    }
                }
            }
            return;
        }
        case TreeTableAction.EDIT_NAME: {
            String name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.editpackagename.message", "Neuer Paketname"), selected_packages.get(0).getName());
            if (name != null) {
                for (int i = 0; i < selected_packages.size(); i++) {
                    selected_packages.elementAt(i).setName(name);
                }
            }
            return;
        }
        case TreeTableAction.DOWNLOAD_RESUME: {
            for (int i = 0; i < selected_links.size(); i++) {
                selected_links.elementAt(i).getLinkStatus().setStatus(LinkStatus.TODO);
                selected_links.elementAt(i).getLinkStatus().setStatusText(JDLocale.L("gui.linklist.status.doresume", "Warte auf Fortsetzung"));
            }
            return;
        }
        case TreeTableAction.DOWNLOAD_BROWSE_LINK: {
            if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                try {
                    JLinkButton.openURL(link.getBrowserUrl());
                } catch (Exception e1) {
                    JDLogger.exception(e1);
                }
            }
            return;
        }
        case TreeTableAction.DOWNLOAD_DIR: {
            JDUtilities.openExplorer(folder);
            return;
        }
        case TreeTableAction.DOWNLOAD_DLC: {
            JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
            fc.showSaveDialog(SimpleGUI.CURRENTGUI);
            File ret = fc.getSelectedFile();
            if (ret == null) return;
            if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                ret = new File(ret.getAbsolutePath() + ".dlc");
            }
            JDUtilities.getController().saveDLC(ret, selected_links);
            return;
        }
        case TreeTableAction.DOWNLOAD_RESET: {
            boolean b = true;
            if (!SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                b = false;
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.reset", "Reset selected downloads?") + " (" + JDLocale.LF("gui.downloadlist.delete.size_packagev2", "%s links", selected_links.size()) + ")")) {
                    b = true;
                }
            }
            if (b) {
                for (int i = 0; i < selected_links.size(); i++) {
                    selected_links.elementAt(i).reset();
                }
            }
            return;
        }
        case TreeTableAction.DOWNLOAD_COPY_PASSWORD: {
            Set<String> List = new HashSet<String>();
            StringBuilder build = new StringBuilder();
            for (int i = 0; i < selected_links.size(); i++) {
                String pw = selected_links.get(i).getFilePackage().getPassword();
                if (!List.contains(pw)) {
                    List.add(pw);
                    build.append(pw + "\n");
                }
                if (selected_links.get(i).getStringProperty("pass", null) != null) {
                    pw = selected_links.get(i).getStringProperty("pass", null);
                    if (!List.contains(pw)) {
                        List.add(pw);
                        build.append(pw + "\n");
                    }
                }
            }
            String builded = build.toString();
            ClipboardHandler.getClipboard().setOldData(builded);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(builded), null);
            return;
        }
        case TreeTableAction.DOWNLOAD_COPY_URL: {
            Set<String> List = new HashSet<String>();
            StringBuilder build = new StringBuilder();
            for (int i = 0; i < selected_links.size(); i++) {
                String url = selected_links.elementAt(i).getBrowserUrl();
                if (!List.contains(url)) {
                    List.add(url);
                    build.append(url + "\n");
                }
            }
            String builded = build.toString();
            ClipboardHandler.getClipboard().setOldData(builded);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(builded), null);
            return;
        }
        case TreeTableAction.DOWNLOAD_PRIO: {
            int prio = (Integer) prop.get("prio");
            for (int i = 0; i < selected_links.size(); i++) {
                selected_links.elementAt(i).setPriority(prio);
            }
            return;
        }
        case TreeTableAction.CHECK:
            LinkCheck.getLinkChecker().checkLinks(selected_links);
            LinkCheck.getLinkChecker().getBroadcaster().addListener(this);
            break;
        case TreeTableAction.SORT_ALL:
            sort(col);
            break;
        case TreeTableAction.SORT:
            for (int i = 0; i < selected_packages.size(); i++) {
                selected_packages.get(i).sort(col);
            }
            break;
        case TreeTableAction.DE_ACTIVATE: {
            boolean b = (Boolean) prop.get("boolean");
            for (int i = 0; i < selected_links.size(); i++) {
                selected_links.get(i).setEnabled(b);
            }
            JDUtilities.getDownloadController().getBroadcaster().fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_STRUCTURE));
            return;
        }
        case TreeTableAction.NEW_PACKAGE: {
            fp = selected_links.get(0).getFilePackage();
            String name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.newpackage.message", "Name of the new package"), fp.getName());
            if (name != null) {
                FilePackage nfp = new FilePackage();
                nfp.setName(name);
                for (int i = 0; i < selected_links.size(); i++) {
                    selected_links.elementAt(i).setFilePackage(nfp);
                }
                JDUtilities.getDownloadController().addPackage(nfp);
            }
            return;
        }
        case TreeTableAction.SET_PW: {
            String pw = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.setpw.message", "Set download password"), null);
            for (int i = 0; i < selected_links.size(); i++) {
                selected_links.elementAt(i).setProperty("pass", pw);
            }
            return;
        }
        case TreeTableAction.DELETE: {
            boolean b = true;
            if (!SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                b = false;
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?") + " (" + JDLocale.LF("gui.downloadlist.delete.size_packagev2", "%s links", selected_links.size()) + ")")) {
                    b = true;
                }
            }
            if (b) {
                for (int i = 0; i < selected_links.size(); i++) {
                    selected_links.elementAt(i).getFilePackage().remove(selected_links.elementAt(i));
                }
            }
            return;
        }
        }
    }

    private void sort(final int col) {
        lastSort = !lastSort;
        Vector<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
        synchronized (packages) {

            Collections.sort(packages, new Comparator<FilePackage>() {

                public int compare(FilePackage a, FilePackage b) {
                    FilePackage aa = a;
                    FilePackage bb = b;
                    if (lastSort) {
                        aa = b;
                        bb = a;
                    }
                    switch (col) {
                    case 1:
                        return aa.getName().compareToIgnoreCase(bb.getName());
                    case 2:
                        return aa.getHoster().compareToIgnoreCase(bb.getHoster());
                    case 3:
                        return aa.getRemainingLinks() > bb.getRemainingLinks() ? 1 : -1;
                    case 4:
                        return aa.getPercent() > bb.getPercent() ? 1 : -1;
                    default:
                        return -1;
                    }

                }

            });
        }
        JDUtilities.getDownloadController().fireStructureUpdate();
    }

    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getID()) {
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

    public void handle_LinkCheckEvent(LinkCheckEvent event) {
        switch (event.getID()) {
        case LinkCheckEvent.ABORT:
        case LinkCheckEvent.STOP:
            LinkCheck.getLinkChecker().getBroadcaster().removeListener(this);
            break;
        }
    }
}
