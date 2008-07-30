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


package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;

/**
 * versucht alle geladenen Links als container zu öffnen
 * 
 * @author JD-Team
 * 
 */
public class ContainerReloader extends Interaction implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -9071890385850062424L;

    private static final String NAME = "ContainerLoader";

    private Vector<String>    lastAllFiles;

    public ContainerReloader() {
        // Verwende die unrar routinen um alle files im ausgabeordner zu suchen
        
        getNewFiles();
    }
    public void  initInteraction(){
        getNewFiles();
    }
    private Vector<String> getNewFiles() {
        JUnrar unrar = new JUnrar(false);
        if(lastAllFiles==null)lastAllFiles= new Vector<String>();
        Vector<DownloadLink> finishedLinks ;
        if(JDUtilities.getController()==null){
            finishedLinks=new    Vector<DownloadLink>();
        }else{
       finishedLinks = JDUtilities.getController().getFinishedLinks();
        }
        LinkedList<String> folders = new LinkedList<String>();
        Iterator<DownloadLink> iter = finishedLinks.iterator();
        while (iter.hasNext()) {
        	DownloadLink element = (DownloadLink) iter.next();
			  File folder = new File(element.getFileOutput()).getParentFile();
	           
	            if (folder.exists()) {
	                if (folders.indexOf(folder.getAbsolutePath()) == -1) {                
	                    folders.add(folder.getAbsolutePath());
	                }
	            }
			
		}
        folders.add(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        unrar.setFolders(folders);
        Vector<String> newFiles = new Vector<String>();
        Vector<String> allFiles = new Vector<String>();
        HashMap<File, String> files = unrar.files;
        for (Map.Entry<File, String> entry : files.entrySet()) {

            allFiles.add(entry.getKey().getAbsolutePath());
            if (this.lastAllFiles.indexOf(entry.getKey().getAbsolutePath()) == -1) {
                newFiles.add(entry.getKey().getAbsolutePath());
                logger.info("New file:" + entry.getKey().getAbsolutePath());
            }

        }
        lastAllFiles = allFiles;
        return newFiles;
    }

    public boolean doInteraction(Object arg) {
       Vector<String> newFiles = getNewFiles();
        for( int i=0; i<newFiles.size();i++){
            JDUtilities.getController().loadContainerFile(new File(newFiles.get(i)));
        }
        return true;
    }

    /**
	 * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
	 */
    public void run() {}

    public String toString() {
        return "ContainerLoader: Lädt geladene Container";
    }

    
    public String getInteractionName() {
        return NAME;
    }

    
    public void initConfig() {
       // ConfigEntry cfg;
        // int type, Property propertyInstance, String propertyName, Object[]
		// list, String label
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        
    }

    
    public void resetInteraction() {}
}
