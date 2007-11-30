package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.util.Vector;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.plugins.DownloadLink;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;

/**
 * Diese Klasse fürs automatische Entpacken
 * 
 * @author DwD
 */
public class Unrar extends Interaction implements Serializable {

    /**
     * 
     */
    private static final long   serialVersionUID         = 2467582501274722811L;

    /**
     * serialVersionUID
     */

    private static final String NAME                     = "Unrar";

    public static final String  PROPERTY_UNRARCOMMAND    = "PROPERTY_UNRARCMD";

    public static final String  PROPERTY_AUTODELETE      = "PROPERTY_AUTODELETE";

    public static final String  PROPERTY_OVERWRITE_FILES = "PROPERTY_OVERWRITE_FILES";

    public static final String  PROPERTY_MAX_FILESIZE    = "PROPERTY_MAX_FILESIZE";

    @Override
    public boolean doInteraction(Object arg) {
        start();
        return true;

    }

    @Override
    public String toString() {
        return "Unrar Programm ausführen";
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }
    private JUnrar getUnrar() {
        JDController controller = JDUtilities.getController();
        DownloadLink dLink = controller.getLastFinishedDownloadLink();
        String password = null;
        if (dLink != null) password = dLink.getFilePackage().getPassword();
   
        JUnrar unrar = new JUnrar();
        if (password != null && !password.matches("[\\s]*")) {
            if (!password.matches("\\{\".*\"\\}$")) unrar.standardPassword = password;
            unrar.addToPasswordlist(password);
        }
        unrar.overwriteFiles = getBooleanProperty(Unrar.PROPERTY_OVERWRITE_FILES, false);
        unrar.autoDelete = getBooleanProperty(Unrar.PROPERTY_AUTODELETE, false);
        unrar.unrar = getStringProperty(Unrar.PROPERTY_UNRARCOMMAND);
        unrar.maxFilesize = getIntegerProperty(Unrar.PROPERTY_MAX_FILESIZE, 2);
        return unrar;
    }
    @Override
    public void run() {
    
        JUnrar unrar=getUnrar();
        Vector<DownloadLink> finishedLinks = JDUtilities.getController().getFinishedLinks();
        Vector<String> folders = new Vector<String>();
        for (int i = 0; i < finishedLinks.size(); i++) {
            logger.info("finished File: "+finishedLinks.get(i).getFileOutput());
            File folder = new File(finishedLinks.get(i).getFileOutput()).getParentFile();
            logger.info("Folder: "+folder);
            if (folder.exists()) {
                if (folders.indexOf(folder.getAbsolutePath()) == -1) {
                    logger.info("Add unrardir: "+folder.getAbsolutePath());
                    folders.add(folder.getAbsolutePath());
                }
            }
        }
        folders.add(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
        logger.info("dirs: "+folders);
        unrar.setFolders(folders);
        Vector<String> newFolderList = unrar.unrar();
//Entpacken bis nichts mehr gefunden wurde
      
        if(newFolderList!=null&&newFolderList.size()>0){
            unrar=getUnrar();
            unrar.setFolders(newFolderList);
            newFolderList = unrar.unrar();
        }
        if(newFolderList!=null&&newFolderList.size()>0){
            unrar=getUnrar();
            unrar.setFolders(newFolderList);
            newFolderList = unrar.unrar();
        }
     
//        unrar = new jdUnrar(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
//        unrar.overwriteFiles = getBooleanProperty(Unrar.PROPERTY_OVERWRITE_FILES, false);
//        unrar.autoDelete = getBooleanProperty(Unrar.PROPERTY_AUTODELETE, false);
//        unrar.unrar = getStringProperty(Unrar.PROPERTY_UNRARCOMMAND);
//        unrar.maxFilesize = getIntegerProperty(Unrar.PROPERTY_MAX_FILESIZE, 2);
//        progress.setStatusText("Unrar directory");
//        unrar.unrar();
 
        this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        Interaction.handleInteraction(Interaction.INTERACTION_AFTER_UNRAR, null);
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void resetInteraction() {}
}
