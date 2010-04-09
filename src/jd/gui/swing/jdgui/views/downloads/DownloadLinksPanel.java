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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadController;
import jd.controlling.DownloadControllerEvent;
import jd.controlling.DownloadControllerListener;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkCheck;
import jd.controlling.LinkCheckEvent;
import jd.controlling.LinkCheckListener;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.JDFlags;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class DownloadLinksPanel extends SwitchPanel implements ActionListener, DownloadControllerListener, LinkCheckListener {

    private static final long serialVersionUID = -6029423913449902141L;

    private static final int NO_JOB = -1;
    public final static int REFRESH_ALL_DATA_CHANGED = 1;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED = 0;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED_FAST = 10;
    public static final int REFRESH_SPECIFIED_LINKS = 2;

    private final static int UPDATE_TIMING = 250;

    private static DownloadLinksPanel INSTANCE = null;

    private int jobID = REFRESH_DATA_AND_STRUCTURE_CHANGED;
    private ArrayList<Object> jobObjects = new ArrayList<Object>();

    private boolean lastSort = true;

    protected Logger logger = jd.controlling.JDLogger.getLogger();

    private DownloadTable internalTable;

    private Timer asyncUpdate;

    private long latestAsyncUpdate;

    private FilePackageInfo filePackageInfo;

    private JScrollPane scrollPane;

    private boolean notvisible = true;

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

    public boolean isNotVisible() {
        return notvisible;
    }

    public static synchronized DownloadLinksPanel getDownloadLinksPanel() {
        if (INSTANCE == null) INSTANCE = new DownloadLinksPanel();
        return INSTANCE;
    }

    public DownloadTable getInternalTable() {
        return internalTable;
    }

    public void move(byte mode) {
        ArrayList<FilePackage> fps = internalTable.getSelectedFilePackages();
        ArrayList<DownloadLink> links = internalTable.getSelectedDownloadLinks();
        if (fps.size() > 0) DownloadController.getInstance().move(fps, null, mode);
        if (links.size() > 0) DownloadController.getInstance().move(links, null, mode);
    }

    public boolean needsViewport() {
        return false;
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
            ArrayList<Object> objs2 = new ArrayList<Object>(objs);
            int id2 = id;
            internalTable.fireTableChanged(id2, objs2);
        } catch (Exception e) {
            logger.severe("TreeTable Exception, complete refresh!");
            updateTableTask(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);
        }
        return;
    }

    public void onShow() {
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
        synchronized (DownloadController.ControllerLock) {
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
        }
        if (!changed && (System.currentTimeMillis() - latestAsyncUpdate > UPDATE_TIMING + 100)) {
            fireTableTask();
        }
        asyncUpdate.restart();
    }

    private void fireTableTask() {
        latestAsyncUpdate = System.currentTimeMillis();
        synchronized (DownloadController.ControllerLock) {
            synchronized (jobObjects) {
                if (isShown() && jobID != NO_JOB) fireTableChanged(this.jobID, this.jobObjects);
                this.jobID = NO_JOB;
                this.jobObjects.clear();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(final ActionEvent e) {
        new Thread() {
            public void run() {
                this.setName("DownloadLinks: actionPerformed");
                if (e.getSource() == DownloadLinksPanel.this.asyncUpdate) {
                    fireTableTask();
                    return;
                }
                ArrayList<FilePackage> selectedPackages = new ArrayList<FilePackage>();
                ArrayList<DownloadLink> selectedLinks = new ArrayList<DownloadLink>();
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
                        selectedPackages = (ArrayList<FilePackage>) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");
                        break;
                    case TableAction.SORT:
                        col = (Integer) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("col");
                        selectedLinks = new ArrayList<DownloadLink>(DownloadLinksPanel.this.internalTable.getSelectedDownloadLinks());
                        selectedPackages = new ArrayList<FilePackage>(DownloadLinksPanel.this.internalTable.getSelectedFilePackages());
                        break;
                    case TableAction.DOWNLOAD_PRIO:
                    case TableAction.DE_ACTIVATE:
                        prop = (HashMap<String, Object>) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("infos");
                        selectedLinks = (ArrayList<DownloadLink>) prop.get("links");
                        break;
                    case TableAction.DELETE:
                    case TableAction.FORCE_DOWNLOAD:
                    case TableAction.DELETEFILE:
                    case TableAction.SET_PW:
                    case TableAction.NEW_PACKAGE:
                    case TableAction.CHECK:
                    case TableAction.DOWNLOAD_COPY_URL:
                    case TableAction.DOWNLOAD_COPY_PASSWORD:
                    case TableAction.DOWNLOAD_RESET:
                    case TableAction.DOWNLOAD_DLC:
                    case TableAction.DOWNLOAD_RESUME:
                        selectedLinks = (ArrayList<DownloadLink>) ((TableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("links");
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
                        selectedLinks = (ArrayList<DownloadLink>) ((TableAction) e.getSource()).getProperty().getProperty("links");
                        break;
                    }
                }
                switch (e.getID()) {
                case TableAction.FORCE_DOWNLOAD: {
                    DownloadWatchDog.getInstance().forceDownload(selectedLinks);
                    break;
                }
                case TableAction.STOP_MARK:
                    DownloadWatchDog.getInstance().toggleStopMark(obj);
                    break;
                case TableAction.EDIT_DIR:
                    final ArrayList<FilePackage> selected_packages2 = new ArrayList<FilePackage>(selectedPackages);
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
                    String name = UserIO.getInstance().requestInputDialog(0, JDL.L("gui.linklist.editpackagename.message", "Neuer Paketname"), selectedPackages.get(0).getName());
                    if (name != null) {
                        for (int i = 0; i < selectedPackages.size(); i++) {
                            selectedPackages.get(i).setName(name);
                        }
                    }
                    break;
                case TableAction.DOWNLOAD_RESUME:
                    for (int i = 0; i < selectedLinks.size(); i++) {
                        selectedLinks.get(i).getLinkStatus().setStatus(LinkStatus.TODO);
                        selectedLinks.get(i).getLinkStatus().resetWaitTime();
                        selectedLinks.get(i).getLinkStatus().setStatusText(JDL.L("gui.linklist.status.doresume", "Warte auf Fortsetzung"));
                    }
                    Set<String> hosts = DownloadLink.getHosterList(selectedLinks);
                    for (String host : hosts) {
                        DownloadWatchDog.getInstance().resetIPBlockWaittime(host);
                        DownloadWatchDog.getInstance().resetTempUnavailWaittime(host);
                    }
                    break;
                case TableAction.DOWNLOAD_BROWSE_LINK:
                    if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                        try {
                            JLink.openURL(link.getBrowserUrl());
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
                            if (fc.showSaveDialog(SwingGui.getInstance().getMainFrame()) == JDFileChooser.APPROVE_OPTION) return fc.getSelectedFile();
                            return null;
                        }
                    };
                    File ret = temp.getReturnValue();
                    if (ret == null) return;
                    if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {
                        ret = new File(ret.getAbsolutePath() + ".dlc");
                    }
                    JDUtilities.getController().saveDLC(ret, selectedLinks);
                    break;
                case TableAction.DOWNLOAD_RESET:
                    final ArrayList<DownloadLink> links = selectedLinks;
                    new Thread() {
                        public void run() {
                            if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.L("gui.downloadlist.reset", "Reset selected downloads?") + " (" + JDL.LF("gui.downloadlist.delete.size_packagev2", "%s links", links.size()) + ")"), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                                for (int i = 0; i < links.size(); i++) {
                                    links.get(i).reset();
                                }
                            }
                        }
                    }.start();
                    break;
                case TableAction.DOWNLOAD_COPY_PASSWORD:
                    string = getPasswordSelectedLinks(selectedLinks);
                    ClipboardHandler.getClipboard().copyTextToClipboard(string);
                    break;
                case TableAction.DOWNLOAD_COPY_URL:
                    for (int i = 0; i < selectedLinks.size(); i++) {
                        if (selectedLinks.get(i).getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                            String url = selectedLinks.get(i).getBrowserUrl();
                            if (!List.contains(url)) {
                                if (List.size() > 0) build.append("\r\n");
                                List.add(url);
                                build.append(url);
                            }
                        }
                    }
                    string = build.toString();
                    ClipboardHandler.getClipboard().copyTextToClipboard(string);
                    break;
                case TableAction.DOWNLOAD_PRIO:
                    int prio = (Integer) prop.get("prio");
                    for (int i = 0; i < selectedLinks.size(); i++) {
                        selectedLinks.get(i).setPriority(prio);
                    }
                    DownloadController.getInstance().fireDownloadLinkUpdate(selectedLinks);
                    break;
                case TableAction.CHECK:
                    LinkCheck.getLinkChecker().checkLinks(selectedLinks, true);
                    LinkCheck.getLinkChecker().getBroadcaster().addListener(DownloadLinksPanel.this);
                    break;
                case TableAction.SORT_ALL:
                    if (DownloadController.getInstance().size() == 1) {
                        DownloadController.getInstance().getPackages().get(0).sort(col);
                    } else
                        sort(col);
                    break;
                case TableAction.SORT:
                    if (selectedLinks.size() > 0) {
                        selectedLinks.get(0).getFilePackage().sort(col);
                        break;
                    }
                    for (int i = 0; i < selectedPackages.size(); i++) {
                        selectedPackages.get(i).sort(col);
                    }
                    break;
                case TableAction.DE_ACTIVATE:
                    Boolean b = (Boolean) prop.get("boolean");
                    for (int i = 0; i < selectedLinks.size(); i++) {
                        selectedLinks.get(i).setEnabled(b);
                    }
                    JDUtilities.getDownloadController().fireStructureUpdate();
                    break;
                case TableAction.NEW_PACKAGE:
                    fp = selectedLinks.get(0).getFilePackage();
                    string = UserIO.getInstance().requestInputDialog(0, JDL.L("gui.linklist.newpackage.message", "Name of the new package"), fp.getName());
                    if (string != null) {
                        FilePackage nfp = FilePackage.getInstance();
                        nfp.setName(string);
                        nfp.setDownloadDirectory(fp.getDownloadDirectory());
                        nfp.setExtractAfterDownload(fp.isExtractAfterDownload());
                        nfp.setComment(fp.getComment());
                        for (DownloadLink link2 : selectedLinks) {
                            FilePackage fp2 = link2.getFilePackage();
                            link2.addSourcePluginPassword(fp2.getPassword());
                        }
                        for (int i = 0; i < selectedLinks.size(); i++) {
                            selectedLinks.get(i).setFilePackage(nfp);
                        }
                        if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
                            JDUtilities.getDownloadController().addPackageAt(nfp, 0, 0);
                        } else {
                            JDUtilities.getDownloadController().addPackage(nfp);
                        }
                    }
                    break;
                case TableAction.SET_PW:
                    String pw = UserIO.getInstance().requestInputDialog(0, JDL.L("gui.linklist.setpw.message", "Set download password"), null);
                    for (int i = 0; i < selectedLinks.size(); i++) {
                        selectedLinks.get(i).setProperty("pass", pw);
                    }
                    break;
                case TableAction.DELETE: {
                    if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?") + " (" + JDL.LF("gui.downloadlist.delete.size_packagev2", "%s links", selectedLinks.size()) + ")"), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                        for (int i = 0; i < selectedLinks.size(); i++) {
                            selectedLinks.get(i).setEnabled(false);
                        }
                        for (int i = 0; i < selectedLinks.size(); i++) {
                            selectedLinks.get(i).deleteFile(true, false);
                            selectedLinks.get(i).getFilePackage().remove(selectedLinks.get(i));
                        }
                    }
                    return;
                }
                case TableAction.DELETEFILE: {
                    int counter = 0;
                    for (DownloadLink tmp : selectedLinks) {
                        if (tmp.existsFile()) counter++;
                    }
                    if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("gui.downloadlist.delete2", "Delete links from downloadlist and disk?") + " (" + JDL.LF("gui.downloadlist.delete.links", "%s links", selectedLinks.size()) + " / " + " " + JDL.LF("gui.downloadlist.delete.files", "%s files", counter) + ")"), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                        for (int i = 0; i < selectedLinks.size(); i++) {
                            selectedLinks.get(i).setEnabled(false);
                        }
                        for (int i = 0; i < selectedLinks.size(); i++) {
                            selectedLinks.get(i).deleteFile(true, true);
                            selectedLinks.get(i).getFilePackage().remove(selectedLinks.get(i));
                        }
                    }
                    return;
                }
                }
            }
        }.start();

    }

    public static String getPasswordSelectedLinks(ArrayList<DownloadLink> selectedLinks) {
        HashSet<String> List = new HashSet<String>();
        StringBuilder build = new StringBuilder("");
        for (int i = 0; i < selectedLinks.size(); i++) {
            String pw = selectedLinks.get(i).getFilePackage().getPassword();
            if (!List.contains(pw) && pw.length() > 0) {
                if (List.size() > 0) build.append("\r\n");
                List.add(pw);
                build.append(pw);
            }
            if (selectedLinks.get(i).getStringProperty("pass", null) != null) {
                pw = selectedLinks.get(i).getStringProperty("pass", null);
                if (!List.contains(pw) && pw.length() > 0) {
                    if (List.size() > 0) build.append("\r\n");
                    List.add(pw);
                    build.append(pw);
                }
            }
        }
        return build.toString();
    }

    private void sort(final int col) {
        lastSort = !lastSort;
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
