//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
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

import jd.HostPluginWrapper;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.event.UIEvent;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.unrar.UnrarPassword;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
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
     * @author JD-Team
     * 
     */
    class PackageTab extends JPanel implements ActionListener, MouseListener, KeyListener {

        /**
         * 
         * @author JD-Team
         * 
         */
        private class InternalTable extends JTable {

            private static final long serialVersionUID = 4424930948374806098L;

            private InternalTableCellRenderer internalTableCellRenderer = new InternalTableCellRenderer();

            public TableCellRenderer getCellRenderer(int arg0, int arg1) {
                return internalTableCellRenderer;
            }
        }

        /**
         * Celllistrenderer Für die Linklisten
         * 
         * @author JD-Team
         * 
         */
        private class InternalTableCellRenderer extends DefaultTableCellRenderer {

            private static final long serialVersionUID = -7858580590383167251L;

            private final Color COLOR_DONE = new Color(0, 255, 0, 20);

            private final Color COLOR_ERROR_OFFLINE = new Color(255, 0, 0, 60);

            private final Color COLOR_SORT_MARK = new Color(40, 225, 40, 40);

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int id = row;
                DownloadLink dLink = linkList.get(id);

                if (isSelected) {
                    c.setBackground(Color.DARK_GRAY);
                    if (dLink.isAvailabilityChecked() && !dLink.isAvailable()) {
                        c.setBackground(COLOR_ERROR_OFFLINE.darker());

                    } else if (dLink.isAvailabilityChecked()) {
                        c.setBackground(COLOR_DONE.darker());

                    }
                    if (Math.abs(sortedOn) == column) {
                        c.setBackground(COLOR_SORT_MARK.darker());
                    }

                    c.setForeground(Color.WHITE);

                } else {
                    c.setBackground(Color.WHITE);
                    if (dLink.isAvailabilityChecked() && !dLink.isAvailable()) {
                        c.setBackground(COLOR_ERROR_OFFLINE);
                    } else if (dLink.isAvailabilityChecked()) {
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

        private class InternalTableModel extends AbstractTableModel {

            private static final long serialVersionUID = -7475394342173736030L;

            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                case 0:
                    return Integer.class;

                }
                return String.class;
            }

            public int getColumnCount() {
                return 5;
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

            public int getRowCount() {

                return linkList.size();
            }

            public Object getValueAt(int rowIndex, int columnIndex) {

                switch (columnIndex) {
                case 0:
                    return rowIndex + 1;
                case 1:
                    return linkList.get(rowIndex).getHost();
                case 2:
                    return linkList.get(rowIndex).getName();
                case 3:
                    return linkList.get(rowIndex).isAvailabilityChecked() && linkList.get(rowIndex).getDownloadSize() > 0 ? JDUtilities.formatBytesToMB(linkList.get(rowIndex).getDownloadSize()) : "~";
                case 4:
                    return getInfoString(linkList.get(rowIndex));

                }
                return null;
            }
        }

        public static final int HOSTER = 1;

        public static final int INFO = 4;

        public static final int NAME = 2;

        private static final long serialVersionUID = -7394319645950106241L;

        public static final int SIZE = 3;

        protected PackageTab _this;

        private ComboBrowseFile brwSaveTo;

        private Vector<DownloadLink> linkList;

        private int sortedOn = 1;

        private JTable table;

        private JTextField txtComment;

        private JTextField txtName;

        private JTextField txtPassword;

        private JPopupMenu mContextTabPopup;

        private JMenuItem mContextDelete;

        private JMenuItem mContextDeleteOthers;

        private JMenuItem mContextAcceptSelection;

        private JMenuItem mContextNewPackage;

        private JCheckBox chbExtract;

        public PackageTab() {
            linkList = new Vector<DownloadLink>();
            _this = this;
            buildGui();
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == mContextDelete) {
                int[] rows = table.getSelectedRows();

                for (int i = rows.length - 1; i >= 0; i--) {
                    totalLinkList.remove(linkList.remove(rows[i]));
                }

                refreshTable();
            } else if (e.getSource() == mContextNewPackage) {
                PackageTab newTab = addTab();
                int[] rows = table.getSelectedRows();
                if (0 < rows.length) {
                    DownloadLink linksToTransfer[] = new DownloadLink[rows.length];
                    int targetIndex = 0;
                    for (int i = rows.length - 1; i >= 0; i--) {
                        linksToTransfer[targetIndex++] = getLinkAt(rows[i]);
                        linkList.remove(rows[i]);
                    }
                    newTab.addLinks(linksToTransfer);
                    refreshTable();
                }
            } else if (e.getSource() == mContextDeleteOthers) {
                int[] rows = table.getSelectedRows();

                Vector<DownloadLink> list = new Vector<DownloadLink>();
                for (int i = rows.length - 1; i >= 0; --i) {
                    list.add(linkList.remove(rows[i]));
                }

                totalLinkList.removeAll(linkList);
                linkList = list;
                refreshTable();
            } else if (e.getSource() == mContextAcceptSelection) {
                int[] rows = table.getSelectedRows();

                Vector<DownloadLink> list = new Vector<DownloadLink>();
                for (int element : rows) {
                    list.add(linkList.get(element));
                }

                linkList = list;
                int idx = tabbedPane.getSelectedIndex();
                confirmPackage(idx);
                removePackageAt(idx);
                if (tabList.size() == 0) {
                    setVisible(false);
                    dispose();
                }
            }
        }

        public void addLinks(DownloadLink[] list) {
            String password = getPassword();
            String comment = getComment();

            String[] pws = UnrarPassword.getPasswordArray(password);
            Vector<String> pwList = new Vector<String>();
            for (String element : pws) {
                pwList.add(element);
            }

            for (DownloadLink element : list) {

                linkList.add(element);

                pws = UnrarPassword.getPasswordArray(element.getSourcePluginPassword());
                for (String element2 : pws) {
                    if (pwList.indexOf(element2) < 0) {
                        pwList.add(element2);
                    }
                }

                String newComment = element.getSourcePluginComment();
                if (newComment != null && comment.indexOf(newComment) < 0) {
                    comment += "|" + newComment;
                }
            }

            if (comment.startsWith("|")) {
                comment = comment.substring(1);
            }

            String pw = UnrarPassword.passwordArrayToString(pwList.toArray(new String[pwList.size()]));

            if (!txtComment.hasFocus()) {
                txtComment.setText(comment);
            }
            if (!txtPassword.hasFocus()) {
                txtPassword.setText(pw);
            }
            refreshTable();
            sortOn();
        }

        private void buildGui() {
            setLayout(new GridBagLayout());
            JLabel lblName = new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.name", "packagename"));
            JLabel lblSaveto = new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter"));
            JLabel lblPassword = new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort"));
            JLabel lblComment = new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.comment", "Kommentar"));

            txtName = new JTextField();
            txtPassword = new JTextField();
            txtComment = new JTextField();

            chbExtract = new JCheckBox(JDLocale.L("gui.linkgrabber.packagetab.chb.extractAfterdownload", "Extract"));
            chbExtract.setSelected(true);
            //Vorrübergehend noch ohne Funktion
            chbExtract.setEnabled(false);
            chbExtract.setHorizontalTextPosition(SwingConstants.LEFT);
            brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
            brwSaveTo.setEditable(true);
            brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
            brwSaveTo.setText(JDUtilities.getConfiguration().getDefaultDownloadDirectory());

            txtName.setPreferredSize(new Dimension(450, 20));
//            txtPassword.setPreferredSize(new Dimension(450, 20));
            // chbExtract.setPreferredSize(new Dimension(200, 20));
            txtComment.setPreferredSize(new Dimension(450, 20));
            brwSaveTo.setPreferredSize(new Dimension(450, 20));
            txtName.setMinimumSize(new Dimension(250, 20));
//            txtPassword.setMinimumSize(new Dimension(250, 20));
            txtComment.setMinimumSize(new Dimension(250, 20));
            brwSaveTo.setMinimumSize(new Dimension(250, 20));

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
            table = new InternalTable();
            table.getTableHeader().addMouseListener(this);
            InternalTableModel internalTableModel = new InternalTableModel();
            table.addKeyListener(this);
            table.setModel(internalTableModel);

            table.setGridColor(Color.BLUE);
            table.setAutoCreateColumnsFromModel(true);
            table.setModel(internalTableModel);
            table.addMouseListener(this);
            table.setDragEnabled(true);
            table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
            table.getTableHeader().setReorderingAllowed(false);

            setPreferredSize(new Dimension(700, 350));

            TableColumn col = null;
            for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
                col = table.getColumnModel().getColumn(c);
                switch (c) {
                case 0:
                    col.setMinWidth(20);
                    col.setMaxWidth(30);
                    col.setPreferredWidth(30);
                    break;
                case 1:
                    col.setMinWidth(50);
                    col.setMaxWidth(200);
                    col.setPreferredWidth(150);
                    break;
                case 2:
                    col.setMinWidth(50);
                    col.setPreferredWidth(150);
                    break;
                case 3:
                    col.setMinWidth(50);
                    col.setMaxWidth(120);
                    col.setPreferredWidth(100);
                    break;
                case 4:
                    col.setPreferredWidth(150);
                    break;

                }
            }

            int n = 10;
            JPanel north = new JPanel(new BorderLayout(n, n));
            JPanel east = new JPanel(new GridLayout(0, 1, n / 2, n / 2));
            JPanel center = new JPanel(new GridLayout(0, 1, n / 2, n / 2));
            JPanel extractPW = new JPanel(new GridBagLayout());
            north.add(east, BorderLayout.WEST);
            north.add(center, BorderLayout.CENTER);
            // extractPW.add(this.txtPassword);
            JDUtilities.addToGridBag(extractPW, txtPassword, 0, 0, 1, 1, 100, 100, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(extractPW, chbExtract, 1, 0, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
            
        
            east.add(lblName);
            east.add(lblSaveto);
            east.add(lblPassword);
            east.add(lblComment);

            center.add(txtName);
            center.add(brwSaveTo);
            center.add(extractPW);
//            JDUtilities.addToGridBag(center, extractPW, 0, 0, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
            
            center.add(txtComment);

            setLayout(new BorderLayout(n, n));
            setBorder(new EmptyBorder(n, n, n, n));
            add(north, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);

            buildMenu();
        }

        private void buildMenu() {
            mContextTabPopup = new JPopupMenu();

            mContextDelete = new JMenuItem(JDLocale.L("gui.linkgrabber.packagetab.table.context.delete", "Entfernen"));
            mContextDeleteOthers = new JMenuItem(JDLocale.L("gui.linkgrabber.tabs.context.deleteOthers", "Alle anderen Entfernen"));
            mContextAcceptSelection = new JMenuItem(JDLocale.L("gui.linkgrabber.tabs.context.acceptSelection", "Auswahl übernehmen"));
            mContextNewPackage = new JMenuItem(JDLocale.L("gui.linkgrabber.packagetab.table.context.newpackage", "Neues Paket"));

            mContextDelete.addActionListener(this);
            mContextDeleteOthers.addActionListener(this);
            mContextAcceptSelection.addActionListener(this);
            mContextNewPackage.addActionListener(this);

            mContextTabPopup.add(mContextDelete);
            mContextTabPopup.add(mContextDeleteOthers);
            mContextTabPopup.add(mContextAcceptSelection);
            mContextTabPopup.add(mContextNewPackage);
        }

        public String getComment() {
            return txtComment.getText();
        }

        public String getDownloadDirectory() {
            return brwSaveTo.getText();
        }

        public DownloadLink getLinkAt(int id) {
            DownloadLink ret = linkList.get(id);
            return ret;
        }

        public Vector<DownloadLink> getLinkList() {
            Vector<DownloadLink> ret = new Vector<DownloadLink>();
            ret.addAll(linkList);
            return ret;
        }

        public Vector<Vector<DownloadLink>> getMirrors() {
            HashMap<String, Vector<DownloadLink>> mirrormap = new HashMap<String, Vector<DownloadLink>>();
            Vector<Vector<DownloadLink>> mirrorvector = new Vector<Vector<DownloadLink>>();

            for (int i = 0; i < linkList.size(); i++) {
                String key = linkList.get(i).getName().toLowerCase().replaceAll(".html", "").replaceAll(".htm", "");
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

        public String getPackageName() {
            return txtName.getText().trim();
        }
public boolean isExtract(){
    return this.chbExtract.isSelected();
}
        public String getPassword() {
            return txtPassword.getText();
        }

        /**
         * Checks if a packageTab is empty
         * 
         * @return false if there are links within this package, true otherwise
         */
        public boolean isEmpty() {
            return linkList.size() == 0;
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                int[] rows = table.getSelectedRows();
                for (int i = rows.length - 1; i >= 0; i--) {
                    int id = rows[i];

                    totalLinkList.remove(linkList.remove(id));
                }
                refreshTable();
            }
        }

        public void keyTyped(KeyEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (e.getSource() instanceof JTableHeader) {
                JTableHeader header = (JTableHeader) e.getSource();
                int column = header.columnAtPoint(e.getPoint());
                if (sortedOn == column) {
                    sortedOn *= -1;
                } else {
                    sortedOn = column;
                }

                sortOn();
            } else if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                mContextTabPopup.show(table, e.getX(), e.getY());
            }

        }

        public void mouseReleased(MouseEvent e) {
        }

        private void refreshTable() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    table.tableChanged(new TableModelEvent(table.getModel()));
                    onPackageNameChanged(PackageTab.this);

                    __this.setTitle();
                }
            });
        }

        public DownloadLink removeLinkAt(int id) {
            DownloadLink ret = linkList.remove(id);
            refreshTable();
            return ret;
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

        public void setComment(String comm) {
            txtComment.setText(comm);
        }

        public void setDownloadDirectory(String dir) {
            brwSaveTo.setText(dir);
        }

        public void setLinkList(Vector<DownloadLink> finalList) {
            linkList = new Vector<DownloadLink>();
            linkList.addAll(finalList);
            refreshTable();
        }

        public void setPackageName(String name) {
            txtName.setText(JDUtilities.removeEndingPoints(name));
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
                        if (a.getDownloadSize() == b.getDownloadSize()) { return 0; }
                        return sortedOn > 0 ? a.getDownloadSize() > b.getDownloadSize() ? 1 : -1 : a.getDownloadSize() < b.getDownloadSize() ? 1 : -1;
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
    }

    public static final String PROPERTY_AUTOPACKAGE = "PROPERTY_AUTOPACKAGE";

    public static final String PROPERTY_AUTOPACKAGE_LIMIT = "AUTOPACKAGE_LIMIT_V2";

    private static final String PROPERTY_HOSTSELECTIONPACKAGEONLY = "PROPERTY_HOSTSELECTIONPACKAGEONLY";

    private static final String PROPERTY_HOSTSELECTIONREMOVE = "PROPERTY_HOSTSELECTIONREMOVE";

    public static final String PROPERTY_ONLINE_CHECK = "DO_ONLINE_CHECK_V2";

    public static final String PROPERTY_POSITION = "PROPERTY_POSITION";

    protected LinkGrabber __this;

    private static final long serialVersionUID = 4974425479842618402L;

    private JButton accept;

    private JButton acceptAll;

    private int currentTab = -1;

    private Thread gatherer;

    private SubConfiguration guiConfig;

    private JComboBox insertAtPosition;

    protected Logger logger = JDUtilities.getLogger();

    private JCheckBoxMenuItem mAutoPackage;

    private JMenuItem mFreeMirror;

    private JMenuItem[] mHostSelection;

    private JCheckBoxMenuItem mHostSelectionPackageOnly;

    private JCheckBoxMenuItem mHostSelectionRemove;

    private JPopupMenu mContextPopup;

    private JMenuItem mContextDelete;

    private JMenuItem mContextNewPackage;

    private JMenuItem mMerge;

    private JMenuItem mPremiumMirror;

    private JMenuItem mPriorityMirror;

    private JMenuItem mRemoveEmptyPackages;

    private JMenuItem mRemoveOffline;

    private JMenuItem mRemoveOfflineAll;

    private JMenuItem mRemovePackage;

    private JMenuItem mSplitByHost;

    private SimpleGUI parentFrame;

    private JProgressBar progress;

    private JButton sortPackages;

    private JTabbedPane tabbedPane;

    private Vector<PackageTab> tabList;

    private ArrayList<DownloadLink> totalLinkList = new ArrayList<DownloadLink>();

    private Vector<DownloadLink> waitingLinkList;

    /**
     * @param parent
     *            GUI
     * @param linkList
     *            neue links
     */
    public LinkGrabber(SimpleGUI parent, final DownloadLink[] linkList) {
        super();
        __this = this;
        guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.add")));
        parentFrame = parent;
        tabList = new Vector<PackageTab>();
        waitingLinkList = new Vector<DownloadLink>();
        initGUI();

        addLinks(linkList);
        addWindowListener(new LocationListener());
        pack();
        SimpleGUI.restoreWindow(null, this);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mAutoPackage) {
            guiConfig.setProperty(PROPERTY_AUTOPACKAGE, mAutoPackage.isSelected());
            JDUtilities.saveConfig();
            return;
        } else if (e.getSource() == mFreeMirror) {
            Vector<DownloadLink> finalList = new Vector<DownloadLink>();
            Vector<Vector<DownloadLink>> files = getSelectedTab().getMirrors();
            ArrayList<HostPluginWrapper> pfh = JDUtilities.getPluginsForHost();
            for (int a = 0; a < files.size(); a++) {
                Vector<DownloadLink> mirrors = files.get(a);
                if (mirrors.size() == 0) {
                    continue;
                }

                DownloadLink link = null;

                for (int b = 0; b < pfh.size(); b++) {
                    HostPluginWrapper plugin = pfh.get(b);
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
                    if (ch) {
                        break;
                    }
                }

                finalList.add(link);
            }

            getSelectedTab().setLinkList(finalList);
        } else if (e.getSource() == mPremiumMirror) {
            Vector<DownloadLink> finalList = new Vector<DownloadLink>();
            Vector<Vector<DownloadLink>> files = getSelectedTab().getMirrors();
            ArrayList<HostPluginWrapper> pfh = JDUtilities.getPluginsForHost();
            for (int a = 0; a < files.size(); a++) {
                Vector<DownloadLink> mirrors = files.get(a);
                boolean found = false;
                for (int b = 0; b < pfh.size(); b++) {
                    PluginForHost plugin = pfh.get(b).getPlugin();
                    boolean ch = false;
                    for (int c = 0; c < mirrors.size(); c++) {
                        DownloadLink mirror = mirrors.get(c);
                        if (plugin.getMaxSimultanDownloadNum(mirror) > 1 && mirrors.get(c).getHost().equalsIgnoreCase(plugin.getHost())) {
                            finalList.add(mirror);
                            ch = true;
                            found = true;
                            break;
                        }

                    }
                    if (ch) {
                        break;
                    }
                }
                if (!found) {
                    logger.finer("No premium account found for: " + mirrors);
                    DownloadLink link = getPriorityLink(mirrors);
                    finalList.add(link);
                }

            }
            getSelectedTab().setLinkList(finalList);
        } else if (e.getSource() == mPriorityMirror) {
            Vector<DownloadLink> finalList = new Vector<DownloadLink>();
            Vector<Vector<DownloadLink>> files = getSelectedTab().getMirrors();

            for (int a = 0; a < files.size(); a++) {
                Vector<DownloadLink> mirrors = files.get(a);
                if (mirrors.size() == 0) {
                    continue;
                }

                DownloadLink link = getPriorityLink(mirrors);
                finalList.add(link);
            }

            getSelectedTab().setLinkList(finalList);
        } else if (e.getSource() == mRemovePackage) {
            removePackageAt(tabbedPane.getSelectedIndex());
            emptyCheck();
        } else if (e.getSource() == accept) {
            int idx = tabbedPane.getSelectedIndex();
            confirmPackage(idx);
            removePackageAt(idx);
            emptyCheck();
        } else if (e.getSource() == acceptAll) {
            confirmAll();
            setVisible(false);
            dispose();
        } else if (e.getSource() == sortPackages) {
            reprintTabbedPane();
        } else if (e.getSource() == mMerge) {
            PackageTab tab = tabList.get(tabbedPane.getSelectedIndex());
            String name = tab.getPackageName();
            Iterator<PackageTab> iterator = tabList.iterator();
            Vector<DownloadLink> newList = new Vector<DownloadLink>();
            while (iterator.hasNext()) {
                tab = iterator.next();
                newList.addAll(tab.getLinkList());
            }
            while (tabList.size() > 1) {
                removePackageAt(0);
            }
            if (tabList.size() > 0) {
                totalLinkList.clear();
                totalLinkList.addAll(newList);
                tabList.get(0).setLinkList(newList);
                tabList.get(0).setPackageName(name);
                onPackageNameChanged(tabList.get(0));
            }
        } else if (e.getSource() == mSplitByHost) {
            Vector<PackageTab> newTabList = new Vector<PackageTab>();
            PackageTab tab;

            removeEmptyPackages();
            while (!tabList.isEmpty()) {
                String curHost = tabList.get(0).getLinkAt(0).getHost();
                newTabList.add(tab = new PackageTab());
                tab.setPackageName(curHost);

                Vector<DownloadLink> links = new Vector<DownloadLink>();
                for (int i = tabList.size() - 1; i >= 0; --i) {
                    PackageTab curTab = tabList.get(i);
                    for (int j = curTab.getLinkList().size() - 1; j >= 0; --j) {
                        if (curTab.getLinkAt(j).getHost().compareTo(curHost) == 0) {
                            // Link aus dem alten Package entfernen und
                            // Passwort/Kommentar vom Package holen
                            DownloadLink link = curTab.removeLinkAt(j);
                            link.setSourcePluginComment(curTab.getComment());
                            String[] pws = UnrarPassword.getPasswordArray(curTab.getPassword());
                            for (int k = 0; k < pws.length; ++k) {
                                link.addSourcePluginPassword(pws[k]);
                            }
                            links.add(link);
                        }
                    }
                    if (curTab.isEmpty()) {
                        removePackageAt(i);
                    }
                }

                // Vector ins Array kopieren und zum neuen Package hinzufuegen
                DownloadLink[] links2 = new DownloadLink[links.size()];
                links.copyInto(links2);
                tab.addLinks(links2);
            }

            // Neue Tabliste setzen und alle Tabs zum TabbedPane hinzufuegen
            tabList = newTabList;
            for (int i = 0; i < tabList.size(); ++i) {
                tab = tabList.get(i);
                tabbedPane.addTab(tab.getPackageName(), tab);
            }
        } else if (e.getSource() == mRemoveOfflineAll) {
            Iterator<PackageTab> iterator = tabList.iterator();
            while (iterator.hasNext()) {
                removeOfflineLinks(iterator.next());
            }
        } else if (e.getSource() == mRemoveOffline) {
            removeOfflineLinks(tabList.get(tabbedPane.getSelectedIndex()));
        } else if (e.getSource() == mRemoveEmptyPackages) {
            removeEmptyPackages();
            emptyCheck();
        } else if (e.getSource() == mContextDelete) {
            removePackage((PackageTab) tabbedPane.getSelectedComponent());
            emptyCheck();
        } else if (e.getSource() == mContextNewPackage) {
            addTab();
        } else if (e.getSource() == insertAtPosition) {
            guiConfig.setProperty(PROPERTY_POSITION, insertAtPosition.getSelectedIndex());
            JDUtilities.saveConfig();
            return;
        } else if (e.getSource() == mHostSelectionRemove) {
            guiConfig.setProperty(PROPERTY_HOSTSELECTIONREMOVE, mHostSelectionRemove.isSelected());
            JDUtilities.saveConfig();
            return;
        } else if (e.getSource() == mHostSelectionPackageOnly) {
            guiConfig.setProperty(PROPERTY_HOSTSELECTIONPACKAGEONLY, mHostSelectionPackageOnly.isSelected());
            JDUtilities.saveConfig();
            return;
        } else {
            for (int i = 0; i < JDUtilities.getPluginsForHost().size(); ++i) {
                if (e.getSource() == mHostSelection[i]) {
                    if (guiConfig.getBooleanProperty(PROPERTY_HOSTSELECTIONPACKAGEONLY, false)) {
                        confirmSimpleHost(tabbedPane.getSelectedIndex(), mHostSelection[i].getText());
                    } else {
                        String host = mHostSelection[i].getText();
                        for (int j = tabList.size() - 1; j >= 0; --j) {
                            confirmSimpleHost(j, host);
                        }
                    }
                    emptyCheck();
                }
            }
        }
        this.setTitle();
    }

    public synchronized void addLinks(DownloadLink[] linkList) {

        for (DownloadLink element : linkList) {
            if (isDupe(element)) {
                continue;
            }
//            System.out.println(element.getDownloadURL()+" - "+element.getDownloadSize());
            totalLinkList.add(element);
            if (element.isAvailabilityChecked()) {
                attachLinkToPackage(element);
            } else {
                waitingLinkList.add(element);
            }
        }
        if (waitingLinkList.size() > 0) {
            startLinkGatherer();
        }
    }

    public void addLinksToTab(DownloadLink[] linkList, int id) {

        PackageTab tab;
        // logger.info(id + " - " + tabList.size());
        if (id >= tabList.size()) {
            tab = addTab();
        } else {
            tab = tabList.get(id);
        }
        // logger.finer("add " + linkList.length + " links at " + id);

        tab.addLinks(linkList);

        // validate();
        onPackageNameChanged(tab);
    }

    private PackageTab addTab() {
        PackageTab tab = new PackageTab();
        tab.setPackageName(JDLocale.L("gui.linkgrabber.lbl.newpackage", "neues package"));
        tabList.add(tab);
        tabbedPane.addTab(tab.getPackageName(), tab);
        return tab;
    }

    private void attachLinkToPackage(DownloadLink link) {
        String packageName;
        boolean autoPackage = false;
        if (link.getFilePackage() != JDUtilities.getController().getDefaultFilePackage()) {
            packageName = link.getFilePackage().getName();
        } else {
            autoPackage = true;
            packageName = removeExtension(link.getName());
        }
        if (!guiConfig.getBooleanProperty(PROPERTY_AUTOPACKAGE, true)) {
            // logger.finer("No Auto package");
            int lastIndex = tabList.size() - 1;
            if (lastIndex < 0) {
                addTab().setPackageName(packageName);
            }
            lastIndex = tabList.size() - 1;
            addLinksToTab(new DownloadLink[] { link }, lastIndex);
            String newPackageName = JDUtilities.getSimString(tabList.get(lastIndex).getPackageName(), removeExtension(link.getName()));
            tabList.get(lastIndex).setPackageName(newPackageName);
            onPackageNameChanged(tabList.get(lastIndex));

        } else {
            // logger.finer("Auto package");
            int bestSim = 0;
            int bestIndex = -1;
            // logger.info("link: " + link.getName());
            for (int i = 0; i < tabList.size(); i++) {

                int sim = comparePackages(tabList.get(i).getPackageName(), packageName);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestIndex = i;
                }
            }
            // logger.info("Best sym: "+bestSim);
            if (bestSim < guiConfig.getIntegerProperty(PROPERTY_AUTOPACKAGE_LIMIT, 90)) {

                addLinksToTab(new DownloadLink[] { link }, tabList.size());
                tabList.get(tabList.size() - 1).setPackageName(packageName);
            } else {
                // logger.info("Found package " +
                // tabList.get(bestIndex).getpackageName());
                String newPackageName = autoPackage ? JDUtilities.getSimString(tabList.get(bestIndex).getPackageName(), packageName) : packageName;
                tabList.get(bestIndex).setPackageName(newPackageName);
                onPackageNameChanged(tabList.get(bestIndex));
                addLinksToTab(new DownloadLink[] { link }, bestIndex);

            }

        }

    }

    private void buildMenu() {
        // Where the GUI is created:
        JMenuBar menuBar;
        JMenu menu, submenu, subsubmenu;

        // Create the menu bar.
        menuBar = new JMenuBar();

        // Extras Menü
        menu = new JMenu(JDLocale.L("gui.linkgrabber.menu.extras", "Extras"));
        menuBar.add(menu);

        mAutoPackage = new JCheckBoxMenuItem(JDLocale.L("gui.linkgrabber.menu.extras.autopackage", "Auto. Pakete"));
        mAutoPackage.setSelected(guiConfig.getBooleanProperty(PROPERTY_AUTOPACKAGE, true));
        mAutoPackage.addActionListener(this);
        menu.add(mAutoPackage);

        // Edit Menü
        menu = new JMenu(JDLocale.L("gui.linkgrabber.menu.edit", "Bearbeiten"));
        menuBar.add(menu);

        mMerge = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.merge", "Zu einem Paket zusammenfassen"));
        mMerge.addActionListener(this);
        menu.add(mMerge);
        mSplitByHost = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.splitByHost", "Pakete nach Host aufteilen"));
        mSplitByHost.addActionListener(this);
        menu.add(mSplitByHost);
        menu.addSeparator();
        mRemoveOffline = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.removeOffline", "Fehlerhafte Links entfernen (Paket)"));
        mRemoveOffline.addActionListener(this);
        menu.add(mRemoveOffline);
        mRemoveOfflineAll = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.removeOfflineAll", "Fehlerhafte Links entfernen (Alle)"));
        mRemoveOfflineAll.addActionListener(this);
        menu.add(mRemoveOfflineAll);
        menu.addSeparator();
        mRemovePackage = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.removepackage", "Paket verwerfen"));
        mRemovePackage.addActionListener(this);
        menu.add(mRemovePackage);
        mRemoveEmptyPackages = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.edit.removeEmptypackages", "Leere Pakete verwerfen"));
        mRemoveEmptyPackages.addActionListener(this);
        menu.add(mRemoveEmptyPackages);

        // Auswahl Menü
        menu = new JMenu(JDLocale.L("gui.linkgrabber.menu.selection", "Auswahl"));
        menuBar.add(menu);

        submenu = new JMenu(JDLocale.L("gui.linkgrabber.menu.selection.mirror", "Mirrorauswahl"));
        menu.add(submenu);
        mPremiumMirror = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.selection.premium", "Premium"));
        mPremiumMirror.setEnabled(true);
        mPremiumMirror.addActionListener(this);
        submenu.add(mPremiumMirror);
        mFreeMirror = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.selection.free", "Free"));
        mFreeMirror.setEnabled(true);
        mFreeMirror.addActionListener(this);
        submenu.add(mFreeMirror);
        mPriorityMirror = new JMenuItem(JDLocale.L("gui.linkgrabber.menu.selection.priority", "Priority"));
        mPriorityMirror.setEnabled(true);
        mPriorityMirror.addActionListener(this);
        submenu.add(mPriorityMirror);
        menu.addSeparator();
        submenu = new JMenu(JDLocale.L("gui.linkgrabber.menu.hostSelection", "Host Auswahl"));
        menu.add(submenu);
        mHostSelectionPackageOnly = new JCheckBoxMenuItem(JDLocale.L("gui.linkgrabber.menu.hostSelectionPackageOnly", "Nur aktuelles Paket"));
        mHostSelectionPackageOnly.setSelected(guiConfig.getBooleanProperty(PROPERTY_HOSTSELECTIONPACKAGEONLY, false));
        mHostSelectionPackageOnly.addActionListener(this);
        submenu.add(mHostSelectionPackageOnly);
        mHostSelectionRemove = new JCheckBoxMenuItem(JDLocale.L("gui.linkgrabber.menu.hostSelectionRemove", "Restliche Links verwerfen"));
        mHostSelectionRemove.setSelected(guiConfig.getBooleanProperty(PROPERTY_HOSTSELECTIONREMOVE, true));
        mHostSelectionRemove.addActionListener(this);
        submenu.add(mHostSelectionRemove);
        submenu.addSeparator();
        submenu.addSeparator();
        ArrayList<HostPluginWrapper> hosts = JDUtilities.getPluginsForHost();
        mHostSelection = new JMenuItem[hosts.size()];
        subsubmenu = null;
        for (int i = 0; i < hosts.size(); ++i) {
            if (i % 10 == 0) {
                if (subsubmenu != null) {
                    submenu = subsubmenu;
                }
                if (hosts.size() - i > 10) {
                    subsubmenu = new JMenu(JDLocale.L("gui.linkgrabber.menu.hostSelectionMore", "Weitere Hoster"));
                    submenu.add(subsubmenu);
                    submenu.addSeparator();
                }
            }
            mHostSelection[i] = new JMenuItem(hosts.get(i).getHost());
            mHostSelection[i].addActionListener(this);
            submenu.add(mHostSelection[i]);
        }

        setJMenuBar(menuBar);

        // Create Context Menü
        mContextPopup = new JPopupMenu();

        mContextDelete = new JMenuItem(JDLocale.L("gui.linkgrabber.tabs.context.delete", "Entfernen"));
        mContextNewPackage = new JMenuItem(JDLocale.L("gui.linkgrabber.tabs.context.newpackage", "Neues Paket"));

        mContextDelete.addActionListener(this);
        mContextNewPackage.addActionListener(this);

        mContextPopup.add(mContextDelete);
        mContextPopup.add(mContextNewPackage);
    }

    private int comparePackages(String a, String b) {

        int c = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                c++;
            }
        }

        if (Math.min(a.length(), b.length()) == 0) { return 0; }
        // logger.info("comp: " + a + " <<->> " + b + "(" + (c * 100) /
        // (b.length()) + ")");
        return c * 100 / b.length();
    }

    private void confirmAll() {
        if (insertAtPosition.getSelectedItem().equals(JDLocale.L("gui.linkgrabber.pos.top", "Anfang"))) {
            for (int i = tabList.size() - 1; i > -1; --i) {
                confirmPackage(i, null);
            }
        } else {
            for (int i = 0; i < tabList.size(); ++i) {
                confirmPackage(i, null);
            }
        }
    }

    private void confirmPackage(int idx) {
        confirmPackage(idx, null);
    }

    private void confirmPackage(int idx, String host) {
        PackageTab tab = tabList.get(idx);
        Vector<DownloadLink> linkList = tab.getLinkList();
        int files = linkList.size();
        if (files == 0) { return; }

        Color c = new Color((int) (Math.random() * 0xffffff));
        c = c.brighter();
        FilePackage fp = new FilePackage();
        fp.setProperty("color", c);
        fp.setName(tab.getPackageName());
        fp.setComment(tab.getComment());
        fp.setPassword(tab.getPassword());
        fp.setExtractAfterDownload(tab.isExtract());
        UnrarPassword.addToPasswordlist(tab.getPassword());
        UnrarPassword.pushPasswordToTop(tab.getPassword());
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false)) {
            File file = new File(new File(tab.getDownloadDirectory()), tab.getPackageName());
            if (!file.exists()) {
                file.mkdirs();
            }
            if (file.exists()) {
                fp.setDownloadDirectory(file.getAbsolutePath());
            } else {
                fp.setDownloadDirectory(tab.getDownloadDirectory());
            }
        } else {
            fp.setDownloadDirectory(tab.getDownloadDirectory());
        }

        if (host == null) {
            fp.setDownloadLinks(linkList);
            for (int i = 0; i < files; i++) {
                linkList.elementAt(i).setFilePackage(fp);
            }
        } else {
            Vector<DownloadLink> linkListHost = new Vector<DownloadLink>();
            files = 0;
            for (int i = tab.getLinkList().size() - 1; i >= 0; --i) {
                if (linkList.elementAt(i).getHost().compareTo(host) == 0) {
                    DownloadLink link = linkList.remove(i);
                    totalLinkList.remove(link);
                    linkListHost.add(link);
                    link.setFilePackage(fp);
                    ++files;
                }
            }
            if (files == 0) { return; }
            fp.setDownloadLinks(linkListHost);
            tab.setLinkList(linkList);
        }

        parentFrame.fireUIEvent(new UIEvent(this, UIEvent.UI_PACKAGE_GRABBED, fp));

        parentFrame.setDropTargetText(JDLocale.L("gui.dropTarget.downloadsAdded", "Downloads hinzugefügt: ") + files);
    }

    private void confirmSimpleHost(int idx, String host) {
        confirmPackage(idx, host);
        if (guiConfig.getBooleanProperty(PROPERTY_HOSTSELECTIONREMOVE, true) || tabList.get(idx).isEmpty()) {
            removePackageAt(idx);
        }
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        if (currentTab < 0) {
            currentTab = tabbedPane.getSelectedIndex();
        }
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
        int id = tabbedPane.getUI().tabForCoordinate(tabbedPane, (int) dtde.getLocation().getX(), (int) dtde.getLocation().getY());
        if (id >= 0) {
            tabbedPane.setSelectedIndex(id);
        }
    }

    public void drop(DropTargetDropEvent e) {
        int destID = tabbedPane.getUI().tabForCoordinate(tabbedPane, (int) e.getLocation().getX(), (int) e.getLocation().getY());
        PackageTab dest;
        PackageTab source = tabList.get(currentTab);
        if (destID < 0) {
            dest = addTab();
            dest.setPackageName(source.getPackageName() + "(2)");
            dest.setDownloadDirectory(source.getDownloadDirectory());
            dest.setPassword(source.getPassword());
        } else {
            dest = tabList.get(destID);
        }

        if (source == dest) { return; }
        Vector<DownloadLink> move = new Vector<DownloadLink>();
        try {
            Transferable tr = e.getTransferable();
            e.acceptDrop(e.getDropAction());
            if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {

                String data = tr.getTransferData(DataFlavor.stringFlavor).toString();
                if (data != null) {

                    String[] lines = Regex.getLines(data);

                    for (String element : lines) {
                        int id = element.indexOf("\t");
                        if (id <= 0) {
                            continue;
                        }
                        id = Integer.parseInt(element.substring(0, id)) - 1;
                        // DownloadLink link = source.removeLinkAt(id);
                        move.add(source.getLinkAt(id));

                        // logger.info("Move line " + id);

                    }

                }

            }

            // e.dropComplete(true);
        } catch (Exception exc) {
            // e.rejectDrop();
            exc.printStackTrace();
        }
        source.removeLinks(move);
        dest.addLinks(move.toArray(new DownloadLink[] {}));

        onPackageNameChanged(dest);
        onPackageNameChanged(source);
        currentTab = -1;

    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    /**
     * checks the LinkGrabber if there are any packages left If not, the
     * Linkgrabber will be closed and disposed. If there are packages left
     * nothing happens
     */
    private void emptyCheck() {
        if (tabList.size() == 0) {
            setVisible(false);
            dispose();
        }
    }

    protected String getInfoString(DownloadLink link) {
        if (!link.isAvailabilityChecked()) { return link.getLinkStatus().getStatusString().length() == 0 ? JDLocale.L("gui.linkgrabber.lbl.notonlinechecked", "[Verf. nicht überprüft] ") + link.getFileInfomationString() : link.getFileInfomationString() + " " + link.getLinkStatus().getStatusString(); }
        if (link.isAvailable()) {
            return link.getLinkStatus().getStatusString().length() == 0 ? JDLocale.L("gui.linkgrabber.lbl.isonline", "[online] ") + link.getFileInfomationString() : link.getFileInfomationString() + " " + link.getLinkStatus().getStatusString();

        } else {
            return link.getLinkStatus().getStatusString().length() == 0 ? JDLocale.L("gui.linkgrabber.lbl.isoffline", "[offline] ") + link.getFileInfomationString() : link.getFileInfomationString() + " " + link.getLinkStatus().getStatusString();

        }
    }

    private DownloadLink getPriorityLink(Vector<DownloadLink> mirrors) {
        ArrayList<HostPluginWrapper> pfh = JDUtilities.getPluginsForHost();

        for (int b = 0; b < pfh.size(); b++) {
            HostPluginWrapper plugin = pfh.get(b);

            for (int c = 0; c < mirrors.size(); c++) {
                DownloadLink mirror = mirrors.get(c);
                if (mirrors.get(c).getHost().equalsIgnoreCase(plugin.getHost())) { return mirror; }
            }
        }
        logger.severe("Could not find Priorityhoster. This should be impossible. Use first link!");
        return mirrors.get(0);
    }

    protected PackageTab getSelectedTab() {
        return tabList.get(tabbedPane.getSelectedIndex());
    }

    public int getTotalLinkCount() {
        return totalLinkList.size();
    }

    private void initGUI() {
        buildMenu();

        sortPackages = new JButton(JDLocale.L("gui.linkgrabber.btn.sortPackages", "Pakete sortieren"));
        sortPackages.addActionListener(this);
        acceptAll = new JButton(JDLocale.L("gui.linkgrabber.btn.acceptAll", "Alle übernehmen"));
        acceptAll.addActionListener(this);
        accept = new JButton(JDLocale.L("gui.linkgrabber.btn.accept", "Paket übernehmen"));
        accept.addActionListener(this);
        insertAtPosition = new JComboBox(new String[] { JDLocale.L("gui.linkgrabber.pos.top", "Anfang"), JDLocale.L("gui.linkgrabber.pos.bottom", "Ende") });
        insertAtPosition.setSelectedIndex(guiConfig.getIntegerProperty(PROPERTY_POSITION, 1));
        insertAtPosition.addActionListener(this);

        progress = new JProgressBar();
        progress.setBorder(BorderFactory.createEtchedBorder());
        progress.setString(JDLocale.L("gui.linkgrabber.bar.title", "Infosammler"));
        progress.setStringPainted(true);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(this);
        tabbedPane.addMouseListener(this);
        tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
            logger.finer("OS: " + System.getProperty("os.name") + " SET TABS ON TOP");
            tabbedPane.setTabPlacement(JTabbedPane.TOP);
        }
        new DropTarget(tabbedPane, this);

        setName("LINKGRABBER");

        int n = 5;
        JPanel panel = new JPanel(new BorderLayout(n, n));
        panel.setBorder(new EmptyBorder(n, n, n, n));
        setContentPane(panel);

        JPanel inner = new JPanel(new BorderLayout(n, n));

        JPanel south = new JPanel(new BorderLayout(n, n));
        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, n, 0));

        south.add(sortPackages, BorderLayout.WEST);
        bpanel.add(new JLabel(JDLocale.L("gui.linkgrabber.cmb.insertAtPosition", "Einfügen an Position:")));
        bpanel.add(insertAtPosition);
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(5, 20));
        bpanel.add(separator);
        bpanel.add(acceptAll);
        bpanel.add(accept);
        south.add(bpanel, BorderLayout.CENTER);

        panel.add(inner, BorderLayout.CENTER);
        inner.add(tabbedPane, BorderLayout.CENTER);
        inner.add(progress, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(acceptAll);

        setPreferredSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
        pack();

        setVisible(true);
    }

    private boolean isDupe(DownloadLink link) {
        for (DownloadLink l : totalLinkList) {
            if (l.getDownloadURL().equalsIgnoreCase(link.getDownloadURL())) { return true; }
        }
        return false;
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            mContextPopup.show(tabbedPane, e.getX(), e.getY());
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    protected void onPackageNameChanged(PackageTab tab) {
        for (int i = 0; i < tabList.size(); i++) {
            if (tabList.get(i) == tab) {
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

    private void removeEmptyPackages() {
        for (int i = tabList.size() - 1; i >= 0; --i) {
            PackageTab tab = tabList.get(i);
            if (tab.isEmpty()) {
                removePackage(tab);
            }
        }
    }

    private String removeExtension(String a) {
        // logger.finer("file " + a);
        if (a == null) { return a; }
        a = a.replaceAll("\\.part([0-9]+)", "");
        a = a.replaceAll("\\.html", "");
        a = a.replaceAll("\\.htm", "");
        int i = a.lastIndexOf(".");
        // logger.info("FOund . " + i);
        String ret;
        if (i <= 1 || a.length() - i > 5) {
            ret = a.toLowerCase().trim();
        } else {
            // logger.info("Remove ext");
            ret = a.substring(0, i).toLowerCase().trim();
        }

        if (a.equals(ret)) { return ret; }
        return ret;

    }

    private void removeOfflineLinks(PackageTab tab) {
        Vector<DownloadLink> list = tab.getLinkList();
        for (int i = list.size() - 1; i >= 0; --i) {
            if (!list.get(i).isAvailable()) {
                totalLinkList.remove(tab.removeLinkAt(i));
            }
        }
        onPackageNameChanged(tab);
    }

    protected void removePackage(PackageTab tab) {
        removePackageAt(tabList.indexOf(tab));

        totalLinkList.removeAll(tab.getLinkList());
    }

    protected void removePackageAt(int i) {
        PackageTab tab = tabList.remove(i);
        tabbedPane.removeTabAt(i);
        totalLinkList.removeAll(tab.getLinkList());
        this.setTitle();
    }

    private void reprintTabbedPane() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                Collections.sort(tabList, new Comparator<PackageTab>() {
                    public int compare(PackageTab a, PackageTab b) {
                        return a.getPackageName().compareTo(b.getPackageName());
                    }
                });
                synchronized (tabbedPane) {
                    tabbedPane.removeAll();
                    PackageTab tab;
                    for (int i = 0; i < tabList.size(); i++) {
                        tab = tabList.get(i);
                        String title = tab.getPackageName();
                        if (title.length() > 20) {
                            // tabbedPane.setToolTipTextAt(i, title);
                            title = title.substring(0, 6) + "(...)" + title.substring(title.length() - 6);
                        }

                        tabbedPane.add(title + "(" + tab.getLinkList().size() + ")", tab);

                    }
                    __this.setTitle();

                }

            }
        });
    }

    private void setTitle() {
        setTitle(JDLocale.LF("gui.linkgrabber.title", "Linksammler: %s  Link(s) in %s Paket(en)", getTotalLinkCount(), tabList.size()));
    }

    private void startLinkGatherer() {

        progress.setMaximum(waitingLinkList.size());
        progress.setString(null);
        if (gatherer != null && gatherer.isAlive()) { return; }
        gatherer = new Thread() {
            public synchronized void run() {
                DownloadLink link;
                DownloadLink next;
                while (waitingLinkList.size() > 0) {

                    link = waitingLinkList.remove(0);
                    if (!guiConfig.getBooleanProperty(PROPERTY_ONLINE_CHECK, true)) {
                        attachLinkToPackage(link);
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        if (!link.isAvailabilityChecked()) {
                            Iterator<DownloadLink> it = waitingLinkList.iterator();
                            Vector<DownloadLink> links = new Vector<DownloadLink>();
                            Vector<DownloadLink> dlLinks = new Vector<DownloadLink>();
                            links.add(link);
                            dlLinks.add(link);
                            while (it.hasNext()) {
                                next = it.next();
                                if (next.getPlugin().getClass() == link.getPlugin().getClass()) {
                                    dlLinks.add(next);
                                    links.add(next);
                                }
                            }
                            if (links.size() > 1) {
                                boolean[] ret = ((PluginForHost) link.getPlugin()).checkLinks(links.toArray(new DownloadLink[] {}));
                                if (ret != null) {
                                    for (int i = 0; i < links.size(); i++) {
                                        dlLinks.get(i).setAvailable(ret[i]);
                                    }
                                }
                            }
                        }
                        link.isAvailable();
                        // if (link.isAvailable() ) {

                        attachLinkToPackage(link);

                        // }
                    }
                    progress.setValue(waitingLinkList.size());

                }
                progress.setString(JDLocale.L("gui.linkgrabber.bar.title", "Infosammler"));

            }
        };

        gatherer.start();
    }

    public void stateChanged(ChangeEvent e) {
    }

}
