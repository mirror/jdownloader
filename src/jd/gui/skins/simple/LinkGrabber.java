package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import jd.config.Configuration;
import jd.event.UIEvent;
import jd.gui.skins.simple.components.BrowseFile;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.utils.JDUtilities;

/**
 * Diese Klasse sammelt die Links, bündelt sie zu Paketen und führt einen
 * verfügbarkeitscheck durch
 * 
 * @author coalado
 */
public class LinkGrabber extends JFrame implements ActionListener, DropTargetListener, MouseListener {
    /**
     * 
     */
    private static final long    serialVersionUID = 2313000213508156713L;

    protected Insets             insets           = new Insets(0, 0, 0, 0);

    protected Logger             logger           = Plugin.getLogger();

    private SimpleGUI            parent;

    private Vector<DownloadLink> linkList;

    private JButton              btnOk;

    private JList                list;

    private JButton              btnCancel;

    private JTextField           txfComment;

    private JTextField           txfPassword;

    private JTextField           txtName;

    private BrowseFile           bfSubFolder;

    private JScrollPane          scrollPane;

    private JButton              btnRemove;

    private JPanel               panel;

    private JButton              btnCheck;

    /**
     * @param parent GUI
     * @param linkList neue links
     */
    public LinkGrabber(SimpleGUI parent, final DownloadLink[] linkList) {
        super();
        this.linkList = new Vector<DownloadLink>();
        this.parent = parent;

        setLayout(new BorderLayout());
        this.setTitle("Link Sammler");
        initGrabber();
        pack();
        JFrame frame = parent.getFrame();
        this.setTitle("Linksammler aktiv (D&D + Clipboard)");
        this.setIconImage(JDUtilities.getImage("jd_logo"));
        setLocation((int) (frame.getLocation().getX() + frame.getWidth() / 2 - this.getWidth() / 2), (int) (frame.getLocation().getY() + frame.getHeight() / 2 - this.getHeight() / 2));
        addLinks(linkList);
        pack();
    }

    private void initGrabber() {
        list = new JList();
        list.addMouseListener(this);
        btnOk = new JButton("Übernehmen");
        btnOk.setDefaultCapable(true);
        btnCancel = new JButton("Verwerfen");
        btnRemove = new JButton("Markierte entfernen");
        btnCheck = new JButton("Informationen & Verfügbarkeit prüfen");

        getRootPane().setDefaultButton(btnOk);
        txfComment = new JTextField();

        txtName = new JTextField();
        txfPassword = new JTextField();
        bfSubFolder = new BrowseFile();
        bfSubFolder.setEditable(true);
        bfSubFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        bfSubFolder.setText(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
        scrollPane = new JScrollPane(list);
        btnOk.addActionListener(this);
        btnCancel.addActionListener(this);
        btnRemove.addActionListener(this);
        btnCheck.addActionListener(this);

        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // scrollPane.setPreferredSize(new Dimension(400, 400));
        panel = new JPanel(new GridBagLayout());
        new DropTarget(list, this);
        new DropTarget(this, this);

        JDUtilities.addToGridBag(panel, new JLabel("Hier können alle Links zu einem Paket gesammelt und anschließend Übernommen werden."), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        JDUtilities.addToGridBag(panel, scrollPane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        // JDUtilities.addToGridBag(panel, btnRemove,
        // GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,
        // GridBagConstraints.RELATIVE, 1, 0, 0, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, btnRemoveOffline,
        // GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,
        // GridBagConstraints.RELATIVE, 1, 0, 0, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnCheck, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, new JSeparator(), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JLabel("In folgendem Ordner speichern:"), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, bfSubFolder, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JLabel("Name des Paketes:"), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txtName, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, new JLabel("Archivpasswort:"), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txfPassword, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, new JLabel("Kommentar:"), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txfComment, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JSeparator(), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnOk, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnCancel, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        this.add(panel, BorderLayout.CENTER);
    }

    /**
     * Fügt neue Links zum Grabber hinzu
     * 
     * @param linkList
     */
    public void addLinks(DownloadLink[] linkList) {
        for (int i = 0; i < linkList.length; i++) {
            this.linkList.add(linkList[i]);
            logger.info(linkList[i].getUrlDownloadDecrypted() + " # ");
        }
        sortLinkList();
        if(txtName.getName()==null || txtName.getName().trim().length()==0) checkForSameName();
        fireTableChanged();
    }

    /**
     * Entfernt die aktuell selektierten Links
     */
    public void removeSelectedLinks() {
        int rows[] = list.getSelectedIndices();
        for (int i = rows.length - 1; i >= 0; i--) {
            linkList.remove(rows[i]);
        }
        fireTableChanged();
    }

    public void removeLinks(int[] rows) {
        for (int i = rows.length - 1; i >= 0; i--) {
            linkList.remove(rows[i]);
        }
        fireTableChanged();
    }

    public void removeLinksExcept(int[] rows) {
        for (int i = linkList.size() - 1; i >= 0; i--) {
            boolean isin = false;
            for (int x = 0; x < rows.length; x++) {
                if (rows[x] == i) {
                    isin = true;
                    break;
                }
            }
            if (!isin) linkList.remove(i);
        }
        fireTableChanged();
    }

    /**
     * Sortiert die Linklist
     */

    @SuppressWarnings("unchecked")
    public void sortLinkList() {
        /**
         * Vergleichsfunktion um einen downloadliste alphabetisch zu ordnen
         */
        Collections.sort(linkList);
    }

    /**
     * Überprüft die eingetragenen Links, ob Übereinstimmungen im Namen sind.
     * Das Paket wird dann so genannt.
     */
    private void checkForSameName() {
     
        String tempName;
        String sameName = null;
        Iterator<DownloadLink> iterator = linkList.iterator();
        while (iterator.hasNext()) {
            if (sameName == null) {
                sameName = iterator.next().getName();
            }
            else {
                tempName = iterator.next().getName();
                txtName.setText(JDUtilities.getEqualString(sameName, tempName));
            }
        }
    }

    /**
     * Zeichnet die Linklist neu
     */
    public void fireTableChanged() {
        DefaultListModel tmp = new DefaultListModel();
        list.removeAll();
        for (int i = 0; i < linkList.size(); i++) {
            if (!linkList.elementAt(i).isAvailabilityChecked()) {
                tmp.addElement((i + 1) + ". " + linkList.elementAt(i).getPlugin().getPluginName() + ": " + linkList.elementAt(i).extractFileNameFromURL());
            }
            else {
                if (linkList.elementAt(i).isAvailable()) {
                    tmp.addElement((i + 1) + ". [online] " + linkList.elementAt(i).getPlugin().getPluginName() + ": " + linkList.elementAt(i).getFileInfomationString());
                }
                else {
                    tmp.addElement((i + 1) + ". [OFFLINE] " + linkList.elementAt(i).getPlugin().getPluginName() + ": " + linkList.elementAt(i).getFileInfomationString());
                }
            }
        }
        list.setModel(tmp);
    }

    /**
     * Setzt die Sichtbarkeit des Linkgrabbers
     */
    public void setVisible(boolean bol) {
        super.setVisible(bol);
    }

    public void confirm(int[] indeces) {
        if (indeces != null) {
            this.removeLinksExcept(indeces);
        }
        if (linkList.size() == 0) {
            this.setVisible(false);
            return;
        }
        Color c = new Color((int) (Math.random() * 0xffffff));
        c = c.brighter();
        FilePackage fp = new FilePackage();
        fp.setProperty("color", c);
        fp.setName(txtName.getText().trim());
        fp.setComment(txfComment.getText().trim());
        fp.setPassword(txfPassword.getText().trim());
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false)) {
            File file = new File(new File(bfSubFolder.getText().trim()), txtName.getText().trim());
            if (!file.exists()) {
                file.mkdirs();
            }
            if (file.exists()) {
                fp.setDownloadDirectory(file.getAbsolutePath());
            }
            else {
                fp.setDownloadDirectory(bfSubFolder.getText().trim());
            }
        }
        else {
            fp.setDownloadDirectory(bfSubFolder.getText().trim());
        }
        fp.setDownloadLinks(linkList);

        for (int i = 0; i < linkList.size(); i++) {
            linkList.elementAt(i).setFilePackage(fp);
        }

        parent.fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_GRABBED, linkList));
        this.setVisible(false);
        parent.setDropTargetText("Downloads hinzugefügt: " + linkList.size());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.btnOk) {
            confirm(null);
        }
        if (e.getSource() == this.btnRemove) {
            removeSelectedLinks();
        }
        if (e.getSource() == this.btnCheck) {
            checkLinks();
        }
        if (e.getSource() == this.btnCancel) {
            this.setVisible(false);
        }
    }

    private void removeOfflineFiles() {
        new Thread() {
            public void run() {
                for (int i = linkList.size() - 1; i >= 0 && isVisible() && linkList.size() > i; i--) {
                    DownloadLink link = linkList.elementAt(i);
                    if (!link.isAvailable()) {
                        linkList.remove(i);
                    }
                    fireTableChanged();
                    try {
                        Thread.sleep(20);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }.start();
    }

    private void checkLinks() {
        new Thread() {
            public void run() {
                for (int i = 0; i < linkList.size() && isVisible() && linkList.size() > i; i++) {
                    DownloadLink link = linkList.elementAt(i);
                    link.isAvailable();
                    fireTableChanged();
                    try {
                        Thread.sleep(20);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }.start();
    }

    public void dragEnter(DropTargetDragEvent arg0) {}

    public void dragExit(DropTargetEvent arg0) {}

    public void dragOver(DropTargetDragEvent arg0) {}

    /**
     * Wird aufgerufen sobald etwas gedropt wurde. Die Funktion liest den Inhalt
     * des Drops aus und benachrichtigt die Listener
     */
    public void drop(DropTargetDropEvent e) {
        logger.info("Drag: DROP " + e.getDropAction() + " : " + e.getSourceActions() + " - " + e.getSource() + " - ");
        try {
            Transferable tr = e.getTransferable();
            e.acceptDrop(e.getDropAction());
            if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String files = (String) tr.getTransferData(DataFlavor.stringFlavor);
                logger.info(files);
                parent.fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, files));
            }
            else {
                logger.info("Please only Drag Text");
            }
            // e.dropComplete(true);
        }
        catch (Exception exc) {
            // e.rejectDrop();
            exc.printStackTrace();
        }
        repaint();
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {}

    public Vector<DownloadLink> getLinkList() {
        return linkList;
    }

    private class InternalPopup extends JPopupMenu implements ActionListener {
        /**
         * 
         */
        private static final long serialVersionUID = -6561857482676777562L;

        private JMenuItem         delete;

        private JMenuItem         deleteNotSelected;

        private JMenuItem         info;

        private JPopupMenu        popup;

        private JMenuItem         load;

        private JMenuItem         offline;

        private int[]             indeces;

        private JMenuItem         deleteOffline;

        public InternalPopup(JList invoker, int x, int y) {
            popup = new JPopupMenu();
            indeces = invoker.getSelectedIndices();
            // Create and add a menu item
            delete = new JMenuItem("Entfernen");
            info = new JMenuItem("Informationen laden");
            load = new JMenuItem("Übernehmen");
            offline = new JMenuItem("Verfügbarkeit prüfen (Alle)");
            deleteNotSelected = new JMenuItem("Alle anderen entfernen");
            deleteOffline = new JMenuItem("Alle Defekte Dateien entfernen");
            delete.addActionListener(this);
            deleteNotSelected.addActionListener(this);
            info.addActionListener(this);
            deleteOffline.addActionListener(this);
            load.addActionListener(this);
            offline.addActionListener(this);
            popup.add(delete);
            popup.add(info);
            popup.add(load);
            popup.add(new JSeparator());
            popup.add(deleteNotSelected);
            popup.add(deleteOffline);
            popup.add(offline);
            popup.show(list, x, y);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == deleteOffline) {
                removeOfflineFiles();
            }
            if (e.getSource() == delete) {
                removeLinks(indeces);
            }
            if (e.getSource() == deleteNotSelected) {
                removeLinksExcept(indeces);
            }
            if (e.getSource() == info) {
                new Thread() {
                    public void run() {
                        for (int i = 0; i < indeces.length && isVisible() && linkList.size() > i; i++) {
                            linkList.get(indeces[i]).isAvailable();
                            try {
                                Thread.sleep(50);
                            }
                            catch (InterruptedException e) {
                            }
                            fireTableChanged();
                        }
                    }
                }.start();
            }
            if (e.getSource() == load) {
                confirm(indeces);
            }
            if (e.getSource() == offline) {
                checkLinks();
            }
        }
    }

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        // TODO: isPopupTrigger() funktioniert nicht
        logger.info("Press" + e.isPopupTrigger());
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            Point point = e.getPoint();
            int row = list.locationToIndex(point);
            if (list.getSelectedIndices().length >= 0) {
            }
            else {
                list.setSelectedIndex(row);
            }
            int x = e.getX();
            int y = e.getY();
            new InternalPopup(list, x, y);
        }
    }
}
