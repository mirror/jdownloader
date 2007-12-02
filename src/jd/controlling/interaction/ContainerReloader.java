package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.event.ControlEvent;
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

    private Vector<String> getNewFiles() {
        JUnrar unrar = new JUnrar(false);
        if(lastAllFiles==null)lastAllFiles= new Vector<String>();
        Vector<DownloadLink> finishedLinks = JDUtilities.getController().getFinishedLinks();
        Vector<String> folders = new Vector<String>();
        for (int i = 0; i < finishedLinks.size(); i++) {
            logger.info("finished File: " + finishedLinks.get(i).getFileOutput());
            File folder = new File(finishedLinks.get(i).getFileOutput()).getParentFile();
            logger.info("Folder: " + folder);
            if (folder.exists()) {
                if (folders.indexOf(folder.getAbsolutePath()) == -1) {
                    logger.info("Add unrardir: " + folder.getAbsolutePath());
                    folders.add(folder.getAbsolutePath());
                }
            }
        }
        folders.add(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
        unrar.setFolders(folders);
        Vector<String> newFiles = new Vector<String>();
        Vector<String> allFiles = new Vector<String>();
        HashMap<File, String> files = unrar.getFiles();
        for (Map.Entry<File, String> entry : files.entrySet()) {

            allFiles.add(entry.getKey().getAbsolutePath());
            if (this.lastAllFiles.indexOf(entry.getKey().getAbsolutePath()) == -1) {
                newFiles.add(entry.getKey().getAbsolutePath());
                logger.info("New file scanned:" + entry.getKey().getAbsolutePath());
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
        return NAME;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {
       
    }

    @Override
    public void resetInteraction() {}
}
