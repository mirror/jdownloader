package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.PlainDocument;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.event.UIEvent;
import jd.gui.skins.simple.components.BrowseFile;
import jd.gui.skins.simple.components.ContextMenu;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.unrar.JUnrar;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse sammelt die Links, bündelt sie zu Paketen und führt einen
 * verfügbarkeitscheck durch
 * 
 * @author JD-Team
 */
public class LinkGrabber extends JFrame implements ActionListener, DropTargetListener, MouseListener, KeyListener, ChangeListener {

    /**
     * 
     */
    private static final long    serialVersionUID           = 4974425479842618402L;

    private static final String  PROPERTY_AUTOPACKAGE       = "PROPERTY_AUTOPACKAGE";

    public static final String   PROPERTY_AUTOPACKAGE_LIMIT = "AUTOPACKAGE_LIMIT";

    public static final String   PROPERTY_ONLINE_CHECK      = "ONLINE_CHECK";

    private JTabbedPane          tabbedPane;

    protected static int         REL                        = GridBagConstraints.RELATIVE;

    protected static int         REM                        = GridBagConstraints.REMAINDER;

    private JButton              acceptAll;

    protected Logger             logger                     = JDUtilities.getLogger();

    private JButton              accept;

    private SimpleGUI            parentFrame;

    private Vector<PackageTab>   tabList;

    private JProgressBar         progress;

    private Vector<DownloadLink> waitingLinkList;

    private Thread               gatherer;

    private JCheckBoxMenuItem    mAutoPackage;

    private JMenuItem            mRemovePackage;

    private JMenuItem            mPremiumMirror;

    private JMenuItem            mFreeMirror;

    private JMenuItem            mPriorityMirror;

    private JMenuItem            mRemoveOffline;

    private int                  currentTab                 = -1;

    private Configuration        config;

    private SubConfiguration     guiConfig;

    private JMenuItem            mMerge;

    private JMenuItem            mRemoveOfflineAll;

    /**
     * @param parent GUI
     * @param linkList neue links
     */
    public LinkGrabber(SimpleGUI parent, final DownloadLink[] linkList) {
        super();
        config = JDUtilities.getConfiguration();
        guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        this.parentFrame = parent;
        tabList = new Vector<PackageTab>();
        this.waitingLinkList = new Vector<DownloadLink>();
        initGUI();

        addLinks(linkList);
    }

    private void initGUI() {
        setLayout(new GridBagLayout());
        buildMenu();
        this.tabbedPane = new JTabbedPane();

        this.acceptAll = new JButton(JDLocale.L("gui.linkgrabber.btn.acceptAll", "Alle übernehmen"));
        this.accept = new JButton(JDLocale.L("gui.linkgrabber.btn.accept", "Package übernehmen"));
        this.progress = new JProgressBar();
        progress.setBorder(BorderFactory.createEtchedBorder());
        progress.setString(JDLocale.L("gui.linkgrabber.bar.title", "Infosammler"));
        progress.setStringPainted(true);
        acceptAll.addActionListener(this);
        accept.addActionListener(this);
        tabbedPane.addChangeListener(this);

        tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        tabbedPane.addMouseListener(this);
        new DropTarget(tabbedPane, this);
        // tabbedPane.setTransferHandler(new TransferHandler("text"));
        // tabbedPane.addMouseListener(new MouseAdapter() {
        // public void mousePressed(MouseEvent e) {
        // JComponent c = (JComponent)e.getSource();
        // TransferHandler th = c.getTransferHandler();
        // th.exportAsDrag(c, e, TransferHandler.COPY);
        // }
        // });
        JDUtilities.addToGridBag(this, tabbedPane, REL, REL, REM, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(this, progress, REL, REL, REM, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);

        JDUtilities.addToGridBag(this, acceptAll, REL, REL, REL, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.SOUTHEAST);
        JDUtilities.addToGridBag(this, accept, REL, REL, REM, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.SOUTHEAST);

        this.setName("LINKGRABBER");
        this.setPreferredSize(new Dimension(600, 400));
        // http://serienfreaks.tv/?id=6523
        pack();
        this.setVisible(true);
        if (SimpleGUI.getLastDimension(this, null) != null) {
            this.setPreferredSize(SimpleGUI.getLastDimension(this, null));

            pack();
        }
        if (SimpleGUI.getLastLocation(parentFrame.getFrame(), null, this) != null) {
            this.setLocation(SimpleGUI.getLastLocation(parentFrame.getFrame(), null, this));

        }

        LocationListener list = new LocationListener();
        this.addComponentListener(list);
        this.addWindowListener(list);

    }

    // private void refreshTabbedPane() {
    // tabbedPane.removeAll();
    // for (int i = 0; i < tabList.size(); i++)
    // tabbedPane.addTab(tabList.get(i).getPackageName(), tabList.get(i));
    //
    // }

    private void buildMenu() {
        // Where the GUI is created:
        JMenuBar menuBar;
        JMenu menu, submenu;
        JMenuItem menuItem;
        JRadioButtonMenuItem rbMenuItem;
        JCheckBoxMenuItem cbMenuItem;

        // Create the menu bar.
        menuBar = new JMenuBar();

        // Build the first menu.
        menu = new JMenu(JDLocale.L("gui.linkgrabber.menu.extras", "Extras"));
        menuBar.add(menu);

        // extras Menü
        mAutoPackage = new JCheckBoxMenuItem(JDLocale.L("gui.linkgrabber.menu.extras.autopackage", "Auto. Pakete"));

        menu.add(mAutoPackage);
        mAutoPackage.setSelected(guiConfig.getBooleanProperty(PROPERTY_AUTOPACKAGE, true));

        mAutoPackage.addActionListener(this);
        // removePackageAt

        // Edit Menü
        menu = new JMenu(JDLocale.L("gui.linkgrabber.menu.edit", "Bearbeiten"));
        menuBar.add(menu);

        mMerge = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.merge", "zu einem Paket zusammenwerfen"));
        mMerge.addActionListener(this);
        menu.add(mMerge);
        menu.addSeparator();
        mRemoveOffline = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.removeOffline", "Fehlerhafte Link entfernen(Paket)"));
        mRemoveOffline.addActionListener(this);
        menu.add(mRemoveOffline);
        mRemoveOfflineAll = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.removeOfflineAll", "Fehlerhafte Link entfernen(Alle)"));
        mRemoveOfflineAll.addActionListener(this);
        menu.add(mRemoveOfflineAll);
        menu.addSeparator();
        mRemovePackage = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.removePackage", "Paket verwerfen"));
        mRemovePackage.addActionListener(this);
        menu.add(mRemovePackage);

        // mMovePackage = new
        // JCheckBoxMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.movePackage",
        // "Paket verschieben"));
        //
        // menu.add(mMovePackage);
        //
        menu = new JMenu(JDLocale.L("gui.linkgrabber.menu.selection", "Auswahl"));
        menuBar.add(menu);
        mPremiumMirror = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.selection.premiummirror", "Premium Mirrorauswahl"));
        menu.add(mPremiumMirror);
        mFreeMirror = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.selection.freemirror", "Free Mirrorauswahl"));
        menu.add(mFreeMirror);
        mPriorityMirror = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.selection.prioritymirror", "Priorität Mirrorauswahl"));
        menu.add(mPriorityMirror);
        mPremiumMirror.addActionListener(this);
        mFreeMirror.addActionListener(this);
        mPriorityMirror.addActionListener(this);
        mPremiumMirror.setEnabled(true);
        mFreeMirror.setEnabled(true);
        mPriorityMirror.setEnabled(true);
        this.setJMenuBar(menuBar);

    }

    public int getTotalLinkCount() {
        int ret = 0;
        for (int i = 0; i < tabList.size(); i++)
            ret += tabList.get(i).getLinkList().size();
        return ret;
    }

    public synchronized void addLinks(DownloadLink[] linkList) {

        for (int i = 0; i < linkList.length; i++) {
            if (linkList[i].isAvailabilityChecked()) {
                attachLinkToPackage(linkList[i]);
            }
            else {
                waitingLinkList.add(linkList[i]);
            }
        }
        if (waitingLinkList.size() > 0) {
            startLinkGatherer();
        }
    }

    private void startLinkGatherer() {
        progress.setMaximum(waitingLinkList.size());
        progress.setString(null);
        if (gatherer != null && gatherer.isAlive()) return;
        this.gatherer = new Thread() {
            public synchronized void run() {
                DownloadLink link;
                while (waitingLinkList.size() > 0) {

                    link = waitingLinkList.remove(0);
                    if (!guiConfig.getBooleanProperty(PROPERTY_ONLINE_CHECK, false) || link.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                        attachLinkToPackage(link);
                        try {
                            Thread.sleep(50);
                        }
                        catch (InterruptedException e) {
                        }
                    }
                    else {
                        if (link.isAvailable() || ((PluginForHost) link.getPlugin()).isListOffline()) {

                            attachLinkToPackage(link);
                        }
                    }
                    progress.setValue(waitingLinkList.size());

                }
                progress.setString(JDLocale.L("gui.linkgrabber.bar.title", "Infosammler"));

            }
        };
        // Runnable dispatcher = new Runnable() {
        // public void run() {
        // }
        // };
        // try {
        // SwingUtilities.invokeAndWait(dispatcher);
        // }
        // catch (Exception e1) {}
        gatherer.start();
        // DownloadLink link;
        // while (waitingLinkList.size() > 0) {
        //            
        // link = waitingLinkList.remove(0);
        //          
        // link.isAvailable();
        // attachLinkToPackage(link);
        // progress.setValue(waitingLinkList.size());
        // try {
        // Thread.sleep(50);
        // }
        // catch (InterruptedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // }

    }

    private void attachLinkToPackage(DownloadLink link) {

        if (!guiConfig.getBooleanProperty(PROPERTY_AUTOPACKAGE, true)) {
            // logger.finer("No Auto package");
            int lastIndex = tabList.size() - 1;
            if (lastIndex < 0) {
                addTab().setPackageName(this.removeExtension(link.getName()));
            }
            lastIndex = tabList.size() - 1;
            addLinkstoTab(new DownloadLink[] { link }, lastIndex);
            String newPackageName = getSimString(tabList.get(lastIndex).getPackageName(), removeExtension(link.getName()));
            tabList.get(lastIndex).setPackageName(newPackageName);
            onPackageNameChanged(tabList.get(lastIndex));

        }
        else {
            // logger.finer("Auto package");
            int bestSim = 0;
            int bestIndex = -1;
            // logger.info("link: " + link.getName());
            for (int i = 0; i < tabList.size(); i++) {

                int sim = comparePackages(tabList.get(i).getPackageName(), removeExtension(link.getName()));
                if (sim > bestSim) {
                    bestSim = sim;
                    bestIndex = i;
                }
            }
            // logger.info("Best sym: "+bestSim);
            if (bestSim < guiConfig.getIntegerProperty(PROPERTY_AUTOPACKAGE_LIMIT, 98)) {

                logger.info("New Tab");
                addLinkstoTab(new DownloadLink[] { link }, tabList.size());
                tabList.get(tabList.size() - 1).setPackageName(removeExtension(link.getName()));
            }
            else {
                // logger.info("Found Package " +
                // tabList.get(bestIndex).getPackageName());
                String newPackageName = getSimString(tabList.get(bestIndex).getPackageName(), removeExtension(link.getName()));
                tabList.get(bestIndex).setPackageName(newPackageName);
                onPackageNameChanged(tabList.get(bestIndex));
                addLinkstoTab(new DownloadLink[] { link }, bestIndex);

            }

        }

    }

    private String removeExtension(String a) {
        // logger.finer("file " + a);
        a = a.replaceAll("\\.part([0-9]+)", "");
        a = a.replaceAll("\\.html", "");
        a = a.replaceAll("\\.htm", "");
        int i = a.lastIndexOf(".");
        // logger.info("FOund . " + i);
        String ret;
        if (i <= 1 || (a.length() - i) > 5) {
            ret = a.toLowerCase().trim();
        }
        else {
            // logger.info("Remove ext");
            ret = a.substring(0, i).toLowerCase().trim();
        }

        if (a.equals(ret)) return ret;
        return (ret);

    }

    private int comparePackages(String a, String b) {

        int c = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) c++;
        }

        if (Math.min(a.length(), b.length()) == 0) return 0;
        // logger.info("comp: " + a + " <<->> " + b + "(" + (c * 100) /
        // (b.length()) + ")");
        return (c * 100) / (b.length());
    }

    private String getSimString(String a, String b) {

        String ret = "";
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                ret += a.charAt(i);
            }
            else {
                // return ret;
            }
        }
        return ret;
    }

    public void addLinkstoTab(DownloadLink[] linkList, int id) {

        PackageTab tab;
        // logger.info(id + " - " + tabList.size());
        if (id >= tabList.size()) {
            tab = addTab();
        }
        else {
            tab = tabList.get(id);
        }
        // logger.finer("add " + linkList.length + " links at " + id);

        tab.addLinks(linkList);

        // validate();
        onPackageNameChanged(tab);
    }

    protected void removePackageAt(int i) {
        tabList.remove(i);
        tabbedPane.removeTabAt(i);
    }

    protected void removePackage(PackageTab tab) {
        tabbedPane.removeTabAt(tabList.indexOf(tab));
        tabList.remove(tab);

    }

    protected PackageTab getSelectedTab() {
        return tabList.get(this.tabbedPane.getSelectedIndex());
    }

    protected void onPackageNameChanged(PackageTab tab) {
        for (int i = 0; i < tabList.size(); i++) {
            if (tabList.get(i) == tab) {
                if (tab.getLinkList().size() == 0) {

                    return;
                }
                else {
                    String title = tab.getPackageName();
                    if (title.length() > 20) {
                        tabbedPane.setToolTipTextAt(i, title);
                        title = title.substring(0, 6) + "(...)" + title.substring(title.length() - 6);
                    }
                    tabbedPane.setTitleAt(i, title + " (" + tab.getLinkList().size() + ")");
                    return;
                }

            }

        }
        this.setTitle(JDLocale.L("gui.linkgrabber.title", "Linksammler") + " " + getTotalLinkCount() + " " + JDLocale.L("gui.linkgrabber.title_1", " Link(s) in") + " " + tabList.size() + " " + JDLocale.L("gui.linkgrabber.title_2", "Paket(en)"));

    }

    private PackageTab addTab() {
        PackageTab tab;
        tab = new PackageTab();
        tab.setPackageName(JDLocale.L("gui.linkgrabber.lbl.newpackage", "neues Package"));
        this.tabList.add(tab);
        tabbedPane.addTab(tab.getPackageName(), tab);
        logger.finer("ADD new Tab ");
        // refreshTabbedPane();
        return tab;
    }

    protected String getInfoString(DownloadLink link) {
        if (!link.isAvailabilityChecked()) {
            return link.getStatusText().length() == 0 ? JDLocale.L("gui.linkgrabber.lbl.notonlinechecked", "[Verf. nicht überprüft] ") + link.getFileInfomationString() : link.getFileInfomationString() + " " + link.getStatusText();

        }
        return link.getStatusText().length() == 0 ? JDLocale.L("gui.linkgrabber.lbl.isonline", "[online] ") + link.getFileInfomationString() : link.getFileInfomationString() + " " + link.getStatusText();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.mAutoPackage) {
            guiConfig.setProperty(PROPERTY_AUTOPACKAGE, mAutoPackage.isSelected());
            JDUtilities.saveConfig();
            return;
        }
        else if (e.getSource() == this.mFreeMirror) {
            Vector<DownloadLink> finalList = new Vector<DownloadLink>();
            Vector<Vector<DownloadLink>> files = getSelectedTab().getMirrors();
            Vector<PluginForHost> pfh = JDUtilities.getPluginsForHost();
            for (int a = 0; a < files.size(); a++) {
                Vector<DownloadLink> mirrors = files.get(a);
                if (mirrors.size() == 0) continue;

                DownloadLink link = null;

                for (int b = 0; b < pfh.size(); b++) {
                    PluginForHost plugin = pfh.get(b);
                    boolean ch = false;
                    for (int c = 0; c < mirrors.size(); c++) {
                        DownloadLink mirror = mirrors.get(c);
                        if (mirrors.get(c).getHost().equalsIgnoreCase(plugin.getHost())) {
                            link = mirror;
                            ch = true;
                            pfh.remove(plugin);
                            pfh.add(plugin);
                            break;
                        }
                    }
                    if (ch) break;
                }

                finalList.add(link);
            }

            getSelectedTab().setLinkList(finalList);
        }
        else if (e.getSource() == this.mPremiumMirror) {
            Vector<DownloadLink> finalList = new Vector<DownloadLink>();
            Vector<Vector<DownloadLink>> files = getSelectedTab().getMirrors();
            Vector<PluginForHost> pfh = JDUtilities.getPluginsForHost();
            for (int a = 0; a < files.size(); a++) {
                Vector<DownloadLink> mirrors = files.get(a);
                boolean found = false;
                for (int b = 0; b < pfh.size(); b++) {
                    PluginForHost plugin = pfh.get(b);
                    boolean ch = false;
                    for (int c = 0; c < mirrors.size(); c++) {
                        DownloadLink mirror = mirrors.get(c);
                        if (plugin.getMaxSimultanDownloadNum() > 1 && mirrors.get(c).getHost().equalsIgnoreCase(plugin.getHost())) {
                            finalList.add(mirror);
                            ch = true;
                            found = true;
                            break;
                        }

                    }
                    if (ch) break;
                }
                if (!found) {
                    logger.finer("No premium account found for: " + mirrors);
                    DownloadLink link = getPriorityLink(mirrors);
                    finalList.add(link);
                }

            }
            getSelectedTab().setLinkList(finalList);

        }
        else if (e.getSource() == this.mPriorityMirror) {
            Vector<DownloadLink> finalList = new Vector<DownloadLink>();
            Vector<Vector<DownloadLink>> files = getSelectedTab().getMirrors();

            for (int a = 0; a < files.size(); a++) {
                Vector<DownloadLink> mirrors = files.get(a);
                if (mirrors.size() == 0) continue;

                DownloadLink link = getPriorityLink(mirrors);
                finalList.add(link);
            }

            getSelectedTab().setLinkList(finalList);

        }
        else if (e.getSource() == this.mRemovePackage) {
            this.removePackageAt(this.tabbedPane.getSelectedIndex());
            if (this.tabList.size() == 0) {
                this.setVisible(false);
                this.dispose();
            }
        }
        else if (e.getSource() == this.accept) {
            confirmCurrentPackage();
            this.removePackageAt(this.tabbedPane.getSelectedIndex());
            if (this.tabList.size() == 0) {
                this.setVisible(false);
                this.dispose();
            }
        }
        else if (e.getSource() == this.acceptAll) {
            confirmAll();
            this.setVisible(false);
            this.dispose();
        }
        else if (e.getSource() == this.mRemoveOffline) {
            PackageTab tab = tabList.get(tabbedPane.getSelectedIndex());
            Vector<DownloadLink> list = tab.getLinkList();
            for (int i = list.size() - 1; i >= 0; i--) {
                if (!list.get(i).isAvailable()) {
                    tab.removeLinkAt(i);
                }
            }
            this.onPackageNameChanged(tab);
        }

        else if (e.getSource() == this.mMerge) {
            PackageTab tab = tabList.get(tabbedPane.getSelectedIndex());
            String name = tab.getPackageName();
            Iterator<PackageTab> iterator = tabList.iterator();
            Vector<DownloadLink> newList = new Vector<DownloadLink>();
            while (iterator.hasNext()) {
                tab = iterator.next();
                Vector<DownloadLink> list = tab.getLinkList();
                newList.addAll(list);

            }
            while (tabList.size() > 1) {
                this.removePackageAt(0);
            }
            if (tabList.size() > 0) {
                tabList.get(0).setLinkList(newList);
                tabList.get(0).setPackageName(name);
                onPackageNameChanged(tabList.get(0));
            }

        }
        else if (e.getSource() == this.mRemoveOfflineAll) {
            PackageTab tab;
            Iterator<PackageTab> iterator = tabList.iterator();
            Vector<DownloadLink> newList = new Vector<DownloadLink>();
            while (iterator.hasNext()) {
                tab = iterator.next();
                Vector<DownloadLink> list = tab.getLinkList();
                for (int i = list.size() - 1; i >= 0; i--) {
                    if (!list.get(i).isAvailable()) {
                        tab.removeLinkAt(i);
                    }
                }
                this.onPackageNameChanged(tab);

            }

        }
        else if (e.getActionCommand().equals(JDLocale.L("gui.linkgrabber.tabs.context.delete"))) {
            Point loc = ((ContextMenu) ((JMenuItem) e.getSource()).getParent()).getPoint();
            int destID = tabbedPane.getUI().tabForCoordinate(tabbedPane, (int) loc.x, (int) loc.getY());
            this.removePackageAt(destID);
            if (this.tabList.size() == 0) {
                this.setVisible(false);
                this.dispose();
            }
        }
        else if (e.getActionCommand().equals(JDLocale.L("gui.linkgrabber.tabs.context.newPackage"))) {

            addTab();
        }

    }

    private DownloadLink getPriorityLink(Vector<DownloadLink> mirrors) {

        Vector<PluginForHost> pfh = JDUtilities.getPluginsForHost();

        for (int b = 0; b < pfh.size(); b++) {
            PluginForHost plugin = pfh.get(b);
            // boolean ch = false;
            for (int c = 0; c < mirrors.size(); c++) {
                DownloadLink mirror = mirrors.get(c);
                if (mirrors.get(c).getHost().equalsIgnoreCase(plugin.getHost())) {
                    return mirror;

                }
            }
        }
        logger.severe("Could not find Priorityhoster. This should be impossible. use first link");
        return mirrors.get(0);
    }

    private void confirmAll() {
        for (int tt = 0; tt < tabList.size(); tt++) {
            PackageTab tab = tabList.get(tt);
            Vector<DownloadLink> linkList = tab.getLinkList();
            if (linkList.size() == 0) {

                return;
            }

            int rand = (int) (Math.random() * 0xffffff);
            Color c = new Color(rand);
            // c = c.brighter();
            c = c.brighter();
            FilePackage fp = new FilePackage();
            fp.setProperty("color", c);
            fp.setName(tab.getPackageName());
            fp.setComment(tab.getComment());
            fp.setPassword(tab.getPassword());
            JUnrar unrar = new JUnrar(false);
            unrar.addToPasswordlist(tab.getPassword());
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false)) {
                File file = new File(new File(tab.getDownloadDirectory()), tab.getPackageName());
                if (!file.exists()) {
                    file.mkdirs();
                }
                if (file.exists()) {
                    fp.setDownloadDirectory(file.getAbsolutePath());
                }
                else {
                    fp.setDownloadDirectory(tab.getDownloadDirectory());
                }
            }
            else {
                fp.setDownloadDirectory(tab.getDownloadDirectory());
            }
            fp.setDownloadLinks(linkList);

            for (int i = 0; i < linkList.size(); i++) {
                linkList.elementAt(i).setFilePackage(fp);
            }

            parentFrame.fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_GRABBED, linkList));

            parentFrame.setDropTargetText(JDLocale.L("gui.dropTarget.downloadsAdded", "Downloads hinzugefügt: ") + linkList.size());
        }

    }

    private void confirmCurrentPackage() {
        PackageTab tab = tabList.get(tabbedPane.getSelectedIndex());
        Vector<DownloadLink> linkList = tab.getLinkList();
        if (linkList.size() == 0) {

            return;
        }
        Color c = new Color((int) (Math.random() * 0xffffff));
        c = c.brighter();
        FilePackage fp = new FilePackage();
        fp.setProperty("color", c);
        fp.setName(tab.getPackageName());
        fp.setComment(tab.getComment());
        fp.setPassword(tab.getPassword());
        JUnrar unrar = new JUnrar(false);
        unrar.addToPasswordlist(tab.getPassword());
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false)) {
            File file = new File(new File(tab.getDownloadDirectory()), tab.getPackageName());
            if (!file.exists()) {
                file.mkdirs();
            }
            if (file.exists()) {
                fp.setDownloadDirectory(file.getAbsolutePath());
            }
            else {
                fp.setDownloadDirectory(tab.getDownloadDirectory());
            }
        }
        else {
            fp.setDownloadDirectory(tab.getDownloadDirectory());
        }
        fp.setDownloadLinks(linkList);

        for (int i = 0; i < linkList.size(); i++) {
            linkList.elementAt(i).setFilePackage(fp);
        }

        parentFrame.fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_GRABBED, linkList));

        parentFrame.setDropTargetText(JDLocale.L("gui.dropTarget.downloadsAdded", "Downloads hinzugefügt: ") + linkList.size());
    }

    public void dragEnter(DropTargetDragEvent dtde) {

        if (this.currentTab < 0) {
            currentTab = tabbedPane.getSelectedIndex();
        }
    }

    public void dragExit(DropTargetEvent dte) {}

    public void dragOver(DropTargetDragEvent dtde) {
        int id = tabbedPane.getUI().tabForCoordinate(tabbedPane, (int) dtde.getLocation().getX(), (int) dtde.getLocation().getY());
        if (id >= 0) tabbedPane.setSelectedIndex(id);
    }

    public void drop(DropTargetDropEvent e) {
        // TODO Auto-generated method stub

        int destID = tabbedPane.getUI().tabForCoordinate(tabbedPane, (int) e.getLocation().getX(), (int) e.getLocation().getY());
        PackageTab dest;
        PackageTab source = tabList.get(currentTab);
        if (destID < 0) {
            dest = addTab();
            dest.setPackageName(source.getPackageName() + "(2)");
            dest.setDownloadDirectory(source.getDownloadDirectory());
            dest.setPassword(source.getPassword());
        }
        else {
            dest = tabList.get(destID);
        }

        if (source == dest) return;
        Vector<DownloadLink> move = new Vector<DownloadLink>();
        try {
            Transferable tr = e.getTransferable();
            e.acceptDrop(e.getDropAction());
            if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {

                String data = tr.getTransferData(DataFlavor.stringFlavor).toString();
                if (data != null) {

                    String[] lines = JDUtilities.splitByNewline(data);

                    for (int i = 0; i < lines.length; i++) {
                        int id = lines[i].indexOf("\t");
                        if (id <= 0) continue;
                        id = Integer.parseInt(lines[i].substring(0, id));
                        // DownloadLink link = source.removeLinkAt(id);
                        move.add(source.getLinkAt(id));

                        // logger.info("Move line " + id);

                    }

                }

            }

            // e.dropComplete(true);
        }
        catch (Exception exc) {
            // e.rejectDrop();
            exc.printStackTrace();
        }
        source.removeLinks(move);
        dest.addLinks(move.toArray(new DownloadLink[] {}));

        onPackageNameChanged(dest);
        onPackageNameChanged(source);
        currentTab = -1;

    }

    public void dropActionChanged(DropTargetDragEvent dtde) {}

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            Point point = e.getPoint();
            // int x = e.getX();
            // int y = e.getY();
            new ContextMenu(tabbedPane, point, new String[] { JDLocale.L("gui.linkgrabber.tabs.context.delete", "Entfernen"), JDLocale.L("gui.linkgrabber.tabs.context.newPackage", "Neues Package") }, this);
        }

    }

    public void mouseReleased(MouseEvent e) {}

    public void keyPressed(KeyEvent e) {

    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

    public void stateChanged(ChangeEvent e) {}

    /**
     * 
     * @author JD-Team
     * 
     */
    class PackageTab extends JPanel implements ActionListener, MouseListener, KeyListener {

        /**
         * 
         */
        private static final long    serialVersionUID = -7394319645950106241L;

        public static final int      HOSTER           = 1;

        public static final int      NAME             = 2;

        public static final int      SIZE             = 3;

        public static final int      INFO             = 4;

        private JTextField           txtName;

        private JTextField           txtPassword;

        private JTextField           txtComment;

        private BrowseFile           brwSaveto;

        private JTable               table;

        private Vector<DownloadLink> linkList;

        protected PackageTab         _this;

        private int                  sortedOn         = 1;

        public PackageTab() {
            linkList = new Vector<DownloadLink>();
            _this = this;
            buildGui();
        }

        public void setLinkList(Vector<DownloadLink> finalList) {
            linkList = new Vector<DownloadLink>();
            linkList.addAll(finalList);
            refreshTable();
        }

        public Vector<Vector<DownloadLink>> getMirrors() {
            HashMap<String, Vector<DownloadLink>> mirrormap = new HashMap<String, Vector<DownloadLink>>();
            Vector<Vector<DownloadLink>> mirrorvector = new Vector<Vector<DownloadLink>>();

            for (int i = 0; i < linkList.size(); i++) {
                String key = linkList.get(i).getName().toLowerCase();
                if (!mirrormap.containsKey(key)) {
                    Vector<DownloadLink> filelist;
                    mirrormap.put(key, filelist = new Vector<DownloadLink>());
                    mirrorvector.add(filelist);
                }
                mirrormap.get(key).add(linkList.get(i));
                linkList.get(i).setMirror(false);

            }

            return mirrorvector;
        }

        public void removeLinks(Vector<DownloadLink> move) {
            for (int i = 0; i < move.size(); i++) {
                for (int b = 0; b < linkList.size(); b++) {
                    if (linkList.get(b) == move.get(i)) {
                        linkList.remove(b);
                        continue;
                    }
                }
            }
            refreshTable();
        }

        public DownloadLink getLinkAt(int id) {
            DownloadLink ret = linkList.get(id);
            return ret;
        }

        public DownloadLink removeLinkAt(int id) {

            DownloadLink ret = linkList.remove(id);
            refreshTable();
            return ret;
        }

        public Vector<DownloadLink> getLinkList() {
            Vector<DownloadLink> ret = new Vector<DownloadLink>();
            ret.addAll(linkList);
            return ret;
        }

        public String getPackageName() {

            return txtName.getText().trim();
        }

        public void setPackageName(String name) {
            txtName.setText(name);
        }

        public String getDownloadDirectory() {

            return this.brwSaveto.getText();
        }

        public void setDownloadDirectory(String dir) {
            brwSaveto.setText(dir);
        }

        public String getPassword() {

            return this.txtPassword.getText();
        }

        public void setComment(String comm) {
            txtComment.setText(comm);
        }

        public String getComment() {

            return this.txtComment.getText();
        }

        public void setPassword(String pw) {
            txtPassword.setText(pw);
        }

        public void sortOn() {
            switch (Math.abs(sortedOn)) {
                case HOSTER:
                    Collections.sort(linkList, new Comparator<DownloadLink>() {
                        public int compare(DownloadLink a, DownloadLink b) {
                            return sortedOn > 0 ? a.getHost().compareToIgnoreCase(b.getHost()) : b.getHost().compareToIgnoreCase(a.getHost());
                        }

                    });

                    break;
                case NAME:
                    Collections.sort(linkList, new Comparator<DownloadLink>() {
                        public int compare(DownloadLink a, DownloadLink b) {
                            return sortedOn > 0 ? a.getName().compareToIgnoreCase(b.getName()) : b.getName().compareToIgnoreCase(a.getName());

                        }

                    });

                    break;
                case SIZE:
                    Collections.sort(linkList, new Comparator<DownloadLink>() {
                        public int compare(DownloadLink a, DownloadLink b) {
                            if (a.getDownloadMax() == b.getDownloadMax()) return 0;
                            return sortedOn > 0 ? a.getDownloadMax() > b.getDownloadMax() ? 1 : -1 : a.getDownloadMax() < b.getDownloadMax() ? 1 : -1;
                        }

                    });
                    break;
                case INFO:
                    Collections.sort(linkList, new Comparator<DownloadLink>() {
                        public int compare(DownloadLink a, DownloadLink b) {
                            return sortedOn > 0 ? getInfoString(a).compareToIgnoreCase(getInfoString(b)) : getInfoString(b).compareToIgnoreCase(getInfoString(a));
                        }

                    });
                    break;

            }
            refreshTable();
        }

        public void addLinks(DownloadLink[] list) {
            String password = getPassword();
            String comment = getComment();

            String[] pws = JUnrar.getPasswordArray(password);
            Vector<String> pwList = new Vector<String>();
            for (int i = 0; i < pws.length; i++) {
                pwList.add(pws[i]);

            }

            for (int i = 0; i < list.length; i++) {
                linkList.add(list[i]);
                String pass = list[i].getSourcePluginPassword();

                pws = JUnrar.getPasswordArray(pass);

                for (int i2 = 0; i2 < pws.length; i2++) {
                    if (pwList.indexOf(pws[i2]) < 0) {
                        pwList.add(pws[i2]);
                    }
                }

                if (list[i].getSourcePluginComment() != null && comment.indexOf(list[i].getSourcePluginComment()) < 0) {
                    comment += "|" + list[i].getSourcePluginComment();
                }

            }

            if (comment.startsWith("|")) comment = comment.substring(1);

            String pw = JUnrar.passwordArrayToString(pwList.toArray(new String[pwList.size()]));

            if (!txtComment.hasFocus()) txtComment.setText(comment);
            if (!txtPassword.hasFocus()) txtPassword.setText(pw);
            refreshTable();
            sortOn();

        }

        private void refreshTable() {

            table.tableChanged(new TableModelEvent(table.getModel()));
            onPackageNameChanged(this);
        }

        private void buildGui() {
            setLayout(new GridBagLayout());
            JLabel lblName = new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.name", "Packagename"));
            JLabel lblSaveto = new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter"));
            JLabel lblPassword = new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort"));
            JLabel lblComment = new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.comment", "Kommentar"));

            this.txtName = new JTextField();
            this.txtPassword = new JTextField();
            this.txtComment = new JTextField();

            this.brwSaveto = new BrowseFile();
            brwSaveto.setEditable(true);
            brwSaveto.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
            brwSaveto.setText(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
            // bfSubFolder.setText(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));

            txtName.setPreferredSize(new Dimension(450, 20));
            txtPassword.setPreferredSize(new Dimension(450, 20));
            txtComment.setPreferredSize(new Dimension(450, 20));
            brwSaveto.setPreferredSize(new Dimension(450, 20));
            txtName.setMinimumSize(new Dimension(250, 20));
            txtPassword.setMinimumSize(new Dimension(250, 20));
            txtComment.setMinimumSize(new Dimension(250, 20));
            brwSaveto.setMinimumSize(new Dimension(250, 20));

            PlainDocument doc = (PlainDocument) txtName.getDocument();
            doc.addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    onPackageNameChanged(_this);
                }

                public void insertUpdate(DocumentEvent e) {

                    onPackageNameChanged(_this);
                }

                public void removeUpdate(DocumentEvent e) {

                    onPackageNameChanged(_this);
                }
            });
            this.table = new InternalTable();
            table.getTableHeader().addMouseListener(this);
            InternalTableModel internalTableModel = new InternalTableModel();
            table.addKeyListener(this);
            table.setModel(internalTableModel);
            // table.setAutoCreateRowSorter(true);
            // table.setUpdateSelectionOnSort(true);
            table.setGridColor(Color.BLUE);
            table.setAutoCreateColumnsFromModel(true);
            table.setModel(internalTableModel);
            table.addMouseListener(this);
            table.setDragEnabled(true);
            table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
            table.getTableHeader().setReorderingAllowed(false);

            this.setPreferredSize(new Dimension(700, 350));

            TableColumn column = null;
            for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
                column = table.getColumnModel().getColumn(c);
                switch (c) {

                    case 0:
                        // column.setPreferredWidth(20);
                        column.setMaxWidth(20);
                        break;
                    case 1:
                        column.setMaxWidth(150);
                        break;
                    case 2:
                        column.setPreferredWidth(250);
                        break;
                    case 3:
                        column.setPreferredWidth(60);
                        break;
                    case 4:
                        column.setPreferredWidth(210);
                        break;

                }
            }
            JDUtilities.addToGridBag(this, lblName, REL, REL, REL, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, txtName, REL, REL, REM, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(this, lblSaveto, REL, REL, REL, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, brwSaveto, REL, REL, REM, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(this, lblPassword, REL, REL, REL, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, txtPassword, REL, REL, REM, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(this, lblComment, REL, REL, REM, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, txtComment, REL, REL, REM, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);

            JDUtilities.addToGridBag(this, new JSeparator(), REL, REL, REM, 1, 1, 0, new Insets(5, 5, 5, 5), GridBagConstraints.BOTH, GridBagConstraints.WEST);
            JScrollPane scrollpane = new JScrollPane(table);
            scrollpane.setPreferredSize(new Dimension(400, 200));
            JDUtilities.addToGridBag(this, scrollpane, REL, REL, REM, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);

        }

        public void actionPerformed(ActionEvent e) {

            if (e.getActionCommand().equals(JDLocale.L("gui.linkgrabber.packagetab.table.context.delete", "Entfernen"))) {

                int[] rows = table.getSelectedRows();
                for (int i = rows.length - 1; i >= 0; i--) {
                    int id = rows[i];// table.convertRowIndexToModel(rows[i]);
                    // logger.info("remove " + id);
                    linkList.remove(id);

                }
                this.refreshTable();

            }

            if (e.getActionCommand().equals(JDLocale.L("gui.linkgrabber.packagetab.table.context.newpackage", "Neues Package"))) {
                addTab();

            }

            else if (e.getActionCommand().equals(JDLocale.L("gui.linkgrabber.tabs.context.deleteOthers"))) {

                int[] rows = table.getSelectedRows();

                Vector<Integer> ret = new Vector<Integer>();
                Vector<DownloadLink> list = new Vector<DownloadLink>();
                for (int i = 0; i < rows.length; i++) {

                    ret.add(rows[i]);// table.convertRowIndexToModel(rows[i]));
                    list.add(linkList.get(rows[i]));// table.convertRowIndexToModel(rows[i])));

                }

                linkList = list;
                this.refreshTable();
            }
            else if (e.getActionCommand().equals(JDLocale.L("gui.linkgrabber.tabs.context.acceptSelection"))) {
                int[] rows = table.getSelectedRows();

                Vector<Integer> ret = new Vector<Integer>();
                Vector<DownloadLink> list = new Vector<DownloadLink>();
                for (int i = 0; i < rows.length; i++) {

                    ret.add(rows[i]);// table.convertRowIndexToModel(rows[i]));
                    list.add(linkList.get(rows[i]));// table.convertRowIndexToModel(rows[i])));

                }

                linkList = list;
                confirmCurrentPackage();
                removePackageAt(tabbedPane.getSelectedIndex());
                if (tabList.size() == 0) {
                    this.setVisible(false);
                    dispose();
                }
            }

        }

        private class InternalTableModel extends AbstractTableModel {

            /**
             * 
             */
            private static final long serialVersionUID = -7475394342173736030L;

            public int getColumnCount() {
                return 5;
            }

            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return Integer.class;

                }
                return String.class;
            }

            public int getRowCount() {

                return linkList.size();
            }

            public Object getValueAt(int rowIndex, int columnIndex) {

                switch (columnIndex) {
                    case 0:
                        return rowIndex;
                    case 1:
                        return linkList.get(rowIndex).getHost();
                    case 2:
                        return linkList.get(rowIndex).getName();
                    case 3:
                        return linkList.get(rowIndex).isAvailabilityChecked() ? JDUtilities.formatBytesToMB(linkList.get(rowIndex).getDownloadMax()) : "*";
                    case 4:
                        return getInfoString(linkList.get(rowIndex));

                }
                return null;
            }

            public String getColumnName(int column) {

                switch (column) {
                    case 0:
                        return JDLocale.L("gui.linkgrabber.packagetab.table.column.id", "*");
                    case 1:
                        return JDLocale.L("gui.linkgrabber.packagetab.table.column.host", "Host");
                    case 2:
                        return JDLocale.L("gui.linkgrabber.packagetab.table.column.link", "Link");
                    case 3:
                        return JDLocale.L("gui.linkgrabber.packagetab.table.column.size", "Größe");
                    case 4:
                        return JDLocale.L("gui.linkgrabber.packagetab.table.column.info", "Info");

                }
                return super.getColumnName(column);

            }
        }

        public void mouseClicked(MouseEvent e) {}

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {
            if (e.getSource() instanceof JTableHeader) {
                JTableHeader header = (JTableHeader) e.getSource();
                int column = header.columnAtPoint(e.getPoint());
                if (sortedOn == column) {
                    sortedOn *= -1;
                }
                else {
                    this.sortedOn = column;
                }

                this.sortOn();
            }
            else if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                Point point = e.getPoint();
                // int x = e.getX();
                // int y = e.getY();
                new ContextMenu(table, point, new String[] { JDLocale.L("gui.linkgrabber.packagetab.table.context.delete", "Entfernen"), JDLocale.L("gui.linkgrabber.tabs.context.deleteOthers", "Alle anderen Entfernen"), JDLocale.L("gui.linkgrabber.tabs.context.acceptSelection", "Auswahl übernehmen"), JDLocale.L("gui.linkgrabber.packagetab.table.context.newpackage", "Neues Package") }, this);
            }

        }

        public void mouseReleased(MouseEvent e) {}

        public void keyPressed(KeyEvent e) {}

        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                int[] rows = table.getSelectedRows();
                for (int i = rows.length - 1; i >= 0; i--) {
                    int id = rows[i];// table.convertRowIndexToModel(rows[i]);
                    this.linkList.remove(id);
                }
                this.refreshTable();

            }

        }

        /**
         * 
         * @author JD-Team
         * 
         */
        private class InternalTable extends JTable {
            /**
             * serialVersionUID
             */
            private static final long         serialVersionUID          = 4424930948374806098L;

            private InternalTableCellRenderer internalTableCellRenderer = new InternalTableCellRenderer();

            @Override
            public TableCellRenderer getCellRenderer(int arg0, int arg1) {
                return internalTableCellRenderer;
            }
        }

        public void keyTyped(KeyEvent e) {}

        /**
         * Celllistrenderer Für die Linklisten
         * 
         * @author JD-Team
         * 
         */
        private class InternalTableCellRenderer extends DefaultTableCellRenderer {
            /**
             * 
             */
            private static final long serialVersionUID    = -7858580590383167251L;

            /**
             * serialVersionUID
             */
            private final Color       COLOR_DONE          = new Color(0, 255, 0, 20);

            private final Color       COLOR_ERROR_OFFLINE = new Color(255, 0, 0, 60);

            private final Color       COLOR_SORT_MARK     = new Color(40, 225, 40, 40);

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // int id = table.convertRowIndexToModel(row);
                int id = row;
                DownloadLink dLink = linkList.get(id);

                if (isSelected) {
                    c.setBackground(Color.DARK_GRAY);
                    if (dLink.isAvailabilityChecked() && !dLink.isAvailable()) {
                        c.setBackground(COLOR_ERROR_OFFLINE.darker());

                    }
                    else if (dLink.isAvailabilityChecked()) {
                        c.setBackground(COLOR_DONE.darker());
                    
               

                    }
                    if (Math.abs(sortedOn) == column) {
                        c.setBackground(COLOR_SORT_MARK.darker());
                    }

                    c.setForeground(Color.WHITE);

                }
                else {
                    c.setBackground(Color.WHITE);
                    if (dLink.isAvailabilityChecked() && !dLink.isAvailable()) {
                        c.setBackground(COLOR_ERROR_OFFLINE);
                    }
                    else if (dLink.isAvailabilityChecked()) {
                        c.setBackground(COLOR_DONE);
                      
                    }
                   
                    if (Math.abs(sortedOn) == column) {
                        c.setBackground(COLOR_SORT_MARK);
                    }
                    c.setForeground(Color.BLACK);
                }

                return c;
            }
        }
    }

}
