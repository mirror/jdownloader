//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Replacer;

/**
 * Diese Klasse kann eine Benutzerdefinierte Info file schreiben.
 * 
 * @author JD-Team
 */
public class InfoFileWriter extends Interaction implements Serializable {
    private static  InfoFileWriter INSTANCE = null;

    private InfoFileWriter() {
        super();
    }

    public static InfoFileWriter getInstance() {
        if (INSTANCE == null) {
            if (JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FILEWRITER_INSTANCE, null) != null) {
                INSTANCE = (InfoFileWriter) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FILEWRITER_INSTANCE, null);
                return INSTANCE;
            }
            INSTANCE = new InfoFileWriter();
        }
        return INSTANCE;

    }
    /**
     * 
     */
    private static final long   serialVersionUID    = 7680205811176541375L;

    /**
     * serialVersionUID
     */
    private static final String NAME                = JDLocale.L("interaction.infoFileWriter.name","Info File Writer");

    private static final String PARAM_INFO_STRING   = "INFO_STRING";

    private static final String PARAM_FILENAME      = "FILENAME";

    private static final String INFO_STRING_DEFAULT = "Passwort: %LAST_FINISHED_PACKAGE.PASSWORD%\r\n%LAST_FINISHED_PACKAGE.FILELIST%\r\nFertig gestellt am %SYSTEM.DATE% um %SYSTEM.TIME% Uhr";

    private static final String FILENAME_DEFAULT    = "%LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY%/%LAST_FINISHED_PACKAGE.PACKAGENAME%.info";



    @Override
    protected boolean doInteraction(Object arg) {
        
       if(! JDUtilities.getConfiguration().getBooleanProperty("INFOFILEWRITER_ENABLED", false)){
           return false;
       }

        
        String content = JDUtilities.getConfiguration().getStringProperty(PARAM_INFO_STRING, INFO_STRING_DEFAULT);
        String filename = JDUtilities.getConfiguration().getStringProperty(PARAM_FILENAME, FILENAME_DEFAULT);

        content = Replacer.insertVariables(content);
  
        filename = Replacer.insertVariables(filename);

        File dest = new File(filename);

        try {
            if (dest.createNewFile() && dest.canWrite()) {
                JDUtilities.writeLocalFile(dest, content);
                return true;
            }
            else {
                logger.severe("Can not write to: " + dest.getAbsolutePath());
                return false;
            }
        }
        catch (Exception e) {

            e.printStackTrace();
            logger.severe("Can not write2 to: " + dest.getAbsolutePath());
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
        String[] keys = new String[Replacer.KEYS.length];
        for (int i = 0; i < Replacer.KEYS.length; i++) {
            keys[i] = "%" + Replacer.KEYS[i][0] + "%" + "   (" + Replacer.KEYS[i][1] + ")";
        }
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), "INFOFILEWRITER_ENABLED", JDLocale.L("interaction.infoFileWriter.disable","Infofilewriter aktivieren")).setDefaultValue(false));
        
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, JDUtilities.getConfiguration(), "VARS", keys, JDLocale.L("interaction.infoFileWriter.variables","Available variables")));
       // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_RADIOFIELD, JDUtilities.getConfiguration(), "VARS", keys, JDLocale.L("interaction.infoFileWriter.variables","Available variables")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), PARAM_FILENAME, JDLocale.L("interaction.infoFileWriter.filename","Filename:")).setDefaultValue(FILENAME_DEFAULT));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), PARAM_INFO_STRING, JDLocale.L("interaction.infoFileWriter.content","Content:")).setDefaultValue(INFO_STRING_DEFAULT));

    }

    @Override
    public void resetInteraction() {}
}
