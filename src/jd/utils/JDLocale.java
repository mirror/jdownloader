package jd.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.JDFileFilter;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;

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
        if (data.containsKey(key)) return JDUtilities.UTF8Decode(data.get(key)).replace("\\r", "\r").replace("\\n", "\n");
        logger.info("Key not found: " + key);
        data.put(key, JDUtilities.UTF8Encode(def));
        postMissingKey(key, JDUtilities.UTF8Encode(def));
        if (JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_LANG_EDITMODE)) saveData();

        return def;

    }

    private static Vector<String[]> send = new Vector<String[]>();
    private static Vector<String> sent = new Vector<String>();
    private static Thread           sender;

    private static String           lID;

    private static void postMissingKey(String key, String encode) {
        if(sent.indexOf(key)>=0)return;
        send.add(new String[] { JDUtilities.UTF8Decode(key), JDUtilities.UTF8Decode(encode )});
        sent.add(key);
        if (sender == null || !sender.isAlive()) {
            sender = new Thread() {
                public void run() {
                    String[] entry;
                    while (send.size() > 0) {
                        entry = send.remove(0);
                        String user=JDUtilities.getGUI().askForLocalisation(entry[0], entry[1]);
                        if(user!=null)entry[1]=user;
                        try {
                            Plugin.getRequest(new URL("http://web146.donau.serverway.de/jdownloader/update/lang.php?lang=" + lID + "&key=" + JDUtilities.urlEncode(entry[0]) + "&default=" + JDUtilities.urlEncode(entry[1])));
                            
                        }
                        catch (MalformedURLException e) {
                        }
                        catch (IOException e) {
                        }
                    }
                }
            };

            sender.start();
        }
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
            ret.add(i.getKey() + " = " + i.getValue().replace("\r", "\\r").replace("\n", "\\n"));
        }
        Collections.sort(ret);
        for (int x = 0; x < ret.size(); x++)
            str += ret.get(x) + System.getProperty("line.separator");
        JDUtilities.writeLocalFile(localeFile, str);

    }

    public static void setLocale(String localeID) {
        File file = JDUtilities.getResourceFile(LANGUAGES_DIR + localeID + ".lng");
        localeFile = file;
        lID = localeID;
        if (!file.exists()) {
            logger.severe("Lanuage " + localeID + " not installed");
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
                data.put(key, value);
            }
            else {
                data.put(key, value);
            }

        }
        if (JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_LANG_EDITMODE)) saveData();

    }

    public static String getLocale() {
       return lID;
    }

}