package jd.utils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.JDFileFilter;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;

/**
 * Diese Klasse stellt Methoden zur Verfügung um in einen String mitPlatzhaltern
 * werte einzusetzen
 */
public class JDTheme {
    private static String                  THEME_DIR = "jd/themes/";

    private static Logger                  logger    = JDUtilities.getLogger();

    private static HashMap<String, String> data      = new HashMap<String, String>();

    private static HashMap<String, String> defaultData;

    private static File                    themeFile;                                 ;

    public static Vector<String> getThemeIDs() {
        File dir = JDUtilities.getResourceFile(THEME_DIR);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles(new JDFileFilter(null, ".thm", false));
        Vector<String> ret = new Vector<String>();
        for (int i = 0; i < files.length; i++) {
            ret.add(files[i].getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getIcon(String key, String def) {
        if (data == null) {
            logger.severe("Use setTheme() first!");
            return key;
        }
        
        if (data.containsKey(key)) return JDUtilities.UTF8Decode(data.get(key));
        logger.info("Key not found: " + key+" ("+def+")");
        logger.info(defaultData+"");
        if ((def == null||def.length()==0)&&defaultData.containsKey(key)) {
        def=JDUtilities.UTF8Decode(defaultData.get(key));
        logger.finer("Use default Icon: "+ def) ;
        }
        if (def == null) def = key;
            data.put(key, def);
  

        if (JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_LANG_EDITMODE)) saveData();
        return def;

    }

    public static String I(String key) {
        return getIcon(key, null);
    }

    public static String I(String key, String def) {
        return getIcon(key, def);
    }

    private static void saveData() {
        Iterator<Entry<String, String>> iterator;
        if (data == null) return;
        iterator = data.entrySet().iterator();
        // stellt die Wartezeiten zurück
        Entry<String, String> i;
        String str = "";
        Vector<String> ret = new Vector<String>();
        while (iterator.hasNext()) {
            i = iterator.next();
            ret.add(i.getKey() + " = " + i.getValue());
        }
        Collections.sort(ret);
        for (int x = 0; x < ret.size(); x++)
            str += ret.get(x) + System.getProperty("line.separator");
        JDUtilities.writeLocalFile(themeFile, str);

    }

    public static void setTheme(String themeID) {
        File file = JDUtilities.getResourceFile(THEME_DIR + themeID + ".thm");
        themeFile = file;
        if (!file.exists()) {
            logger.severe("Theme " + themeID + " not installed");
            return;
        }
        data = new HashMap<String, String>();
        String str = JDUtilities.getLocalFile(file);
        String[] lines = JDUtilities.splitByNewline(str);
        for (int i = 0; i < lines.length; i++) {
            int split = lines[i].indexOf("=");
            if (split <= 0 || lines[i].startsWith("#")) continue;
            String key = lines[i].substring(0, split).trim();
            String value = lines[i].substring(split + 1).trim();
            if (data.containsKey(key)) {
                logger.severe("Dupe found: " + key);
            }
            else {
                data.put(key, value);
            }

        }
        if (themeID.equals("default")) {
            defaultData = data;
        }
        if (defaultData == null) {
            defaultData = new HashMap<String, String>();
            file = JDUtilities.getResourceFile(THEME_DIR + "default.thm");

            if (!file.exists()) {
                logger.severe("Theme default not installed");
                return;
            }
            data = new HashMap<String, String>();
            str = JDUtilities.getLocalFile(file);
            lines = JDUtilities.splitByNewline(str);
            for (int i = 0; i < lines.length; i++) {
                int split = lines[i].indexOf("=");
                if (split <= 0 || lines[i].startsWith("#")) continue;
                String key = lines[i].substring(0, split).trim();
                String value = lines[i].substring(split + 1).trim();
                if (data.containsKey(key)) {
                    logger.severe("Dupe found: " + key);
                }
                else {
                    data.put(key, value);
                }

            }

        }

    }

}