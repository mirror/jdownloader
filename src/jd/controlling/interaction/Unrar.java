package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.unrar.jdUnrar;
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
    private static final long serialVersionUID = 2467582501274722811L;
    /**
     * serialVersionUID
     */

    private static final String NAME = "Unrar";
    public static final String PROPERTY_UNRARCOMMAND = "PROPERTY_UNRARCOMMAND";
    public static final String PROPERTY_AUTODELETE = "PROPERTY_AUTODELETE";
    public static final String PROPERTY_MODE = "PROPERTY_MODE";
    public static final String PROPERTY_OVERWRITE_FILES = "PROPERTY_OVERWRITE_FILES";
    public static final String PROPERTY_MAX_FILESIZE = "PROPERTY_MAX_FILESIZE";
    @Override
    public boolean doInteraction(Object arg) {
        String mo = getStringProperty(PROPERTY_MODE);
        int mode = 1;
        if (mo != null) {
            if (mo.matches("Alle Dateien im Downloadordner entpacken"))
                mode = 2;
            else if (mo.matches("Die Dateien im Ordner des letzten Packets entpacken (mit PacketPasswort)"))
                mode = 3;
        }

        JDController controller = JDUtilities.getController();
        DownloadLink dLink = controller.getLastFinishedDownloadLink();
        String password = null;
        if (dLink != null)
            password = dLink.getFilePackage().getPassword();


        if (dLink!=null && (mode == 1 || mode == 3)) {


            jdUnrar unrar = new jdUnrar(new File(dLink.getFileOutput()).getParentFile());
            if (!password.isEmpty())
            {
                unrar.standardPassword=password;
                unrar.addToPasswordlist(password);
            }
                
            unrar.overwriteFiles=getBooleanProperty(Unrar.PROPERTY_OVERWRITE_FILES, false);
            unrar.autoDelete=getBooleanProperty(Unrar.PROPERTY_AUTODELETE, false);
            unrar.unrar=getStringProperty(Unrar.PROPERTY_UNRARCOMMAND);
            unrar.maxFilesize=getIntegerProperty(Unrar.PROPERTY_MAX_FILESIZE, 2);
            unrar.unrar();
        }
        if (mode == 1 || mode == 2) {
            jdUnrar unrar = new jdUnrar(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
            if(!password.isEmpty() && mode == 2)
            unrar.addToPasswordlist(password);
            unrar.overwriteFiles=getBooleanProperty(Unrar.PROPERTY_OVERWRITE_FILES, false);
            unrar.autoDelete=getBooleanProperty(Unrar.PROPERTY_AUTODELETE, false);
            unrar.unrar=getStringProperty(Unrar.PROPERTY_UNRARCOMMAND);
            unrar.maxFilesize=getIntegerProperty(Unrar.PROPERTY_MAX_FILESIZE, 2);
            unrar.unrar();
        }
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
    @Override
    public void run() {
        // Nichts zu tun. Interaction braucht keinen Thread
    }
    @Override
    public void initConfig() {

    }
    @Override
    public void resetInteraction() {
    }
}
