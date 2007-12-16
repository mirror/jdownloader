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

/**
 * Diese Klasse stellt Methoden zur Verfügung um in einen String mitPlatzhaltern
 * werte einzusetzen
 */
public class JDLocale {
    private static String                  LANGUAGES_DIR = "jd/languages/";

    private static Logger                  logger        = JDUtilities.getLogger();

    private static HashMap<String, String> data          = new HashMap<String, String>();

    private static File                    localeFile;                                    ;

    public static Vector<String> getLocaleIDs() {
        File dir = JDUtilities.getResourceFile(LANGUAGES_DIR);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles(new JDFileFilter(null, ".lng", false));
        Vector<String> ret = new Vector<String>();
        for (int i = 0; i < files.length; i++) {
            ret.add(files[i].getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getLocaleString(String key, String def) {
        if (data == null) {
            logger.severe("Use setLocale() first!");
            return key;
        }
        if (def == null) def = key;
        if (data.containsKey(key)) return data.get(key);
        logger.info("Key not found: " + key);
        data.put(key, def);
        
        if(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LANG_EDITMODE))saveData();
        return def;

    }

    public static String L(String key) {
        return getLocaleString(key, null);
    }

    public static String L(String key, String def) {
        return getLocaleString(key, def);
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
        JDUtilities.writeLocalFile(localeFile, str);

    }

    public static void setLocale(String localeID) {
        File file = JDUtilities.getResourceFile(LANGUAGES_DIR + localeID + ".lng");
        localeFile = file;
        if (!file.exists()) {
            logger.severe("Lanuage " + localeID + " not installed");
            return;
        }
        data = new HashMap<String, String>();
        String str = JDUtilities.getLocalFile(file);
        String[] lines = JDUtilities.splitByNewline(str);
        for (int i = 0; i < lines.length; i++) {
            int split = lines[i].indexOf("=");
            if (split <= 0) continue;
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