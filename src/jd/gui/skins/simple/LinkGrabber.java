package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import jd.JDUtilities;
import jd.event.UIEvent;
import jd.gui.skins.simple.components.BrowseFile;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;


/**
 * Diese Klasse sammelt die Links, bündelt sie zu Paketen und führt einen verfügbarkeitscheck durch
 * @author coalado
 *
 */
public class LinkGrabber extends JFrame implements ActionListener, DropTargetListener {

    /**
     * 
     */
    private static final long serialVersionUID = 2313000213508156713L;

    protected Insets             insets = new Insets(0, 0, 0, 0);

    protected Logger             logger = Plugin.getLogger();

    private SimpleGUI            parent;

    private Vector<DownloadLink> linkList;

    private JButton              btnOk;

    private JList                list;

    private JButton              btnCancel;

    private JTextField           txfComment;

    private JTextField           txfPassword;

    private BrowseFile           bfSubFolder;

    private JScrollPane          scrollPane;

    private JButton              btnRemove;

    private JPanel               panel;

    private JButton btnCheck;

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

        setLocation((int) (frame.getLocation().getX() + frame.getWidth() / 2 - this.getWidth() / 2), (int) (frame.getLocation().getY() + frame.getHeight() / 2 - this.getHeight() / 2));

        addLinks(linkList);

        pack();
    }

    private void initGrabber() {
        list = new JList();
        btnOk = new JButton("Übernehmen");
        btnCancel = new JButton("Verwerfen");
        btnRemove = new JButton("Markierte entfernen");
        btnCheck = new JButton("Verfügbarkeit prüfen");
        txfComment = new JTextField();

        txfPassword = new JTextField();
        bfSubFolder = new BrowseFile();
        bfSubFolder.setEditable(true);
        bfSubFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        bfSubFolder.setText(JDUtilities.getConfiguration().getDownloadDirectory());
        scrollPane = new JScrollPane(list);
        btnOk.addActionListener(this);
        btnCancel.addActionListener(this);
        btnRemove.addActionListener(this);
        btnCheck.addActionListener(this);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
//        scrollPane.setPreferredSize(new Dimension(400, 400));
        panel = new JPanel(new GridBagLayout());
        new DropTarget(list, this);
        new DropTarget(this, this);
 
       
        JDUtilities.addToGridBag(panel, new JLabel("Hier können alle Links zu einem Paket gesammelt und anschließend Übernommen werden."), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
      

        JDUtilities.addToGridBag(panel, scrollPane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
       
        JDUtilities.addToGridBag(panel, btnRemove, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnCheck, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        
        JDUtilities.addToGridBag(panel, new JSeparator(), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
        JDUtilities.addToGridBag(panel, new JLabel("In folgendem Ordner speichern:"), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, bfSubFolder, GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     

        JDUtilities.addToGridBag(panel, new JLabel("Archivpasswort:"), GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, txfPassword, GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE,GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
       
        JDUtilities.addToGridBag(panel, new JLabel("Kommentar:"), GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
       
        JDUtilities.addToGridBag(panel, txfComment, GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
       
        JDUtilities.addToGridBag(panel, new JSeparator(), GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
        JDUtilities.addToGridBag(panel, btnOk, GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnCancel, GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        this.add(panel, BorderLayout.CENTER);
    }

    /**
     * Fügt neue Links zum Grabber hinzu
     * @param linkList
     */
    public void addLinks(DownloadLink[] linkList) {
        for (int i = 0; i < linkList.length; i++) {
            this.linkList.add(linkList[i]);
        }
        sortLinkList();
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

    /**
     * Sortiert die Linklist
     */
    
    @SuppressWarnings("unchecked")
    public void sortLinkList() {

        Collections.sort(linkList);
    }

    /**
     * Zeichnet die Linklist neu
     */
    public void fireTableChanged() {
        DefaultListModel tmp = new DefaultListModel();
        list.removeAll();
        for (int i = 0; i < linkList.size(); i++) {
            if(!linkList.elementAt(i).isAvailabilityChecked()){
            tmp.addElement((i + 1) + ". " + linkList.elementAt(i).getPlugin().getPluginName() + ": " + linkList.elementAt(i).getFileName());
            }else{
                if(linkList.elementAt(i).isAvailable()){
                    tmp.addElement((i + 1) + ". [online] " + linkList.elementAt(i).getPlugin().getPluginName() + ": " + linkList.elementAt(i).getFileName());
                    
                }else{
                    tmp.addElement((i + 1) + ". [OFFLINE] " + linkList.elementAt(i).getPlugin().getPluginName() + ": " + linkList.elementAt(i).getFileName());
                    
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

 

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.btnOk) {
            if (linkList.size() == 0) {
                this.setVisible(false);
                return;
            }
            Color c= new Color((int)(Math.random()*0xffffff));
            c=c.brighter();
            FilePackage fp = new FilePackage();
            fp.setProperty("color", c);
            fp.setComment(txfComment.getText().trim());
            fp.setPassword(txfPassword.getText().trim());
            fp.setDownloadDirectory(bfSubFolder.getText().trim());

            for (int i = 0; i < linkList.size(); i++) {
                linkList.elementAt(i).setFilePackage(fp);
            }

    
            parent.fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_GRABBED, linkList));
            this.setVisible(false);

            parent.setDropTargetText("Downloads hinzugefügt: " + linkList.size());

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

    private void checkLinks() {
      
         new Thread(){
             public void run(){
                 
       
                for( int i=0; i<linkList.size();i++){
                    DownloadLink link=linkList.elementAt(i);
                    link.isAvailable();
                    fireTableChanged();
                    
                    try {
                        Thread.sleep(20);
                    }
                    catch (InterruptedException e) {}
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

    public void dropActionChanged(DropTargetDragEvent dtde) {
    // TODO Auto-generated method stub

    }

    public Vector<DownloadLink> getLinkList() {
        return linkList;

    }
}
