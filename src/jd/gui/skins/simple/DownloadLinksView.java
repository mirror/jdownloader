package jd.gui.skins.simple;

import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
import jd.utils.JDUtilities;

public abstract class DownloadLinksView extends JPanel implements PluginListener, ControlListener{
	
    protected SimpleGUI            parent;
    protected JPopupMenu           popup;
    
    /**
     * Dieser Vector enthält alle Downloadlinks
     */
    protected Vector<DownloadLink> allLinks            = new Vector<DownloadLink>();
    
    /**
     * contains all packages we have downloadlinks for
     */
    protected Vector<FilePackage> packages = new Vector<FilePackage>();

    /**
     * Der Logger für Meldungen
     */
    protected Logger               logger              = JDUtilities.getLogger();


	
	protected DownloadLinksView(SimpleGUI parent, LayoutManager layout){
		super(layout);
        this.parent = parent;
	}
	
    public void pluginEvent(PluginEvent event) {
        switch (event.getID()) {
            case PluginEvent.PLUGIN_DATA_CHANGED:
                fireTableChanged();
                break;
        }
    }
    
    
    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED:
                fireTableChanged();
                break;
        }
    }
    
    
    
    public void setDownloadLinks(DownloadLink links[]) {
    	
    	if( !allLinks.isEmpty()){
    		logger.warning("set new DownloadLinks, although there are already "+ allLinks.size()+" links available, those will be flushed");
    	}
    	
        allLinks.clear();
        packages.clear();
        addLinks(links);
    }
    
    
    public void doReconnectMinimizationSort(){
    	logger.info("start sorting");
    	if(allLinks.isEmpty()){
    		return;
    	}
    	
		HashMap<String, List<DownloadLink>> sortedDownloadLinks = new HashMap<String, List<DownloadLink>>();
    	
    	for(DownloadLink link : allLinks){
    		String pluginId = link.getPlugin().getPluginID();
    		
    		List<DownloadLink> linksForHoster = sortedDownloadLinks.get(pluginId);
    		if( null == linksForHoster){
    			logger.info("insert List for: "+ pluginId);
    			linksForHoster = new LinkedList<DownloadLink>();
    			sortedDownloadLinks.put(pluginId, linksForHoster);
    		}
    		
    		linksForHoster.add(link);
    	}
    	
    	logger.info("all sorting done");
    	
    	//now create a download list, to minimize the reconnects
    	List<String>pluginIds =  new ArrayList<String>(sortedDownloadLinks.keySet());    	

    	if( 1 >= pluginIds.size()){
    		//all Downloadlinks are from the some Hoster, no optimization possible
    		logger.info("only on downloadlinks from one hoster - no optimisation possible");
    		return;
    	}
    	
    	int insertPosition = 0;
    	DownloadLink sortedLinks[] = new DownloadLink[allLinks.size()];
    	
    	do{
    		for( int i=pluginIds.size()-1; i>=0; --i ){
    			String pluginId = pluginIds.get(i);
    			
    			List<DownloadLink> current = sortedDownloadLinks.get(pluginId);
    			sortedLinks[insertPosition++] = current.remove(0);

    			if(current.isEmpty()){
    				logger.info("no links left for Plugin "+ pluginId);
    				pluginIds.remove(i);
    				sortedDownloadLinks.remove(pluginId);
    			}
    		}
    	}while(!sortedDownloadLinks.isEmpty());
    	
    	setDownloadLinks(sortedLinks);
    }
    
    /**
     * Hier werden Links zu dieser Tabelle hinzugefügt.
     * 
     * @param links Ein Vector mit Downloadlinks, die alle hinzugefügt werden
     *            sollen
     */
    public void addLinks(DownloadLink links[]) {
    	int countAdded = 0;
    	for( DownloadLink link : links){
    		if(null == link) continue;
    		
    		FilePackage filePackage = link.getFilePackage();
    		
    		if( null != filePackage ){
    			int index  = packages.indexOf(filePackage);
    			if( -1 == index){
    				packages.add(filePackage);    				
    			}else{
    				//TODO signed: perfomre some checks...
    			}
    		}else{
    			logger.severe("DownloadLink has not FilePackage set");
    		}
    		allLinks.add( link);
    		++countAdded;
    	}
    	
    	if( 0 != countAdded){
    		logger.info("added " + countAdded + " links");
    	}

        checkColumnSize();
        fireTableChanged();
    }
    
    abstract protected void checkColumnSize();
    
    abstract public void fireTableChanged();
    
    abstract public void moveSelectedItems(int direction);
    abstract public void removeSelectedLinks();
    abstract public Vector<DownloadLink> getLinks();
    

}
