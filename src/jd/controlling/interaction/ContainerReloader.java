package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;

/**
 * versucht alle geladenen Links als container zu öffnen
 * 
 * @author coalado
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
        Vector<String> folders = new Vector<String>();
        for (int i = 0; i < finishedLinks.size(); i++) {
           
            File folder = new File(finishedLinks.get(i).getFileOutput()).getParentFile();
           
            if (folder.exists()) {
                if (folders.indexOf(folder.getAbsolutePath()) == -1) {                
                    folders.add(folder.getAbsolutePath());
                }
            }
        }
        folders.add(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
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

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {
       // ConfigEntry cfg;
        //int type, Property propertyInstance, String propertyName, Object[] list, String label
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        
    }

    @Override
    public void resetInteraction() {}
}
