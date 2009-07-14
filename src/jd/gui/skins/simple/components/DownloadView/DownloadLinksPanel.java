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
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadController;
import jd.controlling.DownloadControllerEvent;
import jd.controlling.DownloadControllerListener;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.JDCollapser;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class DownloadLinksPanel extends JTabbedPanel implements ActionListener, DownloadControllerListener, LinkCheckListener {

    private static final long serialVersionUID = -6029423913449902141L;

    private final int NO_JOB = -1;
    public final static int REFRESH_ALL_DATA_CHANGED = 1;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED = 0;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST = 10;
    public static final int REFRESH_SPECIFIED_LINKS = 2;

    private final static int UPDATE_TIMING = 250;

    private int job_ID = REFRESH_DATA_AND_STRUCTURE_CHANGED;
    private ArrayList<Object> job_objs = new ArrayList<Object>();

    private boolean lastSort = true;

    protected Logger logger = jd.controlling.JDLogger.getLogger();

    private DownloadTable internalTable;

    private Timer Update_Async;

    private long last_async_update;

    private boolean visible = true;

    private FilePackageInfo filePackageInfo;

    private boolean tablerefreshinprogress = false;

    private JScrollPane scrollPane;

    public DownloadLinksPanel() {
        super(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[grow, fill]"));
        internalTable = new DownloadTable(new DownloadJTableModel(), this);
        scrollPane = new JScrollPane(internalTable);
        filePackageInfo = new FilePackageInfo();
        this.add(scrollPane, "cell 0 0,gaptop 6");
        JDUtilities.getDownloadController().addListener(this);
        Update_Async = new Timer(UPDATE_TIMING, this);
        last_async_update = 0;
        Update_Async.setInitialDelay(UPDATE_TIMING);
        Update_Async.setRepeats(false);
        Update_Async.restart();
    }

    public boolean needsViewport() {
        return false;
    }

    public void showFilePackageInfo(FilePackage fp) {
        filePackageInfo.setPackage(fp);
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                JDCollapser.getInstance().setContentPanel(filePackageInfo);
                JDCollapser.getInstance().setTitle(JDL.L("gui.linkgrabber.packagetab.title", "FilePackage"));
                JDCollapser.getInstance().setVisible(true);
                JDCollapser.getInstance().setCollapsed(false);
                return null;
            }
        }.start();
    }

    public void hideFilePackageInfo() {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                JDCollapser.getInstance().setCollapsed(true);
                return null;
            }
        }.start();
    }

    public void fireTableChanged(int id, ArrayList<Object> objs) {
        if (tablerefreshinprogress && id != REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST) return;
        final ArrayList<Object> objs2 = new ArrayList<Object>(objs);
        final int id2 = id;
        new Thread() {
            public void run() {
                if (id2 != REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST) tablerefreshinprogress = true;
                if (id2 == REFRESH_DATA_AND_STRUCTURE_CHANGED || id2 == REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST) internalTable.getTableModel().refreshmodel();
                try {
                    internalTable.fireTableChanged(id2, objs2);
                } catch (Exception e) {
                    logger.severe("TreeTable Exception, complete refresh!");
                    updateTableTask(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);
                }

                if (id2 != REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST) tablerefreshinprogress = false;
                return;
            }
        }.start();
    }

    // @Override
    public void onDisplay() {
        visible = true;
        updateTableTask(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);
        fireTableTask();
        Update_Async.restart();
        JDUtilities.getDownloadController().addListener(this);
        internalTable.removeKeyListener(internalTable);
        internalTable.addKeyListener(internalTable);
    }

    // @Override
    public void onHide() {
        visible = false;
        JDUtilities.getDownloadController().removeListener(this);
        Update_Async.stop();
        internalTable.removeKeyListener(internalTable);
    }

    @SuppressWarnings("unchecked")
    public void updateTableTask(int id, Object Param) {
        if (!visible) {
            Update_Async.stop();
            return;
        }
        boolean changed = false;
        Update_Async.stop();
        if (id == REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST) {
            this.job_ID = REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST;
            fireTableTask();
            Update_Async.restart();
            return;
        }
        synchronized (job_objs) {
            switch (id) {
            case REFRESH_DATA_AND_STRUCTURE_CHANGED: {
                changed = true;
                this.job_ID = REFRESH_DATA_AND_STRUCTURE_CHANGED;
                this.job_objs.clear();
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
                    this.job_objs.clear();
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
                        if (!job_objs.contains(Param)) {
                            changed = true;
                            job_objs.add(Param);
                        }
                        if (!job_objs.contains(((DownloadLink) Param).getFilePackage())) {
                            changed = true;
                            job_objs.add(((DownloadLink) Param).getFilePackage());
                        }
                    } else if (Param instanceof ArrayList) {
                        for (DownloadLink dl : (ArrayList<DownloadLink>) Param) {
                            if (!job_objs.contains(dl)) {
                                changed = true;
                                job_objs.add(dl);
                            }
                            if (!job_objs.contains(dl.getFilePackage())) {
                                changed = true;
                                job_objs.add(dl.getFilePackage());
                            }
                        }
                    }
                    break;
                }
            }
            }
        }
        if (!changed && (System.currentTimeMillis() - last_async_update > UPDATE_TIMING + 100)) {
            fireTableTask();
        }
        Update_Async.restart();
    }

    private void fireTableTask() {
        last_async_update = System.currentTimeMillis();
        synchronized (job_objs) {
            if (visible && job_ID != NO_JOB) fireTableChanged(this.job_ID, this.job_objs);
            this.job_ID = this.NO_JOB;
            this.job_objs.clear();
        }
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(final ActionEvent e) {
        new Thread() {
            public void run() {
                this.setName("DownloadLinks: actionPerformed");
                if (e.getSource() == DownloadLinksPanel.this.Update_Async) {
                    fireTableTask();
                    return;
                }
                ArrayList<FilePackage> selected_packages = new ArrayList<FilePackage>();
                ArrayList<DownloadLink> selected_links = new ArrayList<DownloadLink>();
                HashMap<String, Object> prop = new HashMap<String, Object>();
                HashSet<String> List = new HashSet<String>();
                StringBuilder build = new StringBuilder();
                String string = null;
                Object obj = null;
                FilePackage fp = null;
                DownloadLink link = null;
                File folder = null;
                int col = 0;
                if (e.getSource() instanceof JMenuItem) {
                    switch (e.getID()) {
                    case TableAction.EDIT_NAME:
                    case TableAction.EDIT_DIR:
                        selected_packages = (ArrayList<FilePackage>) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");
                        break;
                    case TableAction.SORT:
                        col = (Integer) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("col");
                        selected_packages = new ArrayList<FilePackage>(DownloadLinksPanel.this.internalTable.getSelectedFilePackages());
                        break;
                    case TableAction.DOWNLOAD_PRIO:
                    case TableAction.DE_ACTIVATE:
                        prop = (HashMap<String, Object>) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("infos");
                        selected_links = (ArrayList<DownloadLink>) prop.get("links");
                        break;
                    case TableAction.DELETE:
                    case TableAction.SET_PW:
                    case TableAction.NEW_PACKAGE:
                    case TableAction.CHECK:
                    case TableAction.DOWNLOAD_COPY_URL:
                    case TableAction.DOWNLOAD_COPY_PASSWORD:
                    case TableAction.DOWNLOAD_RESET:
                    case TableAction.DOWNLOAD_DLC:
                    case TableAction.DOWNLOAD_RESUME:
                        selected_links = (ArrayList<DownloadLink>) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("links");
                        break;
                    case TableAction.DOWNLOAD_DIR:
                        folder = (File) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("folder");
                        break;
                    case TableAction.DOWNLOAD_BROWSE_LINK:
                        link = (DownloadLink) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
                        break;
                    case TableAction.STOP_MARK:
                        obj = ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("item");
                        break;
                    }
                } else if (e.getSource() instanceof TableAction) {
                    switch (e.getID()) {
                    case TableAction.SORT_ALL:
                        col = (Integer) ((TableAction) e.getSource()).getProperty().getProperty("col");
                        break;
                    case TableAction.DELETE:
                        selected_links = (ArrayList<DownloadLink>) ((TableAction) e.getSource()).getProperty().getProperty("links");
                        break;
                    }
                }
                switch (e.getID()) {
                case TableAction.STOP_MARK:
                    DownloadWatchDog.getInstance().toggleStopMark(obj);
                    break;
                case TableAction.EDIT_DIR:
                    final ArrayList<FilePackage> selected_packages2 = new ArrayList<FilePackage>(selected_packages);
                    new GuiRunnable<Object>() {
                        // @Override
                        public Object runSave() {
                            JDFileChooser fc = new JDFileChooser();
                            fc.setApproveButtonText(JDL.L("gui.btn_ok", "OK"));
                            fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
                            fc.setCurrentDirectory(selected_packages2.get(0).getDownloadDirectory() != null ? new File(selected_packages2.get(0).getDownloadDirectory()) : JDUtilities.getResourceFile("downloads"));
                            if (fc.showOpenDialog(DownloadLinksPanel.this) == JDFileChooser.APPROVE_OPTION) {
                                File ret = fc.getSelectedFile();
                                if (ret != null) {
                                    for (int i = 0; i < selected_packages2.size(); i++) {
                                        selected_packages2.get(i).setDownloadDirectory(ret.getAbsolutePath());
                                    }
                                }
                            }
                            return null;
                        }
                    }.start();
                    break;
                case TableAction.EDIT_NAME:
                    String name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDL.L("gui.linklist.editpackagename.message", "Neuer Paketname"), selected_packages.get(0).getName());
                    if (name != null) {
                        for (int i = 0; i < selected_packages.size(); i++) {
                            selected_packages.get(i).setName(name);
                        }
                    }
                    break;
                case TableAction.DOWNLOAD_RESUME:
                    for (int i = 0; i < selected_links.size(); i++) {
                        selected_links.get(i).getLinkStatus().setStatus(LinkStatus.TODO);
                        selected_links.get(i).getLinkStatus().setStatusText(JDL.L("gui.linklist.status.doresume", "Warte auf Fortsetzung"));
                    }
                    break;
                case TableAction.DOWNLOAD_BROWSE_LINK:
                    if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                        try {
                            JLinkButton.openURL(link.getBrowserUrl());
                        } catch (Exception e1) {
                            JDLogger.exception(e1);
                        }
                    }
                    break;
                case TableAction.DOWNLOAD_DIR:
                    JDUtilities.openExplorer(folder);
                    break;
                case TableAction.DOWNLOAD_DLC:
                    GuiRunnable<File> temp = new GuiRunnable<File>() {
                        // @Override
                        public File runSave() {
                            JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
                            fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
                            if (fc.showSaveDialog(SimpleGUI.CURRENTGUI) == JDFileChooser.APPROVE_OPTION) return fc.getSelectedFile();
                            return null;
                        }
                    };
                    File ret = temp.getReturnValue();
                    if (ret == null) return;
                    if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {
                        ret = new File(ret.getAbsolutePath() + ".dlc");
                    }
                    JDUtilities.getController().saveDLC(ret, selected_links);
                    break;
                case TableAction.DOWNLOAD_RESET:
                    final ArrayList<DownloadLink> links = selected_links;
                    new Thread() {
                        public void run() {

                            if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDL.L("gui.downloadlist.reset", "Reset selected downloads?") + " (" + JDL.LF("gui.downloadlist.delete.size_packagev2", "%s links", links.size()) + ")")) {
                                for (int i = 0; i < links.size(); i++) {
                                    links.get(i).reset();
                                }
                            }

                        }
                    }.start();
                    break;
                case TableAction.DOWNLOAD_COPY_PASSWORD:
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
                    string = build.toString();
                    ClipboardHandler.getClipboard().setOldData(string);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
                    break;
                case TableAction.DOWNLOAD_COPY_URL:
                    for (int i = 0; i < selected_links.size(); i++) {
                        if (selected_links.get(i).getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                            String url = selected_links.get(i).getBrowserUrl();
                            if (!List.contains(url)) {
                                List.add(url);
                                build.append(url + "\n\r");
                            }
                        }
                    }
                    string = build.toString();
                    ClipboardHandler.getClipboard().setOldData(string);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
                    break;
                case TableAction.DOWNLOAD_PRIO:
                    int prio = (Integer) prop.get("prio");
                    for (int i = 0; i < selected_links.size(); i++) {
                        selected_links.get(i).setPriority(prio);
                    }
                    DownloadController.getInstance().fireDownloadLinkUpdate(selected_links);
                    break;
                case TableAction.CHECK:
                    LinkCheck.getLinkChecker().checkLinks(selected_links);
                    LinkCheck.getLinkChecker().getBroadcaster().addListener(DownloadLinksPanel.this);
                    break;
                case TableAction.SORT_ALL:
                    if (DownloadController.getInstance().size() == 1) {
                        DownloadController.getInstance().getPackages().get(0).sort(col);
                    } else
                        sort(col);
                    break;
                case TableAction.SORT:
                    for (int i = 0; i < selected_packages.size(); i++) {
                        selected_packages.get(i).sort(col);
                    }
                    break;
                case TableAction.DE_ACTIVATE:
                    boolean b = (Boolean) prop.get("boolean");
                    for (int i = 0; i < selected_links.size(); i++) {
                        selected_links.get(i).setEnabled(b);
                    }
                    JDUtilities.getDownloadController().fireStructureUpdate();
                    break;
                case TableAction.NEW_PACKAGE:
                    fp = selected_links.get(0).getFilePackage();
                    string = SimpleGUI.CURRENTGUI.showUserInputDialog(JDL.L("gui.linklist.newpackage.message", "Name of the new package"), fp.getName());
                    if (string != null) {
                        FilePackage nfp = FilePackage.getInstance();
                        nfp.setName(string);
                        nfp.setDownloadDirectory(fp.getDownloadDirectory());
                        nfp.setExtractAfterDownload(fp.isExtractAfterDownload());
                        nfp.setComment(fp.getComment());
                        ArrayList<String> passwords = null;
                        for (DownloadLink link2 : selected_links) {
                            FilePackage fp2 = link2.getFilePackage();
                            passwords = JDUtilities.mergePasswords(passwords, fp2.getPassword());
                        }
                        for (int i = 0; i < selected_links.size(); i++) {
                            selected_links.get(i).setFilePackage(nfp);
                        }
                        if (passwords != null) nfp.setPassword(JDUtilities.passwordArrayToString(passwords.toArray(new String[passwords.size()])));
                        if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
                            JDUtilities.getDownloadController().addPackageAt(nfp, 0, 0);
                        } else {
                            JDUtilities.getDownloadController().addPackage(nfp);
                        }
                    }
                    break;
                case TableAction.SET_PW:
                    String pw = SimpleGUI.CURRENTGUI.showUserInputDialog(JDL.L("gui.linklist.setpw.message", "Set download password"), null);
                    for (int i = 0; i < selected_links.size(); i++) {
                        selected_links.get(i).setProperty("pass", pw);
                    }
                    break;
                case TableAction.DELETE:
                    if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDL.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?") + " (" + JDL.LF("gui.downloadlist.delete.size_packagev2", "%s links", selected_links.size()) + ")")) {
                        for (int i = 0; i < selected_links.size(); i++) {
                            selected_links.get(i).setEnabled(false);
                            selected_links.get(i).getFilePackage().remove(selected_links.get(i));
                        }
                    }
                    break;
                }
            }
        }.start();

    }

    private void sort(final int col) {
        lastSort = !lastSort;
        System.out.println("sorting " + lastSort);
        ArrayList<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
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
                    case 0:
                        return aa.getName().compareToIgnoreCase(bb.getName());
                    case 1:
                        return aa.getHoster().compareToIgnoreCase(bb.getHoster());
                    case 2:
                        return aa.getRemainingLinks() > bb.getRemainingLinks() ? 1 : -1;
                    case 3:
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

    public void onLinkCheckEvent(LinkCheckEvent event) {
        switch (event.getID()) {
        case LinkCheckEvent.ABORT:
        case LinkCheckEvent.STOP:
            LinkCheck.getLinkChecker().getBroadcaster().removeListener(this);
            break;
        }
    }

    public void showDownloadLinkInfo(DownloadLink downloadLink) {
        filePackageInfo.setDownloadLink(downloadLink);
        JDCollapser.getInstance().setContentPanel(filePackageInfo);
        JDCollapser.getInstance().setTitle(JDL.L("gui.linkgrabber.infopanel.link.title", "Link information"));
        JDCollapser.getInstance().setVisible(true);
        JDCollapser.getInstance().setCollapsed(false);

    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
