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

import java.awt.EventQueue;
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

    public final static int REFRESH_ALL_DATA_CHANGED = 1;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED = 0;
    public static final int REFRESH_SPECIFIED_LINKS = 2;
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * Dieser Vector enthält alle Downloadlinks
     */
    protected Vector<DownloadLink> allLinks = new Vector<DownloadLink>();
    /**
     * Der Logger für Meldungen
     */
    protected Logger logger = JDUtilities.getLogger();
    /**
     * contains all packages we have downloadlinks for
     */
    protected Vector<FilePackage> packages = new Vector<FilePackage>();

    protected SimpleGUI parent;

    protected JPopupMenu popup;

    protected DownloadLinksView(SimpleGUI parent, LayoutManager layout) {
        super(layout);
        this.parent = parent;
        JDUtilities.getController().addControlListener(this);
    }

    public void controlEvent(final ControlEvent event) {
        if (event == null) {
            logger.warning("vent==null");
            return;
        }
        // Moved the whole content of this method into a Runnable run by
        // invokeLater(). Ensures that everything inside is executed on the EDT.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED:
                    fireTableChanged(REFRESH_SPECIFIED_LINKS, event.getParameter());
                    // fireTableChanged(REFRESH_ID_COMPLETE_REPAINT);
                    break;

                case ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED:
                    fireTableChanged(REFRESH_ALL_DATA_CHANGED, null);
                    // fireTableChanged(REFRESH_ID_COMPLETE_REPAINT);
                    break;
                case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
                    if (event.getSource().getClass() == JDController.class) {
                        DownloadLinksView.this.setPackages(JDUtilities.getController().getPackages());
                    }
                    fireTableChanged(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);

                }
            }
        });
    }

    abstract public void fireTableChanged(int id, Object object);

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

    public Vector<FilePackage> getPackages() {
        return packages;
    }

    // abstract public void moveSelectedItems(int direction);
    // abstract public void removeSelectedLinks();

    private void setPackages(Vector<FilePackage> packages) {
        this.packages = packages;// new Vector<FilePackage>(packages);
    }

}
