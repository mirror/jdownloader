package jd.gui.skins.simple;

import java.awt.LayoutManager;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import jd.utils.JDUtilities;

public abstract class DownloadLinksView extends JPanel implements ControlListener {

    protected SimpleGUI parent;
    protected JPopupMenu popup;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED = 0;
    public final static int REFRESH_ONLY_DATA_CHANGED = 1;
    /**
     * Dieser Vector enthält alle Downloadlinks
     */
    protected Vector<DownloadLink> allLinks = new Vector<DownloadLink>();

    /**
     * contains all packages we have downloadlinks for
     */
    protected Vector<FilePackage> packages = new Vector<FilePackage>();

    /**
     * Der Logger für Meldungen
     */
    protected Logger logger = JDUtilities.getLogger();

    protected DownloadLinksView(SimpleGUI parent, LayoutManager layout) {
        super(layout);
        this.parent = parent;
        JDUtilities.getController().addControlListener(this);
    }

    public void controlEvent(ControlEvent event) {

        switch (event.getID()) {
        case ControlEvent.PLUGIN_CONTROL_DATA_CHANGED:
            fireTableChanged(REFRESH_ONLY_DATA_CHANGED);
            // fireTableChanged(REFRESH_ID_COMPLETE_REPAINT);
            break;

        case ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED:
            fireTableChanged(REFRESH_ONLY_DATA_CHANGED);
            // fireTableChanged(REFRESH_ID_COMPLETE_REPAINT);
            break;
        case ControlEvent.CONTROL_LINKLIST_CHANGED:
            if (event.getSource().getClass() == JDController.class) {
                this.setPackages(JDUtilities.getController().getPackages());
            }
            fireTableChanged(REFRESH_DATA_AND_STRUCTURE_CHANGED);

        }
    }

    private void setPackages(Vector<FilePackage> packages) {
        this.packages = packages;// new Vector<FilePackage>(packages);

    }

    // /**
    // * Hier werden Links zu dieser Tabelle hinzugefügt.
    // *
    // * @param links Ein Vector mit Downloadlinks, die alle hinzugefügt werden
    // * sollen
    // */
    // public void addLinks(DownloadLink links[]) {
    // int countAdded = 0;
    // logger.info("SET LINKS: "+links.length);
    // for( DownloadLink link : links){
    // if(null == link) continue;
    //    		
    // FilePackage filePackage = link.getFilePackage();
    //    		
    // if(filePackage!=null ){
    //    			
    // if(! packages.contains(filePackage)){
    // packages.add(filePackage);
    // filePackage.setDownloadLinks(new Vector<DownloadLink>());
    // filePackage.getDownloadLinks().add(link);
    // }else{
    // //TODO signed: perfomre some checks...
    // filePackage.getDownloadLinks().add(link);
    // }
    // }else{
    // logger.severe("DownloadLink has not FilePackage set");
    // }
    // allLinks.add( link);
    // ++countAdded;
    // }
    //    	
    // if( countAdded>0){
    // logger.info("added " + countAdded + " links");
    // }
    //
    // //checkColumnSize();
    // int[] ePackages = getExpandedPackeges();
    // int[] sLinks = this.getSelectedLinks();
    // fireTableChanged(REFRESH_ID_COMPLETE_REPAINT);
    // this.setExpandedPackages(ePackages);
    // this.setSelectedLinks(sLinks);
    //        
    // }

    // abstract protected void checkColumnSize();

    abstract public void fireTableChanged(int id);

    // abstract public void moveSelectedItems(int direction);
    // abstract public void removeSelectedLinks();

    public Vector<FilePackage> getPackages() {
        return packages;
    }

}
