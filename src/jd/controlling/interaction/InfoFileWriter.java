package jd.controlling.interaction;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.utils.JDUtilities;
import jd.utils.Replacer;

/**
 * Diese Klasse kann eine Benutzerdefinierte Info file schreiben.
 * 
 * @author coalado
 */
public class InfoFileWriter extends Interaction implements Serializable {
  
    /**
     * 
     */
    private static final long serialVersionUID = 7680205811176541375L;
    /**
     * serialVersionUID
     */
    private static final String NAME              = "Info File Writer";
    private static final String PARAM_INFO_STRING = "INFO_STRING";
    private static final String PARAM_FILENAME = "FILENAME";
    private static final String INFO_STRING_DEFAULT = "Passwort: %LAST_FINISHED_PACKAGE.PASSWORD%\r\n%LAST_FINISHED_PACKAGE.FILELIST%\r\nFertig gestellt am %SYSTEM.DATE% um %SYSTEM.TIME% Uhr";
    private static final String FILENAME_DEFAULT = "%LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY%/%LAST_FINISHED_PACKAGE.PACKAGENAME%.info";


    public InfoFileWriter() {
       
    }
    @Override
    public boolean doInteraction(Object arg) {
        String content=getStringProperty(PARAM_INFO_STRING,INFO_STRING_DEFAULT);
        String filename=getStringProperty(PARAM_FILENAME,FILENAME_DEFAULT);
        
        content=Replacer.insertVariables(content);
logger.info(filename);
        filename=Replacer.insertVariables(filename);
        
        File dest= new File(filename);
        
        try {
            if(dest.createNewFile()&&dest.canWrite()){
                JDUtilities.writeLocalFile(dest, content); 
                return true;
            }else{
                logger.severe("Can not write to: "+dest.getAbsolutePath());
                return false;
            }
        }
        catch (IOException e) {
         
             e.printStackTrace();
            logger.severe("Can not write2 to: "+dest.getAbsolutePath());
            return false;
        }
        
        
 
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
        ConfigEntry cfg;
        String[] keys=new  String[Replacer.KEYS.length];
        for( int i=0; i<Replacer.KEYS.length;i++){
            keys[i]="%"+Replacer.KEYS[i][0]+"%"+"   ("+Replacer.KEYS[i][1]+")";
        }
        
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, this, "VARS", keys, "Available variables"));
        
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,this, PARAM_FILENAME, "Filename:").setDefaultValue(FILENAME_DEFAULT));
        
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA,this, PARAM_INFO_STRING, "Content:").setDefaultValue(INFO_STRING_DEFAULT));
       
        
    }
    @Override
    public void resetInteraction() {}
}
